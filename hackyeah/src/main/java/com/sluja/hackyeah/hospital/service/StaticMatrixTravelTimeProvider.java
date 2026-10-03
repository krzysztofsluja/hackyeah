package com.sluja.hackyeah.hospital.service;

import com.sluja.hackyeah.hospital.repository.TravelTimeRepository;
import com.sluja.hackyeah.matching.TravelTimeProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StaticMatrixTravelTimeProvider implements TravelTimeProvider {
    private final TravelTimeRepository travelTimeRepository;

    public StaticMatrixTravelTimeProvider(TravelTimeRepository travelTimeRepository) {
        this.travelTimeRepository = travelTimeRepository;
    }

    @Override
    public Optional<Integer> travelMinutes(Long fromHospitalId, Long toHospitalId) {
        return travelTimeRepository.findByFromHospital_IdAndToHospital_Id(fromHospitalId, toHospitalId)
                .map(tt -> tt.getMinutes());
    }
}
