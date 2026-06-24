package fu.sap490.g23.backend.service.course.impl;

import fu.sap490.g23.backend.service.course.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class YouTubeTranscriptServiceImpl implements YouTubeTranscriptService {

    private static final Pattern WATCH_PATTERN = Pattern.compile("[?&]v=([A-Za-z0-9_-]{6,})");
    private static final Pattern SHORT_PATTERN = Pattern.compile("(?:youtu\\.be/|/embed/|/shorts/)([A-Za-z0-9_-]{6,})");
    private static final Pattern INNERTUBE_API_KEY_PATTERN = Pattern.compile("\"INNERTUBE_API_KEY\":\"([^\"]+)\"");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final String ANDROID_CLIENT_NAME = "ANDROID";
    private static final String ANDROID_CLIENT_VERSION = "20.10.38";
    private static final String ANDROID_USER_AGENT = "com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<String> extractVideoId(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            return Optional.empty();
        }

        Matcher watchMatcher = WATCH_PATTERN.matcher(videoUrl);
        if (watchMatcher.find()) {
            return Optional.of(watchMatcher.group(1));
        }

        Matcher shortMatcher = SHORT_PATTERN.matcher(videoUrl);
        if (shortMatcher.find()) {
            return Optional.of(shortMatcher.group(1));
        }

        String trimmed = videoUrl.trim();
        if (trimmed.matches("^[A-Za-z0-9_-]{6,}$")) {
            return Optional.of(trimmed);
        }
        return Optional.empty();
    }

    public List<TranscriptSegmentResponse> fetchTranscriptSegments(String videoUrl) {
        return extractVideoId(videoUrl)
                .map(this::fetchTranscriptSegmentsByVideoId)
                .orElseGet(List::of);
    }

    private List<TranscriptSegmentResponse> fetchTranscriptSegmentsByVideoId(String videoId) {
        try {
            List<CaptionTrack> tracks = fetchCaptionTracks(videoId);
            if (tracks.isEmpty()) {
                tracks = fetchPlayerCaptionTracks(videoId);
            }
            if (tracks.isEmpty()) {
                return List.of();
            }

            CaptionTrack selectedTrack = selectTrack(tracks);
            List<TranscriptSegmentResponse> jsonSegments = fetchJsonTranscript(selectedTrack);
            if (!jsonSegments.isEmpty()) {
                return jsonSegments;
            }
            return fetchXmlTranscript(selectedTrack);
        } catch (Exception ex) {
            log.warn("Không thể lấy bản chép lời YouTube cho videoId={}: {}", videoId, ex.getMessage());
            return List.of();
        }
    }

    private List<CaptionTrack> fetchCaptionTracks(String videoId) throws Exception {
        String body = get("https://www.youtube.com/api/timedtext?type=list&v=" + encode(videoId));
        if (body == null || body.isBlank()) {
            return List.of();
        }

        Document document = parseXml(body);
        NodeList trackNodes = document.getElementsByTagName("track");
        List<CaptionTrack> tracks = new ArrayList<>();
        for (int index = 0; index < trackNodes.getLength(); index++) {
            Element track = (Element) trackNodes.item(index);
            String languageCode = track.getAttribute("lang_code");
            if (languageCode == null || languageCode.isBlank()) {
                continue;
            }
            tracks.add(new CaptionTrack(
                    videoId,
                    languageCode,
                    track.getAttribute("name"),
                    track.getAttribute("kind"),
                    null
            ));
        }
        return tracks;
    }

    private List<CaptionTrack> fetchPlayerCaptionTracks(String videoId) throws Exception {
        String watchBody = get(
                "https://www.youtube.com/watch?v=" + encode(videoId),
                "Mozilla/5.0"
        );
        Matcher apiKeyMatcher = INNERTUBE_API_KEY_PATTERN.matcher(watchBody);
        if (!apiKeyMatcher.find()) {
            return List.of();
        }

        String requestBody = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("videoId", videoId)
                .set("context", objectMapper.createObjectNode()
                        .set("client", objectMapper.createObjectNode()
                                .put("clientName", ANDROID_CLIENT_NAME)
                                .put("clientVersion", ANDROID_CLIENT_VERSION)
                                .put("hl", "en"))));
        String playerBody = postJson(
                "https://www.youtube.com/youtubei/v1/player?key=" + encode(apiKeyMatcher.group(1)),
                requestBody,
                ANDROID_USER_AGENT
        );
        JsonNode captionTracks = objectMapper.readTree(playerBody)
                .path("captions")
                .path("playerCaptionsTracklistRenderer")
                .path("captionTracks");
        if (!captionTracks.isArray()) {
            return List.of();
        }

        List<CaptionTrack> tracks = new ArrayList<>();
        for (JsonNode track : captionTracks) {
            String languageCode = track.path("languageCode").asText("");
            String baseUrl = track.path("baseUrl").asText("");
            if (languageCode.isBlank() || baseUrl.isBlank()) {
                continue;
            }
            tracks.add(new CaptionTrack(
                    videoId,
                    languageCode,
                    track.path("name").path("simpleText").asText(""),
                    track.path("kind").asText(""),
                    baseUrl
            ));
        }
        return tracks;
    }

    private CaptionTrack selectTrack(List<CaptionTrack> tracks) {
        return tracks.stream()
                .min(Comparator
                        .comparing((CaptionTrack track) -> !track.languageCode().toLowerCase().startsWith("en"))
                        .thenComparing(track -> "asr".equalsIgnoreCase(track.kind()))
                        .thenComparing(CaptionTrack::languageCode))
                .orElse(tracks.getFirst());
    }

    private List<TranscriptSegmentResponse> fetchJsonTranscript(CaptionTrack track) throws Exception {
        String body = get(buildTimedTextUrl(track, "json3"), ANDROID_USER_AGENT);
        if (body == null || body.isBlank() || !body.trim().startsWith("{")) {
            return List.of();
        }

        JsonNode events = objectMapper.readTree(body).path("events");
        if (!events.isArray()) {
            return List.of();
        }

        List<TranscriptSegmentResponse> segments = new ArrayList<>();
        for (JsonNode event : events) {
            JsonNode textParts = event.path("segs");
            if (!textParts.isArray()) {
                continue;
            }

            StringBuilder text = new StringBuilder();
            for (JsonNode part : textParts) {
                String token = part.path("utf8").asText("");
                if (!token.isBlank()) {
                    text.append(token);
                }
            }

            String normalizedText = normalizeText(text.toString());
            if (normalizedText.isBlank()) {
                continue;
            }

            double startSeconds = event.path("tStartMs").asDouble(0) / 1000.0;
            double durationSeconds = event.path("dDurationMs").asDouble(0) / 1000.0;
            segments.add(TranscriptSegmentResponse.builder()
                    .startSeconds(roundSeconds(startSeconds))
                    .endSeconds(roundSeconds(startSeconds + Math.max(durationSeconds, 0)))
                    .text(normalizedText)
                    .build());
        }
        return segments;
    }

    private List<TranscriptSegmentResponse> fetchXmlTranscript(CaptionTrack track) throws Exception {
        String body = get(buildTimedTextUrl(track, "srv1"), ANDROID_USER_AGENT);
        if (body == null || body.isBlank()) {
            return List.of();
        }

        Document document = parseXml(body);
        NodeList textNodes = document.getElementsByTagName("text");
        List<TranscriptSegmentResponse> segments = new ArrayList<>();
        for (int index = 0; index < textNodes.getLength(); index++) {
            Element node = (Element) textNodes.item(index);
            double startSeconds = parseDouble(node.getAttribute("start"));
            double durationSeconds = parseDouble(node.getAttribute("dur"));
            String text = normalizeText(node.getTextContent());
            if (text.isBlank()) {
                continue;
            }
            segments.add(TranscriptSegmentResponse.builder()
                    .startSeconds(roundSeconds(startSeconds))
                    .endSeconds(roundSeconds(startSeconds + Math.max(durationSeconds, 0)))
                    .text(text)
                    .build());
        }
        return segments;
    }

    private String buildTimedTextUrl(CaptionTrack track, String format) {
        if (track.baseUrl() != null && !track.baseUrl().isBlank()) {
            String baseUrl = track.baseUrl();
            if (baseUrl.matches(".*[?&]fmt=[^&]*.*")) {
                return baseUrl.replaceFirst("([?&])fmt=[^&]*", "$1fmt=" + encode(format));
            }
            return baseUrl + (baseUrl.contains("?") ? "&" : "?") + "fmt=" + encode(format);
        }

        StringBuilder url = new StringBuilder("https://www.youtube.com/api/timedtext?v=")
                .append(encode(track.videoId()))
                .append("&lang=")
                .append(encode(track.languageCode()));
        if (track.name() != null && !track.name().isBlank()) {
            url.append("&name=").append(encode(track.name()));
        }
        if (format != null && !format.isBlank()) {
            url.append("&fmt=").append(encode(format));
        }
        return url.toString();
    }

    private String get(String url) throws Exception {
        return get(url, "EnglishLab/1.0");
    }

    private String get(String url, String userAgent) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", userAgent)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return "";
        }
        return response.body();
    }

    private String postJson(String url, String body, String userAgent) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return "";
        }
        return response.body();
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private String normalizeText(String value) {
        return value == null ? "" : value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private double roundSeconds(double seconds) {
        return Math.round(seconds * 100.0) / 100.0;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private record CaptionTrack(String videoId, String languageCode, String name, String kind, String baseUrl) {
        private CaptionTrack {
            baseUrl = baseUrl == null ? "" : baseUrl;
        }
    }
}
