package fu.sep490.g23.backend.service.ai;

public class AiEvaluationException extends RuntimeException {
    private final Integer statusCode;

    public AiEvaluationException(String message) {
        super(message);
        this.statusCode = null;
    }

    public AiEvaluationException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public AiEvaluationException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AiEvaluationException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
