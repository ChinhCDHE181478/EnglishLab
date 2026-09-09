package fu.sep490.g23.backend.service.assessment.impl;
import fu.sep490.g23.backend.dto.response.assessment.AssessmentAudioUploadResponse;
import fu.sep490.g23.backend.service.assessment.AssessmentAudioStorageService;
import fu.sep490.g23.backend.service.storage.LegacyLocalFileReader;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AssessmentAudioStorageServiceImpl implements AssessmentAudioStorageService {

    static final String PREFIX = "assessment-audio";

    private final ObjectStore objectStore;
    private final LegacyLocalFileReader legacyLocalReader;

    public AssessmentAudioStorageServiceImpl(ObjectStore objectStore, LegacyLocalFileReader legacyLocalReader) {
        this.objectStore = objectStore;
        this.legacyLocalReader = legacyLocalReader;
    }

    public AssessmentAudioUploadResponse store(MultipartFile file, String publicUrlBase) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Audio file is required");
        }

        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!isSupportedAssessmentRecordingContentType(contentType)) {
            throw new RuntimeException("Only audio files are supported for speaking submissions");
        }

        String normalizedContentType = normalizeAssessmentRecordingContentType(contentType);
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String safeExtension = extension == null || extension.isBlank()
                ? guessExtension(contentType)
                : "." + extension.toLowerCase(Locale.ROOT);
        String fileName = "assessment-audio-" + UUID.randomUUID() + safeExtension;
        String objectKey = objectStore.objectKey(PREFIX, fileName);

        try (InputStream stream = file.getInputStream()) {
            ObjectStore.StoredObject stored = objectStore.put(objectKey, stream, file.getSize(), normalizedContentType);
            // Audio recordings stay private (no R2 public access) – fall back to the legacy
            // backend proxy URL so the controller's access checks remain authoritative.
            String url = stored.publicUrl();
            if (url.startsWith("/local-files/")) {
                String base = publicUrlBase == null ? "/api/student/assessments/audio" : publicUrlBase;
                url = base.endsWith("/") ? base + fileName : base + "/" + fileName;
            }
            return AssessmentAudioUploadResponse.builder()
                    .fileName(fileName)
                    .contentType(normalizedContentType)
                    .size(file.getSize())
                    .url(url)
                    .build();
        } catch (IOException exception) {
            throw new RuntimeException("Cannot store assessment audio", exception);
        }
    }

    public Resource loadAsResource(String fileName) {
        String sanitized = safeFileName(fileName);
        var legacy = legacyLocalReader.tryLoad(PREFIX, sanitized);
        if (legacy.isPresent()) {
            return legacy.get();
        }
        String objectKey = objectStore.objectKey(PREFIX, sanitized);
        return objectStore.getBytes(objectKey)
                .map(ByteArrayResource::new)
                .orElseThrow(() -> new RuntimeException("Assessment audio not found"));
    }

    public String detectContentType(String fileName) {
        String sanitized = safeFileName(fileName);
        if (legacyLocalReader.isEnabled() && legacyLocalReader.tryLoad(PREFIX, sanitized).isPresent()) {
            return legacyLocalReader.probeContentType(PREFIX, sanitized);
        }
        String detected = objectStore.probeContentType(objectStore.objectKey(PREFIX, sanitized));
        return detected == null || "application/octet-stream".equals(detected)
                ? guessContentType(fileName)
                : detected;
    }

    /**
     * Deletes the audio file referenced by {@code fileName} from the active object store.
     *
     * <p>Failures are swallowed so the caller (typically a CRUD flow) can still complete its
     * database update – the scheduled orphan-cleanup job will eventually reclaim the file.
     */
    public void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            objectStore.delete(objectStore.objectKey(PREFIX, safeFileName(fileName)));
        } catch (RuntimeException exception) {
            log.warn("Failed to delete assessment audio {}: {}", fileName, exception.getMessage());
        }
    }

    /**
     * Convenience wrapper that extracts the file name from a previously-stored URL before
     * delegating to {@link #delete(String)}.
     */
    public void deleteByUrl(String audioUrl) {
        extractStoredFileName(audioUrl).ifPresent(this::delete);
    }

    public Optional<StoredAssessmentAudio> loadStoredAudioFromUrl(String audioUrl) {
        Optional<String> fileName = extractStoredFileName(audioUrl);
        if (fileName.isEmpty()) {
            return Optional.empty();
        }
        String sanitized = fileName.get();

        var legacy = legacyLocalReader.tryLoad(PREFIX, sanitized);
        if (legacy.isPresent()) {
            try {
                byte[] bytes = legacy.get().getInputStream().readAllBytes();
                return Optional.of(new StoredAssessmentAudio(
                        sanitized,
                        normalizeAssessmentRecordingContentType(legacyLocalReader.probeContentType(PREFIX, sanitized)),
                        bytes.length,
                        bytes
                ));
            } catch (IOException exception) {
                throw new RuntimeException("Cannot read assessment audio", exception);
            }
        }

        String objectKey = objectStore.objectKey(PREFIX, sanitized);
        Optional<byte[]> bytes = objectStore.getBytes(objectKey);
        if (bytes.isEmpty()) {
            return Optional.empty();
        }
        byte[] data = bytes.get();
        return Optional.of(new StoredAssessmentAudio(
                sanitized,
                normalizeAssessmentRecordingContentType(detectContentType(sanitized)),
                data.length,
                data
        ));
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.startsWith("assessment-audio-")
                || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new RuntimeException("Invalid assessment audio file name");
        }
        return fileName;
    }

    private Optional<String> extractStoredFileName(String audioUrl) {
        if (audioUrl == null || audioUrl.isBlank()) {
            return Optional.empty();
        }
        String normalized = audioUrl.trim().replace('\\', '/');
        String marker = "/api/student/assessments/audio/";
        int markerIndex = normalized.indexOf(marker);
        String fileName = markerIndex >= 0
                ? normalized.substring(markerIndex + marker.length())
                : normalized.substring(normalized.lastIndexOf('/') + 1);
        int queryIndex = fileName.indexOf('?');
        if (queryIndex >= 0) {
            fileName = fileName.substring(0, queryIndex);
        }
        try {
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException ignored) {
            // Malformed encoding – keep raw.
        }
        if (fileName.isBlank()
                || fileName.contains("/")
                || fileName.contains("..")
                || !fileName.startsWith("assessment-audio-")) {
            return Optional.empty();
        }
        return Optional.of(fileName);
    }

    private String guessExtension(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "audio/mpeg" -> ".mp3";
            case "audio/mp4" -> ".m4a";
            case "video/mp4" -> ".m4a";
            case "audio/wav", "audio/x-wav" -> ".wav";
            case "audio/ogg" -> ".ogg";
            case "audio/webm", "video/webm" -> ".webm";
            default -> ".webm";
        };
    }

    private String guessContentType(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (extension == null) {
            return "audio/webm";
        }
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "mp3", "mpeg" -> "audio/mpeg";
            case "m4a", "mp4" -> "audio/mp4";
            case "wav" -> "audio/wav";
            case "ogg", "oga" -> "audio/ogg";
            case "webm" -> "audio/webm";
            default -> "audio/webm";
        };
    }

    private boolean isSupportedAssessmentRecordingContentType(String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return normalized.startsWith("audio/")
                || normalized.equals("video/webm")
                || normalized.equals("video/mp4");
    }

    private String normalizeAssessmentRecordingContentType(String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (normalized.equals("video/webm")) {
            return "audio/webm";
        }
        if (normalized.equals("video/mp4")) {
            return "audio/mp4";
        }
        return normalized.isBlank() ? "audio/webm" : normalized;
    }
}
