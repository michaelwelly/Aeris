package com.aeris.bot.repository;

import com.aeris.bot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByTelegramId(String telegramId);
}