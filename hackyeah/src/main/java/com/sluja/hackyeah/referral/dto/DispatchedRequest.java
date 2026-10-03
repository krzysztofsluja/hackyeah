package com.sluja.hackyeah.referral.dto;

import java.time.LocalDateTime;

/** A request just sent out as part of a wave. */
public record DispatchedRequest(
        Long requestId,
        Long hospitalId,
        String hospitalName,
        int wave,
        LocalDateTime deadline) {}
