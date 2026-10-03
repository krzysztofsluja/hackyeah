package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Demo setup: which hospital the referring doctor works in, how long a manual flag lives, and the
 * flags the scenario starts with (hospital id -> flag types), restored by every reset.
 * Wave size and timeouts live in {@code app.waves} - the UI runs on the real wave dispatch.
 */
@ConfigurationProperties("demo")
public record DemoProperties(Long originHospitalId, Duration flagDuration,
                             Map<Long, List<HospitalFlag.FlagType>> scenarioFlags) {

    public DemoProperties {
        if (originHospitalId == null) {
            originHospitalId = 1L;
        }
        if (flagDuration == null) {
            flagDuration = Duration.ofHours(2);
        }
        scenarioFlags = scenarioFlags == null ? Map.of() : Map.copyOf(scenarioFlags);
    }
}
