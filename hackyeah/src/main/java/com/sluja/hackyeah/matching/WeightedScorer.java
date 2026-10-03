package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.criteria.AcceptanceProbabilityCriterion;
import com.sluja.hackyeah.matching.criteria.BedCapacityCriterion;
import com.sluja.hackyeah.matching.criteria.TravelTimeCriterion;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeightedScorer {
    private final List<ScoringCriterion> criteria;
    private final Map<Referral.Urgency, Map<String, Double>> weightsByUrgency;

    public WeightedScorer(List<ScoringCriterion> criteria, Map<Referral.Urgency, Map<String, Double>> weightsByUrgency) {
        this.criteria = criteria;
        this.weightsByUrgency = weightsByUrgency;
    }

    public static WeightedScorer standard(TravelTimeProvider travelTimeProvider) {
        return new WeightedScorer(
                List.of(
                        new BedCapacityCriterion(),
                        new AcceptanceProbabilityCriterion(),
                        new TravelTimeCriterion(travelTimeProvider)
                ),
                WeightProfiles.defaults()
        );
    }

    public List<HospitalScore> score(Referral referral, List<Hospital> hospitals,
                                      Map<Long, HospitalAcceptanceStats> statsByHospitalId, LocalDateTime now) {
        Map<String, Double> weights = weightsByUrgency.getOrDefault(referral.getUrgency(), weightsByUrgency.get(Referral.Urgency.PLANNED));

        return hospitals.stream()
                .map(hospital -> scoreHospital(hospital, referral, statsByHospitalId, weights, now))
                .sorted((a, b) -> Double.compare(b.totalScore(), a.totalScore()))
                .collect(Collectors.toList());
    }

    private HospitalScore scoreHospital(Hospital hospital, Referral referral,
                                        Map<Long, HospitalAcceptanceStats> statsByHospitalId,
                                        Map<String, Double> weights, LocalDateTime now) {
        HospitalAcceptanceStats stats = statsByHospitalId.getOrDefault(hospital.getId(), HospitalAcceptanceStats.empty(hospital.getId()));
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        Map<String, Double> criterionScores = new HashMap<>();
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (ScoringCriterion criterion : criteria) {
            double criterionScore = criterion.score(context);
            String criterionName = criterion.name();
            criterionScores.put(criterionName, criterionScore);

            Double weight = weights.getOrDefault(criterionName, 0.0);
            weightedSum += criterionScore * weight;
            totalWeight += weight;
        }

        double totalScore = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
        return new HospitalScore(hospital, totalScore, criterionScores);
    }
}
