package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.*;

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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class HomeworkAttachmentStorageServiceImpl implements HomeworkAttachmentStorageService {
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "rar",
            "jpg", "jpeg", "png", "mp3", "m4a", "wav", "webm", "mp4"
    );

    private final Path storageDirectory;
    private final long maxStorageBytes;
    private final int maxUploadsPerHour;
    private final long maxBytesPerDay;
    private final AtomicLong storedBytes;
    private final Map<String, UploadQuota> uploadQuotas = new ConcurrentHashMap<>();
    private final Object storageLock = new Object();

    public HomeworkAttachmentStorageServiceImpl(
            @Value("${englishlab.homework-attachments.dir:backend/uploads/homework-attachments}") String storageDir,
            @Value("${englishlab.homework-attachments.max-storage-bytes:5368709120}") long maxStorageBytes,
            @Value("${englishlab.homework-attachments.max-uploads-per-hour:30}") int maxUploadsPerHour,
            @Value("${englishlab.homework-attachments.max-bytes-per-day:209715200}") long maxBytesPerDay
    ) {
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
        this.maxStorageBytes = Math.max(MAX_FILE_SIZE_BYTES, maxStorageBytes);
        this.maxUploadsPerHour = Math.max(1, maxUploadsPerHour);
        this.maxBytesPerDay = Math.max(MAX_FILE_SIZE_BYTES, maxBytesPerDay);
        try {
            Files.createDirectories(storageDirectory);
            this.storedBytes = new AtomicLong(calculateStoredBytes());
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo thư mục tệp bài tập.", exception);
        }
    }

    @Override
    public HomeworkAttachmentUploadResponse store(MultipartFile file, String publicUrlBase, String ownerKey) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp đính kèm.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Tệp đính kèm không được vượt quá 20 MB.");
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(normalizedExtension)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ tài liệu, tệp nén, ảnh và âm thanh/video thông dụng.");
        }
        String normalizedOwner = String.valueOf(ownerKey == null ? "" : ownerKey).trim().toLowerCase(Locale.ROOT);
        if (normalizedOwner.isBlank()) {
            throw new IllegalArgumentException("Không xác định được người tải tệp.");
        }

        String fileName = "homework-" + UUID.randomUUID() + "." + normalizedExtension;
        Path target = storageDirectory.resolve(fileName).normalize();
        UploadQuota quota = uploadQuotas.computeIfAbsent(normalizedOwner, ignored -> new UploadQuota());
        synchronized (quota) {
            Instant now = Instant.now();
            quota.prune(now);
            if (quota.lastHour.size() >= maxUploadsPerHour) {
                throw new IllegalArgumentException("Bạn đã tải quá nhiều tệp trong một giờ. Vui lòng thử lại sau.");
            }
            long bytesToday = quota.lastDay.stream().mapToLong(UploadEvent::size).sum();
            if (bytesToday + file.getSize() > maxBytesPerDay) {
                throw new IllegalArgumentException("Bạn đã vượt quá dung lượng tải tệp cho phép trong ngày.");
            }

            synchronized (storageLock) {
                if (storedBytes.get() + file.getSize() > maxStorageBytes) {
                    throw new IllegalStateException("Kho lưu trữ tệp đang đầy. Vui lòng liên hệ quản trị viên.");
                }
                try {
                    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                    storedBytes.addAndGet(file.getSize());
                } catch (IOException exception) {
                    throw new IllegalStateException("Không thể lưu tệp đính kèm.", exception);
                }
            }
            UploadEvent event = new UploadEvent(now, file.getSize());
            quota.lastHour.addLast(event);
            quota.lastDay.addLast(event);
        }

        return HomeworkAttachmentUploadResponse.builder()
                .fileName(fileName)
                .originalFileName(safeOriginalFileName(file.getOriginalFilename()))
                .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .size(file.getSize())
                .url(publicUrlBase.endsWith("/") ? publicUrlBase + fileName : publicUrlBase + "/" + fileName)
                .build();
    }

    @Override
    public Resource load(String fileName) {
        Path target = resolveStoredFile(fileName);
        if (!target.startsWith(storageDirectory) || !Files.exists(target)) {
            throw new IllegalArgumentException("Không tìm thấy tệp đính kèm.");
        }
        return new FileSystemResource(target);
    }

    @Override
    public String contentType(String fileName) {
        try {
            String type = Files.probeContentType(resolveStoredFile(fileName));
            return type == null ? "application/octet-stream" : type;
        } catch (IOException exception) {
            return "application/octet-stream";
        }
    }

    @Override
    public List<String> findStoredFileNamesOlderThan(Duration minimumAge) {
        Instant cutoff = Instant.now().minus(minimumAge == null ? Duration.ofHours(24) : minimumAge);
        List<String> result = new ArrayList<>();
        try (var files = Files.list(storageDirectory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                        } catch (IOException exception) {
                            return false;
                        }
                    })
                    .map(path -> path.getFileName().toString())
                    .forEach(result::add);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể kiểm tra tệp đính kèm cũ.", exception);
        }
        return result;
    }

    @Override
    public void delete(String fileName) {
        Path target = resolveStoredFile(fileName);
        synchronized (storageLock) {
            try {
                long size = Files.exists(target) ? Files.size(target) : 0L;
                if (Files.deleteIfExists(target)) {
                    storedBytes.updateAndGet(current -> Math.max(0L, current - size));
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Không thể xóa tệp đính kèm không còn sử dụng.", exception);
            }
        }
    }

    private Path resolveStoredFile(String fileName) {
        String safeName = StringUtils.getFilename(String.valueOf(fileName == null ? "" : fileName));
        if (safeName == null || safeName.isBlank() || !safeName.equals(fileName)) {
            throw new IllegalArgumentException("Tên tệp đính kèm không hợp lệ.");
        }
        Path target = storageDirectory.resolve(safeName).normalize();
        if (!target.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Tên tệp đính kèm không hợp lệ.");
        }
        return target;
    }

    private long calculateStoredBytes() throws IOException {
        try (var files = Files.list(storageDirectory)) {
            return files.filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException exception) {
                            return 0L;
                        }
                    })
                    .sum();
        }
    }

    private String safeOriginalFileName(String value) {
        String fileName = StringUtils.getFilename(value == null ? "" : value);
        return fileName == null || fileName.isBlank() ? "tep-dinh-kem" : fileName;
    }

    private record UploadEvent(Instant createdAt, long size) {
    }

    private static final class UploadQuota {
        private final Deque<UploadEvent> lastHour = new ArrayDeque<>();
        private final Deque<UploadEvent> lastDay = new ArrayDeque<>();

        private void prune(Instant now) {
            Instant oneHourAgo = now.minus(Duration.ofHours(1));
            Instant oneDayAgo = now.minus(Duration.ofDays(1));
            while (!lastHour.isEmpty() && lastHour.peekFirst().createdAt().isBefore(oneHourAgo)) {
                lastHour.removeFirst();
            }
            while (!lastDay.isEmpty() && lastDay.peekFirst().createdAt().isBefore(oneDayAgo)) {
                lastDay.removeFirst();
            }
        }
    }
}
