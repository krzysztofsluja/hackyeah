package com.sluja.hackyeah.matching;

public record HospitalAcceptanceStats(Long hospitalId, long acceptedCount, long respondedCount) {
    public static HospitalAcceptanceStats empty(Long hospitalId) {
        return new HospitalAcceptanceStats(hospitalId, 0, 0);
    }
}
