package com.aeris.bot.util;

import com.aeris.bot.service.MenuCardService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Component
public class MenuCardDataLoader implements CommandLineRunner {

    private final MenuCardService menuCardService;

    public MenuCardDataLoader(MenuCardService menuCardService) {
        this.menuCardService = menuCardService;
    }

    @Override
    public void run(String... args) throws Exception {
        String directoryPath = "/Users/michaelwelly/Desktop/AERISMENU";
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".pdf"));
            if (files != null) {
                for (File file : files) {
                    saveMenuCardFromFile(file);
                }
            }
        } else {
            System.err.println("Directory not found: " + directoryPath);
        }
    }

    private void saveMenuCardFromFile(File file) throws IOException {
        String fileName = file.getName();
        String description = "Описание для " + fileName; // Вы можете кастомизировать описание.

        // Вызываем сервис для сохранения данных
        menuCardService.saveMenuCard(
                fileName.replace(".pdf", ""), // Имя меню (без расширения)
                description,
                (MultipartFile) file
        );

        System.out.println("Файл сохранен: " + fileName);
    }
}