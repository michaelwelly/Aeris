package com.aeris.bot.controller;

import com.aeris.bot.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class BotController {

    private final UserService userService;

    public BotController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String name, @RequestParam String telegramId) {
        userService.registerUser(name, telegramId);
        return "User registered successfully!";
    }
}