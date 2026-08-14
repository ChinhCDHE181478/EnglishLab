package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.service.course.CourseThumbnailStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CourseThumbnailStorageServiceImpl implements CourseThumbnailStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 4096;
    private static final String FILE_PREFIX = "course-thumbnail-";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png")
    );

    private final Path storageDirectory;

    public CourseThumbnailStorageServiceImpl(
            @Value("${englishlab.course-thumbnails.dir:backend/uploads/course-thumbnails}") String storageDir
    ) {
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo thư mục lưu ảnh bìa khóa học.", exception);
        }
    }

    @Override
    public String store(MultipartFile file) {
        validate(file);

        String extension = normalizeExtension(file.getOriginalFilename());
        String fileName = FILE_PREFIX + UUID.randomUUID() + "." + extension;
        try {
            Files.copy(file.getInputStream(), resolveStoredFile(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh bìa khóa học.", exception);
        }
        return fileName;
    }

    @Override
    public Resource load(String fileName) {
        Path target = resolveStoredFile(fileName);
        if (!Files.isRegularFile(target)) {
            throw new IllegalArgumentException("Không tìm thấy ảnh bìa khóa học.");
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

    private Path resolveStoredFile(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.startsWith(FILE_PREFIX)
                || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IllegalArgumentException("Tên ảnh bìa khóa học không hợp lệ.");
        }
        Path target = storageDirectory.resolve(fileName).normalize();
        if (!target.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Tên ảnh bìa khóa học không hợp lệ.");
        }
        return target;
    }
}
