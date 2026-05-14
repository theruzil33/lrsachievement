package com.lrsachievement.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "youtube")
public class YouTubeProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
}
