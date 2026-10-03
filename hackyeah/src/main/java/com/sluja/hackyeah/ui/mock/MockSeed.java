package com.sluja.hackyeah.ui.mock;

import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.matching.HospitalAcceptanceStats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sluja.hackyeah.hospital.entity.Procedure.*;

/**
 * Demo data: fictional hospitals placed in Kraków. Scenario: the voivodeship hospital is the closest
 * stroke centre but is overloaded and its cath lab is busy, so the system should route to the
 * university hospital instead.
 */
final class MockSeed {

    static final Long ORIGIN_HOSPITAL_ID = 1L;
    static final Long VOIVODESHIP_HOSPITAL_ID = 2L;
    static final Long UNIVERSITY_HOSPITAL_ID = 3L;

    private MockSeed() {}

    static List<MockHospital> hospitals(LocalDateTime now) {
        MockHospital voivodeship = new MockHospital(VOIVODESHIP_HOSPITAL_ID, "Szpital Wojewódzki", "Prądnik Biały",
                50.0870, 19.9590, 120, 113, Set.of("NEUROLOGY", "CARDIOLOGY", "INTERNAL", "SURGERY"),
                Set.of(CT, MRI, PCI, ICU, VENTILATION, THROMBOLYSIS, THROMBECTOMY), true, "+48 12 400 02 02");
        voivodeship.flags.put(HospitalFlag.FlagType.CATH_LAB_BUSY, now.plusHours(3));

        return List.of(
                new MockHospital(ORIGIN_HOSPITAL_ID, "Szpital Powiatowy", "Myślenice",
                        49.8340, 19.9380, 60, 41, Set.of("INTERNAL", "SURGERY"),
                        Set.of(CT), false, "+48 12 400 01 01"),
                voivodeship,
                new MockHospital(UNIVERSITY_HOSPITAL_ID, "Szpital Uniwersytecki", "Prokocim",
                        50.0110, 20.0040, 150, 108, Set.of("NEUROLOGY", "CARDIOLOGY", "INTERNAL", "SURGERY"),
                        Set.of(CT, MRI, PCI, ICU, VENTILATION, THROMBOLYSIS, THROMBECTOMY), true, "+48 12 400 03 03"),
                new MockHospital(4L, "Szpital Miejski", "Nowa Huta",
                        50.0790, 20.0500, 90, 72, Set.of("NEUROLOGY", "INTERNAL", "SURGERY"),
                        Set.of(CT, THROMBOLYSIS, THROMBECTOMY, ICU), false, "+48 12 400 04 04"),
                new MockHospital(5L, "Szpital Specjalistyczny", "Łagiewniki",
                        50.0210, 19.9330, 80, 52, Set.of("CARDIOLOGY", "INTERNAL"),
                        Set.of(CT, PCI, ICU), false, "+48 12 400 05 05"),
                new MockHospital(6L, "Szpital Zakaźny", "Krowodrza",
                        50.0800, 19.9100, 50, 31, Set.of("INFECTIOUS", "INTERNAL"),
                        Set.of(CT, ICU, VENTILATION), true, "+48 12 400 06 06"),
                new MockHospital(7L, "Szpital Kliniczny", "Grzegórzki",
                        50.0600, 19.9560, 110, 96, Set.of("NEUROLOGY", "CARDIOLOGY", "INTERNAL"),
                        Set.of(CT, MRI, PCI, THROMBOLYSIS, THROMBECTOMY, ICU), true, "+48 12 400 07 07"),
                new MockHospital(8L, "Szpital Rejonowy", "Wieliczka",
                        49.9870, 20.0610, 60, 30, Set.of("INTERNAL", "SURGERY"),
                        Set.of(CT), false, "+48 12 400 08 08")
        );
    }

    /** Travel minutes from the origin hospital (without rush-hour multiplier). */
    static Map<Long, Integer> travelMinutesFromOrigin() {
        return Map.of(
                2L, 32,
                3L, 38,
                4L, 45,
                5L, 34,
                6L, 40,
                7L, 36,
                8L, 28
        );
    }

    /** Historic answers per hospital (accepted / responded), feeding the acceptance probability criterion. */
    static Map<Long, HospitalAcceptanceStats> acceptanceStats() {
        return Map.of(
                2L, new HospitalAcceptanceStats(2L, 9, 30),
                3L, new HospitalAcceptanceStats(3L, 18, 20),
                4L, new HospitalAcceptanceStats(4L, 10, 20),
                5L, new HospitalAcceptanceStats(5L, 12, 16),
                6L, new HospitalAcceptanceStats(6L, 8, 10),
                7L, new HospitalAcceptanceStats(7L, 6, 20),
                8L, new HospitalAcceptanceStats(8L, 5, 8)
        );
    }
}
