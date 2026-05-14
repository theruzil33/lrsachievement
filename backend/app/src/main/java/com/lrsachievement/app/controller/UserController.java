package com.lrsachievement.app.controller;

import com.lrsachievement.app.model.MeResponse;
import com.lrsachievement.app.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    public MeResponse me(HttpSession session) {
        return MeResponse.builder()
                .twitchLogin(authService.getTwitchLogin(session))
                .youtubeConnected(authService.isYoutubeConnected(session))
                .build();
    }
}
