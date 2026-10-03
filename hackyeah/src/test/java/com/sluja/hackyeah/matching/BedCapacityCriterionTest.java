package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.criteria.BedCapacityCriterion;
import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedCapacityCriterionTest {

    private final BedCapacityCriterion criterion = new BedCapacityCriterion();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void returnsOneWhenFullCapacity() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Full Hospital")
                .totalBeds(100)
                .occupiedBeds(0)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        ScoringContext context = new ScoringContext(hospital, referral, HospitalAcceptanceStats.empty(1L), now);

        double score = criterion.score(context);

        assertEquals(1.0, score, 0.001);
    }

    @Test
    void returnsZeroWhenFull() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Full Hospital")
                .totalBeds(100)
                .occupiedBeds(100)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        ScoringContext context = new ScoringContext(hospital, referral, HospitalAcceptanceStats.empty(1L), now);

        double score = criterion.score(context);

        assertEquals(0.0, score, 0.001);
    }

    @Test
    void returnsHalfWhenHalfOccupied() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Half Full Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        ScoringContext context = new ScoringContext(hospital, referral, HospitalAcceptanceStats.empty(1L), now);

        double score = criterion.score(context);

        assertEquals(0.5, score, 0.001);
    }

    @Test
    void returnsThreeQuartersWhenQuarterOccupied() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Quarter Full Hospital")
                .totalBeds(100)
                .occupiedBeds(25)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        ScoringContext context = new ScoringContext(hospital, referral, HospitalAcceptanceStats.empty(1L), now);

        double score = criterion.score(context);

        assertEquals(0.75, score, 0.001);
    }

    @Test
    void clampsNegativeToZero() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Invalid Hospital")
                .totalBeds(-10)
                .occupiedBeds(5)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        ScoringContext context = new ScoringContext(hospital, referral, HospitalAcceptanceStats.empty(1L), now);

        double score = criterion.score(context);

        assertEquals(0.0, score, 0.001);
    }

    @Test
    void handlesZeroTotalBedsAsZero() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Zero Bed Hospital")
                .totalBeds(0)
                .occupiedBeds(0)
                .isolationCapable(false)
                .build();
        Referral referral = Referral.builder().build();
        ScoringContext context = new ScoringContext(hospital, referral, HospitalAcceptanceStats.empty(1L), now);

        double score = criterion.score(context);

        assertEquals(0.0, score, 0.001);
    }
}
