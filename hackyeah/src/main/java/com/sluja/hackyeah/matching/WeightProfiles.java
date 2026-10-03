package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.referral.entity.Referral;

import java.util.HashMap;
import java.util.Map;

public class WeightProfiles {

    private WeightProfiles() {}

    public static Map<Referral.Urgency, Map<String, Double>> defaults() {
        Map<Referral.Urgency, Map<String, Double>> profiles = new HashMap<>();

        profiles.put(Referral.Urgency.TIME_CRITICAL, Map.of(
                "TRAVEL_TIME", 0.7,
                "ACCEPTANCE_PROBABILITY", 0.15,
                "BED_CAPACITY", 0.15
        ));

        profiles.put(Referral.Urgency.URGENT_STABLE, Map.of(
                "TRAVEL_TIME", 0.4,
                "ACCEPTANCE_PROBABILITY", 0.4,
                "BED_CAPACITY", 0.2
        ));

        profiles.put(Referral.Urgency.PLANNED, Map.of(
                "TRAVEL_TIME", 0.2,
                "ACCEPTANCE_PROBABILITY", 0.6,
                "BED_CAPACITY", 0.2
        ));

        return profiles;
    }
}
