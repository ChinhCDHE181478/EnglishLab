package fu.sap490.g23.backend.exception;

import fu.sap490.g23.backend.dto.response.ErrorResponse;
import fu.sap490.g23.backend.service.ai.AiEvaluationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Invalid email or password")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AiEvaluationException.class)
    public ResponseEntity<ErrorResponse> handleAiEvaluationException(AiEvaluationException ex) {
        HttpStatus status = resolveAiStatus(ex);
        ErrorResponse response = ErrorResponse.builder()
                .status(status.value())
                .message(resolveAiMessage(ex, status))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(CourseUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleCourseUnavailableException(CourseUnavailableException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Internal server error")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus resolveAiStatus(AiEvaluationException ex) {
        Integer statusCode = ex.getStatusCode();
        if (statusCode != null) {
            if (statusCode == 404) {
                return HttpStatus.BAD_GATEWAY;
            }
            if (statusCode == 429 || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
        }
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("api key is missing") || message.contains("unsupported ai provider")) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (message.contains("temporarily unavailable")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.BAD_GATEWAY;
    }

    private String resolveAiMessage(AiEvaluationException ex, HttpStatus status) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return "Dịch vụ AI đang tạm thời gián đoạn hoặc quá tải. Hãy thử nộp bài lại sau ít phút.";
        }
        if (message.toLowerCase().contains("model is unavailable or unsupported")) {
            return "Cấu hình model Gemini hiện tại không còn hỗ trợ. Hãy cập nhật model AI trong backend rồi thử lại.";
        }
        if (message.toLowerCase().contains("api key is missing")) {
            return "Backend chưa được cấu hình Gemini API key.";
        }
        return "Không thể chấm bài bằng AI lúc này. Vui lòng kiểm tra cấu hình AI hoặc thử lại sau.";
    }
}
