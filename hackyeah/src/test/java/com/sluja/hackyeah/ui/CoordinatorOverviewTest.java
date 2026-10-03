package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.ui.mock.MockDemoBackend;
import com.sluja.hackyeah.ui.view.CoordinatorOverview;
import com.sluja.hackyeah.ui.view.CoordinatorOverview.DeclineStat;
import com.sluja.hackyeah.ui.view.HospitalView;
import com.sluja.hackyeah.ui.view.NewReferral;
import com.sluja.hackyeah.ui.view.ReferralView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CoordinatorOverviewTest {

    private static final NewReferral STROKE = new NewReferral("NEUROLOGY", Set.of(Procedure.CT, Procedure.THROMBECTOMY),
            Referral.Urgency.TIME_CRITICAL, Referral.PatientState.STABLE, false, null);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-04T01:00:00Z"), ZoneId.of("Europe/Warsaw"));
    private MockDemoBackend backend;

    @BeforeEach
    void setUp() {
        backend = new MockDemoBackend(clock, DemoProperties.defaults());
    }

    @Test
    void emptyStartShowsOnlyTheOverloadedHospital() {
        CoordinatorOverview overview = overview();

        assertThat(overview.alerts()).extracting(HospitalView::name).containsExactly("Szpital Wojewódzki");
        assertThat(overview.activeReferrals()).isEmpty();
        assertThat(overview.escalations()).isEmpty();
        assertThat(overview.totalDeclines()).isZero();
        assertThat(overview.declines()).extracting(DeclineStat::labelKey).containsExactly(
                "declineReason.NO_BEDS", "declineReason.NO_SPECIALIST", "declineReason.EQUIPMENT_UNAVAILABLE",
                "declineReason.OTHER", "requestStatus.EXPIRED");
    }

    @Test
    void openReferralIsActiveUntilEscalated() {
        Long open = backend.createReferral(STROKE);
        assertThat(overview().activeReferrals()).extracting(ReferralView::id).containsExactly(open);

        backend.decline(3L, backend.inbox(3L).getFirst().requestId(), ReferralRequest.DeclineReason.NO_BEDS);
        backend.decline(7L, backend.inbox(7L).getFirst().requestId(), ReferralRequest.DeclineReason.NO_BEDS);
        backend.decline(4L, backend.inbox(4L).getFirst().requestId(), ReferralRequest.DeclineReason.NO_SPECIALIST);

        CoordinatorOverview overview = overview();
        assertThat(overview.activeReferrals()).isEmpty();
        assertThat(overview.escalations()).extracting(ReferralView::id).containsExactly(open);
        assertThat(overview.totalDeclines()).isEqualTo(3);
        assertThat(overview.declines()).filteredOn(d -> d.labelKey().equals("declineReason.NO_BEDS"))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.count()).isEqualTo(2);
                    assertThat(d.barPercent()).isEqualTo(100);
                });
        assertThat(overview.declines()).filteredOn(d -> d.labelKey().equals("declineReason.NO_SPECIALIST"))
                .singleElement()
                .satisfies(d -> assertThat(d.barPercent()).isEqualTo(50));
    }

    @Test
    void timeoutsAreCountedSeparately() {
        backend.createReferral(STROKE);

        clock.advance(DemoProperties.defaults().waveTimeoutFor(Referral.Urgency.TIME_CRITICAL));
        backend.expireOverdueRequests();

        assertThat(overview().declines()).filteredOn(d -> d.labelKey().equals("requestStatus.EXPIRED"))
                .singleElement()
                .satisfies(d -> assertThat(d.count()).isEqualTo(3));
    }

    @Test
    void acceptedReferralLeavesBothLists() {
        backend.createReferral(STROKE);
        backend.accept(3L, backend.inbox(3L).getFirst().requestId());

        CoordinatorOverview overview = overview();
        assertThat(overview.activeReferrals()).isEmpty();
        assertThat(overview.escalations()).isEmpty();
    }

    private CoordinatorOverview overview() {
        return CoordinatorOverview.of(backend.hospitals(), backend.referrals());
    }
}
