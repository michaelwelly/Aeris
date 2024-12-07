package com.aeris.bot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("${bot.username}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    private final OpenAIService openAIService;
    private final UserService userService;

    public TelegramBotService(OpenAIService openAIService, UserService userService) {
        this.openAIService = openAIService;
        this.userService = userService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            String response;
            // Обработка команды /start
            if (messageText.equalsIgnoreCase("/start")) {
                org.telegram.telegrambots.meta.api.objects.User user = update.getMessage().getFrom();
                try {
                    // Регистрация пользователя
                    userService.registerUser(
                            user.getFirstName(),
                            user.getLastName(),
                            user.getId().toString(),
                            user.getUserName(),
                            user.getLanguageCode()
                    );
                    response = "Добро пожаловать! Теперь вы можете задать вопрос, используя команду /ask.";
                } catch (RuntimeException e) {
                    response = "Пользователь уже зарегистрирован.";
                }
            } else if (messageText.startsWith("/ask")) {
                String userQuery = messageText.replace("/ask", "").trim();
                response = openAIService.getResponse(userQuery);
            } else {
                response = "Неизвестная команда. Используйте /ask для вопросов.";
            }

            // Отправляем сообщение пользователю
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(response);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}