package fu.sap490.g23.backend.service.ai;

public interface OpenAiEvaluationClient {

    AiEvaluationResult evaluate(String prompt);
}
