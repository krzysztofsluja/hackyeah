package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.matching.rules.ProcedureCoverageRule;
import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProcedureCoverageRuleTest {

    private final ProcedureCoverageRule rule = new ProcedureCoverageRule();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void passesWhenNoProceduresRequired() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .procedures(Set.of(Procedure.CT, Procedure.MRI))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of())
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void passesWhenAllProceduresAvailable() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .procedures(Set.of(Procedure.CT, Procedure.MRI, Procedure.THROMBOLYSIS, Procedure.THROMBECTOMY))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.CT, Procedure.THROMBOLYSIS))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void failsWhenOneProcedureMissing() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .procedures(Set.of(Procedure.CT, Procedure.THROMBOLYSIS))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.CT, Procedure.THROMBOLYSIS, Procedure.THROMBECTOMY))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("PROCEDURES_NOT_COVERED"));
        assertTrue(violation.get().contains("THROMBECTOMY"));
    }

    @Test
    void failsWhenMultipleProceduresMissing() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .procedures(Set.of(Procedure.CT))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.CT, Procedure.MRI, Procedure.THROMBOLYSIS))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("PROCEDURES_NOT_COVERED"));
        assertTrue(violation.get().contains("MRI"));
        assertTrue(violation.get().contains("THROMBOLYSIS"));
    }

    @Test
    void failsWhenAllProceduresMissing() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .procedures(Set.of())
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.CT, Procedure.MRI))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("PROCEDURES_NOT_COVERED"));
    }

    @Test
    void passesWhenNullProcedures() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .procedures(Set.of())
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(null)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }
}
