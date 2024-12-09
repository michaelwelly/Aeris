package com.aeris.bot.service;

import com.aeris.bot.model.MenuCard;
import com.aeris.bot.repository.MenuCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Service
public class MenuCardService {

    private final MenuCardRepository menuCardRepository;

    public MenuCardService(MenuCardRepository menuCardRepository) {
        this.menuCardRepository = menuCardRepository;
    }

    public void saveMenuCard(String name, String description, MultipartFile file) throws IOException {
        // Указываем директорию для сохранения файлов
        String uploadDir = "/Users/michaelwelly/Desktop/AERISMENU";
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs(); // Создаем папку, если ее нет
        }

        String filePath = uploadDir + "/" + file.getOriginalFilename();
        file.transferTo(new File(filePath));

        MenuCard menuCard = new MenuCard();
        menuCard.setName(name);
        menuCard.setDescription(description);
        menuCard.setFilePath(filePath);

        menuCardRepository.save(menuCard);
    }

    public Optional<MenuCard> getMenuCard(Long id) {
        return menuCardRepository.findById(id);
    }

    public MenuCard getMenuCardByName(String name) {
        Optional<MenuCard> menuCardOptional = menuCardRepository.findByName(name);
        return menuCardOptional.orElse(null);
    }

}