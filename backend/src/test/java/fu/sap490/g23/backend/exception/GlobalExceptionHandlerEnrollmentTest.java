package fu.sap490.g23.backend.exception;

import fu.sap490.g23.backend.dto.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerEnrollmentTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEnrollmentAccessException_returnsForbiddenWithCode() {
        EnrollmentAccessException ex = new EnrollmentAccessException(
                EnrollmentErrorCode.ENROLLMENT_CANCELLED,
                "Bạn đã hủy đăng ký khóa học này. Vui lòng đăng ký lại để tiếp tục."
        );

        ResponseEntity<ErrorResponse> response = handler.handleEnrollmentAccessException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("ENROLLMENT_CANCELLED", response.getBody().getCode());
        assertEquals(ex.getMessage(), response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertTrue(response.getBody().getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void handleResponseStatusException_preservesStatusAndReason() {
        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học.")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Không tìm thấy lớp học.", response.getBody().getMessage());
    }

    @Test
    void handleIllegalArgumentException_usesSafeFallbackForMissingMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException()
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Dữ liệu không hợp lệ.", response.getBody().getMessage());
    }

    @Test
    void handleValidationException_returnsObjectLevelValidationMessage() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new ObjectError("request", "Giờ kết thúc phải sau giờ bắt đầu"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                mock(MethodParameter.class),
                bindingResult
        );

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Giờ kết thúc phải sau giờ bắt đầu", response.getBody().getMessage());
    }
}
