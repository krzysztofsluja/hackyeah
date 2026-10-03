package com.sluja.hackyeah.ui.view;

public enum OccupancyLevel {
    LOW,
    MEDIUM,
    HIGH;

    public static final int MEDIUM_THRESHOLD = 75;
    public static final int HIGH_THRESHOLD = 90;

    public static OccupancyLevel of(int occupancyPercent) {
        if (occupancyPercent > HIGH_THRESHOLD) {
            return HIGH;
        }
        if (occupancyPercent >= MEDIUM_THRESHOLD) {
            return MEDIUM;
        }
        return LOW;
    }
}
