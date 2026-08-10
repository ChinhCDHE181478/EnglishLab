package fu.sap490.g23.backend.service.course.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.service.course.DictionaryTranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DictionaryTranslationServiceImpl implements DictionaryTranslationService {

    private static final int MAX_CACHE_ENTRIES = 500;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient.Builder restClientBuilder;
    private final Map<String, String> translationCache = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            }
    );

    @Value("${englishlab.dictionary.translation-base-url:https://api.mymemory.translated.net}")
    private String translationBaseUrl;

    @Override
    public String translateWord(String word) {
        String cacheKey = word.toLowerCase(Locale.ROOT);
        String cached = translationCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String responseBody = restClientBuilder.clone()
                .baseUrl(translationBaseUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/get")
                        .queryParam("q", word)
                        .queryParam("langpair", "en|vi")
                        .build())
                .retrieve()
                .body(String.class);
        String translation = parseTranslation(responseBody);
        translationCache.put(cacheKey, translation);
        return translation;
    }

    private String parseTranslation(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Dịch vụ dịch không trả về dữ liệu.");
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            if (root.path("quotaFinished").asBoolean(false) || root.path("responseStatus").asInt() != 200) {
                throw new IllegalStateException("Dịch vụ dịch miễn phí đã hết hạn mức hoặc không phản hồi.");
            }
            String translatedText = root.path("responseData").path("translatedText").asText("").trim();
            if (translatedText.isBlank()
                    || translatedText.toLowerCase(Locale.ROOT).startsWith("mymemory warning:")) {
                throw new IllegalStateException("Dịch vụ dịch không trả về nghĩa tiếng Việt phù hợp.");
            }
            return translatedText;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Dịch vụ dịch trả về dữ liệu không hợp lệ.", exception);
        }
    }
}
