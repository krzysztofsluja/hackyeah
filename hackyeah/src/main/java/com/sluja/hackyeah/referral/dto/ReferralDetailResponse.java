package com.sluja.hackyeah.referral.dto;

import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.List;

/**
 * What the sending doctor watches: referral state, every wave sent so far with its answers, and
 * the full ranking including the hospitals that were filtered out and why.
 *
 * <p>{@code contacts} stays empty while the referral is OPEN. On ACCEPTED it holds both sides of
 * the doctor-to-doctor channel; on ESCALATED it holds the on-call numbers for the ranking, which is
 * the point at which phoning round becomes the intended fallback.
 */
public record ReferralDetailResponse(
        Long id,
        Referral.ReferralStatus status,
        LocalDateTime createdAt,
        Long acceptedHospitalId,
        int currentWave,
        List<HospitalCandidate> candidates,
        List<ExcludedHospital> excluded,
        List<RequestView> requests,
        List<HospitalContact> contacts) {}
