package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.matching.rules.AvailableBedsRule;
import com.sluja.hackyeah.matching.rules.BlockedProcedureRule;
import com.sluja.hackyeah.matching.rules.BlockingFlagRule;
import com.sluja.hackyeah.matching.rules.IsolationCapabilityRule;
import com.sluja.hackyeah.matching.rules.ProcedureCoverageRule;
import com.sluja.hackyeah.matching.rules.SpecialtyMatchRule;
import com.sluja.hackyeah.matching.rules.TravelRouteExistsRule;
import com.sluja.hackyeah.referral.entity.Referral;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class HardConstraintFilter {
    private final List<HardConstraintRule> rules;

    public HardConstraintFilter(List<HardConstraintRule> rules) {
        this.rules = rules;
    }

    public static HardConstraintFilter standard(TravelTimeProvider travelTimeProvider) {
        return new HardConstraintFilter(
                List.of(
                        new AvailableBedsRule(),
                        new IsolationCapabilityRule(),
                        new SpecialtyMatchRule(),
                        new ProcedureCoverageRule(),
                        new BlockingFlagRule(),
                        new BlockedProcedureRule(),
                        new TravelRouteExistsRule(travelTimeProvider)
                )
        );
    }

    public List<HospitalEligibility> evaluate(Referral referral, List<Hospital> hospitals, LocalDateTime now) {
        return hospitals.stream()
                .map(hospital -> evaluateHospital(hospital, referral, now))
                .collect(Collectors.toList());
    }

    public List<Hospital> filterEligible(Referral referral, List<Hospital> hospitals, LocalDateTime now) {
        return evaluate(referral, hospitals, now).stream()
                .filter(HospitalEligibility::eligible)
                .map(HospitalEligibility::hospital)
                .collect(Collectors.toList());
    }

    private HospitalEligibility evaluateHospital(Hospital hospital, Referral referral, LocalDateTime now) {
        final List<String> violations = new LinkedList<>();

        for (final HardConstraintRule rule : rules) {
            final var violation = rule.checkViolation(hospital, referral, now);
            violation.ifPresent(violations::add);
        }

        final boolean eligible = violations.isEmpty();
        return new HospitalEligibility(hospital, eligible, violations);
    }
}
