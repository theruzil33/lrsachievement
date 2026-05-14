package com.lrsachievement.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "twitch")
public class TwitchProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
}
