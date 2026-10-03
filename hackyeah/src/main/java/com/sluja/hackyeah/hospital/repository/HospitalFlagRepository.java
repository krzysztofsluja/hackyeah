package com.sluja.hackyeah.hospital.repository;

import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HospitalFlagRepository extends JpaRepository<HospitalFlag, Long> {

    List<HospitalFlag> findByHospital_IdAndType(Long hospitalId, HospitalFlag.FlagType type);
}
