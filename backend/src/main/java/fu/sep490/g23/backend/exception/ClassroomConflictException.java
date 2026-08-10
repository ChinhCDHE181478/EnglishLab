package fu.sap490.g23.backend.exception;

import fu.sap490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import lombok.Getter;

@Getter
public class ClassroomConflictException extends RuntimeException {
    private final ConflictCheckResultResponse conflictResult;

    public ClassroomConflictException(String message, ConflictCheckResultResponse conflictResult) {
        super(message);
        this.conflictResult = conflictResult;
    }
}
