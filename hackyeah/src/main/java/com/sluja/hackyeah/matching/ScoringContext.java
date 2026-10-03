package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;

public record ScoringContext(Hospital hospital,
        Referral referral,
        HospitalAcceptanceStats acceptanceStats,
        LocalDateTime now) {}

