package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.matching.rules.BlockingFlagRule;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.hospital.entity.Procedure;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockingFlagRuleTest {

    private final BlockingFlagRule rule = new BlockingFlagRule();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void passesWhenNoFlags() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of())
                .build();
        Referral referral = Referral.builder()
                .requiresIsolation(true)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void passesWhenFlagExpired() {
        HospitalFlag expiredFlag = HospitalFlag.builder()
                .id(1L)
                .type(HospitalFlag.FlagType.ISOLATION_WARD_UNAVAILABLE)
                .validUntil(now.minusHours(1))
                .build();
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(expiredFlag))
                .build();
        Referral referral = Referral.builder()
                .requiresIsolation(true)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void failsWhenIsolationWardUnavailable() {
        HospitalFlag flag = HospitalFlag.builder()
                .id(1L)
                .type(HospitalFlag.FlagType.ISOLATION_WARD_UNAVAILABLE)
                .validUntil(now.plusHours(1))
                .build();
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(flag))
                .build();
        Referral referral = Referral.builder()
                .requiresIsolation(true)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("ISOLATION_WARD_UNAVAILABLE"));
    }

    @Test
    void passesWhenIsolationWardUnavailableButNotRequired() {
        HospitalFlag flag = HospitalFlag.builder()
                .id(1L)
                .type(HospitalFlag.FlagType.ISOLATION_WARD_UNAVAILABLE)
                .validUntil(now.plusHours(1))
                .build();
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(flag))
                .build();
        Referral referral = Referral.builder()
                .requiresIsolation(false)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void failsWhenTKDownAndCTRequired() {
        HospitalFlag flag = HospitalFlag.builder()
                .id(1L)
                .type(HospitalFlag.FlagType.TK_DOWN)
                .validUntil(now.plusHours(1))
                .build();
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(flag))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.CT, Procedure.MRI))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("TK_DOWN"));
    }

    @Test
    void passesWhenTKDownButCTNotRequired() {
        HospitalFlag flag = HospitalFlag.builder()
                .id(1L)
                .type(HospitalFlag.FlagType.TK_DOWN)
                .validUntil(now.plusHours(1))
                .build();
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(flag))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.MRI))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void failsWhenCathLabBusyAndPCIRequired() {
        HospitalFlag flag = HospitalFlag.builder()
                .id(1L)
                .type(HospitalFlag.FlagType.CATH_LAB_BUSY)
                .validUntil(now.plusHours(1))
                .build();
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(flag))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.PCI, Procedure.CT))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("CATH_LAB_BUSY"));
    }

    @Test
    void failsWhenICUFullAndICURequired() {
        HospitalFlag flag = HospitalFlag.builder()
                .id(1L)
                .type(HospitalFlag.FlagType.ICU_FULL)
                .validUntil(now.plusHours(1))
                .build();
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(flag))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.ICU))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("ICU_FULL"));
    }

    @Test
    void failsWhenICUFullAndVentilationRequired() {
        HospitalFlag flag = HospitalFlag.builder()
                .id(1L)
                .type(HospitalFlag.FlagType.ICU_FULL)
                .validUntil(now.plusHours(1))
                .build();
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(flag))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.VENTILATION))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("ICU_FULL"));
    }

    @Test
    void passesWhenNeuroAvailableFlag() {
        HospitalFlag flag = HospitalFlag.builder()
                .id(1L)
                .type(HospitalFlag.FlagType.NEURO_AVAILABLE)
                .validUntil(now.plusHours(1))
                .build();
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .flags(Set.of(flag))
                .build();
        Referral referral = Referral.builder()
                .requiredProcedures(Set.of(Procedure.CT))
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }
}
