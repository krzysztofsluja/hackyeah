package com.sluja.hackyeah.ui.mock;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.matching.HospitalScore;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.ui.DemoBackend;
import com.sluja.hackyeah.ui.DemoProperties;
import com.sluja.hackyeah.ui.view.CandidateView;
import com.sluja.hackyeah.ui.view.DecisionResult;
import com.sluja.hackyeah.ui.view.ExcludedHospitalView;
import com.sluja.hackyeah.ui.view.ExclusionReason;
import com.sluja.hackyeah.ui.view.HospitalView;
import com.sluja.hackyeah.ui.view.InboxRequestView;
import com.sluja.hackyeah.ui.view.NewReferral;
import com.sluja.hackyeah.ui.view.ReferralView;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/** In-memory backend used until the real REST/services are ready. All state lives in this bean. */
@Service
public class MockDemoBackend implements DemoBackend {

    private final Clock clock;
    private final DemoProperties properties;
    private final MockMatcher matcher;
    private final Map<Long, MockHospital> hospitals = new LinkedHashMap<>();
    private final Map<Long, MockReferral> referrals = new LinkedHashMap<>();
    private final Map<Long, MockRequest> requests = new LinkedHashMap<>();
    private final AtomicLong referralIds = new AtomicLong();
    private final AtomicLong requestIds = new AtomicLong();
    private final RandomGenerator random = RandomGenerator.getDefault();

    public MockDemoBackend(Clock clock, DemoProperties properties) {
        this.clock = clock;
        this.properties = properties;
        this.matcher = new MockMatcher(MockSeed.ORIGIN_HOSPITAL_ID, MockSeed.travelMinutesFromOrigin());
        reset();
    }

    @Override
    public synchronized List<HospitalView> hospitals() {
        LocalDateTime now = now();
        return hospitals.values().stream()
                .sorted(Comparator.comparing(h -> h.id))
                .map(h -> h.toView(now))
                .toList();
    }

    @Override
    public synchronized Optional<HospitalView> hospital(Long hospitalId) {
        return Optional.ofNullable(hospitals.get(hospitalId)).map(h -> h.toView(now()));
    }

    @Override
    public synchronized HospitalView originHospital() {
        return hospitals.get(MockSeed.ORIGIN_HOSPITAL_ID).toView(now());
    }

    @Override
    public synchronized Long createReferral(NewReferral data) {
        LocalDateTime now = now();
        Long originId = MockSeed.ORIGIN_HOSPITAL_ID;

        List<Hospital> destinations = hospitals.values().stream()
                .filter(h -> !h.id.equals(originId))
                .map(MockHospital::toEntity)
                .toList();
        MockMatcher.Ranking ranking = matcher.rank(toEntity(data, originId), destinations, MockSeed.acceptanceStats(), now);

        List<MockReferral.RankedHospital> ranked = ranking.ranked().stream()
                .map(score -> rankedHospital(score, originId))
                .toList();
        List<MockReferral.ExcludedHospital> excluded = ranking.excluded().stream()
                .map(e -> new MockReferral.ExcludedHospital(e.hospital().getId(),
                        e.violations().stream().map(ExclusionReason::fromViolation).toList()))
                .toList();

        MockReferral referral = new MockReferral(referralIds.incrementAndGet(), data, originId, now, ranked, excluded);
        referrals.put(referral.id, referral);
        sendNextWave(referral, now);
        return referral.id;
    }

    @Override
    public synchronized Optional<ReferralView> referral(Long referralId) {
        return Optional.ofNullable(referrals.get(referralId)).map(r -> toView(r, now()));
    }

    @Override
    public synchronized List<ReferralView> referrals() {
        LocalDateTime now = now();
        return referrals.values().stream()
                .sorted(Comparator.comparing((MockReferral r) -> r.id).reversed())
                .map(r -> toView(r, now))
                .toList();
    }

    @Override
    public synchronized List<InboxRequestView> inbox(Long hospitalId) {
        LocalDateTime now = now();
        return requests.values().stream()
                .filter(r -> r.hospitalId.equals(hospitalId))
                .sorted(Comparator.comparing((MockRequest r) -> r.id).reversed())
                .map(r -> toInboxView(r, now))
                .toList();
    }

