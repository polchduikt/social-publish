package com.socialpublish.integrations.reddit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "reddit")
public class RedditOAuthProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String userAgent = "SocialPublish/1.0 by SocialPublishApp";
}
