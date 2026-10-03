package com.sluja.hackyeah.ui.view;

import java.util.Arrays;
import java.util.List;

/**
 * Why a hospital was filtered out, parsed from a {@code HardConstraintRule} violation code.
 * {@code argKeys} are message keys (e.g. {@code flag.CATH_LAB_BUSY}) rendered next to the label.
 */
public record ExclusionReason(String code, List<String> argKeys) {

    public static final String UNKNOWN = "UNKNOWN";

    public static ExclusionReason fromViolation(String violation) {
        String[] parts = violation.split(":", 2);
        String code = parts[0];
        String detail = parts.length > 1 ? parts[1] : "";

        return switch (code) {
            case "NO_AVAILABLE_BEDS", "ISOLATION_NOT_CAPABLE", "SPECIALTY_NOT_COVERED", "NO_TRAVEL_ROUTE" ->
                    new ExclusionReason(code, List.of());
            // PROCEDURES_NOT_COVERED:[THROMBECTOMY, MRI]
            case "PROCEDURES_NOT_COVERED" -> new ExclusionReason(code, Arrays.stream(detail.replaceAll("[\\[\\]\\s]", "").split(","))
                    .filter(p -> !p.isBlank())
                    .sorted()
                    .map(p -> "procedure." + p)
                    .toList());
            // BLOCKED_BY_FLAG:TK_DOWN
            case "BLOCKED_BY_FLAG" -> new ExclusionReason(code, List.of("flag." + detail));
            // PROCEDURE_BLOCKED_BY_FLAG:CATH_LAB_BUSY:blocked_procedure=THROMBECTOMY
            case "PROCEDURE_BLOCKED_BY_FLAG" -> {
                String[] flagAndProcedure = detail.split(":blocked_procedure=", 2);
                yield flagAndProcedure.length == 2
                        ? new ExclusionReason(code, List.of("flag." + flagAndProcedure[0], "procedure." + flagAndProcedure[1]))
                        : new ExclusionReason(code, List.of("flag." + detail));
            }
            default -> new ExclusionReason(UNKNOWN, List.of());
        };
    }
}
