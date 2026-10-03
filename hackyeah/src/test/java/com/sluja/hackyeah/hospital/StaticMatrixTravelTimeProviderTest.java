package com.sluja.hackyeah.hospital;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.TravelTime;
import com.sluja.hackyeah.hospital.repository.TravelTimeRepository;
import com.sluja.hackyeah.hospital.service.StaticMatrixTravelTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest 
@Import(StaticMatrixTravelTimeProvider.class)
class StaticMatrixTravelTimeProviderTest {

    @Autowired
    private TravelTimeRepository travelTimeRepository;

    @Autowired
    private StaticMatrixTravelTimeProvider provider;

    @Test
    void returnsTravelTimeForExistingRoute() {
        Hospital from = Hospital.builder()
                .id(1L)
                .name("Hospital A")
                .isolationCapable(false)
                .build();
        Hospital to = Hospital.builder()
                .id(2L)
                .name("Hospital B")
                .isolationCapable(false)
                .build();

        TravelTime travelTime = TravelTime.builder()
                .fromHospital(from)
                .toHospital(to)
                .minutes(25)
                .build();

        //travelTimeRepository.saveAll(java.util.List.of(from, to));
        travelTimeRepository.save(travelTime);

        Optional<Integer> result = provider.travelMinutes(1L, 2L);

        assertTrue(result.isPresent());
        assertEquals(25, result.get());
    }

    @Test
    void returnsEmptyForMissingRoute() {
        Hospital from = Hospital.builder()
                .id(1L)
                .name("Hospital A")
                .isolationCapable(false)
                .build();
        Hospital to = Hospital.builder()
                .id(2L)
                .name("Hospital B")
                .isolationCapable(false)
                .build();

        //travelTimeRepository.saveAll(java.util.List.of(from, to));

        Optional<Integer> result = provider.travelMinutes(from.getId(), to.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void directionMatters() {
        Hospital from = Hospital.builder()
                .id(1L)
                .name("Hospital A")
                .isolationCapable(false)
                .build();
        Hospital to = Hospital.builder()
                .id(2L)
                .name("Hospital B")
                .isolationCapable(false)
                .build();

        TravelTime travelTime = TravelTime.builder()
                .fromHospital(from)
                .toHospital(to)
                .minutes(25)
                .build();

        //travelTimeRepository.saveAll(java.util.List.of(from, to));
        travelTimeRepository.save(travelTime);

        Optional<Integer> forward = provider.travelMinutes(from.getId(), to.getId());
        Optional<Integer> backward = provider.travelMinutes(to.getId(), from.getId());

        assertTrue(forward.isPresent());
        assertEquals(25, forward.get());
        assertTrue(backward.isEmpty());
    }

    @Test
    void returnsCorrectMinutesValue() {
        Hospital from = Hospital.builder()
                .id(1L)
                .name("Hospital A")
                .isolationCapable(false)
                .build();
        Hospital to = Hospital.builder()
                .id(2L)
                .name("Hospital B")
                .isolationCapable(false)
                .build();

        TravelTime travelTime = TravelTime.builder()
                .fromHospital(from)
                .toHospital(to)
                .minutes(42)
                .build();

        //travelTimeRepository.saveAll(java.util.List.of(from, to));
        travelTimeRepository.save(travelTime);

        Optional<Integer> result = provider.travelMinutes(from.getId(), to.getId());

        assertEquals(Optional.of(42), result);
    }
}
