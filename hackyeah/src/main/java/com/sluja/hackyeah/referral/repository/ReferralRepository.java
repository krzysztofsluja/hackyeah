package com.sluja.hackyeah.referral.repository;

import com.sluja.hackyeah.referral.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    List<Referral> findByStatus(Referral.ReferralStatus status);

    List<Referral> findAllByOrderByIdDesc();

    /**
     * First accept wins: a conditional update instead of optimistic locking. Returns 1 for the
     * winner and 0 for everyone who arrives after the referral has left OPEN.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Referral r
               set r.status = :accepted,
                   r.acceptedHospitalId = :hospitalId
             where r.id = :id
               and r.status = :open
            """)
    int tryAccept(@Param("id") Long id,
                  @Param("hospitalId") Long hospitalId,
                  @Param("accepted") Referral.ReferralStatus accepted,
                  @Param("open") Referral.ReferralStatus open);

    default int tryAccept(Long id, Long hospitalId) {
        return tryAccept(id, hospitalId,
                Referral.ReferralStatus.ACCEPTED, Referral.ReferralStatus.OPEN);
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Referral r
               set r.status = :escalated
             where r.id = :id
               and r.status = :open
            """)
    int tryEscalate(@Param("id") Long id,
                    @Param("escalated") Referral.ReferralStatus escalated,
                    @Param("open") Referral.ReferralStatus open);

    default int tryEscalate(Long id) {
        return tryEscalate(id,
                Referral.ReferralStatus.ESCALATED, Referral.ReferralStatus.OPEN);
    }
}
