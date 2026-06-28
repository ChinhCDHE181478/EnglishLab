package fu.sap490.g23.backend.exception;

import fu.sap490.g23.backend.dto.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

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
}
