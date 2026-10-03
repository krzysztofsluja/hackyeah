package com.sluja.hackyeah.referral.service;

import com.sluja.hackyeah.matching.HospitalAcceptanceStats;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.referral.repository.ReferralRequestRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HospitalAcceptanceStatsService {
    private final ReferralRequestRepository referralRequestRepository;

    public HospitalAcceptanceStatsService(ReferralRequestRepository referralRequestRepository) {
        this.referralRequestRepository = referralRequestRepository;
    }

    public Map<Long, HospitalAcceptanceStats> getStats(List<Long> hospitalIds) {
        Map<Long, HospitalAcceptanceStats> stats = new HashMap<>();

        for (Long hospitalId : hospitalIds) {
            long acceptedCount = referralRequestRepository.countByHospitalIdAndStatus(hospitalId, ReferralRequest.RequestStatus.ACCEPTED);
            long declinedCount = referralRequestRepository.countByHospitalIdAndStatus(hospitalId, ReferralRequest.RequestStatus.DECLINED);
            long expiredCount = referralRequestRepository.countByHospitalIdAndStatus(hospitalId, ReferralRequest.RequestStatus.EXPIRED);

            long respondedCount = acceptedCount + declinedCount + expiredCount;

            stats.put(hospitalId, new HospitalAcceptanceStats(hospitalId, acceptedCount, respondedCount));
        }

        return stats;
    }
}
