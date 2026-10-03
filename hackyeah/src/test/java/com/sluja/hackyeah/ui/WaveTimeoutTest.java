package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.ui.mock.MockDemoBackend;
import com.sluja.hackyeah.ui.view.CandidateView;
import com.sluja.hackyeah.ui.view.DecisionResult;
import com.sluja.hackyeah.ui.view.NewReferral;
import com.sluja.hackyeah.ui.view.ReferralView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WaveTimeoutTest {

    private static final NewReferral STROKE = new NewReferral("NEUROLOGY", Set.of(Procedure.CT, Procedure.THROMBECTOMY),
            Referral.Urgency.TIME_CRITICAL, Referral.PatientState.STABLE, false, null);
    private static final Duration WAVE_TIMEOUT = DemoProperties.defaults().waveTimeoutFor(Referral.Urgency.TIME_CRITICAL);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-04T01:00:00Z"), ZoneId.of("Europe/Warsaw"));
    private MockDemoBackend backend;

    @BeforeEach
    void setUp() {
        backend = new MockDemoBackend(clock, DemoProperties.defaults());
    }

    @Test
    void requestsStayPendingBeforeDeadline() {
        Long referralId = backend.createReferral(STROKE);

        clock.advance(WAVE_TIMEOUT.minusSeconds(1));
        backend.expireOverdueRequests();

        ReferralView referral = backend.referral(referralId).orElseThrow();
        assertThat(referral.currentWave()).isEqualTo(1);
        assertThat(referral.candidates()).extracting(CandidateView::requestStatus)
                .containsOnly(ReferralRequest.RequestStatus.PENDING);
    }

    @Test
    void unansweredWaveExpiresAndNextWaveGoesOut() {
        backend.clearFlag(2L, HospitalFlag.FlagType.CATH_LAB_BUSY);
        Long referralId = backend.createReferral(STROKE);

        clock.advance(WAVE_TIMEOUT);
        backend.expireOverdueRequests();

        ReferralView referral = backend.referral(referralId).orElseThrow();
        assertThat(referral.status()).isEqualTo(Referral.ReferralStatus.OPEN);
        assertThat(referral.currentWave()).isEqualTo(2);
        assertThat(referral.secondsToWaveDeadline()).isEqualTo(WAVE_TIMEOUT.toSeconds());
        assertThat(referral.candidates()).filteredOn(c -> Integer.valueOf(1).equals(c.wave()))
                .extracting(CandidateView::requestStatus)
                .containsOnly(ReferralRequest.RequestStatus.EXPIRED);
        assertThat(referral.candidates()).filteredOn(c -> Integer.valueOf(2).equals(c.wave()))
                .extracting(CandidateView::requestStatus)
                .containsOnly(ReferralRequest.RequestStatus.PENDING);
    }

    @Test
    void exhaustedRankingEscalates() {
        Long referralId = backend.createReferral(STROKE);

        clock.advance(WAVE_TIMEOUT);
        backend.expireOverdueRequests();

        ReferralView referral = backend.referral(referralId).orElseThrow();
        assertThat(referral.status()).isEqualTo(Referral.ReferralStatus.ESCALATED);
        assertThat(referral.candidates()).extracting(CandidateView::requestStatus)
                .containsOnly(ReferralRequest.RequestStatus.EXPIRED);
    }

    @Test
    void expiredRequestCanNoLongerBeAccepted() {
        backend.createReferral(STROKE);
        Long requestId = backend.inbox(3L).getFirst().requestId();

        clock.advance(WAVE_TIMEOUT);
        backend.expireOverdueRequests();

        assertThat(backend.accept(3L, requestId)).isEqualTo(DecisionResult.NO_LONGER_PENDING);
    }

    @Test
    void mixedAnswersOnlyExpireTheSilentOnes() {
        Long referralId = backend.createReferral(STROKE);
        backend.decline(3L, backend.inbox(3L).getFirst().requestId(), ReferralRequest.DeclineReason.NO_BEDS);

        clock.advance(WAVE_TIMEOUT);
        backend.expireOverdueRequests();

        ReferralView referral = backend.referral(referralId).orElseThrow();
        assertThat(referral.candidates()).extracting(CandidateView::requestStatus).containsExactly(
                ReferralRequest.RequestStatus.DECLINED,
                ReferralRequest.RequestStatus.EXPIRED,
                ReferralRequest.RequestStatus.EXPIRED);
    }

    @Test
    void acceptedReferralIsLeftAlone() {
        Long referralId = backend.createReferral(STROKE);
        backend.accept(3L, backend.inbox(3L).getFirst().requestId());

        clock.advance(WAVE_TIMEOUT.multipliedBy(5));
        backend.expireOverdueRequests();

        ReferralView referral = backend.referral(referralId).orElseThrow();
        assertThat(referral.status()).isEqualTo(Referral.ReferralStatus.ACCEPTED);
        assertThat(referral.candidates()).extracting(CandidateView::requestStatus).containsExactly(
                ReferralRequest.RequestStatus.ACCEPTED,
                ReferralRequest.RequestStatus.CANCELLED,
                ReferralRequest.RequestStatus.CANCELLED);
    }
}
