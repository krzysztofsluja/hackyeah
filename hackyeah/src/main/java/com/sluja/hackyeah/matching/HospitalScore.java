package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;

import java.util.Map;

public record HospitalScore(Hospital hospital, double totalScore, Map<String, Double> criterionScores) {}
