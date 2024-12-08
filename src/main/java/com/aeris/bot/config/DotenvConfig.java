package com.aeris.bot.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DotenvConfig {

    @PostConstruct
    public void loadEnvVariables() {
        Dotenv dotenv = Dotenv.configure()
                .directory("/Users/michaelwelly/IdeaProjects/Aeris") // Укажите путь к .env
                .load();

        // Проверим, загружается ли DB_URL
        System.out.println("DB_URL: " + dotenv.get("DB_URL"));

        // Если требуется, установите переменные окружения вручную
        System.setProperty("DB_URL", dotenv.get("DB_URL"));
        System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
        System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
    }
}