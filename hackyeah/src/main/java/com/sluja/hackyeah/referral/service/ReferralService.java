package com.sluja.hackyeah.referral.service;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.repository.HospitalRepository;
import com.sluja.hackyeah.matching.HospitalEligibility;
import com.sluja.hackyeah.matching.HospitalScore;
import com.sluja.hackyeah.matching.MatchResult;
import com.sluja.hackyeah.matching.TravelTimeProvider;
import com.sluja.hackyeah.referral.dto.CreateReferralRequest;
import com.sluja.hackyeah.referral.dto.DispatchedRequest;
import com.sluja.hackyeah.referral.dto.ExcludedHospital;
import com.sluja.hackyeah.referral.dto.HospitalContact;
import com.sluja.hackyeah.referral.dto.HospitalCandidate;
import com.sluja.hackyeah.referral.dto.ReferralCreatedResponse;
import com.sluja.hackyeah.referral.dto.ReferralDetailResponse;
import com.sluja.hackyeah.referral.dto.RequestView;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralCandidate;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.referral.repository.ReferralCandidateRepository;
import com.sluja.hackyeah.referral.repository.ReferralRepository;
import com.sluja.hackyeah.referral.repository.ReferralRequestRepository;
import com.sluja.hackyeah.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final ReferralRequestRepository requestRepository;
    private final ReferralCandidateRepository candidateRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalMatchingService matchingService;
    private final WaveService waveService;
    private final TravelTimeProvider travelTimeProvider;

    public ReferralService(ReferralRepository referralRepository,
                           ReferralRequestRepository requestRepository,
                           ReferralCandidateRepository candidateRepository,
                           HospitalRepository hospitalRepository,
                           HospitalMatchingService matchingService,
                           WaveService waveService,
                           TravelTimeProvider travelTimeProvider) {
        this.referralRepository = referralRepository;
        this.requestRepository = requestRepository;
        this.candidateRepository = candidateRepository;
        this.hospitalRepository = hospitalRepository;
        this.matchingService = matchingService;
        this.waveService = waveService;
        this.travelTimeProvider = travelTimeProvider;
    }

    /**
     * Files the referral, freezes the ranking it produced and sends wave 1 straight away. The
     * referral is saved even when nothing is eligible - it is escalated instead of dispatched, so
     * the coordinator picks it up with the full list of reasons.
     *
     * @param limit how many ranked candidates to return for display; wave size is configured
     *              separately via {@code app.waves.size}
     */
    @Transactional
    public ReferralCreatedResponse create(CreateReferralRequest request, int limit) {
        if (!hospitalRepository.existsById(request.originHospitalId())) {
            throw new IllegalArgumentException("Unknown originHospitalId: " + request.originHospitalId());
        }

        LocalDateTime now = LocalDateTime.now();
        Referral referral = referralRepository.save(Referral.builder()
                .targetSpecialty(request.targetSpecialty())
                .requiredProcedures(request.requiredProcedures() == null ? Set.of() : request.requiredProcedures())
                .urgency(request.urgency())
                .patientState(request.patientState())
                .requiresIsolation(request.requiresIsolation())
                .note(request.note())
                .status(Referral.ReferralStatus.OPEN)
                .originHospitalId(request.originHospitalId())
                .build());

        MatchResult match = matchingService.match(referral, now);
        freezeRanking(referral, match);

        List<ReferralRequest> dispatched = waveService.dispatchNextWave(referral, now);
        if (dispatched.isEmpty()) {
            // Nobody to ask - straight to the coordinator rather than a referral waiting on no one.
            referralRepository.tryEscalate(referral.getId());
        }

        Map<Long, Hospital> hospitals = hospitalsById();
        Referral current = requireReferral(referral.getId());

        return new ReferralCreatedResponse(
                current.getId(),
                current.getStatus(),
                current.getCreatedAt(),
                dispatched.isEmpty() ? 0 : dispatched.get(0).getWave(),
                dispatched.stream().map(sent -> toDispatched(sent, hospitals)).toList(),
                rankedCandidates(current.getId(), hospitals, current.getOriginHospitalId()).stream()
                        .limit(limit)
                        .toList(),
                excludedCandidates(current.getId()));
    }

    @Transactional(readOnly = true)
    public ReferralDetailResponse findById(Long referralId) {
        Referral referral = requireReferral(referralId);
        Map<Long, Hospital> hospitals = hospitalsById();

        List<RequestView> requests = requestRepository.findByReferralIdOrderByWaveAscIdAsc(referralId)
                .stream()
                .map(request -> toRequestView(request, hospitals))
                .toList();

        List<HospitalCandidate> candidates =
                rankedCandidates(referralId, hospitals, referral.getOriginHospitalId());

        return new ReferralDetailResponse(
                referral.getId(),
                referral.getStatus(),
                referral.getCreatedAt(),
                referral.getAcceptedHospitalId(),
                requestRepository.findHighestWave(referralId),
                candidates,
                excludedCandidates(referralId),
                requests,
                contactsFor(referral, candidates, hospitals));
    }

    /**
     * On-call numbers are withheld while the referral is still OPEN. Handing the doctor every
     * candidate's number up front would invite them to phone down the ranking, which is exactly the
     * sequential calling the waves replace.
     */
    private List<HospitalContact> contactsFor(Referral referral,
                                              List<HospitalCandidate> candidates,
                                              Map<Long, Hospital> hospitals) {
        return switch (referral.getStatus()) {
            case OPEN -> List.of();
            // Doctor to doctor, both directions.
            case ACCEPTED -> Stream.of(referral.getAcceptedHospitalId(), referral.getOriginHospitalId())
                    .map(hospitals::get)
                    .filter(Objects::nonNull)
                    .map(ReferralService::toContact)
                    .toList();
            // Phoning round is now the intended fallback, so the ranking comes with numbers.
            case ESCALATED -> candidates.stream()
                    .map(candidate -> hospitals.get(candidate.hospitalId()))
                    .filter(Objects::nonNull)
                    .map(ReferralService::toContact)
                    .toList();
        };
    }

    private static HospitalContact toContact(Hospital hospital) {
        return new HospitalContact(hospital.getId(), hospital.getName(), hospital.getDutyPhone());
    }

    /**
     * Persists the ranking exactly as the matcher produced it, so later waves and the doctor's view
     * read the same order even as occupancy moves underneath.
     */
    private void freezeRanking(Referral referral, MatchResult match) {
        List<ReferralCandidate> candidates = new ArrayList<>();

        int rank = 1;
        for (HospitalScore score : match.ranked()) {
            candidates.add(ReferralCandidate.builder()
                    .referral(referral)
                    .hospitalId(score.hospital().getId())
                    .hospitalName(score.hospital().getName())
                    .rank(rank++)
                    .totalScore(score.totalScore())
                    .eligible(true)
                    .criterionScores(new HashMap<>(score.criterionScores()))
                    .violations(List.of())
                    .build());
        }
        for (HospitalEligibility excluded : match.excluded()) {
            candidates.add(ReferralCandidate.builder()
                    .referral(referral)
                    .hospitalId(excluded.hospital().getId())
                    .hospitalName(excluded.hospital().getName())
                    .eligible(false)
                    .criterionScores(Map.of())
                    .violations(List.copyOf(excluded.violations()))
                    .build());
        }

        candidateRepository.saveAll(candidates);
    }

    private List<HospitalCandidate> rankedCandidates(Long referralId,
                                                     Map<Long, Hospital> hospitals,
                                                     Long originHospitalId) {
        return candidateRepository.findByReferralIdAndEligibleTrueOrderByRankAsc(referralId).stream()
                .map(candidate -> {
                    Hospital hospital = hospitals.get(candidate.getHospitalId());
                    return new HospitalCandidate(
                            candidate.getRank(),
                            candidate.getHospitalId(),
                            candidate.getHospitalName(),
                            candidate.getTotalScore() == null ? 0.0 : candidate.getTotalScore(),
                            candidate.getCriterionScores(),
                            hospital == null ? null : hospital.getAvailableBeds(),
                            travelTimeProvider.travelMinutes(originHospitalId, candidate.getHospitalId())
                                    .orElse(null));
                })
                .toList();
    }

    private List<ExcludedHospital> excludedCandidates(Long referralId) {
        return candidateRepository.findByReferralIdOrderByRankAsc(referralId).stream()
                .filter(candidate -> !candidate.isEligible())
                .sorted(Comparator.comparing(ReferralCandidate::getHospitalId))
                .map(candidate -> new ExcludedHospital(
                        candidate.getHospitalId(),
                        candidate.getHospitalName(),
                        candidate.getViolations()))
                .toList();
    }

    private DispatchedRequest toDispatched(ReferralRequest request, Map<Long, Hospital> hospitals) {
        return new DispatchedRequest(
                request.getId(),
                request.getHospitalId(),
                nameOf(request.getHospitalId(), hospitals),
                request.getWave(),
                request.getDeadline());
    }

    private RequestView toRequestView(ReferralRequest request, Map<Long, Hospital> hospitals) {
        return new RequestView(
                request.getId(),
                request.getHospitalId(),
                nameOf(request.getHospitalId(), hospitals),
                request.getWave(),
                request.getStatus(),
                request.getDeclineReason(),
                request.getSentAt(),
                request.getDeadline());
    }

    private static String nameOf(Long hospitalId, Map<Long, Hospital> hospitals) {
        Hospital hospital = hospitals.get(hospitalId);
        return hospital == null ? null : hospital.getName();
    }

    private Map<Long, Hospital> hospitalsById() {
        return hospitalRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Hospital::getId, Function.identity()));
    }

    private Referral requireReferral(Long referralId) {
        return referralRepository.findById(referralId)
                .orElseThrow(() -> new NotFoundException("No referral " + referralId));
    }
}
