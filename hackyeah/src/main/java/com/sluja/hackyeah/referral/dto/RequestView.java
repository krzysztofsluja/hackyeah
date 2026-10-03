package com.sluja.hackyeah.referral.dto;

import com.sluja.hackyeah.referral.entity.ReferralRequest;

import java.time.LocalDateTime;

public record RequestView(
        Long requestId,
        Long hospitalId,
        String hospitalName,
        int wave,
        ReferralRequest.RequestStatus status,
        ReferralRequest.DeclineReason declineReason,
        LocalDateTime sentAt,
        LocalDateTime deadline) {}
