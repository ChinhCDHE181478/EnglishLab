package fu.sep490.g23.backend.service.user.impl;

import fu.sep490.g23.backend.service.user.AvatarStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AvatarStorageServiceImpl implements AvatarStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 1024L * 1024;
    private static final int MAX_IMAGE_DIMENSION = 4096;
    private static final String FILE_PREFIX = "avatar-";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "gif", Set.of("image/gif")
    );

    private final Path storageDirectory;

    public AvatarStorageServiceImpl(@Value("${englishlab.avatars.dir:backend/uploads/avatars}") String storageDir) {
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo thư mục lưu ảnh hồ sơ.", exception);
        }
    }

    @Override
    public String store(MultipartFile file) {
        validate(file);

        String extension = normalizeExtension(file.getOriginalFilename());
        String fileName = FILE_PREFIX + UUID.randomUUID() + "." + extension;
        Path target = resolveStoredFile(fileName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh hồ sơ.", exception);
        }
        return fileName;
    }

    @Override
    public Resource load(String fileName) {
        Path target = resolveStoredFile(fileName);
        if (!Files.isRegularFile(target)) {
            throw new IllegalArgumentException("Không tìm thấy ảnh hồ sơ.");
        }
        return new FileSystemResource(target);
    }

    @Override
    public String contentType(String fileName) {
        try {
            String contentType = Files.probeContentType(resolveStoredFile(fileName));
            return contentType == null ? "application/octet-stream" : contentType;
        } catch (IOException exception) {
            return "application/octet-stream";
        }
    }

    @Override
    public void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveStoredFile(fileName));
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể xóa ảnh hồ sơ cũ.", exception);
        }
    }

    @Override
    public void deleteByUrl(String avatarUrl) {
        String fileName = extractFileName(avatarUrl);
        if (fileName != null) {
            delete(fileName);
        }
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

    private Path resolveStoredFile(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.startsWith(FILE_PREFIX)
                || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IllegalArgumentException("Tên ảnh hồ sơ không hợp lệ.");
        }
        Path target = storageDirectory.resolve(fileName).normalize();
        if (!target.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Tên ảnh hồ sơ không hợp lệ.");
        }
        return target;
    }

    private String extractFileName(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }
        String normalized = avatarUrl.trim().replace('\\', '/');
        String marker = "/api/user/avatars/";
        int markerIndex = normalized.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        String fileName = normalized.substring(markerIndex + marker.length());
        int queryIndex = fileName.indexOf('?');
        if (queryIndex >= 0) {
            fileName = fileName.substring(0, queryIndex);
        }
        fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8).trim();
        return fileName.contains("/") || fileName.contains("..") || !fileName.startsWith(FILE_PREFIX)
                ? null
                : fileName;
    }
}
