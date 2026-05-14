package com.lrsachievement.app.controller;

import com.lrsachievement.app.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/twitch/login")
    public RedirectView twitchLogin() {
        return new RedirectView(authService.buildTwitchAuthUrl());
    }

    @GetMapping("/twitch/callback")
    public RedirectView twitchCallback(@RequestParam String code, HttpSession session) {
        authService.handleTwitchCallback(code, session);
        return new RedirectView(frontendUrl + "/");
    }

    // /auth/youtube/** is protected by AuthInterceptor
    @GetMapping("/youtube/connect")
    public RedirectView youtubeConnect() {
        return new RedirectView(authService.buildYoutubeAuthUrl());
    }

    @GetMapping("/youtube/callback")
    public RedirectView youtubeCallback(@RequestParam String code, HttpSession session) {
        authService.handleYoutubeCallback(code, session);
        return new RedirectView(frontendUrl + "/settings");
    }

    @PostMapping("/youtube/disconnect")
    public void youtubeDisconnect(HttpSession session) {
        authService.disconnectYoutube(session);
    }

    @PostMapping("/logout")
    public void logout(HttpSession session) {
        session.invalidate();
    }
}
