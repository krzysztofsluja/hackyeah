package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.rules.TravelRouteExistsRule;
import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelRouteExistsRuleTest {

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void passesWhenRouteExists() {
        TravelTimeProvider provider = (from, to) -> Optional.of(25);
        TravelRouteExistsRule rule = new TravelRouteExistsRule(provider);

        Hospital hospital = Hospital.builder()
                .id(2L)
                .name("Target Hospital")
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .originHospitalId(1L)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void failsWhenRouteDoesNotExist() {
        TravelTimeProvider provider = (from, to) -> Optional.empty();
        TravelRouteExistsRule rule = new TravelRouteExistsRule(provider);

        Hospital hospital = Hospital.builder()
                .id(2L)
                .name("Target Hospital")
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .originHospitalId(1L)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertEquals(Optional.of("NO_TRAVEL_ROUTE"), violation);
    }

    @Test
    void passesWhenOriginHospitalIdNull() {
        TravelTimeProvider provider = (from, to) -> Optional.empty();
        TravelRouteExistsRule rule = new TravelRouteExistsRule(provider);

        Hospital hospital = Hospital.builder()
                .id(2L)
                .name("Target Hospital")
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .originHospitalId(null)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }

    @Test
    void callsProviderWithCorrectIds() {
        TravelTimeProvider provider = new TravelTimeProvider() {
            @Override
            public Optional<Integer> travelMinutes(Long fromHospitalId, Long toHospitalId) {
                assertEquals(1L, fromHospitalId);
                assertEquals(2L, toHospitalId);
                return Optional.of(30);
            }
        };
        TravelRouteExistsRule rule = new TravelRouteExistsRule(provider);

        Hospital hospital = Hospital.builder()
                .id(2L)
                .name("Target Hospital")
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .originHospitalId(1L)
                .build();

        Optional<String> violation = rule.checkViolation(hospital, referral, now);

        assertTrue(violation.isEmpty());
    }
}
