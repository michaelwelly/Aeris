package com.aeris.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class TelegramBotConfig {

    @Bean
    public Map<String, String> imagePaths() {
        String basePath = "/Users/michaelwelly/Aeris-Dvoretsky/";
        Map<String, String> paths = new HashMap<>();
        paths.put("avatar start", basePath + "avatarStart.jpeg");
        paths.put("avatar telegram", basePath + "avatarTelegram.jpeg");
        paths.put("conform", basePath + "conform.jpeg");
        paths.put("date", basePath + "date.jpeg");
        paths.put("default", basePath + "default.jpeg");
        paths.put("events", basePath + "events.jpeg");
        paths.put("interview", basePath + "interview.jpeg");
        paths.put("main menu", basePath + "mainMenu.jpeg");
        paths.put("menu", basePath + "menu.jpeg");
        paths.put("navigation", basePath + "navigation.jpeg");
        paths.put("table", basePath + "table.jpeg");
        paths.put("time", basePath + "time.jpeg");
        paths.put("zone", basePath + "zone.jpeg");
        return paths;
    }
}