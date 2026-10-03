package com.sluja.hackyeah.matching.rules;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.HardConstraintRule;
import com.sluja.hackyeah.matching.TravelTimeProvider;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.Optional;

public class TravelRouteExistsRule implements HardConstraintRule {
    private final TravelTimeProvider travelTimeProvider;

    public TravelRouteExistsRule(TravelTimeProvider travelTimeProvider) {
        this.travelTimeProvider = travelTimeProvider;
    }

    @Override
    public Optional<String> checkViolation(Hospital hospital, Referral referral, LocalDateTime now) {
        if (referral.getOriginHospitalId() == null) {
            return Optional.empty();
        }

        Optional<Integer> travelMinutes = travelTimeProvider.travelMinutes(referral.getOriginHospitalId(), hospital.getId());
        if (travelMinutes.isEmpty()) {
            return Optional.of("NO_TRAVEL_ROUTE");
        }

        return Optional.empty();
    }
}
