package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.Optional;

public interface HardConstraintRule {
    Optional<String> checkViolation(Hospital hospital, Referral referral, LocalDateTime now);
}
