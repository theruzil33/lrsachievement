package com.lrsachievement.app.service;

import com.lrsachievement.app.config.TwitchProperties;
import com.lrsachievement.app.model.AchievementDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwitchService {

    private final WebClient webClient;
    private final TwitchProperties twitchProps;

    public boolean checkAchievement(AchievementDefinition def, String userId, String accessToken) {
        try {
            return switch (def.getRule()) {
                case SUBSCRIPTION -> checkSubscription(def.getChannelName(), userId, accessToken);
                case VIEW_STREAK -> checkViewStreak(def.getThreshold(), userId, accessToken);
                default -> false;
            };
        } catch (Exception e) {
            log.warn("Twitch check failed for {}: {}", def.getId(), e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean checkSubscription(String channelName, String userId, String accessToken) {
        String broadcasterId = getBroadcasterId(channelName, accessToken);
        if (broadcasterId == null) return false;

        try {
            Map<String, Object> response = webClient.get()
                    .uri(u -> u.scheme("https").host("api.twitch.tv")
                            .path("/helix/subscriptions/user")
                            .queryParam("broadcaster_id", broadcasterId)
                            .queryParam("user_id", userId)
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Client-Id", twitchProps.getClientId())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<?> data = (List<?>) response.get("data");
            return data != null && !data.isEmpty();
        } catch (WebClientResponseException.NotFound e) {
            return false;
        }
    }

    private boolean checkViewStreak(int threshold, String userId, String accessToken) {
        // Twitch REST API does not expose view streak data directly.
        // This is a placeholder — real implementation would require EventSub.
        log.debug("VIEW_STREAK check not available via REST API for user {}", userId);
        return false;
    }

    @SuppressWarnings("unchecked")
    private String getBroadcasterId(String channelName, String accessToken) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(u -> u.scheme("https").host("api.twitch.tv")
                            .path("/helix/users")
                            .queryParam("login", channelName)
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Client-Id", twitchProps.getClientId())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data == null || data.isEmpty()) return null;
            return (String) data.get(0).get("id");
        } catch (Exception e) {
            log.warn("Could not resolve broadcaster id for {}: {}", channelName, e.getMessage());
            return null;
        }
    }
}
