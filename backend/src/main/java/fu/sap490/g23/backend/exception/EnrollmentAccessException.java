package fu.sap490.g23.backend.exception;

import lombok.Getter;

@Getter
public class EnrollmentAccessException extends RuntimeException {

    private final EnrollmentErrorCode code;

    public EnrollmentAccessException(EnrollmentErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
