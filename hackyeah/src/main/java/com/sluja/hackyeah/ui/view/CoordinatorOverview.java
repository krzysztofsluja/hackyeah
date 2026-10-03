package com.sluja.hackyeah.ui.view;

import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Everything the coordinator dashboard shows next to the map. */
public record CoordinatorOverview(
        List<HospitalView> alerts,
        List<ReferralView> activeReferrals,
        List<ReferralView> escalations,
        List<DeclineStat> declines,
        long totalDeclines
) {

    /** One bar of the decline counter; {@code barPercent} is relative to the largest bar. */
    public record DeclineStat(String labelKey, long count, int barPercent) {}

    public static CoordinatorOverview of(List<HospitalView> hospitals, List<ReferralView> referrals) {
        List<HospitalView> alerts = hospitals.stream()
                .filter(h -> h.occupancyLevel() == OccupancyLevel.HIGH)
                .toList();
        List<ReferralView> active = referrals.stream()
                .filter(r -> r.status() == Referral.ReferralStatus.OPEN)
                .toList();
        List<ReferralView> escalations = referrals.stream()
                .filter(r -> r.status() == Referral.ReferralStatus.ESCALATED)
                .toList();

        Map<ReferralRequest.DeclineReason, Long> byReason = new EnumMap<>(ReferralRequest.DeclineReason.class);
        for (ReferralRequest.DeclineReason reason : ReferralRequest.DeclineReason.values()) {
            byReason.put(reason, 0L);
        }
        long expired = 0;
        for (ReferralView referral : referrals) {
            for (CandidateView candidate : referral.candidates()) {
                if (candidate.declineReason() != null) {
                    byReason.merge(candidate.declineReason(), 1L, Long::sum);
                } else if (candidate.requestStatus() == ReferralRequest.RequestStatus.EXPIRED) {
                    expired++;
                }
            }
        }

        long max = Math.max(expired, byReason.values().stream().mapToLong(Long::longValue).max().orElse(0));
        List<DeclineStat> declines = new ArrayList<>();
        byReason.forEach((reason, count) -> declines.add(stat("declineReason." + reason, count, max)));
        // Silence counts as a refusal too (timeout = automatic decline), shown separately.
        declines.add(stat("requestStatus.EXPIRED", expired, max));
        long total = declines.stream().mapToLong(DeclineStat::count).sum();

        return new CoordinatorOverview(alerts, active, escalations, declines, total);
    }

    private static DeclineStat stat(String labelKey, long count, long max) {
        return new DeclineStat(labelKey, count, max == 0 ? 0 : (int) Math.round(count * 100.0 / max));
    }
}
