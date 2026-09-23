package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fu.sep490.g23.backend.service.course.event.CourseThumbnailReplacedEvent;
import fu.sep490.g23.backend.service.payment.PaymentService;
import fu.sep490.g23.backend.service.payment.PayosProperties;
import fu.sep490.g23.backend.service.schedule_job.LearningReminderService;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import fu.sep490.g23.backend.service.user.event.AvatarUpdatedEvent;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static fu.sep490.g23.backend.it.ItSupport.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import vn.payos.PayOS;

/** Canonical workbook scenarios for Placement Test. */
@EnglishLabIT
@RecordApplicationEvents
@Tag("workbook")
public class WorkbookPlacementIT extends WorkbookTestSupport {

    private String placementDefinitionSnapshot() {
        return str("""
                select coalesce(jsonb_agg(to_jsonb(item) order by id)::text, '[]')
                from (select * from content_bank_items where bank_type='PLACEMENT_TEST') item
                """);
    }

    /**
     * Workbook: Placement Test — Update Placement Test Settings
     * Preconditions:
     * LEARNER account exists; an active placement test definition exists.
     * Procedure:
     * 1. Login as LEARNER and call PUT /api/content-manager/placement-test with a changed title.
     * 2. Query the placement test definition.
     * Expected results:
     * The request is rejected with 403.
     * The definition title is unchanged.
     */
    @Test
    @DisplayName("IT_PLACEMENT_05 — Verify a learner cannot update placement test settings.")
    void IT_PLACEMENT_05() throws Exception {
        String learner = freshLearner();
        String before = placementDefinitionSnapshot();
        status(call("PUT", "/api/content-manager/placement-test", learner,
                body("title", "Hacked title", "listeningConfigJson", "{}", "readingConfigJson", "{}",
                        "writingConfigJson", "{}", "speakingConfigJson", "{}")), 403);
        assertEquals(before, placementDefinitionSnapshot());
    }

    /**
     * SRS 2.5.2 View Placement Test Settings.
     * Preconditions:
     * CM account exists and an active placement-test definition has been initialized.
     * Procedure:
     * 1. Login as CM and call GET /api/content-manager/placement-test.
     * 2. Inspect the returned exam metadata and section configuration.
     * 3. Compare the placement-test definition before and after the read.
     * Expected results:
     * The current settings and section configuration are returned to CM.
     * The read operation does not modify placement-test data.
     */
    @Test
    @DisplayName("IT_PLACEMENT_06 — Verify Content Manager can view placement test settings without modifying them.")
    void IT_PLACEMENT_06() throws Exception {
        String before = placementDefinitionSnapshot();
        JsonNode settings = ok("GET", "/api/content-manager/placement-test", CM, null);
        assertFalse(settings.path("testCode").asText().isBlank());
        assertFalse(settings.path("title").asText().isBlank());
        assertTrue(settings.has("listeningConfigJson"));
        assertTrue(settings.has("readingConfigJson"));
        assertTrue(settings.has("writingConfigJson"));
        assertTrue(settings.has("speakingConfigJson"));
        assertEquals(before, placementDefinitionSnapshot());
    }

    // ─── SRS 2.5.1 Take Placement Test ────────────────────────────────────────

    /**
     * SRS 2.5.1 Take Placement Test.
     * Preconditions:
     * Learner account exists; active placement test definition exists.
     * Procedure:
     * 1. Login as a fresh learner.
     * 2. Call GET /api/student/placement-tests/current.
     * 3. Inspect the returned exam metadata and section content.
     * Expected results:
     * The placement paper is loaded with a non-blank testCode and a sections map.
     * No placement attempt is created by the read-only operation.
     */
    @Test
    @DisplayName("IT_PLACEMENT_07 — Verify a learner can load the current placement test paper.")
    void IT_PLACEMENT_07() throws Exception {
        String learner = freshLearner();
        long beforeCount = n("select count(*) from placement_test_attempts where student_id=?", uid(learner));
        JsonNode paper = ok("GET", "/api/student/placement-tests/current", learner, null);
        assertFalse(paper.path("testCode").asText().isBlank());
        assertTrue(paper.has("sections"));
        assertEquals(beforeCount, n("select count(*) from placement_test_attempts where student_id=?", uid(learner)));
    }

