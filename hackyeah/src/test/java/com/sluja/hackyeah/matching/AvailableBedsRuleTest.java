package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.rules.AvailableBedsRule;
import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailableBedsRuleTest {

    private final AvailableBedsRule rule = new AvailableBedsRule();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void passeWhenBedsAvailable() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void failsWhenNoBeds() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(100)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertEquals(Optional.of("NO_AVAILABLE_BEDS"), violation);
    }

    @Test
    void failsWhenBedsExactlyZero() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(50)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertEquals(Optional.of("NO_AVAILABLE_BEDS"), violation);
    }

    @Test
    void passesWhenOneBedsAvailable() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(99)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }
}
