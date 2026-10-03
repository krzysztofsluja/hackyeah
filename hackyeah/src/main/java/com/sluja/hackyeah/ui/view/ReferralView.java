package com.sluja.hackyeah.ui.view;

import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record ReferralView(
        Long id,
        String originHospitalName,
        String targetSpecialty,
        Set<Procedure> requiredProcedures,
        Referral.Urgency urgency,
        Referral.PatientState patientState,
        boolean requiresIsolation,
        String note,
        Referral.ReferralStatus status,
        LocalDateTime createdAt,
        long elapsedSeconds,
        int currentWave,
        long secondsToWaveDeadline,
        HospitalView acceptedHospital,
        List<CandidateView> candidates,
        List<ExcludedHospitalView> excluded
) {

    public boolean isOpen() {
        return status == Referral.ReferralStatus.OPEN;
    }

    public boolean isAccepted() {
        return status == Referral.ReferralStatus.ACCEPTED;
    }

    public boolean isEscalated() {
        return status == Referral.ReferralStatus.ESCALATED;
    }
}
