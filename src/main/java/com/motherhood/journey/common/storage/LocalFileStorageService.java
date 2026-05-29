package com.motherhood.journey.common.storage;

import com.motherhood.journey.common.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_MIME = Set.of(
        "application/pdf", "image/jpeg", "image/png"
    );

    private static final List<MagicByte> SIGNATURES = List.of(
        new MagicByte("application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46}),
        new MagicByte("image/jpeg",      new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        new MagicByte("image/png",       new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})
    );

    @Value("${storage.local.root:./uploads}")
    private String root;

    @Value("${storage.max-file-size:10485760}")
    private long maxFileSize;

    @Override
    public StoredFile store(MultipartFile file, String subdirectory) {
        if (file == null || file.isEmpty()) {
            throw new CustomException("File is empty", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > maxFileSize) {
            throw new CustomException("File exceeds max size of " + maxFileSize + " bytes",
                HttpStatus.PAYLOAD_TOO_LARGE);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new CustomException("Failed to read upload", HttpStatus.BAD_REQUEST);
        }

        String detectedMime = detectMime(bytes);
        if (detectedMime == null || !ALLOWED_MIME.contains(detectedMime)) {
            throw new CustomException("Unsupported file type", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        String sha256 = sha256(bytes);
        String filename = UUID.randomUUID() + extensionFor(detectedMime);

        Path rootAbs = Paths.get(root).normalize().toAbsolutePath();
        Path dir = rootAbs.resolve(subdirectory).normalize();
        Path target = dir.resolve(filename).normalize();
        // Guard against subdirectory traversal: dir must stay inside root,
        // and target must stay inside dir (filename is server-generated so
        // this second check is defense-in-depth).
        if (!dir.startsWith(rootAbs) || !target.startsWith(dir)) {
            throw new CustomException("Invalid storage path", HttpStatus.BAD_REQUEST);
        }

        try {
            Files.createDirectories(dir);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new CustomException("Failed to persist file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new StoredFile(subdirectory + "/" + filename, sha256, detectedMime, bytes.length);
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String detectMime(byte[] bytes) {
        for (MagicByte sig : SIGNATURES) {
            if (bytes.length < sig.bytes.length) {
                continue;
            }
            boolean match = true;
            for (int i = 0; i < sig.bytes.length; i++) {
                if (bytes[i] != sig.bytes[i]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return sig.mime;
            }
        }
        return null;
    }

    private String extensionFor(String mime) {
        return switch (mime) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> "";
        };
    }

    private record MagicByte(String mime, byte[] bytes) {}
}
