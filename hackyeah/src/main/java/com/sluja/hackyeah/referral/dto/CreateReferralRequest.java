package com.sluja.hackyeah.referral.dto;

import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Anonymized referral filed by the sending hospital: a clinical profile only, no personal data.
 * {@code requiresIsolation} is required - the isolation rules unbox it, so a null would blow up
 * inside the matching engine instead of being reported as a bad request.
 */
public record CreateReferralRequest(
        @NotBlank String targetSpecialty,
        Set<Procedure> requiredProcedures,
        @NotNull Referral.Urgency urgency,
        @NotNull Referral.PatientState patientState,
        @NotNull Boolean requiresIsolation,
        @Size(max = 500) String note,
        @NotNull Long originHospitalId) {}
