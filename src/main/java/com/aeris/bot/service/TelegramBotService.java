package com.aeris.bot.service;

import com.aeris.bot.model.Order;
import com.aeris.bot.model.RestaurantTable;
import com.aeris.bot.model.SlotAvailability;
import com.aeris.bot.model.User;
import com.aeris.bot.repository.OrderRepository;
import com.aeris.bot.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TelegramBotService extends TelegramLongPollingBot {


    @Value("${bot.username}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    private final UserService userService;
    private final OrderService orderService;
    private final RestaurantTableService restaurantTableService;
    private final SlotAvailabilityService slotAvailabilityService;
    private final OrderRepository orderRepository;
    private UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public TelegramBotService(UserService userService, OrderService orderService, RestaurantTableService restaurantTableService, SlotAvailabilityService slotAvailabilityService, OrderRepository orderRepository, UserRepository userRepository) {
        this.userService = userService;
        this.orderService = orderService;
        this.restaurantTableService = restaurantTableService;
        this.slotAvailabilityService = slotAvailabilityService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
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
        // Проверяем, пришло ли текстовое сообщение
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            if (messageText.equalsIgnoreCase("/start")) {
                handleStartCommand(chatId, update.getMessage().getFrom());
            } else if (messageText.equalsIgnoreCase("Главное меню")) {
                sendMainMenu(chatId, "Вы вернулись в главное меню.");
            } else if (messageText.equalsIgnoreCase("Бронирование")) {
                sendBookingMenu(chatId); // Переход в меню бронирования
            } else if (messageText.equalsIgnoreCase("Меню")) {
                sendMenuMain(chatId); // Переход в подменю "Меню"
            } else if (messageText.equalsIgnoreCase("Отменить бронирование")) {
                handleCancelBooking(chatId); // Отмена бронирования
            } else if (messageText.equalsIgnoreCase("Адрес")) {
                sendAddress(chatId); // Отправка адреса
            } else if (messageText.equalsIgnoreCase("Интерьер")) {
                sendInteriorMenu(chatId); // Отправка информации об интерьере
            } else if (messageText.equalsIgnoreCase("Афиша")) {
                sendEventsMenu(chatId); // Отправка афиши мероприятий
            } else {
                handleUserMessage(chatId, messageText); // Обработка остальных сообщений
            }
        }
        // Обработка callback-запросов
        else if (update.hasCallbackQuery()) {
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            String callbackData = update.getCallbackQuery().getData();

            // Передаем callback для обработки
            handleCallbackQuery(chatId, callbackData);
        }
        // Обработка фото
        else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            String chatId = update.getMessage().getChatId().toString();
            handlePhoto(chatId, update.getMessage().getPhoto());
        }
        // Обработка локации
        else if (update.hasMessage() && update.getMessage().hasLocation()) {
            String chatId = update.getMessage().getChatId().toString();
            handleLocation(chatId, update.getMessage().getLocation());
        }
        // Обработка файлов (например, документов)
        else if (update.hasMessage() && update.getMessage().hasDocument()) {
            String chatId = update.getMessage().getChatId().toString();
            handleDocument(chatId, update.getMessage().getDocument());
        }
        // Обработка голосовых сообщений
        else if (update.hasMessage() && update.getMessage().hasVoice()) {
            String chatId = update.getMessage().getChatId().toString();
            handleVoice(chatId, update.getMessage().getVoice());
        }
        // Обработка видео
        else if (update.hasMessage() && update.getMessage().hasVideo()) {
            String chatId = update.getMessage().getChatId().toString();
            handleVideo(chatId, update.getMessage().getVideo());
        }
        // Обработка других типов сообщений
        else if (update.hasMessage()) {
            String chatId = update.getMessage().getChatId().toString();
            handleUnsupportedMessage(chatId, update.getMessage());
        }
    }

    private void handlePhoto(String chatId, List<PhotoSize> photos) {
        sendMessage(chatId, "Спасибо за отправленное фото! Мы его обработаем.");
    }
    private void handleLocation(String chatId, Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        sendMessage(chatId, "Спасибо за отправленную локацию! Широта: " + latitude + ", Долгота: " + longitude);
    }
    private void handleDocument(String chatId, Document document) {
        sendMessage(chatId, "Спасибо за отправленный файл. В данный момент мы не обрабатываем файлы.");
    }
    private void handleCancelBooking(String chatId) {
        try {
            // Получаем или создаем идентификатор заказа
            UUID orderId = getOrCreateOrderId(chatId);

            // Отмена бронирования
            orderService.cancelOrder(orderId);

            // Уведомление пользователя
            sendMessage(chatId, "❌ Ваше бронирование успешно отменено.");
            sendMainMenu(chatId, "Вы вернулись в главное меню.");
        } catch (EntityNotFoundException e) {
            sendMessage(chatId, "Заказ не найден. Возможно, бронирование уже было отменено.");
            e.printStackTrace();
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка при отмене бронирования. Попробуйте снова.");
            e.printStackTrace();
        }
    }
    private void handleVoice(String chatId, Voice voice) {
        sendMessage(chatId, "Спасибо за голосовое сообщение! В данный момент эта функция не поддерживается.");
    }
    private void handleVideo(String chatId, Video video) {
        sendMessage(chatId, "Спасибо за отправленное видео! В данный момент эта функция не поддерживается.");
    }
    private void handleUnsupportedMessage(String chatId, Message message) {
        sendMessage(chatId, "Извините, я пока не могу обработать этот тип сообщений.");
    }

    private void handleStartCommand(String chatId, org.telegram.telegrambots.meta.api.objects.User user) {
        try {
            // Сохраняем пользователя
            userService.saveUser(user);

            // Формируем сообщение приветствия
            String welcomeMessage = """
            Привет! Меня зовут Альфред. Я — ваш персональный помощник, который поможет вам:
            🕒 Забронировать столик в удобное для вас время.
            📋 Узнать наше меню, интерьер и афишу мероприятий.
            🗺 Найти адрес и информацию о нашем заведении.
            
            Всё просто: выбирайте нужный пункт меню, а я всё сделаю за вас!
            
            Добро пожаловать в главное меню:
            """;

            // Отправляем главное меню с приветствием
            sendMainMenu(chatId, welcomeMessage);
        } catch (RuntimeException e) {
            // Пользователь уже зарегистрирован
            String alreadyRegisteredMessage = """
            Добро пожаловать обратно! 
            Я уже знаю вас и готов помочь вам снова:
            🕒 Забронировать столик.
            📋 Ознакомиться с меню, интерьером и афишей.
            🗺 Найти адрес нашего заведения.

            Выбирайте в меню, и я всё сделаю за вас!
            """;

            sendMainMenu(chatId, alreadyRegisteredMessage);
        }
    }

    private void handleUserMessage(String chatId, String messageText) {
        switch (messageText) {
            case "Бронирование":
                // Отображаем вводное сообщение и меню бронирования
                sendBookingMenu(chatId);
                break;

            case "Выбрать дату":
                sendDateSelection(chatId);
            case "Выбрать время":

            case "Выбрать слот":
            case "Выбрать зону":
            case "Выбрать номер стола":
            case "Отменить бронирование":
//                handleBookingMenuSelection(chatId, messageText);
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

            case "Адрес":
                sendAddress(chatId);
                break;

            case "Интерьер":
                sendInteriorMenu(chatId);
                break;

            case "Афиша":
                sendEventsMenu(chatId);
                break;

            case "Задать вопрос":
                sendMessage(chatId, "Функция отмены бронирования пока в разработке.");
                break;

            case "Кейтеринг":
                sendMessage(chatId, "Функция отмены бронирования пока в разработке.");
                break;

            case "Главное меню":
                sendMainMenu(chatId, "Вы вернулись в главное меню.");
                break;

            default:
                sendMessage(chatId, "Извините, я не понял эту команду. Вы можете вернуться в главное меню.");
                sendMainMenu(chatId, "Главное меню:");
                break;
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
        switch (menuSelection) {
            case "Бар":
                sendBarMenu(chatId);
                break;
            case "Ежедневное меню":
                sendDailyMenu(chatId);
                break;
            case "Элементы":
                sendElementsMenu(chatId);
                break;
            case "Кухня":
                sendKitchenMenu(chatId);
                break;
            case "Винный зал":
                sendWineRoomMenu(chatId);
                break;
            default:
                sendMessage(chatId, "Извините, такой карты нет. Пожалуйста, выберите из доступных вариантов.");
        }
    }
    private void sendBarMenu(String chatId) {
        // Отправляем описание барной карты
        String description = "*Барная карта*\n\n" +
                "_Каждый элемент в этой карте объединяет в себе все стихии природы и создает AERIS. Создается за 7 секунд_";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(description);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        File file = new File("/Users/michaelwelly/Desktop/AERISMENU/BAR CARD.pdf");
        if (file.exists() && file.isFile()) {
            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new org.telegram.telegrambots.meta.api.objects.InputFile(file));
            try {
                execute(document);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            // Сообщение об ошибке, если файл не найден
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("Извините, файл с барной картой не найден.");
            try {
                execute(errorMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }}
    }
    private void sendDailyMenu(String chatId) {
        // Текст описания ежедневного меню
        String description = "*𝐀𝐄𝐑𝐈𝐒 𝐃𝐀𝐈𝐋𝐘 𝐌𝐄𝐍𝐔*\n\n" +
                "_AERIS — запускаем дневное меню!_\n" +
                "В будние дни с *11:45 до 16:00* мы открываем двери для тех, кто ищет что-то большее, чем просто обед. " +
                "Лаконичное меню в средиземноморском стиле с акцентом на сезонные продукты и чистый вкус — " +
                "идеальный повод сделать паузу и насладиться моментом.\n\n" +
                "🍷 *Вино Дня* — новая композиция в вашем бокале каждый день по приятной цене. Мы искусно подбираем вино, чтобы оно стало идеальным аккомпанементом вашего обеда.\n\n" +
                "Забудьте про суету будней — заходите в AERIS за вкусом, который запомнится!\n\n" +
                "*𝐀 𝐐 𝐔 𝐀 | 𝐈 𝐆 𝐍 𝐈 𝐒 | 𝐀 𝐄 𝐑 | 𝐓 𝐄 𝐑 𝐑 𝐀*\n\n";

        // Отправляем текст с описанием
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(description);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // Отправляем изображение
        File imageFile = new File("/Users/michaelwelly/Desktop/AERISMENU/DAILY_MENU_IMAGE.png"); // Укажите путь к изображению
        if (imageFile.exists() && imageFile.isFile()) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new org.telegram.telegrambots.meta.api.objects.InputFile(imageFile));
            try {
                execute(photo);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            // Сообщение об ошибке, если изображение не найдено
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("Извините, изображение с ежедневным меню не найдено.");
            try {
                execute(errorMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        // Отправляем PDF-файл с ежедневным меню
        File file = new File("/Users/michaelwelly/Desktop/AERISMENU/DAILY MENU CARD.pdf");
        if (file.exists() && file.isFile()) {
            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new org.telegram.telegrambots.meta.api.objects.InputFile(file));
            try {
                execute(document);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            // Сообщение об ошибке, если файл не найден
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("Извините, файл с ежедневным меню не найден.");
            try {
                execute(errorMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    private void sendElementsMenu(String chatId) {
        // Текст описания меню "Элементы"
        String description = "*ELEMENTS*\n\n" +
                "_Авторская коктейльная карта по мотивам четырех стихий, от нашего шеф-бармена Даниила Маленковича._\n\n" +
                "Коктейли *огня* — это яркие вспышки вкуса, где сладость и кислинка искрятся на фоне пряных акцентов, как огонь на углях. " +
                "Табаско в «Фарро» и перечный ликер в «Ладоне» придают остроту, а фрукты и мята смягчают жар, добавляя свежести. Контрасты и многослойность создают напитки с характером, пылающие эмоциями.\n\n" +
                "Коктейли *воздуха* — легки, как облака. Кокосовая пена в «Зефире» и кремовость «Ваю-Вата» придают им невесомость, а вкусы балансируют между сладостью, кислинкой и травяными оттенками. Эти напитки, будто шепот ветра, скрывают глубину за мягкостью, оставляя тонкое и запоминающееся послевкусие.\n\n" +
                "Коктейли *земли* — звучат, как эхо природы. Древесные и пряные ноты, соединенные травяным джином, дополняются сладостью цветов и фруктов, сочетания играют словно солнечные лучи на лесной поляне. Воздушная пена «Нимфы» и сложность «Флоры» раскрывают естественную гармонию и многослойную глубину.\n\n" +
                "Коктейли *воды* — текут, как прохладные ручьи. Фруктовая сладость и кислинка струятся в пузырьковой текстуре, а солоноватые акценты в «Агве» напоминают морской бриз. Эти освежающие напитки словно созданы для летних вечеров и гастрономических экспериментов.\n\n" +
                "*𝐀 𝐐 𝐔 𝐀 | 𝐈 𝐆 𝐍 𝐈 𝐒 | 𝐀 𝐄 𝐑 | 𝐓 𝐄 𝐑 𝐑 𝐀*\n\n";
        // Отправляем текст с описанием
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(description);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // Отправляем изображение
        File imageFile = new File("/Users/michaelwelly/Desktop/AERISMENU/ELEMENTS_IMAGE.png"); // Укажите путь к изображению
        if (imageFile.exists() && imageFile.isFile()) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new org.telegram.telegrambots.meta.api.objects.InputFile(imageFile));
            try {
                execute(photo);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            // Сообщение об ошибке, если изображение не найдено
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("Извините, изображение с меню \"Элементы\" не найдено.");
            try {
                execute(errorMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        // Отправляем PDF-файл с меню "Элементы"
        File file = new File("/Users/michaelwelly/Desktop/AERISMENU/ELEMENTS CARD.pdf");
        if (file.exists() && file.isFile()) {
            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new org.telegram.telegrambots.meta.api.objects.InputFile(file));
            try {
                execute(document);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            // Сообщение об ошибке, если файл не найден
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("Извините, файл с меню \"Элементы\" не найден.");
            try {
                execute(errorMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    private void sendKitchenMenu(String chatId) {
        // Описание меню "Кухня"
        String description = "*𝐀𝐄𝐑𝐈𝐒 𝐊𝐈𝐓𝐂𝐇𝐄𝐍*\n\n" +
                "_Средиземноморская кухня – совокупность лучших элементов и кулинарных традиций, родом из Древней Греции, Рима и современных государств, расположенных на их территории._\n\n" +
                "*Олива* – символ плодородия, мира и изобилия, дар богов людям. Незаменимый источник жирных кислот, необходимых для сохранения молодости и спокойствия. Масло оливы – бесценный сок жизни, дарованный нам стихиями земли и солнца, источник тонкого и пряного аромата, так присущего блюдам солнечных регионов Европы.\n\n" +
                "*Свежие овощи* – энергия солнца и земли, запасённая в ярких и сочных плодах, источник витаминов и хорошего настроения.\n\n" +
                "*Рыба и морепродукты* – дары моря, название говорит само за себя. Драгоценный источник белка и незаменимых кислот, залог долголетия, крепости тела и страсти к жизни.\n\n" +
                "*Красное мясо* – в количествах источник белка, железа, связь человечества со стихией земли, ну и конечно же источник ни с чем не сравнимого аромата, при одной мысли о котором пробуждается аппетит.\n\n" +
                "*Паста* – энергия солнца, земли и воды, запасённая для человечества.\n\n" +
                "А всё вместе – гармония вкусов, рождающих совершенство.\n\n" +
                "⟡ *ОЗНАКОМИТЬСЯ С МЕНЮ*  ⟡\n\n" +
                "*𝐀 𝐐 𝐔 𝐀 | 𝐈 𝐆 𝐍 𝐈 𝐒 | 𝐀 𝐄 𝐑 | 𝐓 𝐄 𝐑 𝐑 𝐀*\n\n";

        // Отправляем текстовое сообщение
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(description);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // Отправляем изображение
        File imageFile = new File("/Users/michaelwelly/Desktop/AERISMENU/KITCHEN_IMAGE.png"); // Замените путь на правильный
        if (imageFile.exists() && imageFile.isFile()) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new org.telegram.telegrambots.meta.api.objects.InputFile(imageFile));
            try {
                execute(photo);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        // Отправляем PDF-файл с меню "Кухня"
        File file = new File("/Users/michaelwelly/Desktop/AERISMENU/KITCHEN CARD.pdf");
        if (file.exists() && file.isFile()) {
            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new org.telegram.telegrambots.meta.api.objects.InputFile(file));
            try {
                execute(document);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            sendErrorFileNotFound(chatId, "Кухня");
        }
    }
    private void sendWineRoomMenu(String chatId) {
        // Текст описания меню "Винный зал"
        String description = "*𝐀𝐄𝐑𝐈𝐒 𝐖𝐈𝐍𝐄 𝐑𝐎𝐎𝐌*\n\n" +
                "_Анклав (от лат. Inclavatus, окружённый – государство в государстве) истинных ценителей жаркого солнца, разлитого по бутылкам: винная комната, вместимостью до 10 персон._\n\n" +
                "По совместительству она является порталом в любую точку мира, если только в ней производят вино.\n\n" +
                "Проводником же в увлекательном путешествии по странам, рождающим лучшие вина, станет сомелье гастробара *“AERIS”*. В коллекции ключей от портала – более восьмидесяти наименований, как широко известных публике, так и ждущих своих поклонников. Имейте в виду: путешествие может быть продолжительным, в формате дегустации, винного вечера или иного мероприятия.\n\n" +
                "Тем, кто не готов к длительному путешествию, но лишь желает совершить краткую прогулку и насладиться частицей жидкого солнца, сомелье заведения с профессиональной чуткостью помогут создать идеальное сочетание с дарами земли и моря.\n\n" +
                "*Wine Pairing* – искусство подбирать подходящие напитки к блюдам на молекулярном, высшем уровне. Присутствие сомелье открывает доступ к этому искусству для каждого гостя.\n\n";

        // Отправляем текст с описанием
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(description);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // Отправляем изображение
        File imageFile = new File("/Users/michaelwelly/Desktop/AERISMENU/WINE_ROOM_IMAGE.png"); // Укажите путь к изображению
        if (imageFile.exists() && imageFile.isFile()) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new org.telegram.telegrambots.meta.api.objects.InputFile(imageFile));
            try {
                execute(photo);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            // Сообщение об ошибке, если изображение не найдено
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("Извините, изображение с меню \"Винный зал\" не найдено.");
            try {
                execute(errorMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        // Отправляем PDF-файл с меню "Винный зал"
        File file = new File("/Users/michaelwelly/Desktop/AERISMENU/WINE ROOM CARD.pdf");
        if (file.exists() && file.isFile()) {
            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new org.telegram.telegrambots.meta.api.objects.InputFile(file));
            try {
                execute(document);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            // Сообщение об ошибке, если файл не найден
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("Извините, файл с меню \"Винный зал\" не найден.");
            try {
                execute(errorMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    private void sendInteriorMenu(String chatId) {
        // Отправляем описание интерьера
        String description = "*AERIS INTERIOR*\n\n" +
                "В интерьере заведения нашла отражение концепция гармонии четырёх стихий, известная ещё античным грекам. " +
                "Эллины по праву считаются основоположниками таких наук, как философия, геометрия, физика, алхимия. Именно они определили развитие европейской культуры на века и тысячелетия.\n\n" +
                "Гармония рождается из баланса, баланс из пропорции – сочетание проверенных веками натуральных материалов и современных технологий, лёгкости воздушного пространства и весомости мрамора, тепла натурального дерева и прохлады сочной зелени. Всё это в полной мере про интерьер «AERIS».\n\n" +
                "Порцию экзотики добавляют элементы стихий в их первозданном виде, но актуальном прочтении: стена воды и языки пламени, соседствующие бок о бок и дополняющие друг друга.\n\n" +
                "Компания из 4-5 человек за просторным столом и тет-а-тет вечер с авторскими коктейлями за барной стойкой, октет ценителей вин и полноценный форум на 12 персон – всем формам и числам найдётся подобающее место в геометрии «AERIS».\n\n";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(description);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // Отправка фотографии
        File photoFile = new File("/Users/michaelwelly/Desktop/AERISMENU/INTERIOR.jpg");
        if (photoFile.exists() && photoFile.isFile()) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new org.telegram.telegrambots.meta.api.objects.InputFile(photoFile));

            try {
                execute(photo);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            sendErrorFileNotFound(chatId, "Интерьер (фото)");
        }

        // Отправка видео
        File videoFile = new File("/Users/michaelwelly/Desktop/AERISMENU/INTERIOR.mp4");
        if (videoFile.exists() && videoFile.isFile()) {
            SendDocument video = new SendDocument();
            video.setChatId(chatId);
            video.setDocument(new org.telegram.telegrambots.meta.api.objects.InputFile(videoFile));

            try {
                execute(video);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            sendErrorFileNotFound(chatId, "Интерьер (видео)");
        }
    }
    private void sendEventsMenu(String chatId) {
        // Описание афиши
        String description = "*AERIS Events*\n\n" +
                "В нашем гастробаре всегда происходят яркие события, которые вы не захотите пропустить! " +
                "Узнайте о предстоящих мероприятиях, вечерах живой музыки и эксклюзивных дегустациях. " +
                "Следите за анонсами на нашем Telegram-канале: [@aeris_gastrobar](https://t.me/aeris_gastrobar).\n\n";

        // Отправка описания
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(description);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // Отправка картинки
        File photoFile = new File("/Users/michaelwelly/Desktop/AERISMENU/EVENTS.jpg");
        if (photoFile.exists() && photoFile.isFile()) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new org.telegram.telegrambots.meta.api.objects.InputFile(photoFile));

            try {
                execute(photo);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            sendErrorFileNotFound(chatId, "Афиша (фото)");
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
        // Текст с форматированием
        String addressText = "📍 *Уважаемый посетитель,*\n\n" +
                "Наш ресторан расположен по адресу: *ул. Мамина Сибиряка 58, г. Екатеринбург.*\n\n" +
                "_Это место с богатой историей_: здесь когда-то находилась библиотека, где собирались выдающиеся умы города, " +
                "а позже — уютный салон, где обсуждали искусство и музыку.\n\n" +
                "*Сегодня это ресторан AERIS* — место, сочетающее утонченную атмосферу с современным стилем.\n\n" +
                "🔗 [AERIS на Яндекс.Картах](https://yandex.com/maps/-/CHAlAWN~)\n";

        // Создание сообщения с текстом
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(addressText);
        message.enableMarkdown(true);

        // Отправка сообщения
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // Отправка геолокации
        sendLocation(chatId, 56.839104, 60.606564); // Координаты ресторана
    }
    private void sendLocation(String chatId, double latitude, double longitude) {
        // Создание объекта для отправки геолокации
        SendLocation sendLocation = new SendLocation();
        sendLocation.setChatId(chatId);
        sendLocation.setLatitude(latitude);
        sendLocation.setLongitude(longitude);

        // Отправка геолокации
        try {
            execute(sendLocation);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void sendErrorFileNotFound(String chatId, String menuName) {
        SendMessage errorMessage = new SendMessage();
        errorMessage.setChatId(chatId);
        errorMessage.setText("Извините, файл с меню \"" + menuName + "\" не найден.");
        try {
            execute(errorMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void sendMainMenu(String chatId, String text) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Бронирование");
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

    /**
     * Отображение подменю "Бронирование".
     */
    private void sendBookingMenu(String chatId) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Выбрать дату");

        KeyboardRow row6 = new KeyboardRow();
        row6.add("Главное меню"); // Кнопка возврата в главное меню

        keyboard.add(row1);
        keyboard.add(row6);

        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        String description = """
        🤵 *Позвольте мне предложить вам выбрать дату и время для вашего бронирования.*\n\n
        ⏳ *Ваш стол будет забронирован на 2 часа.*\n
        🕰 *Режим работы заведения*:\n
        • *Понедельник – Четверг:* с 11:45 до 02:00\n
        • *Пятница и Суббота:* с 11:45 до 04:00\n
        • *Воскресенье:* с 11:45 до 02:00\n\n
        📌 *Особенные моменты недели*:\n
        • *SUNDAY WINE* — Воскресенье: скидка *20%* на все вина в бутылках. 🍷\n
        • *Музыкальные выходные* — Пятница и Суббота: *диджей-сеты с 21:00.* 🎵\n
        • *Daily Menu* — Будние дни с 11:45 до 16:00: Средиземноморское меню и вино дня. 🍽\n\n
        👉 *Пожалуйста, выберите действие в меню бронирования.*\n
        """;

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(description);
        message.enableMarkdown(true); // Включение форматирования Markdown
        message.setReplyMarkup(replyKeyboardMarkup);


        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
//    private void handleBookingMenuSelection(String chatId, String bookingMenuSelection) {
//        try {
//            UUID orderId = getOrCreateOrderId(chatId); // Получаем или создаем текущий заказ пользователя
//
//            switch (bookingMenuSelection) {
//                case "Выбрать дату":
//                    sendDateSelection(chatId); // Переход к выбору даты
//                    break;
//                case "Выбрать время":
//                    try {
//                        LocalDate bookingDate = orderService.getOrderDate(orderId);
//                        if (bookingDate == null) {
//                            sendMessage(chatId, "Пожалуйста, выберите дату перед выбором времени.");
//                        } else {
//                            sendTimeSelection(chatId, bookingDate.toString());
//                        }
//                    } catch (Exception e) {
//                        log.error("Ошибка при обработке выбора времени: {}", e.getMessage(), e);
//                        sendMessage(chatId, "Произошла ошибка при обработке выбора времени. Попробуйте снова.");
//                    }
//                    break;
//                case "Выбрать слот":
//                    try {
//                        LocalDate bookingDate = orderService.getOrderDate(orderId);
//                        if (bookingDate == null) {
//                            sendMessage(chatId, "Пожалуйста, выберите дату перед выбором слота.");
//                        } else {
//                            sendSlotSelection(chatId, bookingDate.toString());
//                        }
//                    } catch (Exception e) {
//                        log.error("Ошибка при обработке выбора слота: {}", e.getMessage(), e);
//                        sendMessage(chatId, "Произошла ошибка при обработке выбора слота. Попробуйте снова.");
//                    }
//                    break;
//                case "Выбрать зону":
//                    sendZoneSelection(chatId); // Переход к выбору зоны
//                    break;
//                case "Выбрать номер стола":
//                    try {
//                        String selectedZone = orderService.getOrderZone(orderId);
//                        if (selectedZone == null) {
//                            sendMessage(chatId, "Пожалуйста, выберите зону перед выбором стола.");
//                        } else {
//                            sendTableSelection(chatId, selectedZone); // Переход к выбору номера стола
//                        }
//                    } catch (Exception e) {
//                        log.error("Ошибка при обработке выбора стола: {}", e.getMessage(), e);
//                        sendMessage(chatId, "Произошла ошибка при обработке выбора стола. Попробуйте снова.");
//                    }
//                    break;
//                case "Отменить бронирование":
//                    try {
//                        orderService.cancelOrder(orderId);
//                        sendMessage(chatId, "❌ Ваше бронирование отменено. Вы можете начать заново.");
//                    } catch (Exception e) {
//                        log.error("Ошибка при отмене бронирования: {}", e.getMessage(), e);
//                        sendMessage(chatId, "Произошла ошибка при отмене бронирования. Попробуйте снова.");
//                    }
//                    break;
//                case "Главное меню":
//                    sendMainMenu(chatId, "Вы вернулись в главное меню.");
//                    break;
//                default:
//                    sendMessage(chatId, "Неизвестная команда. Пожалуйста, выберите из доступных вариантов.");
//            }
//        } catch (Exception e) {
//            log.error("Общая ошибка при обработке действия: {}", e.getMessage(), e);
//            sendMessage(chatId, "Произошла ошибка при обработке действия. Попробуйте снова.");
//        }
//    }
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    private void sendDateSelection(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📅 Прошу вас выбрать дату для бронирования. Позвольте мне предложить подходящие варианты:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Генерируем даты на 14 дней вперед
        for (int i = 0; i < 14; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"));
            InlineKeyboardButton button = new InlineKeyboardButton();

            // Форматируем дату
            button.setText(dayOfWeek + " 📅 " + date);
            button.setCallbackData("select_date:" + date);
            rows.add(List.of(button));
        }

        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void sendTimeSelection(String chatId, String selectedDate) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        // Определяем расписание работы для выбранной даты
        LocalDate date = LocalDate.parse(selectedDate);
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        LocalTime startTime = LocalTime.of(11, 45);
        LocalTime endTime = (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY)
                ? LocalTime.of(4, 0).plusHours(24) // Для пятницы и субботы до 4 утра
                : LocalTime.of(2, 0).plusHours(24); // Для остальных дней до 2 утра

        message.setText("🗓 Выбранная дата: " + formatDateForUser(date) +
                "\n🕰 Режим работы на этот день: с " + startTime + " до " + endTime +
                ".\n\nПожалуйста, выберите доступное время для бронирования. ⏳ Бронь действует на 2 часа.");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Получаем доступные слоты из сервиса SlotAvailabilityService
        List<SlotAvailability> availableSlots = slotAvailabilityService.getAvailableSlots(date);

        if (availableSlots.isEmpty()) {
            // Если нет доступных слотов
            sendMessage(chatId, "К сожалению, на выбранную дату все временные слоты заняты.");
            return;
        }

        // Генерация кнопок для доступных временных слотов
        for (SlotAvailability slot : availableSlots) {
            LocalTime slotTime = slot.getTimeSlot();
            if (slotTime.isBefore(startTime) || slotTime.isAfter(endTime)) {
                // Игнорируем слоты, которые выходят за рамки рабочего времени
                continue;
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("🕒 " + slotTime.toString());
            button.setCallbackData("select_time:" + selectedDate + "T" + slotTime.toString());
            rows.add(List.of(button));
        }

        // Если после фильтрации нет доступных слотов
        if (rows.isEmpty()) {
            sendMessage(chatId, "К сожалению, все слоты на выбранное время недоступны. Попробуйте выбрать другую дату.");
            return;
        }

        // Устанавливаем кнопки и отправляем сообщение
        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendMessage(chatId, "Произошла ошибка при отображении временных слотов. Пожалуйста, попробуйте снова.");
        }
    }
    private void sendSlotSelection(String chatId, String selectedDate) {
        LocalDate date = LocalDate.parse(selectedDate);

        // Получаем доступные слоты или генерируем их
        List<SlotAvailability> slots = slotAvailabilityService.getAvailableSlots(date);
        if (slots.isEmpty()) {
            slotAvailabilityService.generateSlotsForDate(
                    date,
                    restaurantTableService.getAllTables(),
                    LocalTime.of(11, 0),
                    LocalTime.of(23, 0),
                    new BigDecimal("500.00") // Фиксированная цена
            );
            slots = slotAvailabilityService.getAvailableSlots(date);
        }

        // Создаем кнопки для слотов
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (SlotAvailability slot : slots) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("🕒 " + slot.getTimeSlot() + " - " + slot.getPrice() + "₽");
            button.setCallbackData("select_slot:" + slot.getId());
            rows.add(List.of(button));
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите доступное время:");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendMessage(chatId, "Произошла ошибка при отображении слотов. Попробуйте позже.");
        }
    }
    private void sendTableSelection(String chatId, String selectedZone) {
        // Получаем доступные столы в выбранной зоне
        List<RestaurantTable> availableTables = restaurantTableService.getAvailableTablesByZone(selectedZone);

        if (availableTables.isEmpty()) {
            // Если в зоне нет доступных столов
            sendMessage(chatId, "К сожалению, в зоне \"" + selectedZone + "\" сейчас нет доступных столов. Попробуйте выбрать другую зону.");
            return;
        }

        // Создаем кнопки для выбора стола
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (RestaurantTable table : availableTables) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Столик №" + table.getTableNumber() + " (" + table.getCapacity() + " чел.)");
            button.setCallbackData("select_table:" + table.getId());
            rows.add(List.of(button));
        }

        // Кнопка для возврата в меню выбора зоны
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Вернуться к выбору зоны");
        backButton.setCallbackData("select_zone");

        rows.add(List.of(backButton));

        keyboardMarkup.setKeyboard(rows);

        // Отправляем сообщение пользователю
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите доступный столик в зоне \"" + selectedZone + "\":");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendMessage(chatId, "Произошла ошибка при отображении доступных столов. Попробуйте позже.");
        }
    }

    private void handleCallbackQuery(String chatId, String callbackData) {
        try {
            UUID orderId = getOrCreateOrderId(chatId); // Получаем или создаем новый заказ для пользователя

            if (callbackData.startsWith("select_date:")) {
                handleDateSelection(chatId, callbackData, orderId);
            } else if (callbackData.startsWith("select_time:")) {
                handleTimeSelection(chatId, callbackData, orderId);
            } else if (callbackData.startsWith("select_slot:")) {
                handleSlotSelection(chatId, callbackData, orderId);
            } else if (callbackData.startsWith("select_zone:")) {
                handleZoneSelection(chatId, callbackData, orderId);
            } else if (callbackData.startsWith("select_table:")) {
                handleTableSelection(chatId, callbackData, orderId);
            } else if (callbackData.equals("cancel_booking")) {
                cancelBooking(chatId, orderId);
            } else if (callbackData.equals("main_menu")) {
                sendMainMenu(chatId, "Вы вернулись в главное меню.");
            } else {
                sendMessage(chatId, "Неизвестное действие. Попробуйте снова.");
            }
        } catch (IllegalStateException e) {
            log.error("Ошибка логики: {}", e.getMessage(), e);
            sendMessage(chatId, "Произошла ошибка: " + e.getMessage());
        } catch (Exception e) {
            log.error("Общая ошибка: {}", e.getMessage(), e);
            sendMessage(chatId, "Произошла ошибка при обработке действия. Попробуйте снова.");
        }
    }
    private String extractCallbackValue(String callbackData, String prefix) {
        if (callbackData.startsWith(prefix)) {
            return callbackData.split(":")[1];
        }
        throw new IllegalArgumentException("Некорректные данные callback: " + callbackData);
    }
    private void handleDateSelection(String chatId, String callbackData, UUID orderId) {
        try {
            // Извлекаем выбранную дату из callbackData
            String selectedDate = extractCallbackValue(callbackData, "select_date:");
            LocalDate bookingDate = LocalDate.parse(selectedDate);

            // Обновляем дату бронирования в заказе
            orderService.updateOrderDate(orderId, bookingDate);
            log.info("Дата бронирования установлена: {} для заказа {}", bookingDate, orderId);

            // Отправляем доступные временные слоты
            sendTimeSelection(chatId, selectedDate);

            // Автоматически вызываем handleTimeSelection после выбора времени пользователем
            // handleTimeSelection будет вызываться через callbackData "select_time:<date>T<time>"
        } catch (Exception e) {
            log.error("Ошибка при выборе даты для пользователя {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Ошибка при выборе даты. Попробуйте снова.");
        }
    }
    private void handleTimeSelection(String chatId, String callbackData, UUID userId) {
        try {
            String selectedTime = extractCallbackValue(callbackData, "select_time:");
            LocalTime bookingTime = LocalTime.parse(selectedTime);
            LocalDate bookingDate = orderService.getOrderDate(userId);

            if (bookingDate == null) {
                throw new IllegalStateException("Сначала выберите дату.");
            }

            orderService.updateOrderSlot(userId, bookingDate, bookingTime);

            sendMessage(chatId, "🕒 Вы выбрали время: " + bookingTime + ". Теперь выберите зону.");
            sendZoneSelection(chatId);
        } catch (Exception e) {
            log.error("Ошибка при выборе времени для пользователя {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Ошибка при выборе времени. Попробуйте снова.");
        }
    }
    private void handleSlotSelection(String chatId, String callbackData, UUID orderId) {
        try {
            // Извлекаем ID слота из callbackData
            UUID slotId = UUID.fromString(callbackData.split(":")[1]);

            // Получаем информацию о слоте
            SlotAvailability slot = slotAvailabilityService.getSlotById(slotId);

            // Проверяем доступность слота
            if (!slotAvailabilityService.isSlotAvailable(slot.getTable().getId(), slot.getDate(), slot.getTimeSlot())) {
                sendMessage(chatId, "Извините, выбранный слот уже занят. Попробуйте другой.");
                return;
            }

            // Обновляем заказ с учетом выбранных даты и времени
            orderService.updateOrderSlot(orderId, slot.getDate(), slot.getTimeSlot());

            // Логируем успешное обновление
            log.info("Слот бронирования установлен: {} для заказа {}", slotId, orderId);

            // Уведомляем пользователя
            sendMessage(chatId, "✅ Вы успешно забронировали слот на " +
                    formatDateTimeForUser(slot.getDate().atTime(slot.getTimeSlot())) +
                    ". Теперь выберите зону.");

            // Переходим к выбору зоны
            sendZoneSelection(chatId);
        } catch (Exception e) {
            // Логируем и уведомляем об ошибке
            log.error("Ошибка при выборе слота для пользователя {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Ошибка при выборе слота. Попробуйте снова.");
        }
    }
    private void handleZoneSelection(String chatId, String callbackData, UUID userId) {
        try {
            String selectedZone = extractCallbackValue(callbackData, "select_zone:");
            orderService.updateOrderZone(userId, selectedZone);

            sendMessage(chatId, "📍 Вы выбрали зону: " + selectedZone + ". Теперь выберите номер стола.");
            sendTableSelection(chatId, selectedZone);
        } catch (Exception e) {
            log.error("Ошибка при выборе зоны для пользователя {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Ошибка при выборе зоны. Попробуйте снова.");
        }
    }
    private void handleTableSelection(String chatId, String callbackData, UUID userId) {
        try {
            UUID tableId = UUID.fromString(extractCallbackValue(callbackData, "select_table:"));
            RestaurantTable table = restaurantTableService.getTableById(tableId);
            LocalDate bookingDate = orderService.getOrderDate(userId);
            LocalTime bookingTime = orderService.getOrderTime(userId);

            if (!orderService.isTableAvailable(tableId, bookingDate, bookingTime)) {
                sendMessage(chatId, "К сожалению, выбранный столик занят. Попробуйте другой.");
                return;
            }

            orderService.updateOrderTable(userId, tableId);
            sendMessage(chatId, "🪑 Вы выбрали столик №" + table.getTableNumber() +
                    " (Зона: " + table.getZone() + "). Теперь подтвердите бронирование.");
        } catch (Exception e) {
            log.error("Ошибка при выборе стола для пользователя {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Ошибка при выборе стола. Попробуйте снова.");
        }
    }
    private void cancelBooking(String chatId, UUID orderId) {
        try {
            orderService.cancelOrder(orderId);
            sendMessage(chatId, "❌ Ваше бронирование отменено. Вы можете начать заново, выбрав действие в главном меню.");
            sendMainMenu(chatId, "Вы вернулись в главное меню.");
        } catch (Exception e) {
            log.error("Ошибка при отмене бронирования: {}", e.getMessage(), e);
            sendMessage(chatId, "Ошибка при отмене бронирования. Попробуйте снова.");
        }
    }

    private String formatDateTimeForUser(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, HH:mm", Locale.forLanguageTag("ru"));
        return dateTime.format(formatter);
    }
    private String formatDateForUser(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.forLanguageTag("ru"));
        return date.format(formatter);
    }
    private String formatSelectedDate(String selectedDate) {
        LocalDate date = LocalDate.parse(selectedDate);
        String dayOfWeek = date.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"));
        return dayOfWeek + ", " + date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("ru")));
    }
//    private void askForBookingDateTime(String chatId) {
//        String description = """
//        🤵 *Позвольте мне предложить вам выбрать дату и время для вашего бронирования.*\n\n
//        ⏳ *Ваш стол будет забронирован на 2 часа.*\n
//        🕰 *Режим работы заведения*:\n
//        • *Понедельник – Четверг:* с 11:45 до 02:00\n
//        • *Пятница и Суббота:* с 11:45 до 04:00\n
//        • *Воскресенье:* с 11:45 до 02:00\n\n
//        📌 *Особенные моменты недели*:\n
//        • *SUNDAY WINE* — Воскресенье: скидка *20%* на все вина в бутылках. 🍷\n
//        • *Музыкальные выходные* — Пятница и Суббота: *диджей-сеты с 21:00.* 🎵\n
//        • *Daily Menu* — Будние дни с 11:45 до 16:00: Средиземноморское меню и вино дня. 🍽\n\n
//        👉 *Пожалуйста, выберите дату для вашего бронирования.*\n
//        """;
//
//        SendMessage message = new SendMessage();
//        message.setChatId(chatId);
//        message.setText(description);
//        message.enableMarkdown(true); // Включение форматирования Markdown
//
//        // Добавление кнопки для перехода к выбору даты
//        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
//        InlineKeyboardButton selectDateButton = new InlineKeyboardButton();
//        selectDateButton.setText("📅 Выбрать дату");
//        selectDateButton.setCallbackData("select_date");
//
//        keyboardMarkup.setKeyboard(List.of(List.of(selectDateButton)));
//        message.setReplyMarkup(keyboardMarkup);
//
//        try {
//            execute(message);
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//            sendMessage(chatId, "Произошла ошибка при отображении информации. Попробуйте позже.");
//        }
//    }
    private void sendZonePlan(String chatId) {
        File planFile = new File("/Users/michaelwelly/Desktop/AERISMENU/AERIS PLAN.pdf");
        if (planFile.exists()) {
            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new org.telegram.telegrambots.meta.api.objects.InputFile(planFile));
            document.setCaption("Пожалуйста, ознакомьтесь с планом зала. Затем выберите зону и номер стола.");
            try {
                execute(document);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        // Задаем следующий шаг — выбор зоны
        askForZoneSelection(chatId);
    }
    private UUID getOrCreateOrderId(String chatId) {
        try {
            UUID userId = getUserId(chatId); // Получаем UUID пользователя

            // Проверяем, есть ли уже заказ в статусе PENDING
            return orderRepository.findByUserId(userId).stream()
                    .filter(order -> "PENDING".equals(order.getStatus()))
                    .map(Order::getId)
                    .findFirst()
                    .orElseGet(() -> {
                        // Генерируем новый UUID для заказа
                        UUID orderId = UUID.randomUUID();

                        // Создаем новый заказ без даты и времени
                        Order newOrder = new Order();
                        newOrder.setId(orderId); // Устанавливаем сгенерированный UUID
                        newOrder.setUser(userRepository.findById(userId).orElseThrow(() ->
                                new EntityNotFoundException("User not found with ID: " + userId)));
                        newOrder.setStatus("PENDING");
                        newOrder.setComment("Бронирование через Telegram Bot");

                        // Сохраняем заказ в базе данных
                        Order savedOrder = orderRepository.save(newOrder);
                        log.info("Создан новый заказ с ID: {}", savedOrder.getId());

                        return savedOrder.getId();
                    });
        } catch (Exception e) {
            log.error("Ошибка при создании или получении заказа: {}", e.getMessage(), e);
            throw new IllegalStateException("Ошибка при создании или получении заказа: " + e.getMessage());
        }
    }

    private void confirmOrder(String chatId, Order order) {
        try {
            // Формируем текст подтверждения
            String confirmationText = String.format(
                    "Ваш заказ подтвержден!\n\nДата: %s\nВремя: %s\nЗона: %s\nСтол: %s\n\nСпасибо за бронирование!",
                    order.getBookingDate(),
                    order.getBookingTime(),
                    order.getTable().getZone(),
                    order.getTable().getTableNumber()
            );

            // Отправляем подтверждение пользователю
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(confirmationText);
            execute(message);

            // Отправляем картинку с благодарностью (если доступна)
            File confirmationImage = new File("/path/to/confirmation-image.jpg");
            if (confirmationImage.exists()) {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(chatId);
                photo.setPhoto(new org.telegram.telegrambots.meta.api.objects.InputFile(confirmationImage));
                execute(photo);
            } else {
                log.warn("Изображение подтверждения не найдено по пути: /path/to/confirmation-image.jpg");
            }

            // Уведомляем хостесс о новом подтвержденном заказе
            notifyHostess(order);

            // Логируем успешное завершение
            log.info("Заказ {} подтвержден для пользователя {}", order.getId(), chatId);
        } catch (TelegramApiException e) {
            // Логируем ошибку при отправке сообщения
            log.error("Ошибка при отправке подтверждения заказа пользователю {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Ошибка при отправке подтверждения. Попробуйте снова.");
        } catch (Exception e) {
            // Логируем общую ошибку
            log.error("Ошибка при подтверждении заказа для пользователя {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Ошибка при подтверждении заказа. Попробуйте снова.");
        }
    }
    private void notifyHostess(Order order) {
        try {
            // ID чата хостесс
            String hostessChatId = "HOSTESS_CHAT_ID";

            // Формируем текст уведомления
            String notificationText = String.format(
                    "Новый заказ:\n\nДата: %s\nВремя: %s\nЗона: %s\nСтол: %s\nКомментарий: %s\n\nПожалуйста, подтвердите или отклоните заказ.",
                    order.getBookingDate(),
                    order.getBookingTime(),
                    order.getTable().getZone(),
                    order.getTable().getTableNumber(),
                    order.getComment() != null ? order.getComment() : "Комментарий отсутствует"
            );

            // Отправляем сообщение хостесс
            SendMessage message = new SendMessage();
            message.setChatId(hostessChatId);
            message.setText(notificationText);
            execute(message);

            // Логируем успешное уведомление
            log.info("Хостесс уведомлена о новом заказе: {}", order.getId());
        } catch (TelegramApiException e) {
            // Логируем ошибку при отправке уведомления
            log.error("Ошибка при отправке уведомления хостесс о заказе {}: {}", order.getId(), e.getMessage(), e);
        } catch (Exception e) {
            // Логируем общую ошибку
            log.error("Ошибка при уведомлении хостесс о заказе {}: {}", order.getId(), e.getMessage(), e);
        }
    }
    private void askForZoneSelection(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Введите название зоны (например, 'Зона 1') и номер стола (например, 'Стол 3').");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void createOrder(String chatId, UUID userId, UUID tableId, LocalDate bookingDate, LocalTime bookingTime, String comment) {
        try {
            // Создаем заказ через OrderService
            Order order = orderService.createOrder(userId, tableId, bookingDate, bookingTime, comment);

            // Подтверждаем заказ и отправляем сообщение пользователю
            confirmOrder(chatId, order);

            log.info("Заказ успешно создан: Пользователь {} забронировал стол {} на дату {} и время {}.",
                    userId, tableId, bookingDate, bookingTime);
        } catch (EntityNotFoundException e) {
            log.error("Ошибка: Пользователь {} или стол {} не найдены.", userId, tableId, e);
            sendMessage(chatId, "❌ Ошибка: Пользователь или стол не найдены. Проверьте введенные данные.");
        } catch (IllegalStateException e) {
            log.warn("Попытка создать заказ на занятый стол: Пользователь {}, стол {}, дата {}, время {}.",
                    userId, tableId, bookingDate, bookingTime);
            sendMessage(chatId, "❌ Извините, выбранный стол уже занят на это время. Попробуйте другой.");
        } catch (Exception e) {
            log.error("Произошла ошибка при создании заказа для пользователя {}: {}", userId, e.getMessage(), e);
            sendMessage(chatId, "❌ Произошла ошибка при создании заказа. Попробуйте снова.");
        }
    }
    private void checkTableAvailability(String chatId, UUID tableId, LocalDate bookingDate, LocalTime bookingTime) {
        try {
            if (orderService.isTableAvailable(tableId, bookingDate, bookingTime)) {
                sendMessage(chatId, "✅ Стол доступен для бронирования.");
            } else {
                sendMessage(chatId, "❌ К сожалению, стол занят. Попробуйте выбрать другое время или стол.");
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке доступности стола {}: {}", tableId, e.getMessage(), e);
            sendMessage(chatId, "❌ Произошла ошибка при проверке доступности стола. Попробуйте снова.");
        }
    }
    private void confirmOrderByHostess(String chatId, UUID orderId) {
        try {
            // Обновляем статус заказа на "CONFIRMED"
            Order order = orderService.updateOrderStatus(orderId, "CONFIRMED");

            // Уведомляем администратора
            sendMessage(chatId, "✅ Заказ подтвержден! Пользователь уведомлен.");

            // Уведомляем пользователя
            notifyUserAboutConfirmation(order);

            log.info("Заказ {} подтвержден хостесс.", orderId);
        } catch (EntityNotFoundException e) {
            log.error("Ошибка: Заказ с ID {} не найден.", orderId, e);
            sendMessage(chatId, "❌ Ошибка: Заказ с таким ID не найден.");
        } catch (Exception e) {
            log.error("Ошибка при подтверждении заказа {}: {}", orderId, e.getMessage(), e);
            sendMessage(chatId, "❌ Произошла ошибка при подтверждении заказа. Попробуйте снова.");
        }
    }
    private void rejectOrderByHostess(String chatId, UUID orderId, String reason) {
        try {
            // Обновляем статус заказа на "REJECTED"
            Order order = orderService.updateOrderStatus(orderId, "REJECTED");

            // Уведомляем администратора
            sendMessage(chatId, "❌ Заказ отклонен! Пользователь уведомлен.");

            // Уведомляем пользователя о причине отклонения
            notifyUserAboutRejection(order, reason);

            log.info("Заказ {} отклонен хостесс по причине: {}", orderId, reason);
        } catch (EntityNotFoundException e) {
            log.error("Ошибка: Заказ с ID {} не найден.", orderId, e);
            sendMessage(chatId, "❌ Ошибка: Заказ с таким ID не найден.");
        } catch (Exception e) {
            log.error("Ошибка при отклонении заказа {}: {}", orderId, e.getMessage(), e);
            sendMessage(chatId, "❌ Произошла ошибка при отклонении заказа. Попробуйте снова.");
        }
    }
    private void notifyUserAboutConfirmation(Order order) {
        String userChatId = order.getUser().getTelegramId();
        sendMessage(userChatId, "Ваш заказ подтвержден! Ждем вас.");
    }
    private void notifyUserAboutRejection(Order order, String reason) {
        String userChatId = order.getUser().getTelegramId();
        sendMessage(userChatId, "К сожалению, ваш заказ отклонен.\nПричина: " + reason);
    }
    private void sendZoneSelection(String chatId) {
        // Отправляем PDF с планом зала
        File planFile = new File("/Users/michaelwelly/Desktop/AERISMENU/PLAN.pdf");
        if (planFile.exists()) {
            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new org.telegram.telegrambots.meta.api.objects.InputFile(planFile));
            document.setCaption("Ознакомьтесь с планом зала, затем выберите зону для бронирования:");
            try {
                execute(document);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            sendMessage(chatId, "План зала временно недоступен.");
        }

        // Отправляем кнопки с зонами
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Пожалуйста, выберите зону:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Пример зон
        String[] zones = {"Бар", "Основной зал", "Терраса", "VIP-зона"};
        for (String zone : zones) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(zone);
            button.setCallbackData("select_zone:" + zone);
            rows.add(List.of(button));
        }

        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void confirmOrder(String chatId, UUID tableId, LocalDateTime bookingDateTime) {
        RestaurantTable table = restaurantTableService.getTableById(tableId);

        sendMessage(chatId, "Ваш заказ:\n" +
                "Дата: " + bookingDateTime.toLocalDate() + "\n" +
                "Время: " + bookingDateTime.toLocalTime() + "\n" +
                "Зона: " + table.getZone() + "\n" +
                "Столик №" + table.getTableNumber() + "\n\n" +
                "Ожидайте подтверждения от хостесс.");

        // Уведомляем хостесс
        notifyHostess(chatId, table, bookingDateTime);
    }
    private void notifyHostess(String chatId, RestaurantTable table, LocalDateTime bookingDateTime) {
        // Замените "HOSTESS_CHAT_ID" на реальное значение, либо сделайте это конфигурацией
        String hostessChatId = "HOSTESS_CHAT_ID";

        sendMessage(hostessChatId, "Новый заказ:\n" +
                "Пользователь: " + chatId + "\n" +
                "Дата: " + bookingDateTime.toLocalDate() + "\n" +
                "Время: " + bookingDateTime.toLocalTime() + "\n" +
                "Зона: " + table.getZone() + "\n" +
                "Столик №" + table.getTableNumber());
    }
    private void sendZoneOptions(String chatId) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Бар");
        row1.add("Основной зал");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Терраса");
        row2.add("VIP-зона");

        rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите зону:");
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void cacheOrderInfo(String chatId, String key, String value) {
        // Пример использования RedisTemplate
        String redisKey = "order:" + chatId;
        redisTemplate.opsForHash().put(redisKey, key, value);
    }
    private String getCachedValue(String chatId, String key) {
        String redisKey = "order:" + chatId;
        Object value = redisTemplate.opsForHash().get(redisKey, key);
        return value != null ? value.toString() : null;
    }
    private UUID getCachedTableId(String chatId) {
        String value = getCachedValue(chatId, "tableId");
        return value != null ? UUID.fromString(value) : null;
    }
    private LocalDateTime getCachedBookingDateTime(String chatId) {
        String value = getCachedValue(chatId, "bookingDateTime");
        return value != null ? LocalDateTime.parse(value) : null;
    }
    private UUID getUserId(String chatId) {
        User user = userRepository.findByTelegramId(chatId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with Telegram ID: " + chatId));
        return user.getId();
    }
    private UUID parseTableIdFromInput(String tableSelection) {
        try {
            return UUID.fromString(tableSelection); // Преобразование строки в UUID
        } catch (IllegalArgumentException e) {
            return null; // Возвращает null, если входные данные некорректны
        }
    }
    private void finalizeOrder(String chatId, UUID userId, UUID tableId, LocalDate bookingDate, LocalTime bookingTime, String comment) {
        try {
            // Проверяем, что все данные собраны
            if (tableId == null || bookingDate == null || bookingTime == null) {
                throw new IllegalStateException("Не все параметры заказа выбраны. Проверьте дату, время и стол.");
            }

            // Создаем заказ
            Order order = orderService.createOrder(userId, tableId, bookingDate, bookingTime, comment);

            // Подтверждаем заказ пользователю
            sendMessage(chatId, "✅ Ваш заказ успешно создан и подтвержден!\n" +
                    "Дата: " + bookingDate + "\n" +
                    "Время: " + bookingTime + "\n" +
                    "Стол: №" + order.getTable().getTableNumber() +
                    " (Зона: " + order.getTable().getZone() + ")");

            // Уведомляем хостесс
            notifyHostess(order);
        } catch (IllegalStateException e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка при создании заказа. Попробуйте снова.");
            log.error("Ошибка при создании заказа: {}", e.getMessage(), e);
        }
    }
    private void sendTableOptions(String chatId, List<RestaurantTable> tables) {
        // Создаём клавиатуру для выбора столов
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();

        for (RestaurantTable table : tables) {
            KeyboardRow row = new KeyboardRow();
            row.add("Столик №" + table.getTableNumber());
            rows.add(row);
        }

        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);

        // Отправляем сообщение с выбором
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите номер столика:");
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
  }
    private boolean isWithinWorkingHours(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(LocalTime.of(11, 0)) && !time.isAfter(LocalTime.of(23, 0));
    }
}