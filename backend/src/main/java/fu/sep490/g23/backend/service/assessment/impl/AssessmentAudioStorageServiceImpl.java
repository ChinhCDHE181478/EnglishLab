package fu.sap490.g23.backend.service.assessment.impl;

import fu.sap490.g23.backend.service.assessment.*;

import fu.sap490.g23.backend.dto.response.assessment.AssessmentAudioUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssessmentAudioStorageServiceImpl implements AssessmentAudioStorageService {

    private final Path storageDirectory;

    public AssessmentAudioStorageServiceImpl(@Value("${englishlab.assessment-audio.dir:backend/uploads/assessment-audio}") String storageDir) {
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new RuntimeException("Cannot create assessment audio directory", exception);
        }
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
        String safeExtension = extension == null || extension.isBlank() ? guessExtension(contentType) : "." + extension.toLowerCase(Locale.ROOT);
        String fileName = "assessment-audio-" + UUID.randomUUID() + safeExtension;
        Path targetFile = storageDirectory.resolve(fileName).normalize();

        try {
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("Cannot store assessment audio", exception);
        }

        return AssessmentAudioUploadResponse.builder()
                .fileName(fileName)
                .contentType(normalizedContentType)
                .size(file.getSize())
                .url(publicUrlBase.endsWith("/") ? publicUrlBase + fileName : publicUrlBase + "/" + fileName)
                .build();
    }

    public Resource loadAsResource(String fileName) {
        Path targetFile = storageDirectory.resolve(fileName).normalize();
        if (!targetFile.startsWith(storageDirectory) || !Files.exists(targetFile)) {
            throw new RuntimeException("Assessment audio not found");
        }
        return new FileSystemResource(targetFile);
    }

    public String detectContentType(String fileName) {
        try {
            String detected = Files.probeContentType(storageDirectory.resolve(fileName).normalize());
            return detected == null || detected.equals("application/octet-stream")
                    ? guessContentType(fileName)
                    : detected;
        } catch (IOException exception) {
            return guessContentType(fileName);
        }
    }

    public Optional<StoredAssessmentAudio> loadStoredAudioFromUrl(String audioUrl) {
        String fileName = extractStoredFileName(audioUrl);
        if (fileName == null) {
            return Optional.empty();
        }

        Path targetFile = storageDirectory.resolve(fileName).normalize();
        if (!targetFile.startsWith(storageDirectory) || !Files.exists(targetFile)) {
            return Optional.empty();
        }

        try {
            return Optional.of(new StoredAssessmentAudio(
                    fileName,
                    normalizeAssessmentRecordingContentType(detectContentType(fileName)),
                    Files.size(targetFile),
                    Files.readAllBytes(targetFile)
            ));
        } catch (IOException exception) {
            throw new RuntimeException("Cannot read assessment audio", exception);
        }
    }

    private String extractStoredFileName(String audioUrl) {
        if (audioUrl == null || audioUrl.isBlank()) {
            return null;
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
        fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8).trim();
        if (fileName.isBlank()
                || fileName.contains("/")
                || fileName.contains("..")
                || !fileName.startsWith("assessment-audio-")) {
            return null;
        }
        return fileName;
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

