package com.sluja.hackyeah.referral.dto;

import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.List;

public record ReferralCreatedResponse(
        Long id,
        Referral.ReferralStatus status,
        LocalDateTime createdAt,
        int wave,
        List<DispatchedRequest> requests,
        List<HospitalCandidate> candidates,
        List<ExcludedHospital> excluded) {}
