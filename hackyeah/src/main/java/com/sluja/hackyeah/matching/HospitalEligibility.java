package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;

import java.util.List;

public record HospitalEligibility(Hospital hospital, boolean eligible, List<String> violations) {}
