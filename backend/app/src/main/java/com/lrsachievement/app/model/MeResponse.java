package com.lrsachievement.app.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeResponse {
    private String twitchLogin;
    private boolean youtubeConnected;
}
