package com.lrsachievement.app.service;

import com.lrsachievement.app.config.TwitchProperties;
import com.lrsachievement.app.config.YouTubeProperties;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String SESSION_TWITCH_LOGIN = "twitchLogin";
    public static final String SESSION_TWITCH_ID = "twitchId";
    public static final String SESSION_TWITCH_TOKEN = "twitchAccessToken";
    public static final String SESSION_YOUTUBE_TOKEN = "youtubeAccessToken";
    public static final String SESSION_YOUTUBE_REFRESH = "youtubeRefreshToken";

    private final TwitchProperties twitchProps;
    private final YouTubeProperties youtubeProps;
    private final WebClient webClient;

    public String buildTwitchAuthUrl() {
        return UriComponentsBuilder
                .fromUriString("https://id.twitch.tv/oauth2/authorize")
                .queryParam("client_id", twitchProps.getClientId())
                .queryParam("redirect_uri", twitchProps.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "user:read:email channel:read:subscriptions")
                .build().toUriString();
    }

    @SuppressWarnings("unchecked")
    public void handleTwitchCallback(String code, HttpSession session) {
        Map<String, Object> tokenResponse = webClient.post()
                .uri("https://id.twitch.tv/oauth2/token")
                .bodyValue(Map.of(
                        "client_id", twitchProps.getClientId(),
                        "client_secret", twitchProps.getClientSecret(),
                        "code", code,
                        "grant_type", "authorization_code",
                        "redirect_uri", twitchProps.getRedirectUri()
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String accessToken = (String) tokenResponse.get("access_token");

        Map<String, Object> userResponse = webClient.get()
                .uri("https://api.twitch.tv/helix/users")
                .header("Authorization", "Bearer " + accessToken)
                .header("Client-Id", twitchProps.getClientId())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map<String, Object> userData = ((java.util.List<Map<String, Object>>) userResponse.get("data")).get(0);

        session.setAttribute(SESSION_TWITCH_LOGIN, userData.get("login"));
        session.setAttribute(SESSION_TWITCH_ID, userData.get("id"));
        session.setAttribute(SESSION_TWITCH_TOKEN, accessToken);
    }

    public String buildYoutubeAuthUrl() {
        return UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", youtubeProps.getClientId())
                .queryParam("redirect_uri", youtubeProps.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "https://www.googleapis.com/auth/youtube.readonly")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build().toUriString();
    }

    @SuppressWarnings("unchecked")
    public void handleYoutubeCallback(String code, HttpSession session) {
        Map<String, Object> tokenResponse = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .bodyValue(Map.of(
                        "client_id", youtubeProps.getClientId(),
                        "client_secret", youtubeProps.getClientSecret(),
                        "code", code,
                        "grant_type", "authorization_code",
                        "redirect_uri", youtubeProps.getRedirectUri()
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        session.setAttribute(SESSION_YOUTUBE_TOKEN, tokenResponse.get("access_token"));
        if (tokenResponse.containsKey("refresh_token")) {
            session.setAttribute(SESSION_YOUTUBE_REFRESH, tokenResponse.get("refresh_token"));
        }
    }

    public void disconnectYoutube(HttpSession session) {
        session.removeAttribute(SESSION_YOUTUBE_TOKEN);
        session.removeAttribute(SESSION_YOUTUBE_REFRESH);
    }

    public boolean isTwitchAuthenticated(HttpSession session) {
        return session.getAttribute(SESSION_TWITCH_LOGIN) != null;
    }

    public boolean isYoutubeConnected(HttpSession session) {
        return session.getAttribute(SESSION_YOUTUBE_TOKEN) != null;
    }

    public String getTwitchLogin(HttpSession session) {
        return (String) session.getAttribute(SESSION_TWITCH_LOGIN);
    }

    public String getTwitchId(HttpSession session) {
        return (String) session.getAttribute(SESSION_TWITCH_ID);
    }

    public String getTwitchToken(HttpSession session) {
        return (String) session.getAttribute(SESSION_TWITCH_TOKEN);
    }

    public String getYoutubeToken(HttpSession session) {
        return (String) session.getAttribute(SESSION_YOUTUBE_TOKEN);
    }
}
