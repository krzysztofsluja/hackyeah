package com.sluja.hackyeah.referral.dto;

import com.sluja.hackyeah.referral.entity.ReferralRequest;
import jakarta.validation.constraints.NotNull;

/** A decline must say why - the reasons feed the ranking and the coordinator dashboard. */
public record DeclineRequest(@NotNull ReferralRequest.DeclineReason reason) {}
