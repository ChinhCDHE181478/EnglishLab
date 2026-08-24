package fu.sep490.g23.backend.service.course.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.response.course.BunnyVideoUploadResponse;
import fu.sep490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import fu.sep490.g23.backend.service.course.BunnyStreamService;
import fu.sep490.g23.backend.service.course.TranscriptSegmentNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BunnyStreamServiceImpl implements BunnyStreamService {

    private static final String BUNNY_VIDEO_API = "https://video.bunnycdn.com/library/%s/videos";
    private static final Pattern EMBED_OR_PLAY_PATTERN = Pattern.compile(
            "(?:iframe\\.)?mediadelivery\\.net/(?:embed|play)/(\\d+)/([0-9a-fA-F-]{8,})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PLAYER_PATTERN = Pattern.compile(
            "player\\.mediadelivery\\.net/(?:embed|play)/(\\d+)/([0-9a-fA-F-]{8,})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern GUID_ONLY_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern VTT_CUE_PATTERN = Pattern.compile(
            "(?m)^(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{1,2})(?:[.,](\\d{1,3}))?\\s*-->\\s*(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{1,2})(?:[.,](\\d{1,3}))?"
    );
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int TRANSCRIBE_POLL_ATTEMPTS = 8;
    private static final long TRANSCRIBE_POLL_DELAY_MS = 2000L;
    private static final List<String> PREFERRED_CAPTION_LANGS = List.of("en", "vi", "en-US", "en-GB");

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

    @Override
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

    @Override
    public Optional<BunnyVideoRef> resolveVideoRef(String videoUrl, String bunnyVideoId, String bunnyLibraryId) {
        String configuredLibrary = blankToNull(libraryId);
        String explicitLibrary = blankToNull(bunnyLibraryId);
        String explicitVideoId = blankToNull(bunnyVideoId);

        if (explicitVideoId != null) {
            String resolvedLibrary = explicitLibrary != null ? explicitLibrary : configuredLibrary;
            if (resolvedLibrary != null) {
                return Optional.of(new BunnyVideoRef(resolvedLibrary, explicitVideoId));
            }
        }

        String url = blankToNull(videoUrl);
        if (url == null) {
            return Optional.empty();
        }

        Matcher embedMatcher = EMBED_OR_PLAY_PATTERN.matcher(url);
        if (embedMatcher.find()) {
            return Optional.of(new BunnyVideoRef(embedMatcher.group(1), embedMatcher.group(2)));
        }

        Matcher playerMatcher = PLAYER_PATTERN.matcher(url);
        if (playerMatcher.find()) {
            return Optional.of(new BunnyVideoRef(playerMatcher.group(1), playerMatcher.group(2)));
        }

        if (GUID_ONLY_PATTERN.matcher(url).matches() && configuredLibrary != null) {
            return Optional.of(new BunnyVideoRef(configuredLibrary, url));
        }

        return Optional.empty();
    }

    @Override
    public List<TranscriptSegmentResponse> fetchTranscriptSegments(String libraryIdValue, String videoId) {
        validateConfiguration();
        if (blankToNull(libraryIdValue) == null || blankToNull(videoId) == null) {
            return List.of();
        }

        try {
            List<TranscriptSegmentResponse> existing = loadCaptionSegments(libraryIdValue, videoId);
            if (!existing.isEmpty()) {
                return TranscriptSegmentNormalizer.normalize(existing);
            }

            requestTranscription(libraryIdValue, videoId);
            for (int attempt = 0; attempt < TRANSCRIBE_POLL_ATTEMPTS; attempt++) {
                sleepQuietly(TRANSCRIBE_POLL_DELAY_MS);
                List<TranscriptSegmentResponse> polled = loadCaptionSegments(libraryIdValue, videoId);
                if (!polled.isEmpty()) {
                    return TranscriptSegmentNormalizer.normalize(polled);
                }
            }
            return List.of();
        } catch (RuntimeException ex) {
            log.warn("Bunny transcript fetch failed for video {}: {}", videoId, ex.getMessage());
            throw ex;
        }
    }

    List<TranscriptSegmentResponse> parseWebVtt(String vttContent) {
        if (vttContent == null || vttContent.isBlank()) {
            return List.of();
        }

        String normalized = vttContent.replace("\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n');
        String[] blocks = normalized.split("\\n\\s*\\n");
        List<TranscriptSegmentResponse> segments = new ArrayList<>();

        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty() || trimmed.toUpperCase(Locale.ROOT).startsWith("WEBVTT") || trimmed.startsWith("NOTE")) {
                continue;
            }

            String[] lines = trimmed.split("\\n");
            int cueLineIndex = -1;
            for (int i = 0; i < lines.length; i++) {
                if (VTT_CUE_PATTERN.matcher(lines[i].trim()).find()) {
                    cueLineIndex = i;
                    break;
                }
            }
            if (cueLineIndex < 0) {
                continue;
            }

            Matcher cueMatcher = VTT_CUE_PATTERN.matcher(lines[cueLineIndex].trim());
            if (!cueMatcher.find()) {
                continue;
            }

            double start = toSeconds(cueMatcher.group(1), cueMatcher.group(2), cueMatcher.group(3), cueMatcher.group(4));
            double end = toSeconds(cueMatcher.group(5), cueMatcher.group(6), cueMatcher.group(7), cueMatcher.group(8));
            StringBuilder text = new StringBuilder();
            for (int i = cueLineIndex + 1; i < lines.length; i++) {
                String line = lines[i].replaceAll("<[^>]+>", "").trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (!text.isEmpty()) {
                    text.append(' ');
                }
                text.append(line);
            }
            if (text.isEmpty()) {
                continue;
            }
            segments.add(TranscriptSegmentResponse.builder()
                    .startSeconds(start)
                    .endSeconds(Math.max(end, start))
                    .text(text.toString())
                    .build());
        }

        segments.sort(Comparator.comparing(segment -> segment.getStartSeconds() == null ? 0d : segment.getStartSeconds()));
        return segments;
    }

    private List<TranscriptSegmentResponse> loadCaptionSegments(String libraryIdValue, String videoId) {
        JsonNode video = getVideo(libraryIdValue, videoId);
        List<String> languages = extractCaptionLanguages(video);
        for (String language : languages) {
            String vtt = downloadCaptionVtt(videoId, language);
            List<TranscriptSegmentResponse> segments = parseWebVtt(vtt);
            if (!segments.isEmpty()) {
                return segments;
            }
        }
        return List.of();
    }

    private List<String> extractCaptionLanguages(JsonNode video) {
        Set<String> languages = new LinkedHashSet<>();
        JsonNode captions = video == null ? null : video.get("captions");
        if (captions != null && captions.isArray()) {
            for (JsonNode caption : captions) {
                String srclang = caption.path("srclang").asText("").trim();
                if (!srclang.isEmpty()) {
                    languages.add(srclang);
                }
            }
        }

        List<String> ordered = new ArrayList<>();
        for (String preferred : PREFERRED_CAPTION_LANGS) {
            for (String language : languages) {
                if (language.equalsIgnoreCase(preferred) || language.toLowerCase(Locale.ROOT).startsWith(preferred.toLowerCase(Locale.ROOT) + "-")) {
                    if (!ordered.contains(language)) {
                        ordered.add(language);
                    }
                }
            }
        }
        for (String language : languages) {
            if (!ordered.contains(language)) {
                ordered.add(language);
            }
        }
        if (ordered.isEmpty()) {
            ordered.add("en");
            ordered.add("vi");
        }
        return ordered;
    }

    private JsonNode getVideo(String libraryIdValue, String videoId) {
        try {
            String endpoint = BUNNY_VIDEO_API.formatted(libraryIdValue) + "/" + videoId;
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(REQUEST_TIMEOUT)
                    .header("AccessKey", apiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Bunny get video failed: " + response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new RuntimeException("Cannot load Bunny video metadata", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Bunny get video request was interrupted", e);
        }
    }

    private void requestTranscription(String libraryIdValue, String videoId) {
        try {
            String endpoint = BUNNY_VIDEO_API.formatted(libraryIdValue) + "/" + videoId + "/transcribe?force=false";
            String body = objectMapper.writeValueAsString(Map.of(
                    "sourceLanguage", "en",
                    "targetLanguages", List.of("en"),
                    "generateTitle", false,
                    "generateDescription", false,
                    "generateChapters", false,
                    "generateMoments", false
            ));
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(REQUEST_TIMEOUT)
                    .header("AccessKey", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.info("Bunny transcribe request returned {}: {}", response.statusCode(), response.body());
            }
        } catch (IOException e) {
            log.warn("Cannot request Bunny transcription for {}: {}", videoId, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Bunny transcribe request was interrupted", e);
        }
    }

    private String downloadCaptionVtt(String videoId, String language) {
        String hostname = blankToNull(cdnHostname);
        if (hostname == null) {
            return "";
        }
        String host = hostname.replaceFirst("^https?://", "").replaceAll("/+$", "");
        String url = "https://%s/%s/captions/%s.vtt".formatted(host, videoId, language);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "text/vtt, text/plain, */*")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "";
            }
            return response.body() == null ? "" : response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return "";
        }
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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static double toSeconds(String hours, String minutes, String seconds, String millis) {
        double hourValue = hours == null || hours.isBlank() ? 0 : Double.parseDouble(hours);
        double minuteValue = minutes == null || minutes.isBlank() ? 0 : Double.parseDouble(minutes);
        double secondValue = seconds == null || seconds.isBlank() ? 0 : Double.parseDouble(seconds);
        double millisValue = 0;
        if (millis != null && !millis.isBlank()) {
            String padded = (millis + "000").substring(0, 3);
            millisValue = Double.parseDouble(padded) / 1000d;
        }
        return hourValue * 3600d + minuteValue * 60d + secondValue + millisValue;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
