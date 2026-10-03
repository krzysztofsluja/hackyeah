package com.sluja.hackyeah.ui.view;

import com.sluja.hackyeah.hospital.entity.HospitalFlag;

import java.time.LocalDateTime;

public record FlagView(HospitalFlag.FlagType type, LocalDateTime validUntil) {}
