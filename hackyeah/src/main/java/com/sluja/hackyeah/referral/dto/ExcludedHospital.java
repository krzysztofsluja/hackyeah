package com.sluja.hackyeah.referral.dto;

import java.util.List;

/** A hospital dropped by the hard constraints, with every violation code it tripped. */
public record ExcludedHospital(Long hospitalId, String name, List<String> violations) {}
