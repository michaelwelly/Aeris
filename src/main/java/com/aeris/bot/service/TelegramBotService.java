package com.aeris.bot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("${bot.username}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    //    private final OpenAIService openAIService;
    private final UserService userService;

    public TelegramBotService(OpenAIService openAIService, UserService userService) {
//        this.openAIService = openAIService;
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
                    response = "Добро пожаловать, уважаемый гость!\n" +
                            "Меня зовут Альфред, ваш виртуальный дворецкий. Я здесь, чтобы помочь вам:\n" +
                            "\t•\tЗабронировать стол или отменить бронь.\n" +
                            "\t•\tПосмотреть меню ресторана.\n" +
                            "\t•\tУзнать наш адрес или полюбоваться интерьером.\n" +
                            "\t•\tОзнакомиться с афишей мероприятий.\n" +
                            "\t•\tЗадать любой интересующий вопрос.\n" +
                            "\t•\tЗаказать услуги кейтеринга.\n" +
                            "\n" +
                            "Выберите, пожалуйста, один из пунктов меню ниже, и я с удовольствием помогу вам.";
                } catch (RuntimeException e) {
                    response = "Пользователь уже зарегистрирован.";
                }
                // Отправка главного меню после команды /start
                sendMainMenu(chatId, response);

//            } else if (messageText.startsWith("/ask")) {
//                String userQuery = messageText.replace("/ask", "").trim();
//                response = openAIService.getResponse(userQuery);
//                // Здесь OpenAI может отвечать на пользовательские вопросы
            } else {
                response = "Неизвестная команда. Используйте кнопки ниже для взаимодействия.";
                sendMainMenu(chatId, response);
            }
        }
    }

    private void sendMainMenu(String chatId, String response) {
        // Создаем главное меню
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Забронировать стол");
        row1.add("Отменить бронь");
        row1.add("Меню");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Адрес");
        row2.add("Интерьер");
        row2.add("Афиша");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Задать вопрос");
        row3.add("Кейтеринг");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setResizeKeyboard(true); // Подгоняем клавиатуру под экран
        replyKeyboardMarkup.setOneTimeKeyboard(false); // Клавиатура остается открытой

        // Отправляем сообщение с главным меню
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(response);
        message.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}