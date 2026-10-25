package org.akira.ladux.shared.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final String uploadRoot;

    public FileStorageServiceImpl(@Value("${app.upload.root:../uploads}") String uploadRoot) {
        this.uploadRoot = uploadRoot;
    }

    @Override
    public String store(String subDir, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("File anh khong duoc de trong");
        }

        String safeSubDir = normalizeSubDir(subDir);

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = ALLOWED_IMAGE_TYPES.get(contentType);
        if (extension == null) {
            throw new BusinessRuleException("Chi ho tro anh JPG, PNG, WEBP hoac GIF");
        }

        String filename = UUID.randomUUID() + extension;
        Path directory = Path.of(uploadRoot, safeSubDir).toAbsolutePath().normalize();
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(directory)) {
            throw new BusinessRuleException("Duong dan upload khong hop le");
        }

        try (java.io.InputStream input = file.getInputStream()) {
            Files.createDirectories(directory);
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessRuleException("Khong the luu file anh");
        }

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                try { Files.deleteIfExists(target); } catch (IOException ignored) { }
                            }
                        }
                    });
        }
        return "/uploads/" + safeSubDir + "/" + filename;
    }

    @Override
    public void deleteIfLocal(String publicUrl) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() { deleteNow(publicUrl); }
                    });
            return;
        }
        deleteNow(publicUrl);
    }

    private void deleteNow(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank() || !publicUrl.startsWith("/uploads/")) {
            return;
        }
        String relativePath = publicUrl.substring("/uploads/".length());
        Path uploadDirectory = Path.of(uploadRoot).toAbsolutePath().normalize();
        Path storedFile = uploadDirectory.resolve(relativePath).normalize();
        if (!storedFile.startsWith(uploadDirectory)) {
            return;
        }
        try {
            Files.deleteIfExists(storedFile);
        } catch (IOException ignored) {
            // Do not block callers if old file cannot be removed
        }
    }

    /** Only allow a single path segment (e.g. products, avatars, categories). */
    private static String normalizeSubDir(String subDir) {
        if (subDir == null || subDir.isBlank()) {
            throw new BusinessRuleException("Thu muc upload khong hop le");
        }
        String trimmed = subDir.trim().replace('\\', '/');
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.startsWith(".")) {
            throw new BusinessRuleException("Thu muc upload khong hop le");
        }
        return trimmed;
    }
}
