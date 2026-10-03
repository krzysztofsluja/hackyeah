package com.sluja.hackyeah.ui.view;

import com.sluja.hackyeah.referral.entity.ReferralRequest;

/**
 * One row of the ranking (higher score = better match), together with the state of the request
 * sent to this hospital, if any.
 */
public record CandidateView(
        int rank,
        Long hospitalId,
        String name,
        String district,
        String dutyPhone,
        Integer travelMinutes,
        int occupancyPercent,
        int acceptancePercent,
        int scorePercent,
        Integer wave,
        ReferralRequest.RequestStatus requestStatus,
        ReferralRequest.DeclineReason declineReason
) {

    public boolean contacted() {
        return requestStatus != null;
    }

    public OccupancyLevel occupancyLevel() {
        return OccupancyLevel.of(occupancyPercent);
    }
}
