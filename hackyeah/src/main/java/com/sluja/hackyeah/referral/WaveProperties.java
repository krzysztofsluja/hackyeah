package com.sluja.hackyeah.referral;

import com.sluja.hackyeah.referral.entity.Referral;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Wave dispatch settings. {@code timeScale} shrinks every timeout for the demo, so a wave that
 * would really expire in 10 minutes expires in seconds on stage.
 */
@ConfigurationProperties("app.waves")
public record WaveProperties(int size, Map<Referral.Urgency, Duration> timeout, double timeScale) {

    private static final int DEFAULT_SIZE = 3;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

    public WaveProperties {
        size = size < 1 ? DEFAULT_SIZE : size;
        timeScale = timeScale < 1 ? 1 : timeScale;
        timeout = timeout == null ? Map.of() : Map.copyOf(timeout);
    }

    public Duration effectiveTimeout(Referral.Urgency urgency) {
        Duration base = timeout.getOrDefault(urgency, DEFAULT_TIMEOUT);
        return Duration.ofMillis(Math.max(1L, (long) (base.toMillis() / timeScale)));
    }
}
