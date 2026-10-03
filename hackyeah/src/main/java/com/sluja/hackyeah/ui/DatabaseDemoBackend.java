package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.dashboard.OccupancySimulator;
import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.hospital.repository.HospitalFlagRepository;
import com.sluja.hackyeah.hospital.repository.HospitalRepository;
import com.sluja.hackyeah.matching.TravelTimeProvider;
import com.sluja.hackyeah.referral.dto.CreateReferralRequest;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralCandidate;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.referral.repository.ReferralCandidateRepository;
import com.sluja.hackyeah.referral.repository.ReferralRepository;
import com.sluja.hackyeah.referral.repository.ReferralRequestRepository;
import com.sluja.hackyeah.referral.service.ReferralService;
import com.sluja.hackyeah.referral.service.RequestResponseService;
import com.sluja.hackyeah.ui.view.CandidateView;
import com.sluja.hackyeah.ui.view.DecisionResult;
import com.sluja.hackyeah.ui.view.ExcludedHospitalView;
import com.sluja.hackyeah.ui.view.ExclusionReason;
import com.sluja.hackyeah.ui.view.FlagView;
import com.sluja.hackyeah.ui.view.HospitalView;
import com.sluja.hackyeah.ui.view.InboxRequestView;
import com.sluja.hackyeah.ui.view.NewReferral;
import com.sluja.hackyeah.ui.view.ReferralView;
import com.sluja.hackyeah.web.ConflictException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The views on top of the real referral flow: matching, frozen ranking, wave dispatch and the
 * first-accept-wins race all run in the referral services; this class only translates between
 * them and what the Thymeleaf pages render.
 */
@Service
public class DatabaseDemoBackend implements DemoBackend {

    private static final String ACCEPTANCE_CRITERION = "ACCEPTANCE_PROBABILITY";
    /** Only the ranking is shown here, so the created response does not need to carry candidates. */
    private static final int CREATE_RESPONSE_LIMIT = 1;

    private final HospitalRepository hospitalRepository;
    private final HospitalFlagRepository flagRepository;
    private final ReferralRepository referralRepository;
    private final ReferralRequestRepository requestRepository;
    private final ReferralCandidateRepository candidateRepository;
    private final ReferralService referralService;
    private final RequestResponseService requestResponseService;
    private final TravelTimeProvider travelTimeProvider;
    private final OccupancySimulator occupancySimulator;
    private final DemoProperties properties;

    public DatabaseDemoBackend(HospitalRepository hospitalRepository,
                               HospitalFlagRepository flagRepository,
                               ReferralRepository referralRepository,
                               ReferralRequestRepository requestRepository,
                               ReferralCandidateRepository candidateRepository,
                               ReferralService referralService,
                               RequestResponseService requestResponseService,
                               TravelTimeProvider travelTimeProvider,
                               OccupancySimulator occupancySimulator,
                               DemoProperties properties) {
        this.hospitalRepository = hospitalRepository;
        this.flagRepository = flagRepository;
        this.referralRepository = referralRepository;
        this.requestRepository = requestRepository;
        this.candidateRepository = candidateRepository;
        this.referralService = referralService;
        this.requestResponseService = requestResponseService;
        this.travelTimeProvider = travelTimeProvider;
        this.occupancySimulator = occupancySimulator;
        this.properties = properties;
    }

