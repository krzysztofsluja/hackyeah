package com.sluja.hackyeah.matching.rules;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.HardConstraintRule;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.Optional;

public class IsolationCapabilityRule implements HardConstraintRule {
    @Override
    public Optional<String> checkViolation(Hospital hospital, Referral referral, LocalDateTime now) {
        if (referral.getRequiresIsolation() && !hospital.isIsolationCapable()) {
            return Optional.of("ISOLATION_NOT_CAPABLE");
        }
        return Optional.empty();
    }
}
