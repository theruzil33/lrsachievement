package com.lrsachievement.app.model;

import lombok.Data;

@Data
public class AchievementDefinition {
    private String id;
    private String name;
    private String description;
    private String imageFile;
    private Platform platform;
    private AchievementRule rule;

    // Twitch
    private String channelName;

    // Threshold (MESSAGE_COUNT, VIEW_STREAK)
    private int threshold;

    // YouTube
    private String playlistId;
}
