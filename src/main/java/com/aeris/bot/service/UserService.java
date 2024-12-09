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

    // Метод для регистрации нового пользователя
    public User registerUser(String firstName, String lastName, String telegramId, String username, String languageCode) {
        User existingUser = userRepository.findByTelegramId(telegramId);
        if (existingUser != null) {
            throw new RuntimeException("User already exists!");
        }
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setTelegramId(telegramId);
        user.setUsername(username);
        user.setLanguageCode(languageCode);

        User savedUser = userRepository.save(user);
        System.out.println("User successfully registered: " + savedUser);
        return savedUser;
    }

    // Метод для сохранения пользователя из Telegram API
    public void saveUser(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        System.out.println("Attempting to save user: " + telegramUser.getId());

        User existingUser = userRepository.findByTelegramId(telegramUser.getId().toString());
        if (existingUser == null) {
            User user = new User();
            user.setTelegramId(telegramUser.getId().toString());
            user.setFirstName(telegramUser.getFirstName());
            user.setLastName(telegramUser.getLastName());
            user.setUsername(telegramUser.getUserName());
            user.setLanguageCode(telegramUser.getLanguageCode());
            userRepository.save(user);
            System.out.println("User successfully saved: " + user);
        } else {
            System.out.println("User already exists: " + existingUser);
        }
    }

    // Метод для обновления номера телефона пользователя
    public void updatePhoneNumber(String telegramId, String phoneNumber) {
        User user = userRepository.findByTelegramId(telegramId);
        if (user == null) {
            throw new RuntimeException("User with telegramId " + telegramId + " does not exist!");
        }
        user.setPhoneNumber(phoneNumber);
        userRepository.save(user);
        System.out.println("Phone number updated for user: " + user);
    }

    // Метод для поиска пользователя по Telegram ID
    public User findUserByTelegramId(String telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }
}