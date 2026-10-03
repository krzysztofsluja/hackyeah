package com.sluja.hackyeah.ui.mock;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.HardConstraintFilter;
import com.sluja.hackyeah.matching.HospitalAcceptanceStats;
import com.sluja.hackyeah.matching.HospitalEligibility;
import com.sluja.hackyeah.matching.HospitalScore;
import com.sluja.hackyeah.matching.TravelTimeProvider;
import com.sluja.hackyeah.matching.WeightedScorer;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Runs the real matching algorithm (hard filter + weighted score) on the mock's data. */
final class MockMatcher {

    record Ranking(List<HospitalScore> ranked, List<HospitalEligibility> excluded) {}

    private final TravelTimeProvider travelTimeProvider;
    private final HardConstraintFilter filter;
    private final WeightedScorer scorer;

    MockMatcher(Long originHospitalId, Map<Long, Integer> travelMinutesFromOrigin) {
        this.travelTimeProvider = (from, to) -> originHospitalId.equals(from)
                ? Optional.ofNullable(travelMinutesFromOrigin.get(to))
                : Optional.empty();
        this.filter = HardConstraintFilter.standard(travelTimeProvider);
        this.scorer = WeightedScorer.standard(travelTimeProvider);
    }

    Optional<Integer> travelMinutes(Long from, Long to) {
        return travelTimeProvider.travelMinutes(from, to);
    }

    Ranking rank(Referral referral, List<Hospital> hospitals, Map<Long, HospitalAcceptanceStats> stats, LocalDateTime now) {
        List<HospitalEligibility> evaluated = filter.evaluate(referral, hospitals, now);
        List<Hospital> eligible = evaluated.stream().filter(HospitalEligibility::eligible).map(HospitalEligibility::hospital).toList();
        List<HospitalEligibility> excluded = evaluated.stream().filter(e -> !e.eligible()).toList();
        return new Ranking(scorer.score(referral, eligible, stats, now), excluded);
    }
}
