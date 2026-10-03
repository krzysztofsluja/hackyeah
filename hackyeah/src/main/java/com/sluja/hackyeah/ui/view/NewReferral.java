package com.sluja.hackyeah.ui.view;

import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;

import java.util.Set;

/** Referral as submitted by the doctor (anonymised clinical profile). */
public record NewReferral(
        String targetSpecialty,
        Set<Procedure> requiredProcedures,
        Referral.Urgency urgency,
        Referral.PatientState patientState,
        boolean requiresIsolation,
        String note
) {}
