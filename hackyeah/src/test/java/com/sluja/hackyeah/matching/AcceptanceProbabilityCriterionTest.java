package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.criteria.AcceptanceProbabilityCriterion;
import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcceptanceProbabilityCriterionTest {

    private final AcceptanceProbabilityCriterion criterion = new AcceptanceProbabilityCriterion();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void returnsSevenTenthsWhenNoHistory() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        HospitalAcceptanceStats stats = HospitalAcceptanceStats.empty(1L);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        assertEquals(0.7, score, 0.001);
    }

    @Test
    void convergesTo1WhenAllAccepted() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        HospitalAcceptanceStats stats = new HospitalAcceptanceStats(1L, 10, 10);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        assertEquals(1.0, score, 0.001);
    }

    @Test
    void convergesTo0WhenAllDeclined() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        HospitalAcceptanceStats stats = new HospitalAcceptanceStats(1L, 0, 10);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        assertEquals(0.375, score, 0.001);
    }

    @Test
    void blendsPriorWithHistory() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        HospitalAcceptanceStats stats = new HospitalAcceptanceStats(1L, 5, 10);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        double expected = (5.0 + 7.0) / (10.0 + 10.0);
        assertEquals(expected, score, 0.001);
    }

    @Test
    void clampsBoundaries() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        HospitalAcceptanceStats stats = new HospitalAcceptanceStats(1L, 100, 100);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        assertEquals(1.0, score, 0.001);
    }

    @Test
    void largeHistoryShrinksPriorInfluence() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        HospitalAcceptanceStats stats = new HospitalAcceptanceStats(1L, 70, 100);
        ScoringContext context = new ScoringContext(hospital, referral, stats, now);

        double score = criterion.score(context);

        double expected = (70.0 + 7.0) / (100.0 + 10.0);
        assertEquals(expected, score, 0.001);
    }
}
