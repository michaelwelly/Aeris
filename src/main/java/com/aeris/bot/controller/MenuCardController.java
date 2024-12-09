package com.aeris.bot.controller;

import com.aeris.bot.model.MenuCard;
import com.aeris.bot.service.MenuCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RestController
@RequestMapping("/api/menu-cards")
public class MenuCardController {

    @Autowired
    private MenuCardService menuCardService;

    /**
     * Метод для загрузки файла меню.
     */
    @PostMapping("/upload-menu-card")
    public ResponseEntity<String> uploadMenuCard(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("file") MultipartFile file) {
        try {
            menuCardService.saveMenuCard(name, description, file);
            return ResponseEntity.ok("Menu card uploaded successfully!");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload menu card.");
        }
    }

    /**
     * Метод для скачивания файла меню.
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadMenuCard(@PathVariable Long id) {
        Optional<MenuCard> menuCardOptional = menuCardService.getMenuCard(id);

        if (menuCardOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MenuCard menuCard = menuCardOptional.get();
        Path filePath = Path.of(menuCard.getFilePath());

        // Проверка на существование файла
        if (!Files.exists(filePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

        Resource resource = new FileSystemResource(filePath.toFile());

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + new File(menuCard.getFilePath()).getName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}