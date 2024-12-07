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

    public User registerUser(String name, String telegramId) {
        if (userRepository.findByTelegramId(telegramId) != null) {
            throw new RuntimeException("User already exists!");
        }
        return userRepository.save(new User(name, telegramId));
    }
}