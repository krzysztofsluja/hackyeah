package com.sluja.hackyeah.hospital.repository;

import com.sluja.hackyeah.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {}
