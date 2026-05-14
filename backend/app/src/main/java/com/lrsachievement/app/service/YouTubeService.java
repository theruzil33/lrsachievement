package com.lrsachievement.app.service;

import com.lrsachievement.app.model.AchievementDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeService {

    private final WebClient webClient;

    public boolean checkAchievement(AchievementDefinition def, String accessToken) {
        try {
            return switch (def.getRule()) {
                case PLAYLIST_ONE_VIDEO -> checkPlaylistOneVideo(def.getPlaylistId(), accessToken);
                case PLAYLIST_ALL_VIDEOS -> checkPlaylistAllVideos(def.getPlaylistId(), accessToken);
                default -> false;
            };
        } catch (Exception e) {
            log.warn("YouTube check failed for {}: {}", def.getId(), e.getMessage());
            return false;
        }
    }

    private boolean checkPlaylistOneVideo(String playlistId, String accessToken) {
        Set<String> watched = getWatchedVideoIds(accessToken);
        Set<String> playlistItems = getPlaylistVideoIds(playlistId, accessToken);
        for (String id : playlistItems) {
            if (watched.contains(id)) return true;
        }
        return false;
    }

    private boolean checkPlaylistAllVideos(String playlistId, String accessToken) {
        Set<String> watched = getWatchedVideoIds(accessToken);
        Set<String> playlistItems = getPlaylistVideoIds(playlistId, accessToken);
        return !playlistItems.isEmpty() && watched.containsAll(playlistItems);
    }

    @SuppressWarnings("unchecked")
    private Set<String> getPlaylistVideoIds(String playlistId, String accessToken) {
        Set<String> ids = new HashSet<>();
        String pageToken = null;
        do {
            final String token = pageToken;
            Map<String, Object> response = webClient.get()
                    .uri(u -> {
                        var b = u.scheme("https").host("www.googleapis.com")
                                .path("/youtube/v3/playlistItems")
                                .queryParam("part", "contentDetails")
                                .queryParam("playlistId", playlistId)
                                .queryParam("maxResults", "50");
                        if (token != null) b = b.queryParam("pageToken", token);
                        return b.build();
                    })
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    Map<String, Object> cd = (Map<String, Object>) item.get("contentDetails");
                    if (cd != null) ids.add((String) cd.get("videoId"));
                }
            }
            pageToken = (String) response.get("nextPageToken");
        } while (pageToken != null);
        return ids;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getWatchedVideoIds(String accessToken) {
        // YouTube API returns liked videos as a proxy for watch history
        // (full watch history is not available via API).
        Set<String> ids = new HashSet<>();
        try {
            Map<String, Object> response = webClient.get()
                    .uri(u -> u.scheme("https").host("www.googleapis.com")
                            .path("/youtube/v3/videos")
                            .queryParam("part", "id")
                            .queryParam("myRating", "like")
                            .queryParam("maxResults", "200")
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    ids.add((String) item.get("id"));
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch liked videos: {}", e.getMessage());
        }
        return ids;
    }
}
