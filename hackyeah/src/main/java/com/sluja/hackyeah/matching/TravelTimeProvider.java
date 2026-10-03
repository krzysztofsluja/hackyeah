package com.sluja.hackyeah.matching;

import java.util.Optional;

public interface TravelTimeProvider {
    Optional<Integer> travelMinutes(Long fromHospitalId, Long toHospitalId);
}
