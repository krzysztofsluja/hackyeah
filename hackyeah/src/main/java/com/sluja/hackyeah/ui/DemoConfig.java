package com.sluja.hackyeah.ui;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DemoProperties.class)
public class DemoConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
