package com.sluja.hackyeah.referral.repository;

import com.sluja.hackyeah.referral.entity.ReferralRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRequestRepository extends JpaRepository<ReferralRequest, Long> {
    long countByHospitalIdAndStatus(Long hospitalId, ReferralRequest.RequestStatus status);
}
