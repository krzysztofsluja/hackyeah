package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.referral.entity.ReferralRequest.DeclineReason;
import com.sluja.hackyeah.referral.entity.ReferralRequest.RequestStatus;
import com.sluja.hackyeah.ui.view.CandidateView;
import com.sluja.hackyeah.ui.view.CoordinatorOverview;
import com.sluja.hackyeah.ui.view.CoordinatorOverview.DeclineStat;
import com.sluja.hackyeah.ui.view.HospitalView;
import com.sluja.hackyeah.ui.view.ReferralView;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CoordinatorOverviewTest {

    private static final List<HospitalView> HOSPITALS = List.of(
            hospital(1L, "Szpital Powiatowy", 60, 41),
            hospital(2L, "Szpital Wojewódzki", 100, 95),
            hospital(3L, "Szpital Uniwersytecki", 100, 72));

    @Test
    void emptyStartShowsOnlyTheOverloadedHospital() {
        CoordinatorOverview overview = CoordinatorOverview.of(HOSPITALS, List.of());

        assertThat(overview.alerts()).extracting(HospitalView::name).containsExactly("Szpital Wojewódzki");
        assertThat(overview.activeReferrals()).isEmpty();
        assertThat(overview.escalations()).isEmpty();
        assertThat(overview.totalDeclines()).isZero();
        assertThat(overview.declines()).extracting(DeclineStat::labelKey).containsExactly(
                "declineReason.NO_BEDS", "declineReason.NO_SPECIALIST", "declineReason.EQUIPMENT_UNAVAILABLE",
                "declineReason.OTHER", "requestStatus.EXPIRED");
    }

    @Test
    void referralsAreSplitIntoActiveAndEscalatedAndAcceptedOnesLeaveBothLists() {
        ReferralView open = referral(1L, Referral.ReferralStatus.OPEN);
        ReferralView escalated = referral(2L, Referral.ReferralStatus.ESCALATED);
        ReferralView accepted = referral(3L, Referral.ReferralStatus.ACCEPTED);

        CoordinatorOverview overview = CoordinatorOverview.of(HOSPITALS, List.of(open, escalated, accepted));

        assertThat(overview.activeReferrals()).extracting(ReferralView::id).containsExactly(1L);
        assertThat(overview.escalations()).extracting(ReferralView::id).containsExactly(2L);
    }

    @Test
    void declinesAreCountedPerReasonWithBarsRelativeToTheLargest() {
        ReferralView escalated = referral(1L, Referral.ReferralStatus.ESCALATED,
                declined(DeclineReason.NO_BEDS), declined(DeclineReason.NO_BEDS), declined(DeclineReason.NO_SPECIALIST));

        CoordinatorOverview overview = CoordinatorOverview.of(HOSPITALS, List.of(escalated));

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
        ReferralView open = referral(1L, Referral.ReferralStatus.OPEN,
                withStatus(RequestStatus.EXPIRED), withStatus(RequestStatus.EXPIRED), withStatus(RequestStatus.PENDING));

        CoordinatorOverview overview = CoordinatorOverview.of(HOSPITALS, List.of(open));

        assertThat(overview.declines()).filteredOn(d -> d.labelKey().equals("requestStatus.EXPIRED"))
                .singleElement()
                .satisfies(d -> assertThat(d.count()).isEqualTo(2));
    }

    private static HospitalView hospital(Long id, String name, int totalBeds, int occupiedBeds) {
        return new HospitalView(id, name, "Dzielnica", 50.0, 20.0, totalBeds, occupiedBeds,
                Set.of("NEUROLOGY"), Set.of(Procedure.CT), false, "+48 12 000 00 0" + id, List.of());
    }

    private static ReferralView referral(Long id, Referral.ReferralStatus status, CandidateView... candidates) {
        return new ReferralView(id, "Szpital Powiatowy", "NEUROLOGY", Set.of(Procedure.CT),
                Referral.Urgency.TIME_CRITICAL, Referral.PatientState.STABLE, false, null, status,
                LocalDateTime.of(2026, 10, 4, 3, 0), 0, 1, 0, null, List.of(candidates), List.of());
    }

    private static CandidateView declined(DeclineReason reason) {
        return candidate(ReferralRequest.RequestStatus.DECLINED, reason);
    }

    private static CandidateView withStatus(RequestStatus status) {
        return candidate(status, null);
    }

    private static CandidateView candidate(RequestStatus status, DeclineReason reason) {
        return new CandidateView(1, 3L, "Szpital Uniwersytecki", "Dzielnica", "+48", 30, 72, 80, 70, 1, status, reason);
    }
}
