package com.lrsachievement.app.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsResponse {
    private int total;
    private int earned;
    private PlatformStats twitch;
    private PlatformStats youtube;

    @Data
    @Builder
    public static class PlatformStats {
        private int total;
        private int earned;
        private boolean locked;
    }
}
