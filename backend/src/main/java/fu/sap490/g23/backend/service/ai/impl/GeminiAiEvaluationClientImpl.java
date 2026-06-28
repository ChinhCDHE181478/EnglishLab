package fu.sap490.g23.backend.service.ai.impl;

import fu.sap490.g23.backend.service.ai.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GeminiAiEvaluationClientImpl implements GeminiAiEvaluationClient {
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 500, 502, 503, 504);
    private static final List<String> DEFAULT_FALLBACK_MODELS = List.of(
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite"
    );

    private final AiProviderProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiEvaluationResult evaluate(String prompt) {
        return evaluateInternal(prompt, null, null);
    }

    public AiEvaluationResult evaluateWithAudio(String prompt, byte[] audioBytes, String mimeType) {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new AiEvaluationException("Speaking audio file is empty.");
        }
        if (audioBytes.length > properties.getGeminiInlineAudioMaxBytes()) {
            throw new AiEvaluationException("Speaking audio file is too large for inline Gemini analysis. Limit: " + properties.getGeminiInlineAudioMaxBytes() + " bytes.");
        }
        String normalizedMimeType = normalizeAudioMimeType(mimeType);
        if (normalizedMimeType == null) {
            throw new AiEvaluationException("Speaking audio MIME type is not supported: " + mimeType);
        }
        return evaluateInternal(prompt, audioBytes, normalizedMimeType);
    }

    private AiEvaluationResult evaluateInternal(String prompt, byte[] audioBytes, String mimeType) {
        String apiKey = blankToNull(properties.getGeminiApiKey());
        if (apiKey == null) {
            throw new AiEvaluationException("AI API key is missing for Gemini. Set GEMINI_API_KEY before submitting assessments.");
        }

        List<String> candidateModels = resolveCandidateModels();
        String requestBody = buildRequestBody(prompt, audioBytes, mimeType);
        boolean audioInputAnalyzed = audioBytes != null && audioBytes.length > 0;
        AiEvaluationException lastException = null;

        for (String model : candidateModels) {
            for (int attempt = 1; attempt <= properties.getGeminiMaxRetries() + 1; attempt += 1) {
                try {
                    String endpoint = buildEndpoint(model, apiKey);
                    String responseBody = sendJson(endpoint, requestBody);
                    JsonNode root = objectMapper.readTree(responseBody);
                    JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                    if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                        throw new AiEvaluationException("Gemini returned an empty evaluation response.");
                    }

                    String feedbackJson = normalizeJsonResponse(textNode.asText());
                    return AiEvaluationResult.builder()
                            .estimatedScore(extractEstimatedScore(feedbackJson))
                            .feedbackJson(feedbackJson)
                            .provider("GEMINI")
                            .model(model)
                            .rawResponse(responseBody)
                            .audioInputAnalyzed(audioInputAnalyzed)
                            .build();
                } catch (AiEvaluationException exception) {
                    lastException = exception;

                    if (shouldTryNextModel(exception) && hasMoreModels(candidateModels, model)) {
                        break;
                    }
                    if (isRetryable(exception) && attempt <= properties.getGeminiMaxRetries()) {
                        sleepBeforeRetry(attempt);
                        continue;
                    }
                    if (isRetryable(exception) && hasMoreModels(candidateModels, model)) {
                        break;
                    }
                    throw buildFriendlyFailure(model, candidateModels, exception);
                } catch (Exception exception) {
                    throw new AiEvaluationException("Gemini evaluation failed: " + exception.getMessage(), exception);
                }
            }
        }

        if (lastException != null) {
            throw buildFriendlyFailure(candidateModels.get(candidateModels.size() - 1), candidateModels, lastException);
        }
        throw new AiEvaluationException("Gemini evaluation failed before any request could be completed.");
    }

    private String buildRequestBody(String prompt, byte[] audioBytes, String mimeType) {
        try {
            List<Map<String, Object>> parts = new ArrayList<>();
            parts.add(Map.of("text", prompt));
            if (audioBytes != null && audioBytes.length > 0) {
                parts.add(Map.of(
                        "inlineData", Map.of(
                                "mimeType", mimeType,
                                "data", Base64.getEncoder().encodeToString(audioBytes)
                        )
                ));
            }

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "role", "user",
                            "parts", parts
                    )),
                    "generationConfig", Map.of(
                            "temperature", properties.getTemperature(),
                            "responseMimeType", "application/json"
                    )
            );
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new AiEvaluationException("Gemini request body could not be created.", exception);
        }
    }

    private String buildEndpoint(String model, String apiKey) {
        String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://generativelanguage.googleapis.com/v1beta/models/" + encodedModel + ":generateContent?key=" + apiKey;
    }

    private String sendJson(String endpoint, String requestBody) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiEvaluationException("Gemini returned HTTP " + response.statusCode() + ": " + response.body(), response.statusCode());
        }
        return response.body();
    }

    private List<String> resolveCandidateModels() {
        Set<String> models = new LinkedHashSet<>();
        String primaryModel = blankToNull(properties.getGeminiModel());
        if (primaryModel != null) {
            models.add(primaryModel);
        }

        String fallbackModels = blankToNull(properties.getGeminiFallbackModels());
        if (fallbackModels != null) {
            for (String model : fallbackModels.split(",")) {
                String normalized = blankToNull(model);
                if (normalized != null) {
                    models.add(normalized);
                }
            }
        }

        DEFAULT_FALLBACK_MODELS.stream()
                .map(this::blankToNull)
                .filter(model -> model != null && !model.equalsIgnoreCase(primaryModel))
                .forEach(models::add);

        if (models.isEmpty()) {
            models.addAll(DEFAULT_FALLBACK_MODELS);
        }
        return new ArrayList<>(models);
    }

    private boolean hasMoreModels(List<String> candidateModels, String currentModel) {
        return candidateModels.indexOf(currentModel) < candidateModels.size() - 1;
    }

    private boolean shouldTryNextModel(AiEvaluationException exception) {
        Integer statusCode = exception.getStatusCode();
        if (statusCode == null) {
            return false;
        }
        if (statusCode == 404) {
            return true;
        }
        String message = exception.getMessage();
        return message != null
                && message.toLowerCase().contains("not found")
                && message.toLowerCase().contains("models/");
    }

    private boolean isRetryable(AiEvaluationException exception) {
        Integer statusCode = exception.getStatusCode();
        return statusCode != null && RETRYABLE_STATUS_CODES.contains(statusCode);
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            long delayMillis = Math.min(3000L, 400L * attempt);
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new AiEvaluationException("Gemini retry was interrupted.", interruptedException);
        }
    }

    private AiEvaluationException buildFriendlyFailure(String attemptedModel, List<String> candidateModels, AiEvaluationException exception) {
        Integer statusCode = exception.getStatusCode();
        if (statusCode != null && RETRYABLE_STATUS_CODES.contains(statusCode)) {
            return new AiEvaluationException(
                    "Gemini is temporarily unavailable after retrying model(s): " + String.join(", ", candidateModels) + ".",
                    statusCode,
                    exception
            );
        }
        if (shouldTryNextModel(exception) || (statusCode != null && statusCode == 404)) {
            return new AiEvaluationException(
                    "Gemini model is unavailable or unsupported. Checked model(s): " + String.join(", ", candidateModels) + ".",
                    statusCode,
                    exception
            );
        }
        return new AiEvaluationException(
                "Gemini evaluation failed for model " + attemptedModel + ": " + exception.getMessage(),
                statusCode,
                exception
        );
    }

    private String normalizeJsonResponse(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            trimmed = trimmed.substring(firstBrace, lastBrace + 1);
        }
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new AiEvaluationException("Gemini response is not valid JSON.", exception);
        }
    }

    private BigDecimal extractEstimatedScore(String feedbackJson) {
        try {
            JsonNode root = objectMapper.readTree(feedbackJson);
            JsonNode value = root.path("estimatedScore");
            if (value.isNumber() || value.isTextual()) {
                String text = value.asText().replaceAll("[^0-9.]", "");
                if (!text.isBlank()) {
                    return new BigDecimal(text).setScale(1, RoundingMode.HALF_UP);
                }
            }
        } catch (Exception ignored) {
            // JSON validity is checked in normalizeJsonResponse.
        }
        return null;
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeAudioMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return null;
        }
        String normalized = mimeType.toLowerCase();
        if (normalized.equals("video/webm")) {
            return "audio/webm";
        }
        if (normalized.equals("video/mp4")) {
            return "audio/mp4";
        }
        return normalized.startsWith("audio/") ? normalized : null;
    }
}
