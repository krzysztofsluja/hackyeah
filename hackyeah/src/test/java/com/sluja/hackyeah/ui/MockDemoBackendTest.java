package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.ui.mock.MockDemoBackend;
import com.sluja.hackyeah.ui.view.CandidateView;
import com.sluja.hackyeah.ui.view.DecisionResult;
import com.sluja.hackyeah.ui.view.ExclusionReason;
import com.sluja.hackyeah.ui.view.HospitalView;
import com.sluja.hackyeah.ui.view.InboxRequestView;
import com.sluja.hackyeah.ui.view.NewReferral;
import com.sluja.hackyeah.ui.view.OccupancyLevel;
import com.sluja.hackyeah.ui.view.ReferralView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MockDemoBackendTest {

    private static final NewReferral STROKE = new NewReferral("NEUROLOGY", Set.of(Procedure.CT, Procedure.THROMBECTOMY),
            Referral.Urgency.TIME_CRITICAL, Referral.PatientState.STABLE, false, "72 l., objawy od 2 h");

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-04T01:00:00Z"), ZoneId.of("Europe/Warsaw"));
    private MockDemoBackend backend;

    @BeforeEach
    void setUp() {
        backend = new MockDemoBackend(clock, DemoProperties.defaults());
    }

    @Test
    void seedsEightHospitalsWithOrigin() {
        assertThat(backend.hospitals()).hasSize(8);
        assertThat(backend.originHospital().specialties()).doesNotContain("NEUROLOGY");
    }

    @Test
    void voivodeshipHospitalIsOverloadedWithBusyCathLab() {
        HospitalView voivodeship = backend.hospital(2L).orElseThrow();

        assertThat(voivodeship.occupancyLevel()).isEqualTo(OccupancyLevel.HIGH);
        assertThat(voivodeship.flags())
                .extracting(f -> f.type())
                .containsExactly(HospitalFlag.FlagType.CATH_LAB_BUSY);
    }

    @Test
    void unknownHospitalIsEmpty() {
        assertThat(backend.hospital(99L)).isEmpty();
    }

    @Test
    void occupancyLevelThresholds() {
        assertThat(OccupancyLevel.of(74)).isEqualTo(OccupancyLevel.LOW);
        assertThat(OccupancyLevel.of(75)).isEqualTo(OccupancyLevel.MEDIUM);
        assertThat(OccupancyLevel.of(90)).isEqualTo(OccupancyLevel.MEDIUM);
        assertThat(OccupancyLevel.of(91)).isEqualTo(OccupancyLevel.HIGH);
    }

    @Test
    void strokeScenarioRanksUniversityFirstAndExcludesVoivodeshipByFlag() {
        ReferralView referral = backend.referral(backend.createReferral(STROKE)).orElseThrow();

        assertThat(referral.candidates()).extracting(CandidateView::name)
                .containsExactly("Szpital Uniwersytecki", "Szpital Kliniczny", "Szpital Miejski");
        assertThat(referral.excluded())
                .filteredOn(e -> e.name().equals("Szpital Wojewódzki"))
                .singleElement()
                .satisfies(e -> assertThat(e.reasons()).containsExactly(new ExclusionReason(
                        "PROCEDURE_BLOCKED_BY_FLAG", List.of("flag.CATH_LAB_BUSY", "procedure.THROMBECTOMY"))));
        assertThat(referral.excluded()).extracting(e -> e.name()).doesNotContain("Szpital Powiatowy");
    }

    @Test
    void createReferralSendsFirstWaveToTopThree() {
        ReferralView referral = backend.referral(backend.createReferral(STROKE)).orElseThrow();

        assertThat(referral.status()).isEqualTo(Referral.ReferralStatus.OPEN);
        assertThat(referral.currentWave()).isEqualTo(1);
        assertThat(referral.secondsToWaveDeadline()).isEqualTo(30);
        assertThat(referral.candidates())
                .allSatisfy(c -> {
                    assertThat(c.wave()).isEqualTo(1);
                    assertThat(c.requestStatus()).isEqualTo(ReferralRequest.RequestStatus.PENDING);
                });
    }

    @Test
    void referralWithoutCandidatesIsEscalatedImmediately() {
        NewReferral impossible = new NewReferral("INFECTIOUS", Set.of(Procedure.THROMBECTOMY),
                Referral.Urgency.PLANNED, Referral.PatientState.STABLE, true, null);

        ReferralView referral = backend.referral(backend.createReferral(impossible)).orElseThrow();

        assertThat(referral.status()).isEqualTo(Referral.ReferralStatus.ESCALATED);
        assertThat(referral.candidates()).isEmpty();
    }

    @Test
    void firstAcceptWinsAndCancelsTheOthers() {
        Long referralId = backend.createReferral(STROKE);
        Long universityRequest = pendingRequestId(3L);
        Long clinicalRequest = pendingRequestId(7L);

        assertThat(backend.accept(3L, universityRequest)).isEqualTo(DecisionResult.OK);
        assertThat(backend.accept(7L, clinicalRequest)).isEqualTo(DecisionResult.ALREADY_TAKEN);

        ReferralView referral = backend.referral(referralId).orElseThrow();
        assertThat(referral.status()).isEqualTo(Referral.ReferralStatus.ACCEPTED);
        assertThat(referral.acceptedHospital().name()).isEqualTo("Szpital Uniwersytecki");
        assertThat(referral.candidates()).extracting(CandidateView::requestStatus).containsExactly(
                ReferralRequest.RequestStatus.ACCEPTED,
                ReferralRequest.RequestStatus.CANCELLED,
                ReferralRequest.RequestStatus.CANCELLED);
        assertThat(backend.inbox(7L)).singleElement()
                .satisfies(r -> assertThat(r.status()).isEqualTo(ReferralRequest.RequestStatus.CANCELLED));
    }

    @Test
    void hospitalCannotAnswerAnotherHospitalsRequest() {
        backend.createReferral(STROKE);

        assertThat(backend.accept(7L, pendingRequestId(3L))).isEqualTo(DecisionResult.NOT_FOUND);
    }

    @Test
    void declineStoresReasonAndAnsweredRequestCannotBeAccepted() {
        backend.createReferral(STROKE);
        Long requestId = pendingRequestId(3L);

        assertThat(backend.decline(3L, requestId, ReferralRequest.DeclineReason.NO_BEDS)).isEqualTo(DecisionResult.OK);
        assertThat(backend.accept(3L, requestId)).isEqualTo(DecisionResult.NO_LONGER_PENDING);
        assertThat(backend.inbox(3L)).singleElement()
                .satisfies(r -> assertThat(r.declineReason()).isEqualTo(ReferralRequest.DeclineReason.NO_BEDS));
    }

    @Test
    void lastDeclineInWaveEscalatesWhenRankingIsExhausted() {
        Long referralId = backend.createReferral(STROKE);

        for (Long hospitalId : List.of(3L, 7L, 4L)) {
            backend.decline(hospitalId, pendingRequestId(hospitalId), ReferralRequest.DeclineReason.NO_SPECIALIST);
        }

        assertThat(backend.referral(referralId).orElseThrow().status()).isEqualTo(Referral.ReferralStatus.ESCALATED);
    }

    @Test
    void lastDeclineInWaveSendsNextWave() {
        backend.clearFlag(2L, HospitalFlag.FlagType.CATH_LAB_BUSY);
        Long referralId = backend.createReferral(STROKE);
        ReferralView wave1 = backend.referral(referralId).orElseThrow();
        List<Long> firstWave = wave1.candidates().stream().filter(CandidateView::contacted).map(CandidateView::hospitalId).toList();
        assertThat(firstWave).hasSize(3);

        firstWave.forEach(id -> backend.decline(id, pendingRequestId(id), ReferralRequest.DeclineReason.NO_BEDS));

        ReferralView wave2 = backend.referral(referralId).orElseThrow();
        assertThat(wave2.status()).isEqualTo(Referral.ReferralStatus.OPEN);
        assertThat(wave2.currentWave()).isEqualTo(2);
        assertThat(wave2.candidates()).filteredOn(c -> Integer.valueOf(2).equals(c.wave())).hasSize(1);
    }

    @Test
    void flagsCanBeToggledAndAffectNewRankings() {
        backend.activateFlag(3L, HospitalFlag.FlagType.TK_DOWN);

        assertThat(backend.hospital(3L).orElseThrow().flag(HospitalFlag.FlagType.TK_DOWN)).isNotNull();
        ReferralView referral = backend.referral(backend.createReferral(STROKE)).orElseThrow();
        assertThat(referral.candidates()).extracting(CandidateView::name).doesNotContain("Szpital Uniwersytecki");

        backend.clearFlag(3L, HospitalFlag.FlagType.TK_DOWN);
        assertThat(backend.hospital(3L).orElseThrow().flags()).isEmpty();
    }

    @Test
    void occupancySimulationStaysWithinBoundsAndKeepsVoivodeshipOverloaded() {
        for (int i = 0; i < 500; i++) {
            backend.simulateOccupancy();
        }

        assertThat(backend.hospitals()).allSatisfy(h -> {
            assertThat(h.availableBeds()).isPositive();
            assertThat(h.occupancyPercent()).isGreaterThanOrEqualTo(40);
        });
        assertThat(backend.hospital(2L).orElseThrow().occupancyLevel()).isEqualTo(OccupancyLevel.HIGH);
        assertThat(backend.originHospital().occupiedBeds()).isEqualTo(41);
    }

    @Test
    void resetClearsReferrals() {
        backend.createReferral(STROKE);

        backend.reset();

        assertThat(backend.referrals()).isEmpty();
        assertThat(backend.referral(1L)).isEmpty();
    }

    private Long pendingRequestId(Long hospitalId) {
        return backend.inbox(hospitalId).stream()
                .filter(InboxRequestView::isPending)
                .findFirst()
                .orElseThrow()
                .requestId();
    }
}
