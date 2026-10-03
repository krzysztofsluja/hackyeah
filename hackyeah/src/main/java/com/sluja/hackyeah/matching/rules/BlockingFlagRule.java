package com.sluja.hackyeah.matching.rules;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.matching.HardConstraintRule;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Predicate;

public class BlockingFlagRule implements HardConstraintRule {

    private static final java.util.Map<HospitalFlag.FlagType, Predicate<Referral>> BLOCKING_FLAGS =
            java.util.Map.ofEntries(
                    java.util.Map.entry(
                            HospitalFlag.FlagType.ISOLATION_WARD_UNAVAILABLE,
                            ref -> ref.getRequiresIsolation()
                    ),
                    java.util.Map.entry(
                            HospitalFlag.FlagType.TK_DOWN,
                            ref -> ref.getRequiredProcedures() != null &&
                                    ref.getRequiredProcedures().contains(Procedure.CT)
                    ),
                    java.util.Map.entry(
                            HospitalFlag.FlagType.CATH_LAB_BUSY,
                            ref -> ref.getRequiredProcedures() != null &&
                                    ref.getRequiredProcedures().contains(Procedure.PCI)
                    ),
                    java.util.Map.entry(
                            HospitalFlag.FlagType.ICU_FULL,
                            ref -> ref.getRequiredProcedures() != null &&
                                    (ref.getRequiredProcedures().contains(Procedure.ICU) ||
                                            ref.getRequiredProcedures().contains(Procedure.VENTILATION))
                    )
            );

    @Override
    public Optional<String> checkViolation(Hospital hospital, Referral referral, LocalDateTime now) {
        if (hospital.getFlags() == null || hospital.getFlags().isEmpty()) {
            return Optional.empty();
        }

        for (HospitalFlag flag : hospital.getFlags()) {
            if (flag.isValid(now)) {
                Predicate<Referral> blocker = BLOCKING_FLAGS.get(flag.getType());
                if (blocker != null && blocker.test(referral)) {
                    return Optional.of("BLOCKED_BY_FLAG:" + flag.getType());
                }
            }
        }

        return Optional.empty();
    }
}
