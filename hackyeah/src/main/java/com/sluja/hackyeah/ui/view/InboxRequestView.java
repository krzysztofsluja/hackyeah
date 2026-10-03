package com.sluja.hackyeah.ui.view;

import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;

import java.time.LocalDateTime;
import java.util.Set;

/** A request as seen by the receiving hospital: the anonymised clinical profile plus the answer state. */
public record InboxRequestView(
        Long requestId,
        Long referralId,
        int wave,
        ReferralRequest.RequestStatus status,
        ReferralRequest.DeclineReason declineReason,
        String targetSpecialty,
        Set<Procedure> requiredProcedures,
        Referral.Urgency urgency,
        Referral.PatientState patientState,
        boolean requiresIsolation,
        String note,
        String originHospitalName,
        String originDutyPhone,
        Integer travelMinutes,
        LocalDateTime sentAt,
        long secondsToDeadline
) {

    public boolean isPending() {
        return status == ReferralRequest.RequestStatus.PENDING;
    }
}
