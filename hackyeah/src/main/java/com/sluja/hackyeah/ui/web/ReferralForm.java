package com.sluja.hackyeah.ui.web;

import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.ui.view.NewReferral;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.EnumSet;
import java.util.Set;

/** Backing object of the referral form. Defaults to the stroke scenario so the demo fills in seconds. */
@Data
public class ReferralForm {

    @NotBlank(message = "Wybierz specjalność")
    private String targetSpecialty = "NEUROLOGY";

    @NotNull
    private Set<Procedure> requiredProcedures = EnumSet.of(Procedure.CT, Procedure.THROMBECTOMY);

    @NotNull(message = "Wybierz pilność")
    private Referral.Urgency urgency = Referral.Urgency.TIME_CRITICAL;

    @NotNull(message = "Wybierz stan pacjenta")
    private Referral.PatientState patientState = Referral.PatientState.STABLE;

    private boolean requiresIsolation;

    @Size(max = 200, message = "Maksymalnie 200 znaków")
    private String note = "72 l., objawy od 2 h";

    NewReferral toNewReferral() {
        return new NewReferral(targetSpecialty, Set.copyOf(requiredProcedures), urgency, patientState,
                requiresIsolation, note == null || note.isBlank() ? null : note.strip());
    }
}
