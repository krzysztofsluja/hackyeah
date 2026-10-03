package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DemoPropertiesTest {

    @Test
    void bindsScenarioFlagsPerHospitalAndFlagDuration() {
        Binder binder = new Binder(new MapConfigurationPropertySource(Map.of(
                "demo.origin-hospital-id", "4",
                "demo.flag-duration", "15m",
                "demo.scenario-flags.2", "CATH_LAB_BUSY,TK_DOWN")));

        DemoProperties properties = binder.bind("demo", DemoProperties.class).get();

        assertThat(properties.originHospitalId()).isEqualTo(4L);
        assertThat(properties.flagDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.scenarioFlags()).containsExactly(
                Map.entry(2L, List.of(HospitalFlag.FlagType.CATH_LAB_BUSY, HospitalFlag.FlagType.TK_DOWN)));
    }

    @Test
    void defaultsWhenNothingIsConfigured() {
        DemoProperties properties = new DemoProperties(null, null, null);

        assertThat(properties.originHospitalId()).isEqualTo(1L);
        assertThat(properties.flagDuration()).isEqualTo(Duration.ofHours(2));
        assertThat(properties.scenarioFlags()).isEmpty();
    }
}
