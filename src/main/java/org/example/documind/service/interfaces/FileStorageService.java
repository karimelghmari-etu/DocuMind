package org.example.documind.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file, Long ownerId);
    void delete(String path);
}
