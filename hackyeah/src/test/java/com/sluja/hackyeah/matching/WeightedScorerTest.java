package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightedScorerTest {

    private final TravelTimeProvider mockProvider = (from, to) -> java.util.Optional.of(20);
    private final WeightedScorer scorer = WeightedScorer.standard(mockProvider);
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void scoresAndSortsByDescending() {
        Hospital h1 = Hospital.builder()
                .id(1L)
                .name("Hospital 1")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Hospital h2 = Hospital.builder()
                .id(2L)
                .name("Hospital 2")
                .totalBeds(100)
                .occupiedBeds(75)
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .urgency(Referral.Urgency.URGENT_STABLE)
                .build();
        Map<Long, HospitalAcceptanceStats> stats = Map.of(
                1L, HospitalAcceptanceStats.empty(1L),
                2L, HospitalAcceptanceStats.empty(2L)
        );

        List<HospitalScore> scores = scorer.score(referral, List.of(h1, h2), stats, now);

        assertEquals(2, scores.size());
        assertTrue(scores.get(0).totalScore() >= scores.get(1).totalScore());
        assertEquals(1L, scores.get(0).hospital().getId());
        assertEquals(2L, scores.get(1).hospital().getId());
    }

    @Test
    void differentWeightsForDifferentUrgencies() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();

        Referral timeCritical = Referral.builder()
                .urgency(Referral.Urgency.TIME_CRITICAL)
                .build();
        Referral planned = Referral.builder()
                .urgency(Referral.Urgency.PLANNED)
                .build();

        Map<Long, HospitalAcceptanceStats> stats = Map.of(1L, HospitalAcceptanceStats.empty(1L));

        List<HospitalScore> timeCriticalScores = scorer.score(timeCritical, List.of(hospital), stats, now);
        List<HospitalScore> plannedScores = scorer.score(planned, List.of(hospital), stats, now);

        double timeCriticalScore = timeCriticalScores.get(0).totalScore();
        double plannedScore = plannedScores.get(0).totalScore();

        assertTrue(timeCriticalScore > plannedScore);
    }

    @Test
    void includesMissingHospitalStats() {
        Hospital h1 = Hospital.builder()
                .id(1L)
                .name("Hospital 1")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();
        Hospital h2 = Hospital.builder()
                .id(2L)
                .name("Hospital 2")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .urgency(Referral.Urgency.PLANNED)
                .build();

        Map<Long, HospitalAcceptanceStats> stats = Map.of(1L, HospitalAcceptanceStats.empty(1L));

        List<HospitalScore> scores = scorer.score(referral, List.of(h1, h2), stats, now);

        assertEquals(2, scores.size());
        assertTrue(scores.stream().anyMatch(s -> s.hospital().getId() == 2L));
    }

    @Test
    void criterionScoresAreIncluded() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .urgency(Referral.Urgency.PLANNED)
                .build();
        Map<Long, HospitalAcceptanceStats> stats = Map.of(1L, HospitalAcceptanceStats.empty(1L));

        List<HospitalScore> scores = scorer.score(referral, List.of(hospital), stats, now);

        assertEquals(1, scores.size());
        HospitalScore score = scores.get(0);

        assertTrue(score.criterionScores().containsKey("BED_CAPACITY"));
        assertTrue(score.criterionScores().containsKey("ACCEPTANCE_PROBABILITY"));
        assertEquals(0.5, score.criterionScores().get("BED_CAPACITY"), 0.001);
        assertEquals(0.7, score.criterionScores().get("ACCEPTANCE_PROBABILITY"), 0.001);
    }

    @Test
    void acceptanceProbabilityDominatesTimeCritical() {
        Hospital fullBeds = Hospital.builder()
                .id(1L)
                .name("Full Beds")
                .totalBeds(100)
                .occupiedBeds(100)
                .isolationCapable(false)
                .build();
        Hospital emptyBeds = Hospital.builder()
                .id(2L)
                .name("Empty Beds")
                .totalBeds(100)
                .occupiedBeds(0)
                .isolationCapable(false)
                .build();

        Referral timeCritical = Referral.builder()
                .urgency(Referral.Urgency.TIME_CRITICAL)
                .build();

        Map<Long, HospitalAcceptanceStats> stats = Map.of(
                1L, new HospitalAcceptanceStats(1L, 9, 10),
                2L, new HospitalAcceptanceStats(2L, 1, 10)
        );

        List<HospitalScore> scores = scorer.score(timeCritical, List.of(fullBeds, emptyBeds), stats, now);

        assertEquals(2, scores.size());
        assertEquals(1L, scores.get(0).hospital().getId(), "High acceptance rate should rank first for time-critical");
    }

    @Test
    void bedsCapacityDominatesPlanned() {
        Hospital fullBeds = Hospital.builder()
                .id(1L)
                .name("Full Beds")
                .totalBeds(100)
                .occupiedBeds(100)
                .isolationCapable(false)
                .build();
        Hospital emptyBeds = Hospital.builder()
                .id(2L)
                .name("Empty Beds")
                .totalBeds(100)
                .occupiedBeds(0)
                .isolationCapable(false)
                .build();

        Referral planned = Referral.builder()
                .urgency(Referral.Urgency.PLANNED)
                .build();

        Map<Long, HospitalAcceptanceStats> stats = Map.of(
                1L, new HospitalAcceptanceStats(1L, 9, 10),
                2L, new HospitalAcceptanceStats(2L, 1, 10)
        );

        List<HospitalScore> scores = scorer.score(planned, List.of(fullBeds, emptyBeds), stats, now);

        assertEquals(2, scores.size());
        assertEquals(2L, scores.get(0).hospital().getId(), "Good bed capacity should rank first for planned");
    }

    @Test
    void travelTimeDominatesTimeCritical() {
        TravelTimeProvider travelProvider = (from, to) -> {
            if (to == 1L) return java.util.Optional.of(5);
            else return java.util.Optional.of(45);
        };
        WeightedScorer travelAwareScorer = WeightedScorer.standard(travelProvider);

        Hospital closeHospital = Hospital.builder()
                .id(1L)
                .name("Close Hospital")
                .totalBeds(100)
                .occupiedBeds(100)
                .isolationCapable(false)
                .build();
        Hospital farHospital = Hospital.builder()
                .id(2L)
                .name("Far Hospital")
                .totalBeds(100)
                .occupiedBeds(0)
                .isolationCapable(false)
                .build();

        Referral timeCritical = Referral.builder()
                .urgency(Referral.Urgency.TIME_CRITICAL)
                .originHospitalId(3L)
                .build();

        Map<Long, HospitalAcceptanceStats> stats = Map.of(
                1L, new HospitalAcceptanceStats(1L, 1, 10),
                2L, new HospitalAcceptanceStats(2L, 9, 10)
        );

        List<HospitalScore> scores = travelAwareScorer.score(timeCritical, List.of(closeHospital, farHospital), stats, now);

        assertEquals(2, scores.size());
        assertEquals(1L, scores.get(0).hospital().getId(), "Shorter travel should rank first for TIME_CRITICAL");
    }

    @Test
    void includesTravelTimeCriterionInScores() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .urgency(Referral.Urgency.PLANNED)
                .originHospitalId(2L)
                .build();
        Map<Long, HospitalAcceptanceStats> stats = Map.of(1L, HospitalAcceptanceStats.empty(1L));

        List<HospitalScore> scores = scorer.score(referral, List.of(hospital), stats, now);

        assertEquals(1, scores.size());
        HospitalScore score = scores.get(0);

        assertTrue(score.criterionScores().containsKey("TRAVEL_TIME"));
        assertTrue(score.criterionScores().containsKey("BED_CAPACITY"));
        assertTrue(score.criterionScores().containsKey("ACCEPTANCE_PROBABILITY"));
    }

    private void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
