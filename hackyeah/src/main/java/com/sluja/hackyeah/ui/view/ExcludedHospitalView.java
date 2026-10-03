package com.sluja.hackyeah.ui.view;

import java.util.List;

public record ExcludedHospitalView(Long hospitalId, String name, String district, List<ExclusionReason> reasons) {}
