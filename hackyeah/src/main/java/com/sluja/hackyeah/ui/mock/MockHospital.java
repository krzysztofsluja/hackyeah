package com.sluja.hackyeah.ui.mock;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.ui.view.FlagView;
import com.sluja.hackyeah.ui.view.HospitalView;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Mutable in-memory hospital state of the mock. */
class MockHospital {

    final Long id;
    final String name;
    final String district;
    final double latitude;
    final double longitude;
    final int totalBeds;
    int occupiedBeds;
    final Set<String> specialties;
    final Set<Procedure> procedures;
    final boolean isolationCapable;
    final String dutyPhone;
    final Map<HospitalFlag.FlagType, LocalDateTime> flags = new EnumMap<>(HospitalFlag.FlagType.class);

    MockHospital(Long id, String name, String district, double latitude, double longitude,
                 int totalBeds, int occupiedBeds, Set<String> specialties, Set<Procedure> procedures,
                 boolean isolationCapable, String dutyPhone) {
        this.id = id;
        this.name = name;
        this.district = district;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalBeds = totalBeds;
        this.occupiedBeds = occupiedBeds;
        this.specialties = Set.copyOf(specialties);
        this.procedures = Set.copyOf(procedures);
        this.isolationCapable = isolationCapable;
        this.dutyPhone = dutyPhone;
    }

    List<FlagView> activeFlags(LocalDateTime now) {
        return flags.entrySet().stream()
                .filter(e -> now.isBefore(e.getValue()))
                .map(e -> new FlagView(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(FlagView::type))
                .toList();
    }

    /** Transient entity, so the real matching rules and criteria can run on mock data. */
    Hospital toEntity() {
        Set<HospitalFlag> flagEntities = flags.entrySet().stream()
                // hospital back-reference left null: Lombok's @Data hashCode would recurse through it
                .map(e -> HospitalFlag.builder().type(e.getKey()).validUntil(e.getValue()).build())
                .collect(Collectors.toSet());

        return Hospital.builder()
                .id(id)
                .name(name)
                .latitude(latitude)
                .longitude(longitude)
                .totalBeds(totalBeds)
                .occupiedBeds(occupiedBeds)
                .specialties(specialties)
                .procedures(procedures)
                .isolationCapable(isolationCapable)
                .flags(flagEntities)
                .build();
    }

    HospitalView toView(LocalDateTime now) {
        return new HospitalView(id, name, district, latitude, longitude, totalBeds, occupiedBeds,
                specialties, procedures, isolationCapable, dutyPhone, activeFlags(now));
    }
}