    /**
     * SRS 2.5.1 Take Placement Test.
     * Preconditions:
     * Learner account exists; active placement test definition exists.
     * Procedure:
     * 1. Login as a fresh learner.
     * 2. POST /api/student/placement-tests/current/submit with valid TOEIC answers.
     * 3. Inspect the returned attempt response and database record.
     * Expected results:
     * The submission returns HTTP 200 with status COMPLETED.
     * Exactly one new attempt is created in the database for this learner.
     */
    @Test
    @DisplayName("IT_PLACEMENT_08 — Verify a learner can submit a TOEIC placement test and receive a COMPLETED result.")
    void IT_PLACEMENT_08() throws Exception {
        String learner = freshLearner();
        long beforeCount = n("select count(*) from placement_test_attempts where student_id=?", uid(learner));
        JsonNode result = ok("POST", "/api/student/placement-tests/current/submit", learner,
                body("examType", "TOEIC", "listeningAnswers", Map.of(), "readingAnswers", Map.of(),
                        "deviceCheck", Map.of("completed", true)));
        assertEquals("COMPLETED", result.path("status").asText());
        assertTrue(result.path("id").asLong() > 0);
        assertEquals(beforeCount + 1, n("select count(*) from placement_test_attempts where student_id=?", uid(learner)));
    }

    /**
     * SRS 2.5.1 Take Placement Test.
     * Preconditions:
     * Learner has already submitted a placement test.
     * Procedure:
     * 1. Login as a fresh learner and submit a TOEIC placement test.
     * 2. Call GET /api/student/placement-tests/current.
     * 3. Inspect the latestAttempt in the response.
     * Expected results:
     * The latestAttempt.id matches the submitted attempt.
     * The latestAttempt includes an overallScore field.
     */
    @Test
    @DisplayName("IT_PLACEMENT_09 — Verify the latest placement result is retrievable after submission.")
    void IT_PLACEMENT_09() throws Exception {
        String learner = freshLearner();
        JsonNode submitted = ok("POST", "/api/student/placement-tests/current/submit", learner,
                body("examType", "TOEIC", "listeningAnswers", Map.of(), "readingAnswers", Map.of(),
                        "deviceCheck", Map.of("completed", true)));
        long attemptId = submitted.path("id").asLong();
        JsonNode paper = ok("GET", "/api/student/placement-tests/current", learner, null);
        assertEquals(attemptId, paper.path("latestAttempt").path("id").asLong());
        assertTrue(paper.path("latestAttempt").has("overallScore"));
    }

    /**
     * SRS 2.5.1 Take Placement Test.
     * Preconditions:
     * No authentication token is provided.
     * Procedure:
     * 1. Call GET /api/student/placement-tests/current without an Authorization header.
     * 2. Inspect the HTTP status.
     * Expected results:
     * The request is rejected with 401 or 403.
     * No placement attempt is created.
     */
    @Test
    @DisplayName("IT_PLACEMENT_10 — Verify an unauthenticated user cannot access the placement test.")
    void IT_PLACEMENT_10() throws Exception {
        long beforeCount = n("select count(*) from placement_test_attempts");
        MvcResult result = call("GET", "/api/student/placement-tests/current", null, null);
        assertTrue(result.getResponse().getStatus() == 401 || result.getResponse().getStatus() == 403,
                "Expected 401 or 403 but got " + result.getResponse().getStatus());
        assertEquals(beforeCount, n("select count(*) from placement_test_attempts"));
    }

