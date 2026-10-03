package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.rules.SpecialtyMatchRule;
import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialtyMatchRuleTest {

    private final SpecialtyMatchRule rule = new SpecialtyMatchRule();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void passesWhenSpecialtyMatches() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .specialties(Set.of("NEUROLOGY", "CARDIOLOGY"))
                .build();
        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void failsWhenSpecialtyDoesNotMatch() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .specialties(Set.of("CARDIOLOGY", "ORTHOPEDICS"))
                .build();
        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertEquals(Optional.of("SPECIALTY_NOT_COVERED"), violation);
    }

    @Test
    void failsWhenHospitalHasNoSpecialties() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .specialties(Set.of())
                .build();
        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertEquals(Optional.of("SPECIALTY_NOT_COVERED"), violation);
    }

    @Test
    void passesWhenMultipleSpecialties() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .specialties(Set.of("INTERNAL_MEDICINE", "GENERAL_SURGERY", "NEUROLOGY", "CARDIOLOGY"))
                .build();
        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }
}
