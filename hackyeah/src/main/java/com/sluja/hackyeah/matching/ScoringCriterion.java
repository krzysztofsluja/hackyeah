package com.sluja.hackyeah.matching;

public interface ScoringCriterion {
    String name();

    double score(ScoringContext context);
}
