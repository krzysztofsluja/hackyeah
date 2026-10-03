package com.sluja.hackyeah.dashboard;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.repository.HospitalRepository;
import com.sluja.hackyeah.ui.DemoProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;

/**
 * Stand-in for the ADT feed (admissions, discharges, transfers): every few seconds some hospitals
 * gain or lose a bed or two, so the coordinator map moves live. Occupancy only drifts a few points
 * around its seeded value, so a hospital that starts overloaded stays on the alert list.
 * Set {@code demo.occupancy-simulation-cron=-} to switch it off.
 */
@Service
public class OccupancySimulator {

    private static final double MAX_DRIFT = 0.03;

    private final HospitalRepository hospitalRepository;
    private final DemoProperties properties;
    private final Map<Long, Integer> seededOccupancy = new ConcurrentHashMap<>();
    private final RandomGenerator random = RandomGenerator.getDefault();

    public OccupancySimulator(HospitalRepository hospitalRepository, DemoProperties properties) {
        this.hospitalRepository = hospitalRepository;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void captureSeededOccupancy() {
        hospitalRepository.findAll().forEach(hospital -> seededOccupancy.put(hospital.getId(), hospital.getOccupiedBeds()));
    }

    @Scheduled(cron = "${demo.occupancy-simulation-cron:*/5 * * * * *}")
    @Transactional
    public void simulate() {
        for (Hospital hospital : hospitalRepository.findAll()) {
            Integer seeded = seededOccupancy.get(hospital.getId());
            // The referring hospital stays put - the doctor's own ward is not what the demo is about.
            if (seeded == null || hospital.getId().equals(properties.originHospitalId()) || random.nextBoolean()) {
                continue;
            }
            int drift = (int) Math.ceil(hospital.getTotalBeds() * MAX_DRIFT);
            int min = Math.max(0, seeded - drift);
            int max = Math.max(min, Math.min(hospital.getTotalBeds() - 1, seeded + drift));
            hospital.setOccupiedBeds(Math.clamp(hospital.getOccupiedBeds() + random.nextInt(-2, 3), min, max));
        }
    }

    /** Puts every hospital back to its seeded occupancy (demo reset). */
    @Transactional
    public void restoreBaseline() {
        for (Hospital hospital : hospitalRepository.findAll()) {
            Integer seeded = seededOccupancy.get(hospital.getId());
            if (seeded != null) {
                hospital.setOccupiedBeds(seeded);
            }
        }
    }
}
