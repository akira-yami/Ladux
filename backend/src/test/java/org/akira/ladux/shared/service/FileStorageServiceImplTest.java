package org.akira.ladux.shared.service;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorage;

    @BeforeEach
    void setUp() {
        fileStorage = new FileStorageServiceImpl(tempDir.toString());
    }

    @Test
    void store_rejectsEmptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> fileStorage.store("categories", file));
        assertTrue(ex.getMessage().toLowerCase().contains("trong")
                || ex.getMessage().toLowerCase().contains("empty")
                || ex.getMessage().contains("de trong"));
    }

    @Test
    void store_rejectsUnknownContentType() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> fileStorage.store("categories", file));
        assertTrue(ex.getMessage().contains("JPG") || ex.getMessage().contains("ho tro"));
    }

    @Test
    void store_writesFileAndReturnsPublicPath() throws Exception {
        byte[] bytes = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(bytes));

        String url = fileStorage.store("categories", file);

        assertTrue(url.matches("/uploads/categories/[0-9a-fA-F\\-]+\\.png"));
        String relative = url.substring("/uploads/".length());
        Path stored = tempDir.resolve(relative);
        assertTrue(Files.exists(stored));
        assertEquals(bytes.length, Files.size(stored));
    }

    @Test
    void deleteIfLocal_removesStoredFile() throws Exception {
        byte[] bytes = new byte[] {1, 2, 3};
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(bytes));

        String url = fileStorage.store("avatars", file);
        Path stored = tempDir.resolve(url.substring("/uploads/".length()));
        assertTrue(Files.exists(stored));

        fileStorage.deleteIfLocal(url);
        assertTrue(Files.notExists(stored));
    }

    @Test
    void deleteIfLocal_noOpsForExternalUrl() {
        fileStorage.deleteIfLocal("https://cdn.example.com/a.png");
        fileStorage.deleteIfLocal(null);
        fileStorage.deleteIfLocal("");
    }

    @Test
    void rollbackKeepsOriginalImageAndRemovesNewUpload() throws Exception {
        Path oldFile = tempDir.resolve("avatars/old.png");
        Files.createDirectories(oldFile.getParent());
        Files.write(oldFile, new byte[]{1, 2, 3});
        var file = new org.springframework.mock.web.MockMultipartFile("image", "new.png", "image/png", new byte[]{4, 5, 6});
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            fileStorage.deleteIfLocal("/uploads/avatars/old.png");
            String nextUrl = fileStorage.store("avatars", file);
            assertTrue(Files.exists(oldFile), "Original image must survive until the DB commits");
            for (var synchronization : org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations())
                synchronization.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);
            assertTrue(Files.exists(oldFile));
            assertTrue(Files.notExists(tempDir.resolve(nextUrl.substring("/uploads/".length()))));
        } finally { org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization(); }
    }
}
