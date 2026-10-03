package com.sluja.hackyeah.matching.criteria;

import com.sluja.hackyeah.matching.ScoringContext;
import com.sluja.hackyeah.matching.ScoringCriterion;

public class BedCapacityCriterion implements ScoringCriterion {

    @Override
    public String name() {
        return "BED_CAPACITY";
    }

    @Override
    public double score(ScoringContext context) {
        int availableBeds = context.hospital().getAvailableBeds();
        int totalBeds = context.hospital().getTotalBeds();

        if (totalBeds <= 0) {
            return 0.0;
        }

        double ratio = availableBeds / (double) totalBeds;
        return Math.max(0.0, Math.min(1.0, ratio));
    }
}
