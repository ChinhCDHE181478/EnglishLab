package fu.sep490.g23.backend.service.assessment;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PlacementTestSessionTokenTest {
    private static final String SECRET = "placement-test-session-secret-at-least-32-characters";

    @Test
    void tokenIsBoundToStudentAndExamType() {
        PlacementTestSessionToken service = new PlacementTestSessionToken();
        ReflectionTestUtils.setField(service, "secret", SECRET);

        String token = service.issue("learner@example.com", "TOEIC");

        assertThat(service.isValid(token, "learner@example.com", "TOEIC")).isTrue();
        assertThat(service.isValid(token, "another@example.com", "TOEIC")).isFalse();
        assertThat(service.isValid(token, "learner@example.com", "IELTS")).isFalse();
        assertThat(service.isValid("invalid-token", "learner@example.com", "TOEIC")).isFalse();
    }
}
