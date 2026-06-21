package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.response.classroom.HomeworkAttachmentUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class HomeworkAttachmentStorageService {
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "rar", "jpg", "jpeg", "png"
    );

    private final Path storageDirectory;

    public HomeworkAttachmentStorageService(@Value("${englishlab.homework-attachments.dir:backend/uploads/homework-attachments}") String storageDir) {
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo thư mục tệp bài tập.", exception);
        }
    }

    public HomeworkAttachmentUploadResponse store(MultipartFile file, String publicUrlBase) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp đính kèm.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Tệp đính kèm không được vượt quá 20 MB.");
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(normalizedExtension)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ PDF, Word, PowerPoint, Excel, TXT, ZIP/RAR và ảnh JPG/PNG.");
        }

        String fileName = "homework-" + UUID.randomUUID() + "." + normalizedExtension;
        Path target = storageDirectory.resolve(fileName).normalize();
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu tệp đính kèm.", exception);
        }

        return HomeworkAttachmentUploadResponse.builder()
                .fileName(fileName)
                .originalFileName(safeOriginalFileName(file.getOriginalFilename()))
                .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .size(file.getSize())
                .url(publicUrlBase.endsWith("/") ? publicUrlBase + fileName : publicUrlBase + "/" + fileName)
                .build();
    }

    public Resource load(String fileName) {
        Path target = storageDirectory.resolve(fileName).normalize();
        if (!target.startsWith(storageDirectory) || !Files.exists(target)) {
            throw new IllegalArgumentException("Không tìm thấy tệp đính kèm.");
        }
        return new FileSystemResource(target);
    }

    public String contentType(String fileName) {
        try {
            String type = Files.probeContentType(storageDirectory.resolve(fileName).normalize());
            return type == null ? "application/octet-stream" : type;
        } catch (IOException exception) {
            return "application/octet-stream";
        }
    }

    private String safeOriginalFileName(String value) {
        String fileName = StringUtils.getFilename(value == null ? "" : value);
        return fileName == null || fileName.isBlank() ? "tep-dinh-kem" : fileName;
    }
}
