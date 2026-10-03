package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.matching.rules.BlockedProcedureRule;
import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockedProcedureRuleTest {

    private final BlockedProcedureRule rule = new BlockedProcedureRule();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void passesWhenNoProceduresRequired() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(HospitalFlag.builder()
                        .type(HospitalFlag.FlagType.TK_DOWN)
                        .validUntil(now.plusHours(1))
                        .build()))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of())
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void passesWhenNoFlagsPresent() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of())
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.CT))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void passesWhenProcedureNotBlocked() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(HospitalFlag.builder()
                        .type(HospitalFlag.FlagType.TK_DOWN)
                        .validUntil(now.plusHours(1))
                        .build()))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.PCI))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void failsWhenProcedureBlockedByFlag() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(HospitalFlag.builder()
                        .type(HospitalFlag.FlagType.TK_DOWN)
                        .validUntil(now.plusHours(1))
                        .build()))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.CT))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("PROCEDURE_BLOCKED_BY_FLAG"));
        assertTrue(violation.get().contains("TK_DOWN"));
        assertTrue(violation.get().contains("CT"));
    }

    @Test
    void failsWhenOneOfMultipleProceduresBlocked() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(HospitalFlag.builder()
                        .type(HospitalFlag.FlagType.CATH_LAB_BUSY)
                        .validUntil(now.plusHours(1))
                        .build()))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.CT, Procedure.PCI, Procedure.MRI))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("PROCEDURE_BLOCKED_BY_FLAG"));
    }

    @Test
    void passesWhenFlagExpired() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(HospitalFlag.builder()
                        .type(HospitalFlag.FlagType.TK_DOWN)
                        .validUntil(now.minusHours(1))
                        .build()))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.CT))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void failsWhenICUFullAndVentilationRequired() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(HospitalFlag.builder()
                        .type(HospitalFlag.FlagType.ICU_FULL)
                        .validUntil(now.plusHours(1))
                        .build()))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.VENTILATION))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("ICU_FULL"));
    }
}
