package com.sluja.hackyeah.ui.view;

import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.hospital.entity.Procedure;

import java.util.List;
import java.util.Set;

public record HospitalView(
        Long id,
        String name,
        String district,
        double latitude,
        double longitude,
        int totalBeds,
        int occupiedBeds,
        Set<String> specialties,
        Set<Procedure> procedures,
        boolean isolationCapable,
        String dutyPhone,
        List<FlagView> flags
) {

    public int occupancyPercent() {
        return totalBeds == 0 ? 0 : Math.round(occupiedBeds * 100f / totalBeds);
    }

    public int availableBeds() {
        return totalBeds - occupiedBeds;
    }

    /** The active flag of this type, or {@code null} (templates work better with null than Optional). */
    public FlagView flag(HospitalFlag.FlagType type) {
        return flags.stream().filter(f -> f.type() == type).findFirst().orElse(null);
    }

    public OccupancyLevel occupancyLevel() {
        return OccupancyLevel.of(occupancyPercent());
    }
}
