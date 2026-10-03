package com.sluja.hackyeah.referral;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.repository.HospitalRepository;
import com.sluja.hackyeah.hospital.repository.TravelTimeRepository;
import com.sluja.hackyeah.hospital.service.StaticMatrixTravelTimeProvider;
import com.sluja.hackyeah.referral.dto.AcceptanceResponse;
import com.sluja.hackyeah.referral.dto.DispatchedRequest;
import com.sluja.hackyeah.referral.dto.HospitalContact;
import com.sluja.hackyeah.referral.dto.ReferralCreatedResponse;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.referral.repository.ReferralCandidateRepository;
import com.sluja.hackyeah.referral.repository.ReferralRepository;
import com.sluja.hackyeah.referral.repository.ReferralRequestRepository;
import com.sluja.hackyeah.referral.service.HospitalAcceptanceStatsService;
import com.sluja.hackyeah.referral.service.HospitalMatchingService;
import com.sluja.hackyeah.referral.service.ReferralService;
import com.sluja.hackyeah.referral.service.RequestResponseService;
import com.sluja.hackyeah.referral.service.WaveScheduler;
import com.sluja.hackyeah.referral.service.WaveService;
import com.sluja.hackyeah.web.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.sluja.hackyeah.referral.WaveTestSupport.referral;
import static com.sluja.hackyeah.referral.WaveTestSupport.saveHospital;
import static com.sluja.hackyeah.referral.WaveTestSupport.saveRoute;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
@EnableConfigurationProperties(WaveProperties.class)
@Import({StaticMatrixTravelTimeProvider.class, HospitalAcceptanceStatsService.class,
        HospitalMatchingService.class, ReferralService.class, WaveService.class,
        WaveScheduler.class, RequestResponseService.class})
class WaveFlowTest {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private TravelTimeRepository travelTimeRepository;

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private ReferralRequestRepository requestRepository;

    @Autowired
    private ReferralCandidateRepository candidateRepository;

    @Autowired
    private ReferralService referralService;

    @Autowired
    private RequestResponseService requestResponseService;

    @Autowired
    private WaveScheduler waveScheduler;

    @Autowired
    private WaveProperties waveProperties;

    static final String DUTY_PHONE = "+48 12 000 00 99";

    private Hospital origin;

    /** Seven hospitals at increasing travel time, so the ranking is 1..7 by distance. */
    @BeforeEach
    void seedCity() {
        origin = saveHospital(hospitalRepository, "Origin", 50);
        saveRoute(travelTimeRepository, origin, origin, 0);
        for (int i = 1; i <= 6; i++) {
            Hospital hospital = saveHospital(hospitalRepository, "H" + i, 50);
            saveRoute(travelTimeRepository, origin, hospital, i * 5);
        }
    }

    private ReferralCreatedResponse fileReferral(Referral.Urgency urgency) {
        return referralService.create(referral(origin.getId(), urgency), 10);
    }