    /**
     * SRS 2.5.1 Take Placement Test — Learning Recommendations.
     * Preconditions:
     * Learner has a completed placement test attempt.
     * Procedure:
     * 1. Login as a fresh learner and submit a TOEIC placement test.
     * 2. Call GET /api/student/placement-tests/{attemptId}/recommendations.
     * 3. Inspect the returned recommendation data.
     * Expected results:
     * The response contains the attemptId and recommendation-readiness data.
     * The response includes a recommendedLevel or a message explaining the status.
     */
    @Test
    @DisplayName("IT_PLACEMENT_11 — Verify a learner receives recommendations after completing placement test.")
    void IT_PLACEMENT_11() throws Exception {
        String learner = freshLearner();
        JsonNode submitted = ok("POST", "/api/student/placement-tests/current/submit", learner,
                body("examType", "TOEIC", "listeningAnswers", Map.of(), "readingAnswers", Map.of(),
                        "deviceCheck", Map.of("completed", true)));
        long attemptId = submitted.path("id").asLong();
        JsonNode recommendations = ok("GET", "/api/student/placement-tests/" + attemptId + "/recommendations", learner, null);
        assertEquals(attemptId, recommendations.path("attemptId").asLong());
        assertTrue(recommendations.has("recommendationReady"));
        assertTrue(recommendations.has("recommendedLevel") || recommendations.has("message"));
    }

    // ─── SRS 2.5.3 Update Placement Test Settings ─────────────────────────────

    /**
     * SRS 2.5.3 Update Placement Test Settings.
     * Preconditions:
     * CM account exists; active placement test definition exists.
     * Procedure:
     * 1. Login as CM and read current placement test settings.
     * 2. PUT /api/content-manager/placement-test with a modified title.
     * 3. Read the settings again to verify the change persisted.
     * Expected results:
     * The update returns HTTP 200 with the updated title.
     * A subsequent read confirms the title was persisted.
     */
    @Test
    @DisplayName("IT_PLACEMENT_12 — Verify Content Manager can update placement test settings.")
    void IT_PLACEMENT_12() throws Exception {
        JsonNode original = ok("GET", "/api/content-manager/placement-test", CM, null);
        String originalTitle = original.path("title").asText();
        String newTitle = "Round1 Updated Placement Title " + UUID.randomUUID().toString().substring(0, 8);
        JsonNode updated = ok("PUT", "/api/content-manager/placement-test", CM,
                body("title", newTitle,
                        "listeningConfigJson", original.path("listeningConfigJson").asText(),
                        "readingConfigJson", original.path("readingConfigJson").asText(),
                        "writingConfigJson", original.path("writingConfigJson").asText(),
                        "speakingConfigJson", original.path("speakingConfigJson").asText()));
        assertEquals(newTitle, updated.path("title").asText());
        JsonNode afterRead = ok("GET", "/api/content-manager/placement-test", CM, null);
        assertEquals(newTitle, afterRead.path("title").asText());
        // Restore original title so other tests are not affected.
        ok("PUT", "/api/content-manager/placement-test", CM,
                body("title", originalTitle,
                        "listeningConfigJson", original.path("listeningConfigJson").asText(),
                        "readingConfigJson", original.path("readingConfigJson").asText(),
                        "writingConfigJson", original.path("writingConfigJson").asText(),
                        "speakingConfigJson", original.path("speakingConfigJson").asText()));
    }

    /**
     * SRS 2.5.3 Update Placement Test Settings — Monitoring.
     * Preconditions:
     * CM account exists.
     * Procedure:
     * 1. Login as CM.
     * 2. Call GET /api/content-manager/placement-test/monitoring?examType=IELTS.
     * 3. Inspect the returned monitoring data structure.
     * Expected results:
     * The response contains examType, totalAttempts, and bandDistribution fields.
     * The monitoring read does not alter any placement test data.
     */
    @Test
    @DisplayName("IT_PLACEMENT_13 — Verify Content Manager can view placement test monitoring statistics.")
    void IT_PLACEMENT_13() throws Exception {
        String before = placementDefinitionSnapshot();
        JsonNode monitoring = ok("GET", "/api/content-manager/placement-test/monitoring?examType=IELTS", CM, null);
        assertEquals("IELTS", monitoring.path("examType").asText());
        assertTrue(monitoring.has("totalAttempts"));
        assertTrue(monitoring.has("bandDistribution"));
        assertTrue(monitoring.path("totalAttempts").isNumber());
        assertEquals(before, placementDefinitionSnapshot());
    }
}
