package com.aeris.bot.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "files")
@Data // Генерирует геттеры, сеттеры, equals, hashCode и toString
@NoArgsConstructor // Генерирует конструктор без аргументов
@AllArgsConstructor // Генерирует конструктор с аргументами для всех полей
@Builder
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Lob // Указывает, что поле содержит большие двоичные данные
    @Column(name = "data", nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private String createdAt;
}