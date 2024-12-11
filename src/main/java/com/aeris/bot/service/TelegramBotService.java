package com.aeris.bot.service;

import com.aeris.bot.model.Order;
import com.aeris.bot.model.RestaurantTable;
import com.aeris.bot.model.User;
import com.aeris.bot.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
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

@Service
public class TelegramBotService extends TelegramLongPollingBot {


    @Value("${bot.username}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    private final UserService userService;
    private final OrderService orderService;
    private final RestaurantTableService restaurantTableService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private UserRepository userRepository;

    public TelegramBotService(UserService userService, OrderService orderService, RestaurantTableService restaurantTableService) {
        this.userService = userService;
        this.orderService = orderService;
        this.restaurantTableService = restaurantTableService;
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
        // Обработка текстовых сообщений
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
    private void handleUnsupportedMessage(String chatId, Message message) {
        sendMessage(chatId, "Извините, я пока не могу обработать этот тип сообщений.");
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
                askForBookingDateTime(chatId);
                sendDateSelection(chatId);// Начинаем с выбора даты
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
            case "Интерьер":
                sendInteriorMenu(chatId);
                break;
            case "Афиша":
                sendEventsMenu(chatId);
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

        // Генерация временных слотов
        LocalTime currentTime = startTime;
        while (currentTime.isBefore(endTime)) {
            String timeSlot = currentTime.toString();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("🕒 " + timeSlot);
            button.setCallbackData("select_time:" + selectedDate + "T" + timeSlot);
            rows.add(List.of(button));
            currentTime = currentTime.plusHours(1); // Интервал 1 час
        }

        // Если нет доступных слотов
        if (rows.isEmpty()) {
            sendMessage(chatId, "К сожалению, на выбранную дату все временные слоты заняты.");
            return;
        }

        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendMessage(chatId, "Произошла ошибка при отображении временных слотов. Пожалуйста, попробуйте снова.");
        }
    }
    private String generateDayDescription(String selectedDate) {
        LocalDate date = LocalDate.parse(selectedDate);
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        switch (dayOfWeek) {
            case MONDAY:
                return "🌞 Понедельник — день энергии и новых начинаний! Это прекрасное время для планирования ваших достижений.";
            case TUESDAY:
                return "🔥 Вторник — день огненной решимости. Солнце приглашает вас к активности и решению задач.";
            case WEDNESDAY:
                return "💨 Среда — день гармонии и равновесия. Природа способствует ясности мыслей и принятию решений.";
            case THURSDAY:
                return "🌳 Четверг — день роста и процветания. Это идеальное время для новых начинаний и отдыха.";
            case FRIDAY:
                return "✨ Пятница — день радости и завершения. Это время насладиться плодами ваших трудов.";
            case SATURDAY:
                return "🌙 Суббота — день покоя и медитации. Позвольте себе насладиться простыми радостями жизни.";
            case SUNDAY:
                return "🍷 Воскресенье — день созерцания и благодарности. Наполните этот день моментами счастья.";
            default:
                return "Сегодня уникальный день, который обещает быть особенным!";
        }
    }
    private void handleCallbackQuery(String chatId, String callbackData) {
        if (callbackData.startsWith("select_date:")) {
            try {
                // Извлекаем выбранную дату
                String selectedDate = callbackData.split(":")[1];
                cacheOrderInfo(chatId, "selectedDate", selectedDate); // Сохраняем выбранную дату

                // Генерируем описание дня
                String dayDescription = generateDayDescription(selectedDate);

                // Сообщаем пользователю о выбранной дате с описанием
                sendMessage(chatId, "📅 Вы выбрали дату: " + formatSelectedDate(selectedDate) + ".\n\n" +
                        dayDescription + "\n\n" +
                        "Теперь, прошу вас, выберите удобное время для бронирования. ⏳");

                // Переходим к выбору времени через метод sendTimeSelection
                sendTimeSelection(chatId, selectedDate);
            } catch (Exception e) {
                sendMessage(chatId, "Произошла ошибка при выборе даты. Пожалуйста, попробуйте снова.");
            }
        } else if (callbackData.startsWith("select_time:")) {
            try {
                // Извлекаем выбранное время
                String selectedTime = callbackData.split(":")[1];
                String selectedDate = getCachedValue(chatId, "selectedDate"); // Получаем сохранённую дату
                String selectedDateTime = selectedDate + "T" + selectedTime;

                cacheOrderInfo(chatId, "selectedDateTime", selectedDateTime); // Сохраняем дату и время

                // Переходим к выбору зоны через метод sendZoneSelection
                sendMessage(chatId, "🕰 Вы выбрали время: " + selectedTime +
                        ". Теперь позвольте мне предложить зоны для вашего бронирования.");
                sendZoneSelection(chatId);
            } catch (Exception e) {
                sendMessage(chatId, "Произошла ошибка при выборе времени. Пожалуйста, попробуйте снова.");
            }
        } else {
            sendMessage(chatId, "Неизвестное действие: " + callbackData);
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
    private void saveDateTimeToDatabase(String chatId, LocalDateTime bookingDateTime) {
        Long userId = getUserId(chatId);

        // Используем OrderService для сохранения данных
        orderService.createOrder(userId, null, bookingDateTime, "Бронирование времени через бота");
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
    private void askForBookingDateTime(String chatId) {
        String description = """
            🤵 *Позвольте мне предложить вам выбрать дату и время для вашего бронирования.*\n\n
            ⏳ *Ваш стол будет забронирован на 2 часа.*\n
            🕰 *Режим работы заведения*:\n
            • Понедельник – Четверг: 11:45 – 02:00\n
            • Пятница и Суббота: 11:45 – 04:00\n
            • Воскресенье: 11:45 – 02:00\n\n
            📌 *Обратите внимание на особенные моменты*:\n
            • *SUNDAY WINE* — Воскресенье, -20% на все позиции вин по бутылкам.\n
            • *Музыкальные выходные* — пятница и суббота с диджей-сетами с 21:00.\n
            • *Daily Menu* — будние дни с 11:45 до 16:00: средиземноморское меню и вино дня. 🍷
            """;

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(description);
        message.enableMarkdown(true); // Подключение стиля Markdown для форматирования текста
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private boolean validateBookingDateTime(String dateTimeInput) {
        try {
            LocalDateTime bookingDateTime = LocalDateTime.parse(dateTimeInput, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            // Проверяем рабочие часы
            LocalTime time = bookingDateTime.toLocalTime();
            if (time.isBefore(LocalTime.of(12, 0)) || time.isAfter(LocalTime.of(23, 0))) {
                return false;
            }

            // Проверяем шаг времени (только начало часа или 30 минут)
            if (time.getMinute() != 0 && time.getMinute() != 30) {
                return false;
            }

            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
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
    private void confirmOrder(String chatId, Order order) {
        // Отправляем подтверждение пользователю
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(String.format("Ваш заказ подтвержден!\n\nДата и время: %s\nЗона: %s\nСтол: %s\n\nСпасибо за бронирование!",
                order.getBookingDateTime().toString(),
                order.getTable().getZone(),
                order.getTable().getTableNumber()));

        // Отправляем картинку с благодарностью
        File confirmationImage = new File("/path/to/confirmation-image.jpg");
        if (confirmationImage.exists()) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new org.telegram.telegrambots.meta.api.objects.InputFile(confirmationImage));
            try {
                execute(photo);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // Уведомляем хостесс
        notifyHostess(order);
    }
    private void notifyHostess(Order order) {
        String hostessChatId = "HOSTESS_CHAT_ID";
        SendMessage message = new SendMessage();
        message.setChatId(hostessChatId);
        message.setText(String.format("Новый заказ:\n\nДата и время: %s\nЗона: %s\nСтол: %s\nКомментарий: %s\n\nПодтвердить или отклонить.",
                order.getBookingDateTime().toString(),
                order.getTable().getZone(),
                order.getTable().getTableNumber(),
                order.getComment()));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
    private void createOrder(String chatId, Long userId, Long tableId, LocalDateTime bookingDateTime, String comment) {
        try {
            Order order = orderService.createOrder(userId, tableId, bookingDateTime, comment);
            confirmOrder(chatId, order);
        } catch (EntityNotFoundException e) {
            sendMessage(chatId, "Ошибка: Пользователь или стол не найдены. Проверьте введенные данные.");
        } catch (IllegalStateException e) {
            sendMessage(chatId, "Извините, выбранный стол уже занят на это время. Попробуйте другой.");
        }
    }
    private void checkTableAvailability(String chatId, Long tableId, LocalDateTime bookingDateTime) {
        if (orderService.isTableAvailable(tableId, bookingDateTime)) {
            sendMessage(chatId, "Стол доступен для бронирования.");
        } else {
            sendMessage(chatId, "К сожалению, стол занят. Попробуйте выбрать другое время или стол.");
        }
    }
    private void confirmOrderByHostess(String chatId, Long orderId) {
        Order order = orderService.updateOrderStatus(orderId, "CONFIRMED");
        sendMessage(chatId, "Заказ подтвержден! Пользователь уведомлен.");
        notifyUserAboutConfirmation(order);
    }
    private void rejectOrderByHostess(String chatId, Long orderId, String reason) {
        Order order = orderService.updateOrderStatus(orderId, "REJECTED");
        sendMessage(chatId, "Заказ отклонен! Пользователь уведомлен.");
        notifyUserAboutRejection(order, reason);
    }
    private void notifyUserAboutConfirmation(Order order) {
        String userChatId = order.getUser().getTelegramId();
        sendMessage(userChatId, "Ваш заказ подтвержден! Ждем вас.");
    }
    private void notifyUserAboutRejection(Order order, String reason) {
        String userChatId = order.getUser().getTelegramId();
        sendMessage(userChatId, "К сожалению, ваш заказ отклонен.\nПричина: " + reason);
    }
    private void handleDateTimeInput(String chatId, String userInput) {
        try {
            LocalDateTime bookingDateTime = LocalDateTime.parse(userInput, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            if (!isWithinWorkingHours(bookingDateTime)) {
                sendMessage(chatId, "Ресторан работает только с 11:00 до 23:00. Попробуйте выбрать другое время.");
                return;
            }

            // Сохраняем выбранное время в текущий заказ пользователя (например, в Redis)
            cacheOrderInfo(chatId, "bookingDateTime", bookingDateTime.toString());

            sendZoneSelection(chatId);
        } catch (DateTimeParseException e) {
            sendMessage(chatId, "Неверный формат даты. Укажите дату и время в формате 'yyyy-MM-dd HH:mm'.");
        }
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

    private void confirmOrder(String chatId, Long tableId, LocalDateTime bookingDateTime) {
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
    private void sendZonePlan(String chatId, String zoneName) {
        File zonePlanFile = new File("/path/to/zone/plan.pdf");

        if (zonePlanFile.exists() && zonePlanFile.isFile()) {
            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new org.telegram.telegrambots.meta.api.objects.InputFile(zonePlanFile));
            document.setCaption("План зала для зоны: " + zoneName);

            try {
                execute(document);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            sendMessage(chatId, "Извините, план зала недоступен.");
        }
    }
    private Long parseTableIdFromInput(String input) {
        try {
            if (input.startsWith("Столик №")) {
                String numberStr = input.replace("Столик №", "").trim();
                return Long.parseLong(numberStr);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
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
    private Long getCachedTableId(String chatId) {
        String value = getCachedValue(chatId, "tableId");
        return value != null ? Long.parseLong(value) : null;
    }
    private LocalDateTime getCachedBookingDateTime(String chatId) {
        String value = getCachedValue(chatId, "bookingDateTime");
        return value != null ? LocalDateTime.parse(value) : null;
    }
    private Long getUserId(String chatId) {
        User user = userRepository.findByTelegramId(chatId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with Telegram ID: " + chatId));
        return user.getId();
    }
    private void handleTableSelection(String chatId, String tableSelection) {
        Long tableId = parseTableIdFromInput(tableSelection);
        if (tableId == null) {
            sendMessage(chatId, "Неверный выбор столика. Пожалуйста, выберите из доступных опций.");
            return;
        }

        cacheOrderInfo(chatId, "tableId", tableId.toString());
        LocalDateTime bookingDateTime = getCachedBookingDateTime(chatId);
        sendMessage(chatId, "Вы выбрали столик №" + tableId + ". Пожалуйста, подтвердите ваше бронирование.");
        confirmOrder(chatId, tableId, bookingDateTime);
    }
    private void finalizeOrder(String chatId) {
        Long userId = getUserId(chatId);
        Long tableId = getCachedTableId(chatId);
        LocalDateTime bookingDateTime = getCachedBookingDateTime(chatId);

        try {
            Order order = orderService.createOrder(userId, tableId, bookingDateTime, "Бронирование через Telegram Bot");
            sendMessage(chatId, "Ваш заказ подтверждён! Спасибо за использование нашего сервиса.");
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "К сожалению, произошла ошибка при создании вашего заказа. Попробуйте снова.");
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
    private void handleBookingTimeSelection(String chatId, String inputDateTime) {
        try {
            LocalDateTime bookingDateTime = LocalDateTime.parse(inputDateTime);

            if (!isWithinWorkingHours(bookingDateTime)) {
                sendMessage(chatId, "К сожалению, вы выбрали время вне рабочего режима ресторана (11:00 - 23:00). Попробуйте снова.");
                return;
            }

            cacheOrderInfo(chatId, "bookingDateTime", bookingDateTime.toString());
            sendMessage(chatId, "Вы выбрали: " + bookingDateTime.toLocalDate() + " " + bookingDateTime.toLocalTime() + ". Теперь выберите зону.");
            sendZoneOptions(chatId); // Отправляем пользователю доступные зоны
        } catch (DateTimeParseException e) {
            sendMessage(chatId, "Неверный формат даты и времени. Введите в формате: yyyy-MM-ddTHH:mm.");
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


}