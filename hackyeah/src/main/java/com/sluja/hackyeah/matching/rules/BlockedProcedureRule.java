package com.sluja.hackyeah.matching.rules;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.matching.HardConstraintRule;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.Optional;

public class BlockedProcedureRule implements HardConstraintRule {

    @Override
    public Optional<String> checkViolation(Hospital hospital, Referral referral, LocalDateTime now) {
        if (hospital.getFlags() == null || hospital.getFlags().isEmpty()) {
            return Optional.empty();
        }

        if (referral.getRequiredProcedures() == null || referral.getRequiredProcedures().isEmpty()) {
            return Optional.empty();
        }

        for (HospitalFlag flag : hospital.getFlags()) {
            if (flag.isValid(now)) {
                for (var blockedProcedure : flag.getType().getBlockedProcedures()) {
                    if (referral.getRequiredProcedures().contains(blockedProcedure)) {
                        return Optional.of("PROCEDURE_BLOCKED_BY_FLAG:" + flag.getType() +
                                          ":blocked_procedure=" + blockedProcedure);
                    }
                }
            }
        }

        return Optional.empty();
    }
}
