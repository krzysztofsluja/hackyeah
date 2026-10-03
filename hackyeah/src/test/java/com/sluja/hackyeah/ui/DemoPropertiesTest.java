package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DemoPropertiesTest {

    @Test
    void bindsKebabCaseUrgencyKeysAndKeepsDefaultsForTheRest() {
        Binder binder = new Binder(new MapConfigurationPropertySource(Map.of(
                "demo.wave-timeout.time-critical", "10s",
                "demo.flag-duration", "15m")));

        DemoProperties properties = binder.bind("demo", DemoProperties.class).get();

        assertThat(properties.waveTimeoutFor(Referral.Urgency.TIME_CRITICAL)).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.waveTimeoutFor(Referral.Urgency.PLANNED)).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.flagDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.waveSize()).isEqualTo(3);
    }
}
