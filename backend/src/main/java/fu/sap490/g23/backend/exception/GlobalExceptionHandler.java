package fu.sap490.g23.backend.exception;

import fu.sap490.g23.backend.dto.response.ErrorResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomConflictErrorResponse;
import fu.sap490.g23.backend.service.ai.AiEvaluationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ.");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Dữ liệu không hợp lệ.");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên sai định dạng hoặc không thể đọc được.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng.");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, safeMessage(ex, "Không tìm thấy tài khoản."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() == null || ex.getReason().isBlank()
                ? "Yêu cầu không thể được xử lý." : ex.getReason();
        return build(status, message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
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

    @ExceptionHandler(EnrollmentAccessException.class)
    public ResponseEntity<ErrorResponse> handleEnrollmentAccessException(EnrollmentAccessException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .message(ex.getMessage())
                .code(ex.getCode().name())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(CourseUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleCourseUnavailableException(CourseUnavailableException ex) {
        return build(HttpStatus.NOT_FOUND, safeMessage(ex, "Không tìm thấy khóa học."));
    }

    @ExceptionHandler(ClassroomConflictException.class)
    public ResponseEntity<ClassroomConflictErrorResponse> handleClassroomConflictException(ClassroomConflictException ex) {
        ClassroomConflictErrorResponse response = ClassroomConflictErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .conflicts(ex.getConflictResult())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        log.warn("Concurrent update rejected: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Dữ liệu vừa được người khác cập nhật. Vui lòng tải lại và thử lại.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return build(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc không còn hợp lệ. Vui lòng tải lại và kiểm tra.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, safeMessage(ex, "Dữ liệu không hợp lệ."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.warn("Request rejected by business rule", ex);
        return build(HttpStatus.BAD_REQUEST, safeMessage(ex, "Yêu cầu không thể được xử lý."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unhandled server error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống đang gặp lỗi. Vui lòng thử lại sau.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse response = ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(response);
    }

    private String safeMessage(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? fallback : ex.getMessage();
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
