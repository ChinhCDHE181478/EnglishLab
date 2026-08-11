package fu.sep490.g23.backend.service.ai.impl;
import fu.sep490.g23.backend.service.ai.AiEvaluationException;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.OpenAiEvaluationClient;
import fu.sep490.g23.backend.service.ai.GeminiAiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.ai.AiProviderProperties;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiEvaluationClientImpl implements AiEvaluationClient {

    private final AiProviderProperties properties;
    private final GeminiAiEvaluationClient geminiAiEvaluationClient;
    private final OpenAiEvaluationClient openAiEvaluationClient;

    @Override
    public AiEvaluationResult evaluate(String prompt) {
        if (!properties.isEnabled()) {
            throw new AiEvaluationException("AI is disabled. Set ENGLISHLAB_AI_ENABLED=true before submitting assessments.");
        }

        return switch (properties.normalizedProvider()) {
            case "gemini", "google" -> geminiAiEvaluationClient.evaluate(prompt);
            case "openai", "chatgpt" -> openAiEvaluationClient.evaluate(prompt);
            default -> throw new AiEvaluationException("Unsupported AI provider: " + properties.getProvider());
        };
    }

    @Override
    public AiEvaluationResult evaluateWithAudio(String prompt, byte[] audioBytes, String mimeType) {
        if (!properties.isEnabled()) {
            throw new AiEvaluationException("AI is disabled. Set ENGLISHLAB_AI_ENABLED=true before submitting assessments.");
        }

        return switch (properties.normalizedProvider()) {
            case "gemini", "google" -> geminiAiEvaluationClient.evaluateWithAudio(prompt, audioBytes, mimeType);
            case "openai", "chatgpt" -> throw new AiEvaluationException("Audio-native speaking evaluation is not implemented for OpenAI in this backend yet. Use ENGLISHLAB_AI_PROVIDER=gemini for speaking audio assessment.");
            default -> throw new AiEvaluationException("Unsupported AI provider: " + properties.getProvider());
        };
    }
}
