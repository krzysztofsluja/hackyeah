package com.sluja.hackyeah.ui.view;

import java.util.List;

/**
 * One hospital on the coordinator map. Labels are already translated, so the browser only renders them
 * and {@code level} carries the occupancy thresholds decided in {@link OccupancyLevel}.
 */
public record MapHospital(
        Long id,
        String name,
        String district,
        double latitude,
        double longitude,
        int totalBeds,
        int occupiedBeds,
        int occupancyPercent,
        String level,
        boolean origin,
        List<String> flags,
        int pendingRequests,
        List<LabeledCount> declines
) {

    public record LabeledCount(String label, long count) {}
}
