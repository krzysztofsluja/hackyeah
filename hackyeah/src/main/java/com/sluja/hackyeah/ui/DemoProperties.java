package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.referral.entity.Referral;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * Demo tuning: how many hospitals one wave asks, how long a wave waits per urgency
 * (seconds instead of minutes, so the flow fits in a live demo) and how long a manual flag lives.
 */
@ConfigurationProperties("demo")
public record DemoProperties(Integer waveSize, Map<Referral.Urgency, Duration> waveTimeout, Duration flagDuration) {

    public DemoProperties {
        if (waveSize == null) {
            waveSize = 3;
        }
        Map<Referral.Urgency, Duration> timeouts = new EnumMap<>(Referral.Urgency.class);
        timeouts.put(Referral.Urgency.TIME_CRITICAL, Duration.ofSeconds(30));
        timeouts.put(Referral.Urgency.URGENT_STABLE, Duration.ofSeconds(45));
        timeouts.put(Referral.Urgency.PLANNED, Duration.ofSeconds(60));
        if (waveTimeout != null) {
            timeouts.putAll(waveTimeout);
        }
        waveTimeout = Map.copyOf(timeouts);
        if (flagDuration == null) {
            flagDuration = Duration.ofHours(2);
        }
    }

    public static DemoProperties defaults() {
        return new DemoProperties(null, null, null);
    }

    public Duration waveTimeoutFor(Referral.Urgency urgency) {
        return waveTimeout.get(urgency);
    }
}
