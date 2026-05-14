package com.lrsachievement.app.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Achievement {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private Platform platform;
    private boolean earned;
    private boolean locked;
}
