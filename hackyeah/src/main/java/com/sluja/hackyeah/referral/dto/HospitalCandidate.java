package com.sluja.hackyeah.referral.dto;

import java.util.Map;

/**
 * One hospital that passed every hard constraint. {@code rank} is its frozen position in the
 * ranking - waves walk down it in order.
 */
public record HospitalCandidate(
        Integer rank,
        Long hospitalId,
        String name,
        double totalScore,
        Map<String, Double> criterionScores,
        Integer availableBeds,
        Integer travelMinutes) {}
