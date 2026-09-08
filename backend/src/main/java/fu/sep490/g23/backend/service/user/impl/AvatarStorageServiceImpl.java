package fu.sep490.g23.backend.service.user.impl;

import fu.sep490.g23.backend.service.storage.LegacyLocalFileReader;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import fu.sep490.g23.backend.service.user.AvatarStorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Stores user avatars in the configured {@link ObjectStore} (Cloudflare R2 in production,
 * local filesystem during development). All objects live under the {@code avatars/} prefix.
 *
 * <p>Objects are public by default: the returned {@code store()} result contains the full
 * URL of the uploaded object and is written straight to {@code User.avatarUrl}.
 */
@Service
public class AvatarStorageServiceImpl implements AvatarStorageService {

    static final String PREFIX = "avatars";
    private static final String FILE_PREFIX = "avatar-";
    private static final long MAX_FILE_SIZE_BYTES = 1024L * 1024;
    private static final int MAX_IMAGE_DIMENSION = 4096;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "gif", Set.of("image/gif")
    );

    private final ObjectStore objectStore;
    private final LegacyLocalFileReader legacyLocalReader;

    public AvatarStorageServiceImpl(ObjectStore objectStore, LegacyLocalFileReader legacyLocalReader) {
        this.objectStore = objectStore;
        this.legacyLocalReader = legacyLocalReader;
    }

    @Override
    public String store(MultipartFile file) {
        validate(file);

        String extension = normalizeExtension(file.getOriginalFilename());
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("Tệp ảnh hồ sơ phải có phần mở rộng (.jpg, .jpeg, .png, .gif).");
        }
        String fileName = FILE_PREFIX + UUID.randomUUID() + "." + extension;
        String objectKey = objectStore.objectKey(PREFIX, fileName);
        // Null-safe content-type lookup (defensive: if extension is not in the map, default to image/jpeg).
        Set<String> contentTypes = ALLOWED_CONTENT_TYPES.get(extension);
        String contentType = (contentTypes != null && !contentTypes.isEmpty())
                ? contentTypes.iterator().next()
                : "image/jpeg";
        try (InputStream stream = file.getInputStream()) {
            objectStore.put(objectKey, stream, file.getSize(), contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh hồ sơ.", exception);
        }
        return fileName;
    }

    /**
     * Streams the avatar bytes back to the caller. Used by {@code UserController} to support
     * the legacy {@code /api/user/avatars/{fileName}} endpoint – in practice the frontend
     * should always hit the public R2 URL directly.
     *
     * <p>To keep the old URLs working after the R2 migration we first try the legacy local
     * directory (only useful for pre-migration files). If the file is not on disk, we fall
     * back to R2.
     */
    @Override
    public Resource load(String fileName) {
        String sanitized = safeFileName(fileName);
        var legacy = legacyLocalReader.tryLoad(PREFIX, sanitized);
        if (legacy.isPresent()) {
            return legacy.get();
        }
        String objectKey = objectStore.objectKey(PREFIX, sanitized);
        return objectStore.getBytes(objectKey)
                .map(ByteArrayResource::new)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh hồ sơ."));
    }

    @Override
    public String contentType(String fileName) {
        String sanitized = safeFileName(fileName);
        if (legacyLocalReader.isEnabled() && legacyLocalReader.tryLoad(PREFIX, sanitized).isPresent()) {
            return legacyLocalReader.probeContentType(PREFIX, sanitized);
        }
        return objectStore.probeContentType(objectStore.objectKey(PREFIX, sanitized));
    }

    @Override
    public void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        objectStore.delete(objectStore.objectKey(PREFIX, safeFileName(fileName)));
    }

    @Override
    public void deleteByUrl(String avatarUrl) {
        Optional<String> fileName = extractFileName(avatarUrl);
        fileName.ifPresent(this::delete);
    }

    @Override
    public void deleteByKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        objectStore.delete(objectKey);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh hồ sơ.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Ảnh hồ sơ không được vượt quá 1 MB.");
        }

        String extension = normalizeExtension(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)
                || !ALLOWED_CONTENT_TYPES.getOrDefault(extension, Set.of()).contains(contentType)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG, PNG hoặc GIF.");
        }

        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IllegalArgumentException("Tệp đã chọn không phải là ảnh hợp lệ.");
            }
            if (image.getWidth() > MAX_IMAGE_DIMENSION || image.getHeight() > MAX_IMAGE_DIMENSION) {
                throw new IllegalArgumentException("Kích thước ảnh không được vượt quá 4096 x 4096 pixel.");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc ảnh hồ sơ.", exception);
        }
    }

    private String normalizeExtension(String originalFileName) {
        String extension = StringUtils.getFilenameExtension(originalFileName == null ? "" : originalFileName);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.startsWith(FILE_PREFIX)
                || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IllegalArgumentException("Tên ảnh hồ sơ không hợp lệ.");
        }
        return fileName;
    }

    private Optional<String> extractFileName(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return Optional.empty();
        }
        String normalized = avatarUrl.trim().replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        int queryIndex = fileName.indexOf('?');
        if (queryIndex >= 0) {
            fileName = fileName.substring(0, queryIndex);
        }
        fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8).trim();
        if (fileName.isBlank() || fileName.contains("/") || fileName.contains("..")
                || !fileName.startsWith(FILE_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(fileName);
    }
}
