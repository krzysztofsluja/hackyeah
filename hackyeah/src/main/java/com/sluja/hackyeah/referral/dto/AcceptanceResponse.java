package com.sluja.hackyeah.referral.dto;

/** Acceptance opens a direct doctor-to-doctor channel, so both sides get a number. */
public record AcceptanceResponse(
        RequestView request,
        HospitalContact acceptingHospital,
        HospitalContact originHospital) {}
