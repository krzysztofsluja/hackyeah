package com.sluja.hackyeah.matching.rules;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.matching.HardConstraintRule;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public class ProcedureCoverageRule implements HardConstraintRule {
    @Override
    public Optional<String> checkViolation(Hospital hospital, Referral referral, LocalDateTime now) {
        if (referral.getRequiredProcedures() == null || referral.getRequiredProcedures().isEmpty()) {
            return Optional.empty();
        }

        Set<String> missing = referral.getRequiredProcedures().stream()
                .filter(proc -> !hospital.getProcedures().contains(proc))
                .map(Procedure::name)
                .collect(java.util.stream.Collectors.toSet());

        if (!missing.isEmpty()) {
            return Optional.of("PROCEDURES_NOT_COVERED:" + missing);
        }
        return Optional.empty();
    }
}