    @Test
    void firstWaveGoesToTopThreeOfTheFrozenRanking() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);

        assertEquals(1, response.wave());
        assertEquals(waveProperties.size(), response.requests().size());

        // Ranking is frozen 1..7 and the wave takes exactly the top three.
        assertEquals(7, response.candidates().size());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7),
                response.candidates().stream().map(candidate -> candidate.rank()).toList());
        assertEquals(response.candidates().subList(0, 3).stream().map(candidate -> candidate.hospitalId()).toList(),
                response.requests().stream().map(DispatchedRequest::hospitalId).toList());

        assertTrue(requestRepository.findByReferralIdOrderByWaveAscIdAsc(response.id()).stream()
                .allMatch(request -> request.getStatus() == ReferralRequest.RequestStatus.PENDING));
        assertEquals(7, candidateRepository.findByReferralIdAndEligibleTrueOrderByRankAsc(response.id()).size());
    }

    @Test
    void deadlineFollowsUrgency() {
        ReferralCreatedResponse critical = fileReferral(Referral.Urgency.TIME_CRITICAL);
        ReferralCreatedResponse planned = fileReferral(Referral.Urgency.PLANNED);

        Duration criticalWindow = Duration.between(
                requestRepository.findById(critical.requests().get(0).requestId()).orElseThrow().getSentAt(),
                critical.requests().get(0).deadline());
        Duration plannedWindow = Duration.between(
                requestRepository.findById(planned.requests().get(0).requestId()).orElseThrow().getSentAt(),
                planned.requests().get(0).deadline());

        assertEquals(Duration.ofMinutes(3), criticalWindow);
        assertEquals(Duration.ofMinutes(10), plannedWindow);
    }

    @Test
    void sweepExpiresOverdueRequestsAndSendsTheNextWave() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);
        LocalDateTime afterDeadline = response.requests().get(0).deadline().plusSeconds(1);

        waveScheduler.sweep(afterDeadline);

        List<ReferralRequest> all = requestRepository.findByReferralIdOrderByWaveAscIdAsc(response.id());
        assertEquals(6, all.size());

        List<ReferralRequest> waveOne = all.stream().filter(request -> request.getWave() == 1).toList();
        List<ReferralRequest> waveTwo = all.stream().filter(request -> request.getWave() == 2).toList();
        assertTrue(waveOne.stream().allMatch(r -> r.getStatus() == ReferralRequest.RequestStatus.EXPIRED));
        assertTrue(waveTwo.stream().allMatch(r -> r.getStatus() == ReferralRequest.RequestStatus.PENDING));

        // Wave 2 picks up exactly where wave 1 stopped: ranks 4, 5 and 6.
        assertEquals(response.candidates().subList(3, 6).stream().map(c -> c.hospitalId()).toList(),
                waveTwo.stream().map(ReferralRequest::getHospitalId).toList());
        assertEquals(Referral.ReferralStatus.OPEN,
                referralRepository.findById(response.id()).orElseThrow().getStatus());
    }

    @Test
    void escalatesWhenTheRankingRunsOut() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);

        // 7 eligible hospitals over waves of 3 means wave 3 holds one, and wave 4 has nobody left.
        LocalDateTime now = response.requests().get(0).deadline();
        for (int sweep = 1; sweep <= 4; sweep++) {
            now = now.plusMinutes(5);
            waveScheduler.sweep(now);
        }

        assertEquals(Referral.ReferralStatus.ESCALATED,
                referralRepository.findById(response.id()).orElseThrow().getStatus());
        assertEquals(7, requestRepository.findByReferralIdOrderByWaveAscIdAsc(response.id()).size());
    }

    @Test
    void firstAcceptWinsAndCancelsTheRest() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);
        List<DispatchedRequest> wave = response.requests();

        AcceptanceResponse winner = requestResponseService.accept(wave.get(0).requestId());

        assertEquals(ReferralRequest.RequestStatus.ACCEPTED, winner.request().status());
        // Acceptance opens the doctor-to-doctor channel, so both sides get a number.
        assertEquals(wave.get(0).hospitalId(), winner.acceptingHospital().hospitalId());
        assertEquals(origin.getId(), winner.originHospital().hospitalId());
        assertEquals(DUTY_PHONE, winner.acceptingHospital().dutyPhone());
        assertEquals(DUTY_PHONE, winner.originHospital().dutyPhone());

        Referral referral = referralRepository.findById(response.id()).orElseThrow();
        assertEquals(Referral.ReferralStatus.ACCEPTED, referral.getStatus());
        assertEquals(wave.get(0).hospitalId(), referral.getAcceptedHospitalId());

        List<ReferralRequest> losers = requestRepository.findByReferralIdOrderByWaveAscIdAsc(response.id())
                .stream()
                .filter(request -> !request.getId().equals(wave.get(0).requestId()))
                .toList();
        assertEquals(2, losers.size());
        assertTrue(losers.stream().allMatch(r -> r.getStatus() == ReferralRequest.RequestStatus.CANCELLED));
    }

    @Test
    void acceptTakesABedOnlyInTheWinningHospital() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);
        List<DispatchedRequest> wave = response.requests();
        int winnerBefore = occupiedBeds(wave.get(0).hospitalId());
        int loserBefore = occupiedBeds(wave.get(1).hospitalId());

        requestResponseService.accept(wave.get(0).requestId());

        assertEquals(winnerBefore + 1, occupiedBeds(wave.get(0).hospitalId()));
        assertEquals(loserBefore, occupiedBeds(wave.get(1).hospitalId()));
    }

    @Test
    void declineDoesNotTakeABed() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);
        Long hospitalId = response.requests().get(0).hospitalId();
        int before = occupiedBeds(hospitalId);

        requestResponseService.decline(response.requests().get(0).requestId(), ReferralRequest.DeclineReason.NO_BEDS);

        assertEquals(before, occupiedBeds(hospitalId));
    }

    private int occupiedBeds(Long hospitalId) {
        return hospitalRepository.findById(hospitalId).orElseThrow().getOccupiedBeds();
    }

    @Test
    void secondAcceptLosesWithAConflict() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);
        requestResponseService.accept(response.requests().get(0).requestId());

        assertThrows(ConflictException.class,
                () -> requestResponseService.accept(response.requests().get(1).requestId()));
    }

    @Test
    void sweepLeavesAnAcceptedReferralAlone() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);
        requestResponseService.accept(response.requests().get(0).requestId());

        waveScheduler.sweep(response.requests().get(0).deadline().plusMinutes(30));

        assertEquals(3, requestRepository.findByReferralIdOrderByWaveAscIdAsc(response.id()).size());
        assertEquals(Referral.ReferralStatus.ACCEPTED,
                referralRepository.findById(response.id()).orElseThrow().getStatus());
    }

    @Test
    void declineRecordsAReasonAndClosesTheWaveEarly() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);
        for (DispatchedRequest sent : response.requests()) {
            requestResponseService.decline(sent.requestId(), ReferralRequest.DeclineReason.NO_BEDS);
        }

        List<ReferralRequest> waveOne = requestRepository.findByReferralIdOrderByWaveAscIdAsc(response.id());
        assertTrue(waveOne.stream().allMatch(r -> r.getStatus() == ReferralRequest.RequestStatus.DECLINED));
        assertTrue(waveOne.stream()
                .allMatch(r -> r.getDeclineReason() == ReferralRequest.DeclineReason.NO_BEDS));

        // No PENDING request left, so the next sweep sends wave 2 without waiting for a deadline.
        waveScheduler.sweep(LocalDateTime.now());

        assertEquals(6, requestRepository.findByReferralIdOrderByWaveAscIdAsc(response.id()).size());
    }

    @Test
    void decliningTwiceIsAConflict() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);
        Long requestId = response.requests().get(0).requestId();
        requestResponseService.decline(requestId, ReferralRequest.DeclineReason.NO_SPECIALIST);

        assertThrows(ConflictException.class,
                () -> requestResponseService.decline(requestId, ReferralRequest.DeclineReason.OTHER));
    }

    @Test
    void escalatesImmediatelyWhenNothingIsEligible() {
        // Isolation is required but no hospital in the fixture can provide it.
        ReferralCreatedResponse response = referralService.create(
                new com.sluja.hackyeah.referral.dto.CreateReferralRequest(
                        "NEUROLOGY", java.util.Set.of(), Referral.Urgency.TIME_CRITICAL,
                        Referral.PatientState.UNSTABLE, true, null, origin.getId()),
                10);

        assertEquals(0, response.wave());
        assertTrue(response.requests().isEmpty());
        assertTrue(response.candidates().isEmpty());
        assertEquals(7, response.excluded().size());
        assertEquals(Referral.ReferralStatus.ESCALATED, response.status());
    }

    @Test
    void contactsAreWithheldWhileOpenAndRevealedOnAccept() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);

        // While the waves are still running the doctor must not be able to phone down the ranking.
        assertTrue(referralService.findById(response.id()).contacts().isEmpty());

        requestResponseService.accept(response.requests().get(0).requestId());

        List<HospitalContact> contacts = referralService.findById(response.id()).contacts();
        assertEquals(2, contacts.size());
        assertEquals(
                List.of(response.requests().get(0).hospitalId(), origin.getId()),
                contacts.stream().map(HospitalContact::hospitalId).toList());
    }

    @Test
    void escalationHandsOverTheWholeRankingWithNumbers() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);

        LocalDateTime now = response.requests().get(0).deadline();
        for (int sweep = 1; sweep <= 4; sweep++) {
            now = now.plusMinutes(5);
            waveScheduler.sweep(now);
        }

        var detail = referralService.findById(response.id());
        assertEquals(Referral.ReferralStatus.ESCALATED, detail.status());
        assertEquals(7, detail.contacts().size());
        assertTrue(detail.contacts().stream().allMatch(contact -> DUTY_PHONE.equals(contact.dutyPhone())));
    }

    @Test
    void inboxShowsOnlyPendingRequestsForThatHospital() {
        ReferralCreatedResponse response = fileReferral(Referral.Urgency.TIME_CRITICAL);
        DispatchedRequest first = response.requests().get(0);

        assertEquals(1, requestResponseService.inbox(first.hospitalId()).size());
        assertEquals(response.id(), requestResponseService.inbox(first.hospitalId()).get(0).referralId());

        requestResponseService.decline(first.requestId(), ReferralRequest.DeclineReason.NO_BEDS);
        assertTrue(requestResponseService.inbox(first.hospitalId()).isEmpty());
    }
}
