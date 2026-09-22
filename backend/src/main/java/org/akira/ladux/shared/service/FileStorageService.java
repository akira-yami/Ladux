package org.akira.ladux.shared.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Shared local disk I/O for uploads. Domain services store the returned public path in DB.
 * Does not touch entities, transactions, or cache.
 */
public interface FileStorageService {

    /**
     * Validate, write file under {@code {uploadRoot}/{subDir}/{uuid}.ext},
     * return public URL path e.g. {@code /uploads/categories/uuid.webp}.
     */
    String store(String subDir, MultipartFile file);

    /**
     * Delete local file if {@code publicUrl} is under {@code /uploads/}.
     * No-op for null, blank, external URLs, or paths outside the upload root.
     */
    void deleteIfLocal(String publicUrl);
}
