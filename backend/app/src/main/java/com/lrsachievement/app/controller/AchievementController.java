package com.lrsachievement.app.controller;

import com.lrsachievement.app.model.Achievement;
import com.lrsachievement.app.model.StatsResponse;
import com.lrsachievement.app.service.AchievementService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping("/achievements")
    public List<Achievement> getAchievements(HttpSession session) {
        return achievementService.getAchievements(session);
    }

    @GetMapping("/stats")
    public StatsResponse getStats(HttpSession session) {
        return achievementService.getStats(session);
    }
}
