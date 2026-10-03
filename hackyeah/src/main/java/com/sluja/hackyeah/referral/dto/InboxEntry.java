package com.sluja.hackyeah.referral.dto;

import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.hospital.entity.Procedure;

import java.time.LocalDateTime;
import java.util.Set;

/** An open request as the receiving hospital sees it: the clinical profile, no personal data. */
public record InboxEntry(
        Long requestId,
        Long referralId,
        int wave,
        LocalDateTime deadline,
        String targetSpecialty,
        Set<Procedure> requiredProcedures,
        Referral.Urgency urgency,
        Referral.PatientState patientState,
        boolean requiresIsolation,
        String note) {}
