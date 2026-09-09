package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.dto.response.classroom.HomeworkAttachmentUploadResponse;
import fu.sep490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import fu.sep490.g23.backend.service.storage.LegacyLocalFileReader;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HomeworkAttachmentStorageServiceImpl implements HomeworkAttachmentStorageService {
    /** Legacy URL marker used by DB rows written before the R2 migration. */
    private static final String LEGACY_PUBLIC_ATTACHMENT_PATH = "/api/classroom-homework/attachments/";
    /** New R2-style object prefix – keeps objects grouped per logical kind. */
    static final String PREFIX = "classroom-attachments";
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "rar",
            "jpg", "jpeg", "png", "mp3", "m4a", "wav", "webm", "mp4"
    );

    private final ObjectStore objectStore;
    private final LegacyLocalFileReader legacyLocalReader;
    private final long maxStorageBytes;
    private final int maxUploadsPerHour;
    private final long maxBytesPerDay;
    private final AtomicLong storedBytes;
    private final Map<String, UploadQuota> uploadQuotas = new ConcurrentHashMap<>();
    private final Object storageLock = new Object();

    public HomeworkAttachmentStorageServiceImpl(
            ObjectStore objectStore,
            LegacyLocalFileReader legacyLocalReader,
            @Value("${englishlab.homework-attachments.max-storage-bytes:5368709120}") long maxStorageBytes,
            @Value("${englishlab.homework-attachments.max-uploads-per-hour:30}") int maxUploadsPerHour,
            @Value("${englishlab.homework-attachments.max-bytes-per-day:209715200}") long maxBytesPerDay
    ) {
        this.objectStore = objectStore;
        this.legacyLocalReader = legacyLocalReader;
        this.maxStorageBytes = Math.max(MAX_FILE_SIZE_BYTES, maxStorageBytes);
        this.maxUploadsPerHour = Math.max(1, maxUploadsPerHour);
        this.maxBytesPerDay = Math.max(MAX_FILE_SIZE_BYTES, maxBytesPerDay);
        this.storedBytes = new AtomicLong(estimateStoredBytes());
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
        String objectKey = objectStore.objectKey(PREFIX, fileName);
        UploadQuota quota = uploadQuotas.computeIfAbsent(normalizedOwner, ignored -> new UploadQuota());
        Instant now = Instant.now();
        synchronized (quota) {
            quota.prune(now);
            if (quota.lastHour.size() >= maxUploadsPerHour) {
                throw new IllegalArgumentException("Bạn đã tải quá nhiều tệp trong một giờ. Vui lòng thử lại sau.");
            }
            long bytesToday = quota.lastDay.stream().mapToLong(UploadEvent::size).sum();
            if (bytesToday + file.getSize() > maxBytesPerDay) {
                throw new IllegalArgumentException("Bạn đã vượt quá dung lượng tải tệp cho phép trong ngày.");
            }

            // Perform the slow R2 put OUTSIDE the global storageLock so concurrent uploads
            // for different users do not serialize behind each other. The cheap counter
            // update happens after the I/O completes.
            ObjectStore.StoredObject stored;
            try (InputStream stream = file.getInputStream()) {
                stored = objectStore.put(
                        objectKey,
                        stream,
                        file.getSize(),
                        audioContentType(fileName, file.getContentType())
                );
            } catch (IOException ioException) {
                throw new IllegalStateException("Không thể đọc tệp đính kèm.", ioException);
            }

            // Re-check quota after the I/O completes, before adding to the global counter.
            synchronized (storageLock) {
                if (storedBytes.get() + file.getSize() > maxStorageBytes) {
                    // Rollback: delete the file we just uploaded so quota stays consistent.
                    safeDeleteQuietly(objectKey);
                    throw new IllegalStateException("Kho lưu trữ tệp đang đầy. Vui lòng liên hệ quản trị viên.");
                }
                storedBytes.addAndGet(file.getSize());
            }

            String url = stored.publicUrl();
            if (url.startsWith("/local-files/")) {
                String base = publicUrlBase == null ? LEGACY_PUBLIC_ATTACHMENT_PATH : publicUrlBase;
                url = base.endsWith("/") ? base + fileName : base + "/" + fileName;
            }
            UploadEvent event = new UploadEvent(now, file.getSize());
            quota.lastHour.addLast(event);
            quota.lastDay.addLast(event);
            return HomeworkAttachmentUploadResponse.builder()
                    .fileName(fileName)
                    .originalFileName(safeOriginalFileName(file.getOriginalFilename()))
                    .contentType(audioContentType(fileName, file.getContentType()))
                    .size(file.getSize())
                    .url(url)
                    .build();
        }
    }

    /** Best-effort delete used for rollback when quota is exceeded after the upload. */
    private void safeDeleteQuietly(String objectKey) {
        try {
            objectStore.delete(objectKey);
        } catch (RuntimeException exception) {
            log.warn("Failed to roll back uploaded object {}: {}", objectKey, exception.getMessage());
        }
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tệp đính kèm."));
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
    public Optional<StoredHomeworkAttachment> loadStoredAttachmentFromUrl(String attachmentUrl) {
        Optional<String> fileName = extractStoredFileName(attachmentUrl);
        if (fileName.isEmpty()) {
            return Optional.empty();
        }
        String sanitized = fileName.get();

        // Prefer the legacy local file when present so AI grading keeps working on data uploaded
        // before the R2 migration.
        var legacy = legacyLocalReader.tryLoad(PREFIX, sanitized);
        if (legacy.isPresent()) {
            try {
                byte[] bytes = legacy.get().getInputStream().readAllBytes();
                return Optional.of(new StoredHomeworkAttachment(
                        sanitized,
                        audioContentType(sanitized, legacyLocalReader.probeContentType(PREFIX, sanitized)),
                        bytes.length,
                        bytes
                ));
            } catch (IOException exception) {
                throw new IllegalStateException("Không thể đọc tệp đính kèm để chấm bài.", exception);
            }
        }

        String objectKey = objectStore.objectKey(PREFIX, sanitized);
        Optional<byte[]> bytes = objectStore.getBytes(objectKey);
        if (bytes.isEmpty()) {
            return Optional.empty();
        }
        byte[] data = bytes.get();
        return Optional.of(new StoredHomeworkAttachment(
                sanitized,
                audioContentType(sanitized, contentType(sanitized)),
                data.length,
                data
        ));
    }

    @Override
    public List<String> findStoredFileNamesOlderThan(Duration minimumAge) {
        Instant cutoff = Instant.now().minus(minimumAge == null ? Duration.ofHours(24) : minimumAge);
        // The orphan-cleanup job only cares about files uploaded by students; teacher uploads
        // (homework problems, classroom material, center material) live in the same prefix so we
        // list everything and let the caller filter by reference existence.
        List<String> keys = objectStore.listKeysOlderThan(PREFIX, cutoff);
        List<String> result = new ArrayList<>(keys.size());
        for (String key : keys) {
            int slash = key.lastIndexOf('/');
            String name = slash >= 0 ? key.substring(slash + 1) : key;
            result.add(name);
        }
        return result;
    }

    @Override
    public void delete(String fileName) {
        String objectKey = objectStore.objectKey(PREFIX, safeFileName(fileName));
        synchronized (storageLock) {
            long size = objectStore.getSize(objectKey).orElse(0L);
            objectStore.delete(objectKey);
            if (size > 0L) {
                storedBytes.updateAndGet(current -> Math.max(0L, current - size));
            }
        }
    }

    private String safeFileName(String fileName) {
        String safe = StringUtils.getFilename(String.valueOf(fileName == null ? "" : fileName));
        if (safe == null || safe.isBlank() || !safe.equals(fileName)) {
            throw new IllegalArgumentException("Tên tệp đính kèm không hợp lệ.");
        }
        // Explicit path-traversal and shell-meta guard. StringUtils.getFilename strips
        // path segments, but be explicit so the contract is obvious to future readers.
        if (safe.contains("/") || safe.contains("\\") || safe.contains("..")
                || safe.contains(":") || safe.contains("\0") || safe.contains("?")
                || safe.contains("*") || safe.startsWith(".")) {
            throw new IllegalArgumentException("Tên tệp đính kèm không hợp lệ.");
        }
        return safe;
    }

    private Optional<String> extractStoredFileName(String attachmentUrl) {
        String value = String.valueOf(attachmentUrl == null ? "" : attachmentUrl).trim();
        if (value.isBlank()) {
            return Optional.empty();
        }
        String fileName;
        try {
            String path = new URI(value).getPath();
            if (path != null && path.contains(LEGACY_PUBLIC_ATTACHMENT_PATH)) {
                int markerIndex = path.indexOf(LEGACY_PUBLIC_ATTACHMENT_PATH);
                fileName = path.substring(markerIndex + LEGACY_PUBLIC_ATTACHMENT_PATH.length());
            } else {
                // R2 URL – use the last path segment.
                String normalized = path == null ? value : path;
                int slash = normalized.lastIndexOf('/');
                fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
            }
        } catch (URISyntaxException exception) {
            int slash = value.lastIndexOf('/');
            fileName = slash >= 0 ? value.substring(slash + 1) : value;
        }
        int query = fileName.indexOf('?');
        if (query >= 0) {
            fileName = fileName.substring(0, query);
        }
        if (fileName.isBlank() || !fileName.startsWith("homework-") || fileName.contains("/")) {
            return Optional.empty();
        }
        return Optional.of(fileName);
    }

    private String audioContentType(String fileName, String detectedType) {
        String extension = StringUtils.getFilenameExtension(fileName);
        return switch (extension == null ? "" : extension.toLowerCase(Locale.ROOT)) {
            case "mp3" -> "audio/mpeg";
            case "m4a", "mp4" -> "audio/mp4";
            case "wav" -> "audio/wav";
            case "webm" -> "audio/webm";
            default -> detectedType == null ? "application/octet-stream" : detectedType;
        };
    }

    private long estimateStoredBytes() {
        // Sum the size of every existing object under the homework prefix so the in-memory
        // quota starts at a sane value rather than 0. Failures are non-fatal: we fall back
        // to 0 if R2 cannot be listed for any reason, and the counter will self-correct as
        // files are uploaded and deleted.
        try {
            Instant farPast = Instant.EPOCH;
            return objectStore.listKeysOlderThan(PREFIX, farPast).stream()
                    .mapToLong(key -> objectStore.getSize(key).orElse(0L))
                    .sum();
        } catch (RuntimeException exception) {
            log.warn("Failed to estimate stored bytes for {}: {}. Starting at 0.",
                    PREFIX, exception.getMessage());
            return 0L;
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
