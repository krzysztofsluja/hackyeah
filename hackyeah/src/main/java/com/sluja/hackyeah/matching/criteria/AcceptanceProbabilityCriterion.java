package com.sluja.hackyeah.matching.criteria;

import com.sluja.hackyeah.matching.ScoringContext;
import com.sluja.hackyeah.matching.ScoringCriterion;

public class AcceptanceProbabilityCriterion implements ScoringCriterion {

    private static final double PRIOR_ACCEPTED = 7.0;
    private static final double PRIOR_TOTAL = 10.0;

    @Override
    public String name() {
        return "ACCEPTANCE_PROBABILITY";
    }

    @Override
    public double score(ScoringContext context) {
        long acceptedCount = context.acceptanceStats().acceptedCount();
        long respondedCount = context.acceptanceStats().respondedCount();

        double posterior = (acceptedCount + PRIOR_ACCEPTED) / (respondedCount + PRIOR_TOTAL);
        return Math.max(0.0, Math.min(1.0, posterior));
    }
}
