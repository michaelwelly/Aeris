package com.aeris.bot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data // Автоматически создает геттеры, сеттеры, equals, hashCode и toString
@NoArgsConstructor // Создает конструктор без аргументов
@AllArgsConstructor // Создает конструктор с аргументами для всех полей
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", unique = true, nullable = false)
    private String telegramId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "username")
    private String username;

    @Column(name = "language_code")
    private String languageCode;

    @Column(name = "phone_number")
    private String phoneNumber; // Поле для номера телефона (заполняется позже)
}