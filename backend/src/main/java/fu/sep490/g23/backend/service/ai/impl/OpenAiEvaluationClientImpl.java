package fu.sep490.g23.backend.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import fu.sep490.g23.backend.service.ai.AiEvaluationException;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.ai.AiProviderProperties;
import fu.sep490.g23.backend.service.ai.OpenAiEvaluationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiEvaluationClientImpl implements OpenAiEvaluationClient {

    private final AiProviderProperties properties;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public AiEvaluationResult evaluate(String prompt) {
        String apiKey = blankToNull(properties.getOpenAiApiKey());
        if (apiKey == null) {
            throw new AiEvaluationException("AI API key is missing for OpenAI. Set OPENAI_API_KEY before submitting assessments.");
        }

        String model = properties.getOpenAiModel();
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", properties.getTemperature(),
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are EnglishLab AI Evaluator. Return valid JSON only."),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String responseBody = sendJson("https://api.openai.com/v1/chat/completions", objectMapper.writeValueAsString(body), apiKey);
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new AiEvaluationException("OpenAI returned an empty evaluation response.");
            }

            String feedbackJson = normalizeJsonResponse(contentNode.asText());
            return AiEvaluationResult.builder()
                    .estimatedScore(extractEstimatedScore(feedbackJson))
                    .feedbackJson(feedbackJson)
                    .provider("OPENAI")
                    .model(model)
                    .rawResponse(responseBody)
                    .build();
        } catch (AiEvaluationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiEvaluationException("OpenAI evaluation failed: " + exception.getMessage(), exception);
        }
    }

    private String sendJson(String endpoint, String requestBody, String apiKey) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiEvaluationException("OpenAI returned HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
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
            throw new AiEvaluationException("OpenAI response is not valid JSON.", exception);
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
}
