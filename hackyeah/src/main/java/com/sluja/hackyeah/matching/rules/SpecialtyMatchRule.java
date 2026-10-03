package com.sluja.hackyeah.matching.rules;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.HardConstraintRule;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.Optional;

public class SpecialtyMatchRule implements HardConstraintRule {
    @Override
    public Optional<String> checkViolation(Hospital hospital, Referral referral, LocalDateTime now) {
        if (!hospital.getSpecialties().contains(referral.getTargetSpecialty())) {
            return Optional.of("SPECIALTY_NOT_COVERED");
        }
        return Optional.empty();
    }
}
