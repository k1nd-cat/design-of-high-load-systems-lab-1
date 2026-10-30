package ru.k1ndcat.lab1.config;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterFilter excludeServiceRequests() {
        return MeterFilter.deny(id -> {
            String uri = id.getTag("uri");

            return "http.server.requests".equals(id.getName())
                    && ("/health".equals(uri) || "/metrics".equals(uri));
        });
    }
}
