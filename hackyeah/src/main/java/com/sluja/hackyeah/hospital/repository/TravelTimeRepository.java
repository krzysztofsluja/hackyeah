package com.sluja.hackyeah.hospital.repository;

import com.sluja.hackyeah.hospital.entity.TravelTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TravelTimeRepository extends JpaRepository<TravelTime, Long> {
    Optional<TravelTime> findByFromHospital_IdAndToHospital_Id(Long fromHospitalId, Long toHospitalId);
}