    /** The scenario starts with its flags already set, e.g. the busy cath lab of the overloaded hospital. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void applyScenarioOnStartup() {
        applyScenarioFlags(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalView> hospitals() {
        LocalDateTime now = LocalDateTime.now();
        return hospitalRepository.findAll(Sort.by("id")).stream()
                .map(hospital -> toView(hospital, now))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HospitalView> hospital(Long hospitalId) {
        return hospitalRepository.findById(hospitalId).map(hospital -> toView(hospital, LocalDateTime.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalView originHospital() {
        return hospital(properties.originHospitalId())
                .orElseThrow(() -> new IllegalStateException("Origin hospital " + properties.originHospitalId()
                        + " does not exist - check demo.origin-hospital-id against the seed"));
    }

    @Override
    public Long createReferral(NewReferral referral) {
        return referralService.create(new CreateReferralRequest(
                        referral.targetSpecialty(),
                        referral.requiredProcedures(),
                        referral.urgency(),
                        referral.patientState(),
                        referral.requiresIsolation(),
                        referral.note(),
                        properties.originHospitalId()),
                CREATE_RESPONSE_LIMIT).id();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReferralView> referral(Long referralId) {
        return referralRepository.findById(referralId)
                .map(referral -> toView(referral, hospitalsById(), LocalDateTime.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralView> referrals() {
        Map<Long, Hospital> hospitals = hospitalsById();
        LocalDateTime now = LocalDateTime.now();
        return referralRepository.findAllByOrderByIdDesc().stream()
                .map(referral -> toView(referral, hospitals, now))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InboxRequestView> inbox(Long hospitalId) {
        Map<Long, Hospital> hospitals = hospitalsById();
        LocalDateTime now = LocalDateTime.now();
        return requestRepository.findByHospitalIdOrderByIdDesc(hospitalId).stream()
                .map(request -> toInboxView(request, hospitals, now))
                .toList();
    }

    // Deliberately not @Transactional: a ConflictException inside the service's own transaction
    // would otherwise mark this outer one rollback-only and surface as UnexpectedRollbackException.
    @Override
    public DecisionResult accept(Long hospitalId, Long requestId) {
        if (findOwnRequest(hospitalId, requestId).isEmpty()) {
            return DecisionResult.NOT_FOUND;
        }
        try {
            requestResponseService.accept(requestId);
            return DecisionResult.OK;
        } catch (ConflictException e) {
            return whyNotPending(hospitalId, requestId);
        }
    }

    @Override
    public DecisionResult decline(Long hospitalId, Long requestId, ReferralRequest.DeclineReason reason) {
        if (findOwnRequest(hospitalId, requestId).isEmpty()) {
            return DecisionResult.NOT_FOUND;
        }
        try {
            requestResponseService.decline(requestId, reason);
            return DecisionResult.OK;
        } catch (ConflictException e) {
            return whyNotPending(hospitalId, requestId);
        }
    }

    @Override
    @Transactional
    public void activateFlag(Long hospitalId, HospitalFlag.FlagType type) {
        hospitalRepository.findById(hospitalId)
                .ifPresent(hospital -> setFlag(hospital, type, LocalDateTime.now().plus(properties.flagDuration())));
    }

    @Override
    @Transactional
    public void clearFlag(Long hospitalId, HospitalFlag.FlagType type) {
        flagRepository.deleteAll(flagRepository.findByHospital_IdAndType(hospitalId, type));
    }

    /** Wipes every referral, restores seeded occupancy and puts the scenario flags back. */
    @Override
    @Transactional
    public void reset() {
        // Children before parents; candidates one by one so their score/violation rows go too.
        requestRepository.deleteAllInBatch();
        candidateRepository.deleteAll();
        referralRepository.deleteAll();
        flagRepository.deleteAllInBatch();
        occupancySimulator.restoreBaseline();
        applyScenarioFlags(LocalDateTime.now());
    }

    private void applyScenarioFlags(LocalDateTime now) {
        LocalDateTime validUntil = now.plus(properties.flagDuration());
        properties.scenarioFlags().forEach((hospitalId, types) ->
                hospitalRepository.findById(hospitalId)
                        .ifPresent(hospital -> types.forEach(type -> setFlag(hospital, type, validUntil))));
    }

    /** One flag per type: turning it on again just extends it. */
    private void setFlag(Hospital hospital, HospitalFlag.FlagType type, LocalDateTime validUntil) {
        List<HospitalFlag> existing = flagRepository.findByHospital_IdAndType(hospital.getId(), type);
        if (existing.isEmpty()) {
            flagRepository.save(HospitalFlag.builder()
                    .hospital(hospital)
                    .type(type)
                    .validUntil(validUntil)
                    .build());
        } else {
            existing.forEach(flag -> flag.setValidUntil(validUntil));
        }
    }

    private Optional<ReferralRequest> findOwnRequest(Long hospitalId, Long requestId) {
        return requestRepository.findById(requestId)
                .filter(request -> request.getHospitalId().equals(hospitalId));
    }

    /** Tells "someone else took the patient" apart from "you already answered / time ran out". */
    private DecisionResult whyNotPending(Long hospitalId, Long requestId) {
        ReferralRequest request = requestRepository.findById(requestId).orElse(null);
        if (request == null) {
            return DecisionResult.NOT_FOUND;
        }
        Referral referral = referralRepository.findById(request.getReferral().getId()).orElseThrow();
        boolean takenByOther = referral.getStatus() == Referral.ReferralStatus.ACCEPTED
                && !hospitalId.equals(referral.getAcceptedHospitalId());
        return takenByOther ? DecisionResult.ALREADY_TAKEN : DecisionResult.NO_LONGER_PENDING;
    }

