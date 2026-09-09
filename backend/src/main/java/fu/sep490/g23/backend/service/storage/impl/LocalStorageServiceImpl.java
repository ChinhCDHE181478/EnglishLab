package fu.sep490.g23.backend.service.storage.impl;

import fu.sep490.g23.backend.config.ObjectStorageProperties;
import fu.sep490.g23.backend.config.LocalStorageCondition;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Filesystem-backed implementation of {@link ObjectStore}. Selected when
 * {@code englishlab.storage.backend=local} or when no R2 credentials are configured.
 *
 * <p>Used as a development fallback so contributors can run the application without an
 * R2 account. Public URLs are exposed via the existing controller endpoints
 * (see {@code /api/user/avatars/...}, {@code /api/course-thumbnails/...},
 * {@code /api/classroom-homework/attachments/...}, {@code /api/student/assessments/audio/...}).
 */
@Service
@Conditional(LocalStorageCondition.class)
@Slf4j
public class LocalStorageServiceImpl implements ObjectStore {

    private final ObjectStorageProperties properties;
    private final Path baseDirectory;

    public LocalStorageServiceImpl(ObjectStorageProperties properties) {
        this.properties = properties;
        String baseDir = properties.getLocalBaseDir() == null || properties.getLocalBaseDir().isBlank()
                ? "backend/uploads"
                : properties.getLocalBaseDir();
        this.baseDirectory = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo thư mục gốc cho LocalStorageService: " + baseDirectory, exception);
        }
        log.warn("LocalStorageService is active (object store = local). " +
                "Files will be stored at {}. Do NOT use this in production.", baseDirectory);
    }

    @Override
    public StoredObject put(String objectKey, InputStream content, long size, String contentType) {
        Path target = resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new StorageException("Không thể lưu tệp cục bộ: " + objectKey, exception);
        }
        return new StoredObject(objectKey, publicUrl(objectKey), size, contentType);
    }

    @Override
    public Optional<byte[]> getBytes(String objectKey) {
        Path target = resolve(objectKey);
        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(target));
        } catch (IOException exception) {
            throw new StorageException("Không thể đọc tệp cục bộ: " + objectKey, exception);
        }
    }

    @Override
    public Optional<Instant> getLastModified(String objectKey) {
        Path target = resolve(objectKey);
        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.getLastModifiedTime(target).toInstant());
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Long> getSize(String objectKey) {
        Path target = resolve(objectKey);
        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.size(target));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException exception) {
            throw new StorageException("Không thể xóa tệp cục bộ: " + objectKey, exception);
        }
    }

    @Override
    public List<String> listKeysOlderThan(String prefix, Instant cutoff) {
        Path root = resolve(prefix);
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                        } catch (IOException exception) {
                            return false;
                        }
                    })
                    .forEach(path -> result.add(baseDirectory.relativize(path).toString().replace('\\', '/')));
        } catch (IOException exception) {
            throw new StorageException("Không thể duyệt thư mục " + root, exception);
        }
        return result;
    }

    @Override
    public String probeContentType(String objectKey) {
        try {
            String detected = Files.probeContentType(resolve(objectKey));
            return detected == null ? guessFromExtension(objectKey) : detected;
        } catch (IOException exception) {
            return guessFromExtension(objectKey);
        }
    }

    @Override
    public String publicUrl(String objectKey) {
        // For local mode, callers should still rely on backend proxy endpoints (see controllers).
        return "/local-files/" + objectKey;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    private Path resolve(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key không được để trống.");
        }
        if (objectKey.contains("..")) {
            throw new IllegalArgumentException("Object key không hợp lệ: " + objectKey);
        }
        Path target = baseDirectory.resolve(objectKey).normalize();
        if (!target.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("Object key không hợp lệ: " + objectKey);
        }
        return target;
    }

    private static String guessFromExtension(String objectKey) {
        int dot = objectKey.lastIndexOf('.');
        if (dot < 0 || dot == objectKey.length() - 1) {
            return "application/octet-stream";
        }
        String extension = objectKey.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "mp3" -> "audio/mpeg";
            case "m4a", "mp4" -> "audio/mp4";
            case "wav" -> "audio/wav";
            case "webm" -> "audio/webm";
            case "ogg" -> "audio/ogg";
            case "txt" -> "text/plain";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }
}