    @Override
    public synchronized DecisionResult accept(Long hospitalId, Long requestId) {
        MockRequest request = requests.get(requestId);
        if (request == null || !request.hospitalId.equals(hospitalId)) {
            return DecisionResult.NOT_FOUND;
        }
        MockReferral referral = referrals.get(request.referralId);
        // Mirrors the conditional UPDATE ... WHERE status = 'OPEN' of the real backend.
        if (referral.status == Referral.ReferralStatus.ACCEPTED) {
            return DecisionResult.ALREADY_TAKEN;
        }
        if (request.status != ReferralRequest.RequestStatus.PENDING || referral.status != Referral.ReferralStatus.OPEN) {
            return DecisionResult.NO_LONGER_PENDING;
        }

        referral.status = Referral.ReferralStatus.ACCEPTED;
        referral.acceptedHospitalId = hospitalId;
        request.status = ReferralRequest.RequestStatus.ACCEPTED;
        referral.requests.stream()
                .filter(r -> r.status == ReferralRequest.RequestStatus.PENDING)
                .forEach(r -> r.status = ReferralRequest.RequestStatus.CANCELLED);
        return DecisionResult.OK;
    }

    @Override
    public synchronized DecisionResult decline(Long hospitalId, Long requestId, ReferralRequest.DeclineReason reason) {
        MockRequest request = requests.get(requestId);
        if (request == null || !request.hospitalId.equals(hospitalId)) {
            return DecisionResult.NOT_FOUND;
        }
        if (request.status == ReferralRequest.RequestStatus.CANCELLED) {
            return DecisionResult.ALREADY_TAKEN;
        }
        if (request.status != ReferralRequest.RequestStatus.PENDING) {
            return DecisionResult.NO_LONGER_PENDING;
        }

        request.status = ReferralRequest.RequestStatus.DECLINED;
        request.declineReason = reason;
        advanceIfWaveClosed(referrals.get(request.referralId), now());
        return DecisionResult.OK;
    }

    @Override
    public synchronized void activateFlag(Long hospitalId, HospitalFlag.FlagType type) {
        MockHospital hospital = hospitals.get(hospitalId);
        if (hospital != null) {
            hospital.flags.put(type, now().plus(properties.flagDuration()));
        }
    }

    @Override
    public synchronized void clearFlag(Long hospitalId, HospitalFlag.FlagType type) {
        MockHospital hospital = hospitals.get(hospitalId);
        if (hospital != null) {
            hospital.flags.remove(type);
        }
    }

    /**
     * Wave timeouts, as in the real backend: one periodic job instead of a timer per request.
     * Unanswered requests past their deadline count as declined; a closed wave triggers the next one
     * or escalation.
     */
    @Scheduled(fixedDelay = 2000)
    public synchronized void expireOverdueRequests() {
        LocalDateTime now = now();
        for (MockReferral referral : referrals.values()) {
            if (referral.status != Referral.ReferralStatus.OPEN) {
                continue;
            }
            referral.currentWaveRequests().stream()
                    .filter(r -> r.status == ReferralRequest.RequestStatus.PENDING && !now.isBefore(r.deadline))
                    .forEach(r -> r.status = ReferralRequest.RequestStatus.EXPIRED);
            advanceIfWaveClosed(referral, now);
        }
    }

    /**
     * Stand-in for ADT admission/discharge events: nudges occupancy of some hospitals by a bed or two,
     * so the coordinator map changes live.
     */
    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    public synchronized void simulateOccupancy() {
        for (MockHospital hospital : hospitals.values()) {
            if (hospital.id.equals(MockSeed.ORIGIN_HOSPITAL_ID) || random.nextBoolean()) {
                continue;
            }
            int delta = random.nextInt(-2, 3);
            hospital.occupiedBeds = Math.clamp(hospital.occupiedBeds + delta, minOccupied(hospital), hospital.totalBeds - 1);
        }
    }

    /** The overloaded voivodeship hospital stays above the alert threshold, others never drop below 40%. */
    private static int minOccupied(MockHospital hospital) {
        double floor = hospital.id.equals(MockSeed.VOIVODESHIP_HOSPITAL_ID) ? 0.92 : 0.40;
        return (int) Math.ceil(hospital.totalBeds * floor);
    }

    @Override
    public synchronized void reset() {
        hospitals.clear();
        MockSeed.hospitals(now()).forEach(h -> hospitals.put(h.id, h));
        referrals.clear();
        requests.clear();
        referralIds.set(0);
        requestIds.set(0);
    }

    /** Asks the next {@code waveSize} hospitals from the ranking, or escalates when none are left. */
    private void sendNextWave(MockReferral referral, LocalDateTime now) {
        List<MockReferral.RankedHospital> next = referral.notYetContacted().stream()
                .limit(properties.waveSize())
                .toList();
        if (next.isEmpty()) {
            referral.status = Referral.ReferralStatus.ESCALATED;
            return;
        }

        referral.currentWave++;
        LocalDateTime deadline = now.plus(properties.waveTimeoutFor(referral.data.urgency()));
        for (MockReferral.RankedHospital candidate : next) {
            MockRequest request = new MockRequest(requestIds.incrementAndGet(), referral.id, candidate.hospitalId(),
                    referral.currentWave, now, deadline);
            referral.requests.add(request);
            requests.put(request.id, request);
        }
    }

