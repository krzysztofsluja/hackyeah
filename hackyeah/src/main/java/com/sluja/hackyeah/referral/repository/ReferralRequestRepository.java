package com.sluja.hackyeah.referral.repository;

import com.sluja.hackyeah.referral.entity.ReferralRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReferralRequestRepository extends JpaRepository<ReferralRequest, Long> {

    long countByHospitalIdAndStatus(Long hospitalId, ReferralRequest.RequestStatus status);

    List<ReferralRequest> findByReferralIdOrderByWaveAscIdAsc(Long referralId);

    List<ReferralRequest> findByReferralIdAndStatus(Long referralId, ReferralRequest.RequestStatus status);

    List<ReferralRequest> findByHospitalIdAndStatusOrderBySentAtDesc(Long hospitalId,
                                                                    ReferralRequest.RequestStatus status);

    /** Everything ever sent to the hospital, answered or not - the inbox keeps its history visible. */
    List<ReferralRequest> findByHospitalIdOrderByIdDesc(Long hospitalId);

    boolean existsByReferralIdAndStatus(Long referralId, ReferralRequest.RequestStatus status);

    @Query("select coalesce(max(r.wave), 0) from ReferralRequest r where r.referral.id = :referralId")
    int findHighestWave(@Param("referralId") Long referralId);

    /** No answer by the deadline counts as a decline, so the wave can close and the next go out. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ReferralRequest r
               set r.status = :expired
             where r.status = :pending
               and r.deadline < :now
            """)
    int expireOverdue(@Param("now") LocalDateTime now,
                      @Param("expired") ReferralRequest.RequestStatus expired,
                      @Param("pending") ReferralRequest.RequestStatus pending);

    default int expireOverdue(LocalDateTime now) {
        return expireOverdue(now,
                ReferralRequest.RequestStatus.EXPIRED, ReferralRequest.RequestStatus.PENDING);
    }

    /** Called by the winning accept: everyone still waiting is told the referral is already placed. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ReferralRequest r
               set r.status = :cancelled
             where r.referral.id = :referralId
               and r.status = :pending
               and r.id <> :winnerId
            """)
    int cancelLosers(@Param("referralId") Long referralId,
                     @Param("winnerId") Long winnerId,
                     @Param("cancelled") ReferralRequest.RequestStatus cancelled,
                     @Param("pending") ReferralRequest.RequestStatus pending);

    default int cancelLosers(Long referralId, Long winnerId) {
        return cancelLosers(referralId, winnerId,
                ReferralRequest.RequestStatus.CANCELLED, ReferralRequest.RequestStatus.PENDING);
    }
}
