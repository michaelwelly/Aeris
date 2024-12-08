package com.aeris.bot.service;

import com.aeris.bot.model.User;
import com.aeris.bot.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Обновленный метод для регистрации пользователя
    public User registerUser(String firstName, String lastName, String telegramId, String username, String languageCode) {
        // Проверяем, существует ли уже пользователь с таким telegramId
        User existingUser = userRepository.findByTelegramId(telegramId);
        if (existingUser != null) {
            throw new RuntimeException("User already exists!");
        }
        // Сохраняем нового пользователя, используя все поля
        User user = new User(firstName, lastName, telegramId, username, languageCode);
        User savedUser = userRepository.save(user);
        System.out.println("User successfully saved: " + savedUser);
        return savedUser;
    }


    // Метод для сохранения пользователя с данными из Telegram
    public void saveUser(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        System.out.println("Attempting to save user: " + telegramUser.getId());
        // Проверяем, существует ли уже пользователь
        if (userRepository.findByTelegramId(telegramUser.getId().toString()) == null) {
            // Создаем нового пользователя
            User user = new User();
            user.setTelegramId(telegramUser.getId().toString());
            user.setUsername(telegramUser.getUserName());
            user.setFirstName(telegramUser.getFirstName());
            user.setLastName(telegramUser.getLastName());
            user.setLanguageCode(telegramUser.getLanguageCode());
            userRepository.save(user);
            System.out.println("User successfully saved: " + user);
        } else {
            System.out.println("User already exists: " + telegramUser.getId());
        }
    }
    // Дополнительный метод для поиска пользователя по Telegram ID
    public User findUserByTelegramId(String telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }
}