    /** Once nobody in the current wave can still answer, the next wave goes out without waiting for the timeout. */
    private void advanceIfWaveClosed(MockReferral referral, LocalDateTime now) {
        boolean waveClosed = referral.currentWaveRequests().stream()
                .noneMatch(r -> r.status == ReferralRequest.RequestStatus.PENDING);
        if (referral.status == Referral.ReferralStatus.OPEN && waveClosed) {
            sendNextWave(referral, now);
        }
    }

    private InboxRequestView toInboxView(MockRequest request, LocalDateTime now) {
        MockReferral referral = referrals.get(request.referralId);
        MockHospital origin = hospitals.get(referral.originHospitalId);
        return new InboxRequestView(request.id, referral.id, request.wave, request.status, request.declineReason,
                referral.data.targetSpecialty(), referral.data.requiredProcedures(), referral.data.urgency(),
                referral.data.patientState(), referral.data.requiresIsolation(), referral.data.note(),
                origin.name, origin.dutyPhone, matcher.travelMinutes(origin.id, request.hospitalId).orElse(null),
                request.sentAt, Math.max(0, Duration.between(now, request.deadline).toSeconds()));
    }

    private MockReferral.RankedHospital rankedHospital(HospitalScore score, Long originId) {
        Hospital hospital = score.hospital();
        return new MockReferral.RankedHospital(
                hospital.getId(),
                matcher.travelMinutes(originId, hospital.getId()).orElse(null),
                percent(hospital.getOccupiedBeds(), hospital.getTotalBeds()),
                (int) Math.round(score.criterionScores().getOrDefault("ACCEPTANCE_PROBABILITY", 0.0) * 100),
                (int) Math.round(score.totalScore() * 100));
    }

    private ReferralView toView(MockReferral referral, LocalDateTime now) {
        Map<Long, MockRequest> requestByHospital = referral.requests.stream()
                .collect(Collectors.toMap(r -> r.hospitalId, Function.identity()));

        List<CandidateView> candidates = new ArrayList<>();
        for (int i = 0; i < referral.ranking.size(); i++) {
            MockReferral.RankedHospital ranked = referral.ranking.get(i);
            MockHospital hospital = hospitals.get(ranked.hospitalId());
            MockRequest request = requestByHospital.get(ranked.hospitalId());
            candidates.add(new CandidateView(i + 1, hospital.id, hospital.name, hospital.district, hospital.dutyPhone,
                    ranked.travelMinutes(), ranked.occupancyPercent(), ranked.acceptancePercent(), ranked.scorePercent(),
                    request != null ? request.wave : null,
                    request != null ? request.status : null,
                    request != null ? request.declineReason : null));
        }

        List<ExcludedHospitalView> excluded = referral.excluded.stream()
                .map(e -> {
                    MockHospital hospital = hospitals.get(e.hospitalId());
                    return new ExcludedHospitalView(hospital.id, hospital.name, hospital.district, e.reasons());
                })
                .toList();

        long secondsToDeadline = referral.currentWaveRequests().stream()
                .map(r -> Duration.between(now, r.deadline).toSeconds())
                .findFirst()
                .map(s -> Math.max(0, s))
                .orElse(0L);

        HospitalView accepted = referral.acceptedHospitalId == null ? null : hospitals.get(referral.acceptedHospitalId).toView(now);

        return new ReferralView(referral.id, hospitals.get(referral.originHospitalId).name,
                referral.data.targetSpecialty(), referral.data.requiredProcedures(), referral.data.urgency(),
                referral.data.patientState(), referral.data.requiresIsolation(), referral.data.note(),
                referral.status, referral.createdAt, Duration.between(referral.createdAt, now).toSeconds(),
                referral.currentWave, secondsToDeadline, accepted, candidates, excluded);
    }

    private static Referral toEntity(NewReferral data, Long originId) {
        return Referral.builder()
                .targetSpecialty(data.targetSpecialty())
                .requiredProcedures(data.requiredProcedures())
                .urgency(data.urgency())
                .patientState(data.patientState())
                .requiresIsolation(data.requiresIsolation())
                .note(data.note())
                .originHospitalId(originId)
                .status(Referral.ReferralStatus.OPEN)
                .build();
    }

    private static int percent(int part, int total) {
        return total == 0 ? 0 : Math.round(part * 100f / total);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
