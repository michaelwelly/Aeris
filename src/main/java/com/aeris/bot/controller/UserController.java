package com.aeris.bot.controller;

import com.aeris.bot.model.User;
import com.aeris.bot.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Регистрация нового пользователя
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(
            @RequestParam String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam String telegramId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String languageCode) {
        try {
            userService.registerUser(firstName, lastName, telegramId, username, languageCode);
            return ResponseEntity.ok("User registered successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Сохранение номера телефона пользователя
    @PatchMapping("/{telegramId}/phone")
    public ResponseEntity<String> updatePhoneNumber(
            @PathVariable String telegramId,
            @RequestParam String phoneNumber) {
        try {
            userService.updatePhoneNumber(telegramId, phoneNumber);
            return ResponseEntity.ok("Phone number updated successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Сохранение данных пользователя из Telegram API
    @PostMapping("/save-from-telegram")
    public ResponseEntity<String> saveUserFromTelegram(@RequestBody org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        try {
            userService.saveUser(telegramUser);
            return ResponseEntity.ok("User saved successfully from Telegram!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Поиск пользователя по Telegram ID
    @GetMapping("/{telegramId}")
    public ResponseEntity<User> findUserByTelegramId(@PathVariable String telegramId) {
        User user = userService.findUserByTelegramId(telegramId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
}