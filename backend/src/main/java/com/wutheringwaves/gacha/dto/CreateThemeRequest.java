package com.wutheringwaves.gacha.dto;

import java.util.List;

public record CreateThemeRequest(String name, String description, List<String> generateCategories) {}
