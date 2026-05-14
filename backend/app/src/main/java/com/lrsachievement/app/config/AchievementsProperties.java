package com.lrsachievement.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "achievements")
public class AchievementsProperties {
    private String configPath;
    private String imagesPath;
}
