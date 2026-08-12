package fu.sep490.g23.backend.service.course.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.response.course.BunnyVideoUploadResponse;
import fu.sep490.g23.backend.service.course.BunnyStreamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class BunnyStreamServiceImpl implements BunnyStreamService {

    private static final String BUNNY_VIDEO_API = "https://video.bunnycdn.com/library/%s/videos";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${bunny.stream.library-id:}")
    private String libraryId;

    @Value("${bunny.stream.cdn-hostname:}")
    private String cdnHostname;

    @Value("${bunny.stream.api-key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public BunnyVideoUploadResponse uploadVideo(MultipartFile file, String title) {
        validateConfiguration();
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Video file is required");
        }

        String normalizedTitle = normalizeTitle(title, file.getOriginalFilename());
        String createEndpoint = BUNNY_VIDEO_API.formatted(libraryId);
        String videoId = createVideo(createEndpoint, normalizedTitle);
        uploadVideoFile(createEndpoint + "/" + videoId, file);

        String embedUrl = "https://iframe.mediadelivery.net/embed/%s/%s".formatted(libraryId, videoId);
        String cdnUrl = cdnHostname == null || cdnHostname.isBlank()
                ? null
                : "https://%s/%s/playlist.m3u8".formatted(cdnHostname.trim(), videoId);

        return BunnyVideoUploadResponse.builder()
                .videoId(videoId)
                .libraryId(libraryId)
                .title(normalizedTitle)
                .embedUrl(embedUrl)
                .cdnUrl(cdnUrl)
                .build();
    }

    private String createVideo(String endpoint, String title) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("title", title));
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("AccessKey", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Bunny create video failed: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode guid = root.get("guid");
            if (guid == null || guid.asText().isBlank()) {
                throw new RuntimeException("Bunny create video response did not include a video id");
            }
            return guid.asText();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create Bunny video", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Bunny create video request was interrupted", e);
        }
    }

    private void uploadVideoFile(String endpoint, MultipartFile file) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofMinutes(30))
                    .header("AccessKey", apiKey)
                    .header("Content-Type", "application/octet-stream")
                    .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> {
                        try {
                            return file.getInputStream();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Bunny upload video failed: " + response.body());
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot upload video to Bunny", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Bunny upload video request was interrupted", e);
        }
    }

    private void validateConfiguration() {
        if (libraryId == null || libraryId.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Bunny Stream is not configured. Check BUNNY_STREAM_LIBRARY_ID and BUNNY_STREAM_API_KEY.");
        }
    }

    private String normalizeTitle(String title, String filename) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        if (filename != null && !filename.isBlank()) {
            return filename.replaceFirst("\\.[^.]+$", "").trim();
        }
        return "EnglishLab course video";
    }
}
