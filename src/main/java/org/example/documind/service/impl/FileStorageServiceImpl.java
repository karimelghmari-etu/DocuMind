package org.example.documind.service.impl;

import org.example.documind.service.interfaces.FileStorageService;
import org.example.documind.web.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path rootLocation;

    public FileStorageServiceImpl(@Value("${app.storage.location}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, Long ownerId) {
        if (file.isEmpty()) {
            throw new FileStorageException("Failed to store empty file.");
        }

        try {
            Path userDir = rootLocation.resolve(String.valueOf(ownerId)).normalize();
            Files.createDirectories(userDir);

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            String ext = StringUtils.getFilenameExtension(originalFilename);
            if (ext != null && ext.matches("^[A-Za-z0-9]{1,10}$")) {
                extension = "." + ext;
            }

            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path targetPath = userDir.resolve(uniqueFilename).normalize();

            if (!targetPath.startsWith(rootLocation)) {
                throw new FileStorageException("Cannot store file outside current directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return targetPath.toString();
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file.", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path targetPath = Paths.get(path).normalize();

            if (!targetPath.startsWith(rootLocation)) {
                throw new FileStorageException("Cannot delete file outside current directory.");
            }

            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file.", e);
        }
    }

    @Override
    public Resource load(String path) {
        Path targetPath = Paths.get(path).toAbsolutePath().normalize();

        if (!targetPath.startsWith(rootLocation)) {
            throw new FileStorageException("Cannot read file outside current directory.");
        }
        if (!Files.isRegularFile(targetPath) || !Files.isReadable(targetPath)) {
            throw new FileStorageException("Stored file is missing.");
        }
        return new FileSystemResource(targetPath);
    }
}
