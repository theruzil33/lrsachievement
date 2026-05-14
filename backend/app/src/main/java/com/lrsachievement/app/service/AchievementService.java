package com.lrsachievement.app.service;

import com.lrsachievement.app.model.Achievement;
import com.lrsachievement.app.model.AchievementDefinition;
import com.lrsachievement.app.model.Platform;
import com.lrsachievement.app.model.StatsResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final List<AchievementDefinition> definitions;
    private final TwitchService twitchService;
    private final YouTubeService youtubeService;
    private final AuthService authService;

    public List<Achievement> getAchievements(HttpSession session) {
        String userId = authService.getTwitchId(session);
        String twitchToken = authService.getTwitchToken(session);
        boolean youtubeConnected = authService.isYoutubeConnected(session);
        String youtubeToken = authService.getYoutubeToken(session);

        return definitions.stream()
                .map(def -> toAchievement(def, userId, twitchToken, youtubeConnected, youtubeToken))
                .toList();
    }

    public StatsResponse getStats(HttpSession session) {
        List<Achievement> achievements = getAchievements(session);
        boolean youtubeConnected = authService.isYoutubeConnected(session);

        long twitchTotal = achievements.stream().filter(a -> a.getPlatform() == Platform.TWITCH).count();
        long twitchEarned = achievements.stream().filter(a -> a.getPlatform() == Platform.TWITCH && a.isEarned()).count();
        long ytTotal = achievements.stream().filter(a -> a.getPlatform() == Platform.YOUTUBE).count();
        long ytEarned = achievements.stream().filter(a -> a.getPlatform() == Platform.YOUTUBE && a.isEarned()).count();

        return StatsResponse.builder()
                .total((int) (twitchTotal + ytTotal))
                .earned((int) (twitchEarned + ytEarned))
                .twitch(StatsResponse.PlatformStats.builder()
                        .total((int) twitchTotal)
                        .earned((int) twitchEarned)
                        .locked(false)
                        .build())
                .youtube(StatsResponse.PlatformStats.builder()
                        .total((int) ytTotal)
                        .earned((int) ytEarned)
                        .locked(!youtubeConnected)
                        .build())
                .build();
    }

    private Achievement toAchievement(AchievementDefinition def,
                                       String userId, String twitchToken,
                                       boolean youtubeConnected, String youtubeToken) {
        boolean locked = def.getPlatform() == Platform.YOUTUBE && !youtubeConnected;
        boolean earned = false;

        if (!locked) {
            if (def.getPlatform() == Platform.TWITCH) {
                earned = twitchService.checkAchievement(def, userId, twitchToken);
            } else {
                earned = youtubeService.checkAchievement(def, youtubeToken);
            }
        }

        return Achievement.builder()
                .id(def.getId())
                .name(def.getName())
                .description(def.getDescription())
                .imageUrl("/api/images/" + def.getImageFile())
                .platform(def.getPlatform())
                .earned(earned)
                .locked(locked)
                .build();
    }
}
