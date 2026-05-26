package com.motherhood.journey.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile store(MultipartFile file, String subdirectory);

    record StoredFile(String path, String sha256, String contentType, long size) {}
}
