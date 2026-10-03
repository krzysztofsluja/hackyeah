package com.sluja.hackyeah.referral.repository;

import com.sluja.hackyeah.referral.entity.ReferralCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferralCandidateRepository extends JpaRepository<ReferralCandidate, Long> {

    List<ReferralCandidate> findByReferralIdAndEligibleTrueOrderByRankAsc(Long referralId);

    List<ReferralCandidate> findByReferralIdOrderByRankAsc(Long referralId);
}
