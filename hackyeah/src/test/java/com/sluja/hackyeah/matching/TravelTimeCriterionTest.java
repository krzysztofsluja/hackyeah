package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.criteria.TravelTimeCriterion;
import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TravelTimeCriterionTest {

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void scoresBasedOnTravelTime() {
        TravelTimeProvider provider = (from, to) -> Optional.of(20);
        TravelTimeCriterion criterion = new TravelTimeCriterion(provider);

        Hospital hospital = Hospital.builder()
                .id(2L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .originHospitalId(1L)
                .build();
        HospitalAcceptanceStats stats = HospitalAcceptanceStats.empty(2L);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        double adjustedMinutes = 20 * 1.3;
        double expected = 1.0 - (adjustedMinutes / 60.0);
        assertEquals(expected, score, 0.001);
    }

    @Test
    void scoresNearOneForShortTrip() {
        TravelTimeProvider provider = (from, to) -> Optional.of(5);
        TravelTimeCriterion criterion = new TravelTimeCriterion(provider);

        Hospital hospital = Hospital.builder()
                .id(2L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .originHospitalId(1L)
                .build();
        HospitalAcceptanceStats stats = HospitalAcceptanceStats.empty(2L);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        double adjustedMinutes = 5 * 1.3;
        double expected = 1.0 - (adjustedMinutes / 60.0);
        assertEquals(expected, score, 0.001);
        assertTrue(score > 0.8, "Short trip should score high");
    }

    @Test
    void scoresZeroForVeryLongTrip() {
        TravelTimeProvider provider = (from, to) -> Optional.of(100);
        TravelTimeCriterion criterion = new TravelTimeCriterion(provider);

        Hospital hospital = Hospital.builder()
                .id(2L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .originHospitalId(1L)
                .build();
        HospitalAcceptanceStats stats = HospitalAcceptanceStats.empty(2L);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        assertEquals(0.0, score, 0.001);
    }

    @Test
    void returnZeroWhenRouteIsMissing() {
        TravelTimeProvider provider = (from, to) -> Optional.empty();
        TravelTimeCriterion criterion = new TravelTimeCriterion(provider);

        Hospital hospital = Hospital.builder()
                .id(2L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .originHospitalId(1L)
                .build();
        HospitalAcceptanceStats stats = HospitalAcceptanceStats.empty(2L);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        assertEquals(0.0, score, 0.001);
    }

    @Test
    void returnZeroWhenOriginHospitalIdNull() {
        TravelTimeProvider provider = (from, to) -> Optional.of(20);
        TravelTimeCriterion criterion = new TravelTimeCriterion(provider);

        Hospital hospital = Hospital.builder()
                .id(2L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .originHospitalId(null)
                .build();
        HospitalAcceptanceStats stats = HospitalAcceptanceStats.empty(2L);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        assertEquals(0.0, score, 0.001);
    }

    @Test
    void clampsNegativeToZero() {
        TravelTimeProvider provider = (from, to) -> Optional.of(60);
        TravelTimeCriterion criterion = new TravelTimeCriterion(provider);

        Hospital hospital = Hospital.builder()
                .id(2L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder()
                .originHospitalId(1L)
                .build();
        HospitalAcceptanceStats stats = HospitalAcceptanceStats.empty(2L);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        assertEquals(0.0, score, 0.001);
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
