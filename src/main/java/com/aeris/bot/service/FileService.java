package com.aeris.bot.service;

import com.aeris.bot.model.FileEntity;
import com.aeris.bot.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;

    public FileEntity saveFile(MultipartFile file) throws IOException {
        FileEntity fileEntity = FileEntity.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .data(file.getBytes())
                .build();
        return fileRepository.save(fileEntity);
    }

    public Optional<FileEntity> getFile(Long id) {
        return fileRepository.findById(id);
    }
}