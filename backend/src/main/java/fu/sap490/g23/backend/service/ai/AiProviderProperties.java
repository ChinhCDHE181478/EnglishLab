package fu.sap490.g23.backend.service.ai;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AiProviderProperties {

    @Value("${englishlab.ai.enabled:true}")
    private boolean enabled;

    @Value("${englishlab.ai.provider:gemini}")
    private String provider;

    @Value("${englishlab.ai.request-timeout-seconds:60}")
    private long requestTimeoutSeconds;

    @Value("${englishlab.ai.temperature:0.2}")
    private double temperature;

    @Value("${englishlab.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${englishlab.ai.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${englishlab.ai.gemini.fallback-models:gemini-2.5-flash-lite}")
    private String geminiFallbackModels;

    @Value("${englishlab.ai.gemini.max-retries:2}")
    private int geminiMaxRetries;

    @Value("${englishlab.ai.gemini.inline-audio-max-bytes:20971520}")
    private long geminiInlineAudioMaxBytes;

    @Value("${englishlab.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${englishlab.ai.openai.model:gpt-4o-mini}")
    private String openAiModel;

    public String normalizedProvider() {
        return provider == null ? "gemini" : provider.trim().toLowerCase();
    }
}
