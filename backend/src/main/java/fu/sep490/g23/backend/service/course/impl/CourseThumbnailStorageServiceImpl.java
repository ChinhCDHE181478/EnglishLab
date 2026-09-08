package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.service.course.CourseThumbnailStorageService;
import fu.sep490.g23.backend.service.storage.LegacyLocalFileReader;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Stores course thumbnails in the configured {@link ObjectStore} under the {@code course-thumbnails/}
 * prefix. Thumbnails are public (served directly from Cloudflare R2 when configured).
 */
@Service
public class CourseThumbnailStorageServiceImpl implements CourseThumbnailStorageService {

    static final String PREFIX = "course-thumbnails";
    /** Public accessor so callers in other packages (controllers, services) can build object keys. */
    public static String getPrefix() {
        return PREFIX;
    }
    private static final String FILE_PREFIX = "course-thumbnail-";
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 4096;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png")
    );

    private final ObjectStore objectStore;
    private final LegacyLocalFileReader legacyLocalReader;

    public CourseThumbnailStorageServiceImpl(ObjectStore objectStore, LegacyLocalFileReader legacyLocalReader) {
        this.objectStore = objectStore;
        this.legacyLocalReader = legacyLocalReader;
    }

    @Override
    public String store(MultipartFile file) {
        validate(file);

        String extension = normalizeExtension(file.getOriginalFilename());
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("Tệp ảnh bìa phải có phần mở rộng (.jpg, .jpeg, .png, .webp).");
        }
        String fileName = FILE_PREFIX + UUID.randomUUID() + "." + extension;
        String objectKey = objectStore.objectKey(PREFIX, fileName);
        // Null-safe content-type lookup (defensive: default to image/jpeg if map misses).
        Set<String> contentTypes = ALLOWED_CONTENT_TYPES.get(extension);
        String contentType = (contentTypes != null && !contentTypes.isEmpty())
                ? contentTypes.iterator().next()
                : "image/jpeg";
        try (InputStream stream = file.getInputStream()) {
            objectStore.put(objectKey, stream, file.getSize(), contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh bìa khóa học.", exception);
        }
        return fileName;
    }

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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh bìa khóa học."));
    }

    @Override
    public String contentType(String fileName) {
        String sanitized = safeFileName(fileName);
        if (legacyLocalReader.isEnabled() && legacyLocalReader.tryLoad(PREFIX, sanitized).isPresent()) {
            return legacyLocalReader.probeContentType(PREFIX, sanitized);
        }
        return objectStore.probeContentType(objectStore.objectKey(PREFIX, sanitized));
    }

    /**
     * Deletes the thumbnail referenced by a previously-stored URL.
     *
     * <p>This is used by CRUD flows (course update/delete) so the previous thumbnail does not
     * stay orphaned in R2 once the course row no longer references it.
     */
    public void deleteByUrl(String thumbnailUrl) {
        extractFileNameFromUrl(thumbnailUrl).ifPresent(this::delete);
    }

    /** Optional delete helper – currently no controller calls it, kept for future use. */
    public void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        objectStore.delete(objectStore.objectKey(PREFIX, safeFileName(fileName)));
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
            throw new IllegalArgumentException("Vui lòng chọn ảnh bìa.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Ảnh bìa không được vượt quá 5 MB.");
        }

        String extension = normalizeExtension(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)
                || !ALLOWED_CONTENT_TYPES.getOrDefault(extension, Set.of()).contains(contentType)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG hoặc PNG.");
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
            throw new IllegalArgumentException("Không thể đọc ảnh bìa khóa học.", exception);
        }
    }

    private String normalizeExtension(String originalFileName) {
        String extension = StringUtils.getFilenameExtension(originalFileName == null ? "" : originalFileName);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.startsWith(FILE_PREFIX)
                || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IllegalArgumentException("Tên ảnh bìa khóa học không hợp lệ.");
        }
        return fileName;
    }

    public Optional<String> extractFileNameFromUrl(String thumbnailUrl) {
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
            return Optional.empty();
        }
        String normalized = thumbnailUrl.trim().replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        int queryIndex = fileName.indexOf('?');
        if (queryIndex >= 0) {
            fileName = fileName.substring(0, queryIndex);
        }
        // URL-decode so percent-encoded chars (e.g. %20 for spaces) do not leak through.
        try {
            fileName = java.net.URLDecoder.decode(fileName, java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException ignored) {
            // Malformed encoding – keep raw.
        }
        if (fileName.isBlank() || fileName.contains("/") || fileName.contains("..")
                || !fileName.startsWith(FILE_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(fileName);
    }
}
