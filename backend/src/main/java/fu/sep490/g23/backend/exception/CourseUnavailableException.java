package fu.sep490.g23.backend.exception;

public class CourseUnavailableException extends RuntimeException {

    public CourseUnavailableException(String message) {
        super(message);
    }
}
