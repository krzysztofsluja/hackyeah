package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.ui.view.ExclusionReason;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExclusionReasonTest {

    @Test
    void parsesCodeWithoutDetails() {
        assertThat(ExclusionReason.fromViolation("NO_AVAILABLE_BEDS"))
                .isEqualTo(new ExclusionReason("NO_AVAILABLE_BEDS", List.of()));
    }

    @Test
    void parsesMissingProcedures() {
        assertThat(ExclusionReason.fromViolation("PROCEDURES_NOT_COVERED:[THROMBECTOMY, MRI]"))
                .isEqualTo(new ExclusionReason("PROCEDURES_NOT_COVERED", List.of("procedure.MRI", "procedure.THROMBECTOMY")));
    }

    @Test
    void parsesBlockingFlag() {
        assertThat(ExclusionReason.fromViolation("BLOCKED_BY_FLAG:TK_DOWN"))
                .isEqualTo(new ExclusionReason("BLOCKED_BY_FLAG", List.of("flag.TK_DOWN")));
    }

    @Test
    void parsesProcedureBlockedByFlag() {
        assertThat(ExclusionReason.fromViolation("PROCEDURE_BLOCKED_BY_FLAG:CATH_LAB_BUSY:blocked_procedure=PCI"))
                .isEqualTo(new ExclusionReason("PROCEDURE_BLOCKED_BY_FLAG", List.of("flag.CATH_LAB_BUSY", "procedure.PCI")));
    }

    @Test
    void unknownCodeFallsBack() {
        assertThat(ExclusionReason.fromViolation("SOMETHING_NEW:x").code()).isEqualTo(ExclusionReason.UNKNOWN);
    }
}
