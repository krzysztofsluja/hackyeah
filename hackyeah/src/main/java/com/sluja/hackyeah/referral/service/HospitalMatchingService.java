package com.sluja.hackyeah.referral.service;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.repository.HospitalRepository;
import com.sluja.hackyeah.matching.HardConstraintFilter;
import com.sluja.hackyeah.matching.HospitalAcceptanceStats;
import com.sluja.hackyeah.matching.HospitalEligibility;
import com.sluja.hackyeah.matching.HospitalScore;
import com.sluja.hackyeah.matching.MatchResult;
import com.sluja.hackyeah.matching.TravelTimeProvider;
import com.sluja.hackyeah.matching.WeightedScorer;
import com.sluja.hackyeah.referral.entity.Referral;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class HospitalMatchingService {
    private final HospitalRepository hospitalRepository;
    private final HospitalAcceptanceStatsService acceptanceStatsService;
    private final HardConstraintFilter hardConstraintFilter;
    private final WeightedScorer weightedScorer;

    public HospitalMatchingService(HospitalRepository hospitalRepository,
                                    HospitalAcceptanceStatsService acceptanceStatsService,
                                    TravelTimeProvider travelTimeProvider) {
        this.hospitalRepository = hospitalRepository;
        this.acceptanceStatsService = acceptanceStatsService;
        this.hardConstraintFilter = HardConstraintFilter.standard(travelTimeProvider);
        this.weightedScorer = WeightedScorer.standard(travelTimeProvider);
    }

    public List<HospitalScore> findRankedHospitals(Referral referral) {
        return findRankedHospitals(referral, LocalDateTime.now());
    }

    public List<HospitalScore> findRankedHospitals(Referral referral, LocalDateTime now) {
        return match(referral, now).ranked();
    }

    public MatchResult match(Referral referral) {
        return match(referral, LocalDateTime.now());
    }

    /**
     * Runs the hard-constraint rules once and keeps both halves: the eligible hospitals ranked by
     * the weighted score, and the rejected ones together with the violation codes that dropped them.
     */
    public MatchResult match(Referral referral, LocalDateTime now) {
        List<Hospital> allHospitals = hospitalRepository.findAll();

        List<HospitalEligibility> evaluations = hardConstraintFilter.evaluate(referral, allHospitals, now);

        List<Hospital> eligibleHospitals = evaluations.stream()
                .filter(HospitalEligibility::eligible)
                .map(HospitalEligibility::hospital)
                .toList();
        List<HospitalEligibility> excluded = evaluations.stream()
                .filter(evaluation -> !evaluation.eligible())
                .toList();

        List<Long> eligibleIds = eligibleHospitals.stream().map(Hospital::getId).toList();
        Map<Long, HospitalAcceptanceStats> stats = acceptanceStatsService.getStats(eligibleIds);

        return new MatchResult(weightedScorer.score(referral, eligibleHospitals, stats, now), excluded);
    }
}
