package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.rules.IsolationCapabilityRule;
import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsolationCapabilityRuleTest {

    private final IsolationCapabilityRule rule = new IsolationCapabilityRule();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void passesWhenIsolationNotRequired() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .requiresIsolation(false)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void passesWhenIsolationRequiredAndCapable() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .isolationCapable(true)
                .build();
        Referral referral = Referral.builder()
                .requiresIsolation(true)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void failsWhenIsolationRequiredButNotCapable() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .requiresIsolation(true)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertEquals(Optional.of("ISOLATION_NOT_CAPABLE"), violation);
    }

    @Test
    void passesWhenCapableButNotRequired() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .isolationCapable(true)
                .build();
        Referral referral = Referral.builder()
                .requiresIsolation(false)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }
}
