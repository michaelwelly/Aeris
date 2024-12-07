package com.aeris.bot.config;

import com.aeris.bot.service.TelegramBotService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBotService telegramBotService) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(telegramBotService);
        return botsApi;
    }
}