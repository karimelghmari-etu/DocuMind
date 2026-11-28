package org.example.documind.service.interfaces;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file, Long ownerId);
    void delete(String path);

    /** Opens a stored file for reading. Refuses any path outside the storage folder. */
    Resource load(String path);
}
