package fu.sap490.g23.backend.service.ai;

public interface AiEvaluationClient {
    AiEvaluationResult evaluate(String prompt);

    AiEvaluationResult evaluateWithAudio(String prompt, byte[] audioBytes, String mimeType);
}
