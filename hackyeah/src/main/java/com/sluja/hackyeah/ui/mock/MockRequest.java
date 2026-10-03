package com.sluja.hackyeah.ui.mock;

import com.sluja.hackyeah.referral.entity.ReferralRequest;

import java.time.LocalDateTime;

/** Mutable in-memory request (one hospital asked in one wave). */
class MockRequest {

    final Long id;
    final Long referralId;
    final Long hospitalId;
    final int wave;
    final LocalDateTime sentAt;
    final LocalDateTime deadline;
    ReferralRequest.RequestStatus status = ReferralRequest.RequestStatus.PENDING;
    ReferralRequest.DeclineReason declineReason;

    MockRequest(Long id, Long referralId, Long hospitalId, int wave, LocalDateTime sentAt, LocalDateTime deadline) {
        this.id = id;
        this.referralId = referralId;
        this.hospitalId = hospitalId;
        this.wave = wave;
        this.sentAt = sentAt;
        this.deadline = deadline;
    }
}