    private ReferralView toView(Referral referral, Map<Long, Hospital> hospitals, LocalDateTime now) {
        List<ReferralRequest> requests = requestRepository.findByReferralIdOrderByWaveAscIdAsc(referral.getId());
        // Waves walk disjoint slices of the ranking, so each hospital is asked at most once.
        Map<Long, ReferralRequest> requestByHospital = requests.stream()
                .collect(Collectors.toMap(ReferralRequest::getHospitalId, Function.identity(), (first, second) -> second));
        int currentWave = requests.stream().mapToInt(ReferralRequest::getWave).max().orElse(0);

        List<ReferralCandidate> ranking = candidateRepository.findByReferralIdOrderByRankAsc(referral.getId());
        List<CandidateView> candidates = ranking.stream()
                .filter(ReferralCandidate::isEligible)
                .map(candidate -> toCandidateView(candidate, referral, hospitals.get(candidate.getHospitalId()),
                        requestByHospital.get(candidate.getHospitalId())))
                .toList();
        List<ExcludedHospitalView> excluded = ranking.stream()
                .filter(candidate -> !candidate.isEligible())
                // The origin always fails NO_TRAVEL_ROUTE to itself - it was never a real option.
                .filter(candidate -> !candidate.getHospitalId().equals(referral.getOriginHospitalId()))
                .sorted(Comparator.comparing(ReferralCandidate::getHospitalId))
                .map(candidate -> new ExcludedHospitalView(
                        candidate.getHospitalId(),
                        candidate.getHospitalName(),
                        districtOf(hospitals.get(candidate.getHospitalId())),
                        candidate.getViolations().stream().map(ExclusionReason::fromViolation).toList()))
                .toList();

        long secondsToWaveDeadline = requests.stream()
                .filter(request -> request.getWave() == currentWave)
                .map(request -> secondsUntil(request.getDeadline(), now))
                .findFirst()
                .orElse(0L);

        Hospital origin = hospitals.get(referral.getOriginHospitalId());
        Hospital accepted = referral.getAcceptedHospitalId() == null ? null : hospitals.get(referral.getAcceptedHospitalId());

        return new ReferralView(
                referral.getId(),
                origin == null ? null : origin.getName(),
                referral.getTargetSpecialty(),
                Set.copyOf(referral.getRequiredProcedures()),
                referral.getUrgency(),
                referral.getPatientState(),
                Boolean.TRUE.equals(referral.getRequiresIsolation()),
                referral.getNote(),
                referral.getStatus(),
                referral.getCreatedAt(),
                Duration.between(referral.getCreatedAt(), now).toSeconds(),
                currentWave,
                secondsToWaveDeadline,
                accepted == null ? null : toView(accepted, now),
                candidates,
                excluded);
    }

    private CandidateView toCandidateView(ReferralCandidate candidate, Referral referral, Hospital hospital,
                                          ReferralRequest request) {
        return new CandidateView(
                candidate.getRank(),
                candidate.getHospitalId(),
                candidate.getHospitalName(),
                districtOf(hospital),
                hospital == null ? null : hospital.getDutyPhone(),
                travelTimeProvider.travelMinutes(referral.getOriginHospitalId(), candidate.getHospitalId()).orElse(null),
                hospital == null ? 0 : percent(hospital.getOccupiedBeds(), hospital.getTotalBeds()),
                percent(candidate.getCriterionScores().getOrDefault(ACCEPTANCE_CRITERION, 0.0)),
                percent(candidate.getTotalScore() == null ? 0.0 : candidate.getTotalScore()),
                request == null ? null : request.getWave(),
                request == null ? null : request.getStatus(),
                request == null ? null : request.getDeclineReason());
    }

    private InboxRequestView toInboxView(ReferralRequest request, Map<Long, Hospital> hospitals, LocalDateTime now) {
        Referral referral = request.getReferral();
        Hospital origin = hospitals.get(referral.getOriginHospitalId());
        return new InboxRequestView(
                request.getId(),
                referral.getId(),
                request.getWave(),
                request.getStatus(),
                request.getDeclineReason(),
                referral.getTargetSpecialty(),
                Set.copyOf(referral.getRequiredProcedures()),
                referral.getUrgency(),
                referral.getPatientState(),
                Boolean.TRUE.equals(referral.getRequiresIsolation()),
                referral.getNote(),
                origin == null ? null : origin.getName(),
                origin == null ? null : origin.getDutyPhone(),
                travelTimeProvider.travelMinutes(referral.getOriginHospitalId(), request.getHospitalId()).orElse(null),
                request.getSentAt(),
                secondsUntil(request.getDeadline(), now));
    }

    private static HospitalView toView(Hospital hospital, LocalDateTime now) {
        List<FlagView> flags = hospital.getFlags() == null ? List.of() : hospital.getFlags().stream()
                .filter(flag -> flag.isValid(now))
                .sorted(Comparator.comparing(HospitalFlag::getType))
                .map(flag -> new FlagView(flag.getType(), flag.getValidUntil()))
                .toList();
        return new HospitalView(
                hospital.getId(),
                hospital.getName(),
                hospital.getDistrict(),
                hospital.getLatitude(),
                hospital.getLongitude(),
                hospital.getTotalBeds(),
                hospital.getOccupiedBeds(),
                Set.copyOf(hospital.getSpecialties()),
                Set.copyOf(hospital.getProcedures()),
                hospital.isIsolationCapable(),
                hospital.getDutyPhone(),
                flags);
    }

    private Map<Long, Hospital> hospitalsById() {
        return hospitalRepository.findAll().stream()
                .collect(Collectors.toMap(Hospital::getId, Function.identity()));
    }

    private static String districtOf(Hospital hospital) {
        return hospital == null ? null : hospital.getDistrict();
    }

    private static long secondsUntil(LocalDateTime deadline, LocalDateTime now) {
        return Math.max(0, Duration.between(now, deadline).toSeconds());
    }

    private static int percent(int part, int total) {
        return total == 0 ? 0 : Math.round(part * 100f / total);
    }

    private static int percent(double fraction) {
        return (int) Math.round(fraction * 100);
    }
}
