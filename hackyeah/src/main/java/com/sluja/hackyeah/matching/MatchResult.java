package com.sluja.hackyeah.matching;

import java.util.List;

public record MatchResult(List<HospitalScore> ranked, List<HospitalEligibility> excluded) {}
