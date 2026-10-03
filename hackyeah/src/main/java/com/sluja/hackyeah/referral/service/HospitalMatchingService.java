package com.sluja.hackyeah.referral.service;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.repository.HospitalRepository;
import com.sluja.hackyeah.matching.HardConstraintFilter;
import com.sluja.hackyeah.matching.HospitalAcceptanceStats;
import com.sluja.hackyeah.matching.HospitalScore;
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
        List<Hospital> allHospitals = hospitalRepository.findAll();

        List<Hospital> eligibleHospitals = hardConstraintFilter.filterEligible(referral, allHospitals, now);

        List<Long> eligibleIds = eligibleHospitals.stream().map(Hospital::getId).toList();
        Map<Long, HospitalAcceptanceStats> stats = acceptanceStatsService.getStats(eligibleIds);

        return weightedScorer.score(referral, eligibleHospitals, stats, now);
    }
}
