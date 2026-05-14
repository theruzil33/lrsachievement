package com.lrsachievement.app.config;

import com.lrsachievement.app.model.AchievementDefinition;
import com.lrsachievement.app.model.AchievementsFileConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AchievementLoader {

    private final AchievementsProperties props;

    @Bean
    public List<AchievementDefinition> achievementDefinitions() throws IOException {
        Resource resource = resolveResource(props.getConfigPath());
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(AchievementsFileConfig.class, options));
        try (InputStream is = resource.getInputStream()) {
            AchievementsFileConfig config = yaml.load(is);
            return config != null && config.getAchievements() != null
                    ? config.getAchievements()
                    : List.of();
        }
    }

    private Resource resolveResource(String path) {
        if (path.startsWith("file:")) {
            return new FileSystemResource(path.substring(5));
        }
        return new ClassPathResource(path);
    }
}
