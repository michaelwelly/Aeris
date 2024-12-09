package com.aeris.bot.service;

import com.aeris.bot.model.MenuCard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("${bot.username}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    private final UserService userService;
    private final MenuCardService menuCardService;

    public TelegramBotService(UserService userService, MenuCardService menuCardService) {
        this.userService = userService;
        this.menuCardService = menuCardService;
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

            if (messageText.equalsIgnoreCase("/start")) {
                handleStartCommand(chatId, update.getMessage().getFrom());
            } else if (messageText.equalsIgnoreCase("Главное меню")) {
                sendMainMenu(chatId, "Вы вернулись в главное меню.");
            } else {
                handleUserMessage(chatId, messageText);
            }
        }
    }

    private void handleStartCommand(String chatId, org.telegram.telegrambots.meta.api.objects.User user) {
        try {
            userService.saveUser(user);
            sendMainMenu(chatId, "Добро пожаловать! Это главное меню.");
        } catch (RuntimeException e) {
            sendMainMenu(chatId, "Вы уже зарегистрированы. Это главное меню.");
        }
    }

    private void handleUserMessage(String chatId, String messageText) {
        switch (messageText) {
            case "Адрес":
                sendAddress(chatId);
                break;
            case "Забронировать стол":
                sendMessage(chatId, "Функция бронирования стола в разработке. Выберите другой пункт.");
                break;
            case "Отменить бронь":
                sendMessage(chatId, "Функция отмены брони в разработке. Выберите другой пункт.");
                break;
            case "Меню":
                sendMenuMain(chatId); // Переход в подменю "Меню"
                break;
            case "Бар":
            case "Ежедневное меню":
            case "Элементы":
            case "Кухня":
            case "Винный зал":
                handleMenuSelection(chatId, messageText);
                break;
            default:
                sendMessage(chatId, "Неизвестная команда. Вы можете вернуться в главное меню.");
                sendMainMenu(chatId, "Главное меню:");
                break;
        }
    }

    private void sendMainMenu(String chatId, String text) {
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
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMenuMain(String chatId) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Кнопки для 5 карт
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Бар");
        row1.add("Ежедневное меню");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Элементы");
        row2.add("Кухня");
        row2.add("Винный зал");

        // Кнопка возврата в главное меню
        KeyboardRow row3 = new KeyboardRow();
        row3.add("Главное меню");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Добро пожаловать в меню карт! Здесь вы можете ознакомиться с:\n" +
                "1. Барной картой\n" +
                "2. Ежедневным меню\n" +
                "3. Картой \"Элементы\"\n" +
                "4. Картой кухни\n" +
                "5. Винной картой\n\n" +
                "Выберите интересующий раздел:");
        message.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleMenuSelection(String chatId, String menuSelection) {
        String menuName;
        switch (menuSelection) {
            case "Бар":
                menuName = "BAR CARD";
                break;
            case "Ежедневное меню":
                menuName = "DAILY MENU CARD";
                break;
            case "Элементы":
                menuName = "ELEMENTS CARD";
                break;
            case "Кухня":
                menuName = "KITCHEN CARD";
                break;
            case "Винный зал":
                menuName = "WINE ROOM CARD";
                break;
            default:
                sendMessage(chatId, "Извините, такой карты нет. Пожалуйста, выберите из доступных вариантов.");
                sendMenuMain(chatId);
                return;
        }
        sendMenu(chatId, menuName);
    }

    private void sendMenu(String chatId, String menuName) {
        MenuCard menuCard = menuCardService.getMenuCardByName(menuName);

        if (menuCard != null) {
            // Отправляем описание меню
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(menuCard.getDescription());
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            // Отправляем PDF-файл
            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            org.telegram.telegrambots.meta.api.objects.InputFile inputFile = new org.telegram.telegrambots.meta.api.objects.InputFile();

            // Устанавливаем файл из пути
            File file = new File(menuCard.getFilePath());
            if (file.exists() && file.isFile()) {
                inputFile.setMedia(file);
                document.setDocument(inputFile);

                try {
                    execute(document);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                sendMessage(chatId, "Извините, файл меню не найден.");
            }

            sendMenuMain(chatId); // Возврат в меню карт
        } else {
            sendMessage(chatId, "Извините, меню не найдено.");
            sendMenuMain(chatId);
        }
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendAddress(String chatId) {
        String addressText = "Уважаемый посетитель,\n" +
                "Наш ресторан расположен по адресу: ул. Мамина Сибиряка 58, г. Екатеринбург.\n" +
                "Это место с богатой историей: здесь когда-то находилась библиотека, где собирались выдающиеся умы города, " +
                "а позже — уютный салон, где обсуждали искусство и музыку.\n\n" +
                "Сегодня это ресторан AERIS — место, сочетающее утонченную атмосферу с современным стилем.\n\n" +
                "Посетите нас: [AERIS на Яндекс.Картах](https://yandex.com/maps/-/CHAlAWN~).\n";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(addressText);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}