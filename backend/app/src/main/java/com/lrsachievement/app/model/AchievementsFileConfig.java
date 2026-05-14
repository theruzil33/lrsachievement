package com.lrsachievement.app.model;

import lombok.Data;

import java.util.List;

@Data
public class AchievementsFileConfig {
    private List<AchievementDefinition> achievements;
}
