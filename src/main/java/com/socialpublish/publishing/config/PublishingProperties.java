package com.socialpublish.publishing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.publishing")
public class PublishingProperties {

    private int maxRetries = 3;
    private long retryDelayMs = 30000;
    private int gracePeriodSilentMinutes = 15;
    private int gracePeriodMaxMinutes = 120;
}
