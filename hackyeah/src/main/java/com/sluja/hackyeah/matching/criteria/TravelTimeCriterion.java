package com.sluja.hackyeah.matching.criteria;

import com.sluja.hackyeah.matching.ScoringContext;
import com.sluja.hackyeah.matching.ScoringCriterion;
import com.sluja.hackyeah.matching.TravelTimeProvider;

public class TravelTimeCriterion implements ScoringCriterion {

    private static final double RUSH_HOUR_MULTIPLIER = 1.3;
    private static final double MAX_REASONABLE_MINUTES = 60.0;

    private final TravelTimeProvider travelTimeProvider;

    public TravelTimeCriterion(TravelTimeProvider travelTimeProvider) {
        this.travelTimeProvider = travelTimeProvider;
    }

    @Override
    public String name() {
        return "TRAVEL_TIME";
    }

    @Override
    public double score(ScoringContext context) {
        if (context.referral().getOriginHospitalId() == null) {
            return 0.0;
        }

        var travelMinutes = travelTimeProvider.travelMinutes(
                context.referral().getOriginHospitalId(),
                context.hospital().getId()
        );

        if (travelMinutes.isEmpty()) {
            return 0.0;
        }

        double adjustedMinutes = travelMinutes.get() * RUSH_HOUR_MULTIPLIER;
        double score = 1.0 - (adjustedMinutes / MAX_REASONABLE_MINUTES);
        return Math.max(0.0, Math.min(1.0, score));
    }
}
