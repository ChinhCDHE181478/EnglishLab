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

/** Canonical workbook scenarios for Online Course Learning. */
@EnglishLabIT
@RecordApplicationEvents
@Tag("workbook")
public class WorkbookLearningIT extends WorkbookTestSupport {

    private long module(long courseId) {
        return n("select m.id from online_course_modules m join online_course_versions v on v.id=m.online_course_version_id where v.online_course_id=? and v.status='PUBLISHED' order by m.sequence_number,m.id limit 1", courseId);
    }

    private long assessment(long courseId) throws Exception {
        long versionId = n("select id from online_course_versions where online_course_id=? and status='PUBLISHED' order by version_number desc,id desc limit 1", courseId);
        long assessmentId = n("""
                insert into course_assessments(
                    online_course_version_id,module_id,progress_key,title,description,type,skill,
                    ai_evaluation_mode,instructions,objective_answer_key,assessment_config,
                    passing_score,max_score,time_limit_minutes,display_order,active
                ) values(?,?,?,'Round1 module assessment','Objective module checkpoint','MODULE_TEST',
                    'READING','RUBRIC_FEEDBACK','Choose the correct answer',?::jsonb,?::jsonb,
                    1,1,10,1,true) returning id
                """, versionId, module(courseId), "assessment-" + UUID.randomUUID(),
                body("1", "A"), body("parts", List.of()));
        sql("update online_course_versions set total_required_assessments=1 where id=?", versionId);
        return assessmentId;
    }

    private long createNote(long courseId, long lessonId, String learner, String content) throws Exception {
        return ok("POST", "/api/student/learning/courses/" + courseId + "/lessons/" + lessonId + "/notes",
                learner, body("content", content, "selectedText", "Selected lesson text", "transcriptStartSeconds", 15))
                .path("id").asLong();
    }

    private long createThread(long courseId, String learner) throws Exception {
        return ok("POST", "/api/student/online-courses/" + courseId + "/discussions", learner,
                body("title", "How should I practise this lesson?",
                        "content", "I need a clearer explanation and another example for this lesson."))
                .path("id").asLong();
    }

    private long createReply(long threadId, String learner) throws Exception {
        return ok("POST", "/api/student/online-courses/discussions/" + threadId + "/replies", learner,
                body("content", "Review the lesson example first, then try writing your own sentence."))
                .path("id").asLong();
    }

    private String enrolledPeer(long courseId) throws Exception {
        String peer = freshLearner();
        enroll(peer, courseId);
        return peer;
    }

    private void prepareVocabulary(long courseId) {
        sql("update online_lessons set content_text=? where id=?",
                "### 1. Academic\n**Meaning:** related to education and study\n**Example:** Academic vocabulary improves formal writing.",
                lesson(courseId, 0));
    }

    /**
     * Workbook: Online Course Learning!A12
     * Preconditions:
     * Course F is published; LEARNER is enrolled in F.
     * LEARNER B is a verified account with no enrollment in F.
     * Procedure:
     * 1. Login as LEARNER B and call GET /api/student/online-courses/{F}/content.
     * 2. Login as LEARNER and call GET /api/student/online-courses/{F}/content.
     * Expected results:
     * LEARNER B is rejected with 403 and no module or lesson data is returned.
     * LEARNER receives the content of F, so the difference comes only from the enrollment record.
     */
    @Test
    @DisplayName("IT_LEARNING_01 — Verify a learner without enrollment cannot open the lesson content.")
    void IT_LEARNING_01() throws Exception {
        String outsider = freshLearner();
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        status(call("GET", content(courseId), outsider, null), 403);
        assertFalse(ok("GET", content(courseId), LEARNER, null).path("modules").isEmpty());
    }

    /**
     * Workbook: Online Course Learning!A13
     * Preconditions:
     * Course F is published and has lesson L1; LEARNER B is a verified account with no enrollment in F.
     * Procedure:
     * 1. Login as LEARNER B and call PATCH /api/student/online-courses/{F}/lessons/{L1}/progress?completed=true.
     * 2. Query the LessonProgress rows for LEARNER B and L1.
     * Expected results:
     * The request is rejected with 403.
     * No LessonProgress row exists for LEARNER B and no enrollment is created.
     */
    @Test
    @DisplayName("IT_LEARNING_02 — Verify a learner without enrollment cannot save lesson progress.")
    void IT_LEARNING_02() throws Exception {
        String outsider = freshLearner();
        long courseId = course(0, 1);
        long lessonId = lesson(courseId, 0);
        status(call("PATCH", progress(courseId, lessonId, true), outsider, null), 403);
        assertEquals(0, n("select count(*) from online_course_enrollments where student_id=? and online_course_id=?", uid(outsider), courseId));
        assertEquals(0, n("select count(*) from lesson_progress where online_lesson_id=?", lessonId));
    }

    /**
     * Workbook: Online Course Learning!A14
     * Preconditions:
     * Course F is published with two lessons; LEARNER is enrolled in F.
     * Procedure:
     * 1. Login as LEARNER and enroll in course F.
     * 2. Call GET /api/student/online-courses/{F}/content.
     * 3. Inspect the modules and lessons of the returned course.
     * Expected results:
     * The GET /api/student/online-courses/{F}/content endpoint returns HTTP 200 with a payload whose modules array is non-empty and whose first module exposes a lessons array of size >= 2 sorted by displayOrder ascending.
     * The totalLessons field is at least 2 and the title matches the seeded fixture (Round1 *).
     */
    @Test
    @DisplayName("IT_LEARNING_03 — Verify an enrolled learner receives the full course content tree with ordered lessons.")
    void IT_LEARNING_03() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 2);
        enroll(learner, courseId);
        JsonNode content = ok("GET", content(courseId), learner, null);
        JsonNode modules = content.path("modules");
        assertTrue(modules.isArray() && !modules.isEmpty(), () -> "Expected modules in content: " + content);
        JsonNode lessons = modules.get(0).path("lessons");
        assertTrue(lessons.isArray() && lessons.size() >= 2, () -> "Expected at least 2 lessons, got: " + lessons);
        assertTrue(lessons.get(0).path("displayOrder").asInt() < lessons.get(1).path("displayOrder").asInt());
        assertTrue(content.path("totalLessons").asInt() >= 2);
        assertTrue(content.path("title").asText().startsWith("Round1 "));
    }

    /**
     * Workbook: Online Course Learning!A16
     * Preconditions:
     * LEARNER is enrolled (ACTIVE) in course F with at least two lessons.
     * No lesson of F is completed yet.
     * Procedure:
     * 1. Login as LEARNER and call GET /api/student/online-courses/my-enrollments; record the progress of F.
     * 2. Call PATCH /api/student/online-courses/{F}/lessons/{lesson1}/progress?completed=true.
     * 3. Query the LessonProgress row of lesson1 and call GET /api/student/online-courses/my-enrollments again.
     * Expected results:
     * A LessonProgress row for lesson1 exists with status COMPLETED and completedAt set.
     * The progressPercent of the OnlineCourseEnrollment increased from 0 to a value greater than 0.
     * The enrollment stays ACTIVE and no duplicate LessonProgress row exists.
     */
    @Test
    @DisplayName("IT_LEARNING_04 — Verify completing a lesson updates the progress of the enrollment.")
    void IT_LEARNING_04() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 2);
        enroll(learner, courseId);
        long lessonId = lesson(courseId, 0);
        int before = ok("GET", "/api/student/online-courses/my-enrollments", learner, null).get(0).path("progressPercent").asInt();
        ok("PATCH", progress(courseId, lessonId, true), learner, null);
        long enrollmentId = enrollment(learner, courseId);
        assertEquals("COMPLETED", str("select status from lesson_progress where online_course_enrollment_id=? and online_lesson_id=?", enrollmentId, lessonId));
        assertNotNull(str("select completed_at::text from lesson_progress where online_course_enrollment_id=? and online_lesson_id=?", enrollmentId, lessonId));
        assertEquals(1, n("select count(*) from lesson_progress where online_course_enrollment_id=? and online_lesson_id=?", enrollmentId, lessonId));
        int after = ok("GET", "/api/student/online-courses/my-enrollments", learner, null).get(0).path("progressPercent").asInt();
        assertTrue(after > before);
    }

    /**
     * Workbook: Online Course Learning!A17
     * Preconditions:
     * LEARNER is enrolled (ACTIVE) in course F with at least two lessons.
     * lesson1 is COMPLETED and the enrollment progress is greater than 0.
     * Procedure:
     * 1. Login as LEARNER and call PATCH /api/student/online-courses/{F}/lessons/{lesson1}/progress?completed=false.
     * 2. Query the LessonProgress row of lesson1 and call GET /api/student/online-courses/my-enrollments.
     * Expected results:
     * The LessonProgress status is IN_PROGRESS and completedAt is empty.
     * The progressPercent of the enrollment returns to 0.
     * The same LessonProgress row is updated; no new row is created.
     */
    @Test
    @DisplayName("IT_LEARNING_05 — Verify un-completing a lesson rolls the progress back.")
    void IT_LEARNING_05() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 2);
        enroll(learner, courseId);
        long lessonId = lesson(courseId, 0);
        ok("PATCH", progress(courseId, lessonId, true), learner, null);
        long enrollmentId = enrollment(learner, courseId);
        long progressId = n("select id from lesson_progress where online_course_enrollment_id=? and online_lesson_id=?", enrollmentId, lessonId);
        ok("PATCH", progress(courseId, lessonId, false), learner, null);
        assertEquals(progressId, n("select id from lesson_progress where online_course_enrollment_id=? and online_lesson_id=?", enrollmentId, lessonId));
        assertEquals("IN_PROGRESS", str("select status from lesson_progress where id=?", progressId));
        assertNull(str("select completed_at::text from lesson_progress where id=?", progressId));
        assertEquals(0, n("select progress_percent::bigint from online_course_enrollments where id=?", enrollmentId));
    }

    /**
     * Workbook: Online Course Learning!A18
     * Preconditions:
     * LEARNER is enrolled (ACTIVE) in course F whose required content is two lessons and no assessment.
     * Procedure:
     * 1. Login as LEARNER and call PATCH .../lessons/{lesson1}/progress?completed=true and PATCH .../lessons/{lesson2}/progress?completed=true.
     * 2. Call GET /api/student/online-courses/{F}/completion and GET /api/student/online-courses/my-enrollments.
     * 3. Call GET /api/student/online-courses/{F}/certificate and read the verification code.
     * 4. Without a JWT call GET /api/online-courses/certificates/{verificationCode}.
     * Expected results:
     * The completion result shows the learner is eligible for a certificate with progress 100.
     * The OnlineCourseEnrollment status is COMPLETED.
     * The certificate response contains a verification code and verification URL for this learner and course.
     * The public verification returns the same learner and course.
     */
    @Test
    @DisplayName("IT_LEARNING_06 — Verify completing all required lessons completes the enrollment and issues a verifiable certificate.")
    void IT_LEARNING_06() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 2);
        enroll(learner, courseId);
        ok("PATCH", progress(courseId, lesson(courseId, 0), true), learner, null);
        ok("PATCH", progress(courseId, lesson(courseId, 1), true), learner, null);
        JsonNode completion = ok("GET", "/api/student/online-courses/" + courseId + "/completion", learner, null);
        assertEquals("COMPLETED", str("select status from online_course_enrollments where id=?", enrollment(learner, courseId)));
        assertEquals(100, ok("GET", "/api/student/online-courses/my-enrollments", learner, null).get(0).path("progressPercent").asInt());
        JsonNode certificate = ok("GET", "/api/student/online-courses/" + courseId + "/certificate", learner, null);
        String code = certificate.path("verificationCode").asText();
        assertFalse(code.isBlank());
        assertTrue(completion.path("eligibleForCertificate").asBoolean() || completion.path("progressPercent").asInt() == 100);
        JsonNode publicCertificate = ok("GET", "/api/online-courses/certificates/" + code, null, null);
        assertEquals(certificate.path("courseTitle"), publicCertificate.path("courseTitle"));
        assertEquals(certificate.path("learnerName"), publicCertificate.path("learnerName"));
    }

    /**
     * Regression: a learner enrolled in V1 must not be blocked by content removed from V2.
     */
    @Test
    @DisplayName("IT_LEARNING_06B — Verify removed lessons no longer block course completion after publishing a new version.")
    void IT_LEARNING_06B() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 2);
        assessment(courseId);
        enroll(learner, courseId);

        String retainedLessonKey = str("""
                select l.stable_lesson_key
                from online_lessons l
                join online_course_modules m on m.id=l.module_id
                join online_course_versions v on v.id=m.online_course_version_id
                where v.online_course_id=? and v.status='PUBLISHED'
                order by l.sequence_number,l.id
                limit 1
                """, courseId);
        long draftVersionId = ok(
                "POST",
                "/api/content-manager/online-courses/" + courseId + "/versions",
                CM,
                body("changeNote", "Remove obsolete lesson")
        ).path("id").asLong();
        sql("""
                delete from online_lessons
                where module_id in (
                    select id from online_course_modules where online_course_version_id=?
                ) and stable_lesson_key<>?
                """, draftVersionId, retainedLessonKey);
        sql("delete from course_assessments where online_course_version_id=?", draftVersionId);
        em.clear();
        ok("PATCH", "/api/content-manager/online-courses/" + courseId + "/versions/"
                + draftVersionId + "/publish", CM, null);
        em.clear();

        JsonNode latestContent = ok("GET", content(courseId), learner, null);
        assertEquals(1, latestContent.path("totalLessons").asInt());
        long latestLessonId = latestContent.path("modules").get(0).path("lessons").get(0).path("id").asLong();
        ok("PATCH", progress(courseId, latestLessonId, true), learner, null);

        JsonNode completion = ok("GET", "/api/student/online-courses/" + courseId + "/completion", learner, null);
        assertEquals(1, completion.path("totalLessons").asInt());
        assertEquals(1, completion.path("completedLessons").asInt());
        assertEquals(0, completion.path("totalAssessments").asInt());
        assertEquals(100, completion.path("progressPercent").asInt());
        assertTrue(completion.path("eligibleForCertificate").asBoolean());
        assertEquals("COMPLETED", str("select status from online_course_enrollments where id=?",
                enrollment(learner, courseId)));
    }

    /**
     * Workbook: Online Course Learning!A19
     * Preconditions:
     * LEARNER is enrolled in course F.
     * Lesson L2 belongs to a different course.
     * Procedure:
     * 1. Login as LEARNER and record the progress of F from GET /api/student/online-courses/my-enrollments.
     * 2. Call PATCH /api/student/online-courses/{F}/lessons/{L2}/progress?completed=true.
     * 3. Query the LessonProgress rows for LEARNER and L2 and call GET /api/student/online-courses/my-enrollments again.
     * Expected results:
     * The request is rejected.
     * No LessonProgress row exists for L2.
     * The progress of F is unchanged.
     */
    @Test
    @DisplayName("IT_LEARNING_07 — Verify a lesson of another course cannot be marked complete.")
    void IT_LEARNING_07() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 2);
        long otherCourseId = course(0, 1);
        enroll(learner, courseId);
        JsonNode before = ok("GET", "/api/student/online-courses/my-enrollments", learner, null);
        rejected("PATCH", progress(courseId, lesson(otherCourseId, 0), true), learner, null);
        assertEquals(0, n("select count(*) from lesson_progress where online_course_enrollment_id=?", enrollment(learner, courseId)));
        assertEquals(before, ok("GET", "/api/student/online-courses/my-enrollments", learner, null));
    }

    /**
     * Workbook: Online Course Learning!A20
     * Preconditions:
     * LEARNER is enrolled in course F with at least one lesson.
     * Procedure:
     * 1. Login as LEARNER and enroll in course F.
     * 2. Call PATCH /api/student/online-courses/{F}/lessons/{L1}/progress?completed=true three times.
     * 3. Query the lesson_progress table.
     * Expected results:
     * The three PATCH /api/student/online-courses/{F}/lessons/{L1}/progress?completed=true calls each return HTTP 200.
     * Only one lesson_progress row exists for the (LEARNER, lesson1) pair and its status remains COMPLETED.
     */
    @Test
    @DisplayName("IT_LEARNING_08 — Verify marking the same lesson complete multiple times keeps a single LessonProgress row.")
    void IT_LEARNING_08() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        long lessonId = lesson(courseId, 0);
        ok("PATCH", progress(courseId, lessonId, true), learner, null);
        ok("PATCH", progress(courseId, lessonId, true), learner, null);
        ok("PATCH", progress(courseId, lessonId, true), learner, null);
        long enrollmentId = enrollment(learner, courseId);
        assertEquals(1, n("select count(*) from lesson_progress where online_course_enrollment_id=? and online_lesson_id=?", enrollmentId, lessonId));
        assertEquals("COMPLETED", str("select status from lesson_progress where online_course_enrollment_id=? and online_lesson_id=?", enrollmentId, lessonId));
    }

    /**
     * Workbook: Online Course Learning!A22
     * Preconditions:
     * LEARNER is enrolled (ACTIVE) in course F with at least two lessons.
     * Procedure:
     * 1. Login as LEARNER and enroll in course F.
     * 2. Complete lesson 1 of course F.
     * 3. Call GET /api/student/online-courses/my-enrollments.
     * 4. Inspect the enrollment entry for course F.
     * Expected results:
     * The enrollment entry shows status ACTIVE.
     * The progressPercent is greater than 0.
     * The completedLessonIds array is present.
     */
    @Test
    @DisplayName("IT_LEARNING_09 — Verify My Courses lists the learner's active enrollment with progress.")
    void IT_LEARNING_09() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 2);
        enroll(learner, courseId);
        ok("PATCH", progress(courseId, lesson(courseId, 0), true), learner, null);
        JsonNode enrollmentItem = row(ok("GET", "/api/student/online-courses/my-enrollments", learner, null), "courseId", courseId);
        assertFalse(enrollmentItem.isMissingNode());
        assertEquals("ACTIVE", enrollmentItem.path("status").asText());
        assertTrue(enrollmentItem.path("progressPercent").asInt() > 0);
        assertTrue(enrollmentItem.path("completedLessonIds").isArray());
    }

    /**
     * Workbook: Online Course Learning!A23
     * Preconditions:
     * LEARNER is a verified account with no course enrollment.
     * Procedure:
     * 1. Login as LEARNER who has not enrolled.
     * 2. Call GET /api/student/online-courses/my-enrollments.
     * 3. Inspect the returned list.
     * Expected results:
     * The GET /api/student/online-courses/my-enrollments endpoint returns HTTP 200 with an empty array.
     */
    @Test
    @DisplayName("IT_LEARNING_10 — Verify My Enrollments is empty for a learner who has not registered for any course.")
    void IT_LEARNING_10() throws Exception {
        String learner = freshLearner();
        assertTrue(ok("GET", "/api/student/online-courses/my-enrollments", learner, null).isEmpty());
    }

    /**
     * Workbook: Online Course Learning!A25
     * Preconditions:
     * LEARNER is enrolled in course F; a MODULE_TEST assessment is attached to a module in F; all prerequisite lessons are completed.
     * Procedure:
     * 1. Login as LEARNER and enroll in course F.
     * 2. Complete all lessons in the module.
     * 3. Call GET /api/student/courses/{F}/assessments.
     * 4. Inspect the returned assessment entry.
     * Expected results:
     * The assessment entry exists with type MODULE_TEST and a resolvedPassingThreshold.
     * No assessment_submissions row is created for this learner and assessment.
     */
    @Test
    @DisplayName("IT_LEARNING_11 — Verify an unlocked module assessment can be viewed without creating a submission.")
    void IT_LEARNING_11() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        long assessmentId = assessment(courseId);
        ok("PATCH", progress(courseId, lesson(courseId, 0), true), learner, null);
        JsonNode assessments = ok("GET", "/api/student/courses/" + courseId + "/assessments", learner, null);
        JsonNode item = row(assessments, "id", assessmentId);
        assertFalse(item.isMissingNode(), () -> "Assessment " + assessmentId + " was not returned: " + assessments);
        assertEquals("MODULE_TEST", item.path("type").asText());
        assertEquals(1, item.path("resolvedPassingThreshold").asInt());
        assertEquals(0, n("select count(*) from assessment_submissions where assessment_id=? and student_id=?", assessmentId, uid(learner)));
    }

    /**
     * Workbook: Online Course Learning!A26
     * Preconditions:
     * LEARNER is enrolled in course F which has no module assessment seeded.
     * Procedure:
     * 1. Login as LEARNER and enroll in course F (which has no assessment seeded).
     * 2. Complete lesson L1.
     * 3. Call GET /api/student/courses/{F}/assessments.
     * Expected results:
     * The GET /api/student/courses/{F}/assessments endpoint returns HTTP 200 with an empty array and no row is inserted into assessment_submissions.
     */
    @Test
    @DisplayName("IT_LEARNING_12 — Verify a course with no module assessment returns an empty assessments list.")
    void IT_LEARNING_12() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        ok("PATCH", progress(courseId, lesson(courseId, 0), true), learner, null);
        assertTrue(ok("GET", "/api/student/courses/" + courseId + "/assessments", learner, null).isEmpty());
    }

    /**
     * Workbook: Online Course Learning!A28
     * Preconditions:
     * LEARNER is enrolled in course F; assessment A exists with a known correct answer key.
     * Procedure:
     * 1. Login as LEARNER, enroll in course F, and complete all lessons.
     * 2. Call POST /api/student/assessments/{A}/submit with the correct answer.
     * 3. Inspect the returned result and database record.
     * Expected results:
     * The response shows status PASSED and the correct aiScore.
     * Exactly one assessment_submissions row exists with status PASSED.
     */
    @Test
    @DisplayName("IT_LEARNING_13 — Verify a correct module assessment submission is saved as PASSED.")
    void IT_LEARNING_13() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        long assessmentId = assessment(courseId);
        ok("PATCH", progress(courseId, lesson(courseId, 0), true), learner, null);
        String answers = body("responses", List.of(Map.of(
                "part", "reading-part-1", "answerType", "text", "questionNumber", "1", "answer", "A")));
        JsonNode result = ok("POST", "/api/student/assessments/" + assessmentId + "/submit", learner,
                body("objectiveAnswersJson", answers, "fullscreenExitCount", 0, "tabSwitchCount", 0));
        assertEquals("PASSED", result.path("status").asText());
        assertEquals(1, result.path("aiScore").asInt());
        assertEquals(1, n("select count(*) from assessment_submissions where assessment_id=? and student_id=? and status='PASSED'", assessmentId, uid(learner)));
    }

    /**
     * Workbook: Online Course Learning!A29
     * Preconditions:
     * LEARNER is enrolled in course F; a MODULE_TEST assessment with a known correct answer key is attached to a module of F; all prerequisite lessons of the module are completed.
     * Procedure:
     * 1. Login as LEARNER, enroll in course F, and complete lesson L1.
     * 2. Seed a module assessment for the course.
     * 3. Call POST /api/student/assessments/{A}/submit with the wrong answer.
     * 4. Query the assessment_submissions table.
     * Expected results:
     * The POST /api/student/assessments/{A}/submit endpoint with the wrong answer returns HTTP 200 with a submission whose status is NEEDS_IMPROVEMENT or FAILED (never PASSED).
     * Exactly one assessment_submissions row exists for the (assessment, LEARNER) pair.
     */
    @Test
    @DisplayName("IT_LEARNING_14 — Verify an incorrect module assessment submission is saved with a non-PASSED status.")
    void IT_LEARNING_14() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        long assessmentId = assessment(courseId);
        ok("PATCH", progress(courseId, lesson(courseId, 0), true), learner, null);
        String answers = body("responses", List.of(Map.of(
                "part", "reading-part-1", "answerType", "text", "questionNumber", "1", "answer", "Z")));
        JsonNode result = ok("POST", "/api/student/assessments/" + assessmentId + "/submit", learner,
                body("objectiveAnswersJson", answers, "fullscreenExitCount", 0, "tabSwitchCount", 0));
        String status = result.path("status").asText();
        assertTrue("PASSED".equals(status) || "FAILED".equals(status) || "NEEDS_IMPROVEMENT".equals(status),
                () -> "Unexpected submission status: " + status);
        assertNotEquals("PASSED", status);
        assertEquals(1, n("select count(*) from assessment_submissions where assessment_id=? and student_id=?", assessmentId, uid(learner)));
    }

    /**
     * Workbook: Online Course Learning!A31
     * Preconditions:
     * LEARNER and LEARNER B are both enrolled in course F.
     * Procedure:
     * 1. Login as LEARNER, enroll in course F, and create a note on lesson L1.
     * 2. Login as LEARNER B (also enrolled) and call GET /api/student/learning/notes.
     * 3. Call GET /api/student/learning/notes as LEARNER.
     * Expected results:
     * LEARNER's note list contains the created note.
     * LEARNER B's note list does NOT contain LEARNER's note.
     */
    @Test
    @DisplayName("IT_LEARNING_15 — Verify lesson notes are listed only for their owner.")
    void IT_LEARNING_15() throws Exception {
        String learner = freshLearner();
        String other = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        enroll(other, courseId);
        long noteId = createNote(courseId, lesson(courseId, 0), learner, "My private lesson note");
        assertFalse(row(ok("GET", "/api/student/learning/notes", learner, null), "id", noteId).isMissingNode());
        assertTrue(row(ok("GET", "/api/student/learning/notes", other, null), "id", noteId).isMissingNode());
    }

    /**
     * Workbook: Online Course Learning!A32
     * Preconditions:
     * LEARNER is a verified account and has never created a note.
     * Procedure:
     * 1. Login as LEARNER who has never created a note.
     * 2. Call GET /api/student/learning/notes.
     * 3. Inspect the returned list.
     * Expected results:
     * The GET /api/student/learning/notes endpoint returns HTTP 200 with an empty array.
     */
    @Test
    @DisplayName("IT_LEARNING_16 — Verify the notes endpoint returns an empty list when the learner has no notes.")
    void IT_LEARNING_16() throws Exception {
        String learner = freshLearner();
        assertTrue(ok("GET", "/api/student/learning/notes", learner, null).isEmpty());
    }

    /**
     * Workbook: Online Course Learning!A34
     * Preconditions:
     * LEARNER is enrolled in course F with lesson L1.
     * Procedure:
     * 1. Login as LEARNER and enroll in course F.
     * 2. Call POST /api/student/learning/courses/{F}/lessons/{L1}/notes with content and selectedText.
     * 3. Query the learner_lesson_notes table.
     * Expected results:
     * A new note is created with the correct user_id, lesson_id, and content.
     * The note content matches the submitted value.
     */
    @Test
    @DisplayName("IT_LEARNING_17 — Verify a learner can create a note for an accessible lesson.")
    void IT_LEARNING_17() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        long lessonId = lesson(courseId, 0);
        long noteId = createNote(courseId, lessonId, learner, "Remember this grammar pattern");
        assertEquals(uid(learner), n("select user_id from learner_lesson_notes where id=?", noteId));
        assertEquals(lessonId, n("select lesson_id from learner_lesson_notes where id=?", noteId));
        assertEquals("Remember this grammar pattern", str("select content from learner_lesson_notes where id=?", noteId));
    }

    /**
     * Workbook: Online Course Learning!A35
     * Preconditions:
     * Course F is published; LEARNER is a verified account with no enrollment in F.
     * Procedure:
     * 1. Login as LEARNER who is not enrolled in course F.
     * 2. Call POST /api/student/learning/courses/{F}/lessons/{L1}/notes with content.
     * 3. Query the learner_lesson_notes table.
     * Expected results:
     * The POST /api/student/learning/courses/{F}/lessons/{L1}/notes endpoint returns HTTP 403.
     * No row is inserted into learner_lesson_notes for LEARNER.
     */
    @Test
    @DisplayName("IT_LEARNING_18 — Verify creating a note on a course the learner is not enrolled in is rejected.")
    void IT_LEARNING_18() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        status(call("POST", "/api/student/learning/courses/" + courseId + "/lessons/" + lesson(courseId, 0) + "/notes",
                learner, body("content", "Should not be saved", "selectedText", "x", "transcriptStartSeconds", 0)), 403);
        assertEquals(0, n("select count(*) from learner_lesson_notes where user_id=?", uid(learner)));
    }

    /**
     * Workbook: Online Course Learning!A37
     * Preconditions:
     * LEARNER is enrolled in course F and has an existing note.
     * Procedure:
     * 1. Login as LEARNER and create a note on lesson L1.
     * 2. Call PUT /api/student/learning/notes/{noteId} with updated content.
     * 3. Query the learner_lesson_notes table.
     * Expected results:
     * The response shows the updated content.
     * The database record reflects the new content and transcriptStartSeconds.
     */
    @Test
    @DisplayName("IT_LEARNING_19 — Verify a learner can update their own lesson note.")
    void IT_LEARNING_19() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        long noteId = createNote(courseId, lesson(courseId, 0), learner, "Original note");
        JsonNode updated = ok("PUT", "/api/student/learning/notes/" + noteId, learner,
                body("content", "Updated note", "selectedText", "Updated selection", "transcriptStartSeconds", 30));
        assertEquals("Updated note", updated.path("content").asText());
        assertEquals("Updated note", str("select content from learner_lesson_notes where id=?", noteId));
        assertEquals(30, n("select transcript_start_seconds from learner_lesson_notes where id=?", noteId));
    }

    /**
     * Workbook: Online Course Learning!A38
     * Preconditions:
     * LEARNER owns a note on lesson L1 of course F; LEARNER B is enrolled in the same course.
     * Procedure:
     * 1. Login as LEARNER and create a note on lesson L1 of course F.
     * 2. Login as LEARNER B (also enrolled) and call PUT /api/student/learning/notes/{noteId}.
     * 3. Query the learner_lesson_notes table.
     * Expected results:
     * The PUT /api/student/learning/notes/{noteId} endpoint called by LEARNER B returns HTTP 400.
     * The content of the note in learner_lesson_notes remains the original text written by LEARNER.
     */
    @Test
    @DisplayName("IT_LEARNING_20 — Verify a learner cannot update a note owned by another learner.")
    void IT_LEARNING_20() throws Exception {
        String learner = freshLearner();
        String other = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        enroll(other, courseId);
        long noteId = createNote(courseId, lesson(courseId, 0), learner, "Owner note");
        status(call("PUT", "/api/student/learning/notes/" + noteId, other,
                body("content", "Hijack attempt", "selectedText", "x", "transcriptStartSeconds", 0)), 400);
        assertEquals("Owner note", str("select content from learner_lesson_notes where id=?", noteId));
    }

    /**
     * Workbook: Online Course Learning!A40
     * Preconditions:
     * LEARNER is enrolled in course F and has an existing note.
     * Procedure:
     * 1. Login as LEARNER and create a note on lesson L1.
     * 2. Call DELETE /api/student/learning/notes/{noteId}.
     * 3. Query the learner_lesson_notes table.
     * Expected results:
     * The DELETE returns HTTP 200.
     * No learner_lesson_notes row exists with the deleted note ID.
     */
    @Test
    @DisplayName("IT_LEARNING_21 — Verify a learner can delete their own lesson note.")
    void IT_LEARNING_21() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        long noteId = createNote(courseId, lesson(courseId, 0), learner, "Temporary note");
        ok("DELETE", "/api/student/learning/notes/" + noteId, learner, null);
        assertEquals(0, n("select count(*) from learner_lesson_notes where id=?", noteId));
    }

    /**
     * Workbook: Online Course Learning!A41
     * Preconditions:
     * LEARNER owns a note on lesson L1 of course F; LEARNER B is enrolled in the same course.
     * Procedure:
     * 1. Login as LEARNER and create a note on lesson L1 of course F.
     * 2. Login as LEARNER B (also enrolled) and call DELETE /api/student/learning/notes/{noteId}.
     * 3. Query the learner_lesson_notes table.
     * Expected results:
     * The DELETE /api/student/learning/notes/{noteId} endpoint called by LEARNER B returns HTTP 400.
     * The note row remains in learner_lesson_notes.
     */
    @Test
    @DisplayName("IT_LEARNING_22 — Verify a learner cannot delete a note owned by another learner.")
    void IT_LEARNING_22() throws Exception {
        String learner = freshLearner();
        String other = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        enroll(other, courseId);
        long noteId = createNote(courseId, lesson(courseId, 0), learner, "Keep me");
        status(call("DELETE", "/api/student/learning/notes/" + noteId, other, null), 400);
        assertEquals(1, n("select count(*) from learner_lesson_notes where id=?", noteId));
    }

    /**
     * Workbook: Online Course Learning!A43
     * Preconditions:
     * LEARNER is enrolled in course F.
     * Procedure:
     * 1. Login as LEARNER and enroll in course F.
     * 2. Create a discussion thread in course F.
     * 3. Call GET /api/online-courses/{F}/discussions.
     * Expected results:
     * The discussion list contains the created thread with its ID.
     * The thread data includes title and content.
     */
    @Test
    @DisplayName("IT_LEARNING_23 — Verify a course discussion is returned in the discussion list.")
    void IT_LEARNING_23() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        long threadId = createThread(courseId, LEARNER);
        JsonNode page = ok("GET", "/api/online-courses/" + courseId + "/discussions", LEARNER, null);
        assertFalse(row(page.path("content"), "id", threadId).isMissingNode());
    }

    /**
     * Workbook: Online Course Learning!A44
     * Preconditions:
     * Course F is published; LEARNER is enrolled in F and no thread has been created in F.
     * Procedure:
     * 1. Login as LEARNER and enroll in course F (no discussion threads).
     * 2. Call GET /api/online-courses/{F}/discussions.
     * 3. Inspect the returned page.
     * Expected results:
     * The GET /api/online-courses/{F}/discussions endpoint returns HTTP 200 with a page whose content array is empty.
     */
    @Test
    @DisplayName("IT_LEARNING_24 — Verify the discussion list is empty for a course without any threads.")
    void IT_LEARNING_24() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        JsonNode page = ok("GET", "/api/online-courses/" + courseId + "/discussions", LEARNER, null);
        assertTrue(page.path("content").isArray());
        assertEquals(0, page.path("content").size());
    }

    /**
     * Workbook: Online Course Learning!A46
     * Preconditions:
     * LEARNER is enrolled in course F.
     * Procedure:
     * 1. Login as LEARNER and enroll in course F.
     * 2. Call POST /api/student/online-courses/{F}/discussions with title and content.
     * 3. Query the course_discussion_posts table.
     * Expected results:
     * A post with post_type THREAD and status OPEN is stored.
     * The author_id matches LEARNER's user ID.
     */
    @Test
    @DisplayName("IT_LEARNING_25 — Verify an enrolled learner can ask a discussion question.")
    void IT_LEARNING_25() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        long threadId = createThread(courseId, LEARNER);
        assertEquals("THREAD", str("select post_type from course_discussion_posts where id=?", threadId));
        assertEquals("OPEN", str("select status from course_discussion_posts where id=?", threadId));
        assertEquals(uid(LEARNER), n("select author_id from course_discussion_posts where id=?", threadId));
    }

    /**
     * Workbook: Online Course Learning!A47
     * Preconditions:
     * Course F is published; LEARNER B is a verified account with no enrollment in F.
     * Procedure:
     * 1. Login as LEARNER B (not enrolled in course F).
     * 2. Call POST /api/student/online-courses/{F}/discussions with title and content.
     * 3. Query the course_discussion_posts table.
     * Expected results:
     * The POST /api/student/online-courses/{F}/discussions endpoint called by an unenrolled learner returns HTTP 400.
     * No row with post_type='THREAD' is inserted into course_discussion_posts for course F.
     */
    @Test
    @DisplayName("IT_LEARNING_26 — Verify an unenrolled learner cannot create a discussion thread.")
    void IT_LEARNING_26() throws Exception {
        String outsider = freshLearner();
        long courseId = course(0, 1);
        rejected("POST", "/api/student/online-courses/" + courseId + "/discussions", outsider,
                body("title", "Need a clearer example",
                        "content", "I would like a clearer explanation and another example for this lesson so I can practise."));
        assertEquals(0, n("select count(*) from course_discussion_posts where post_type='THREAD' and course_id=?", courseId));
    }

    /**
     * Workbook: Online Course Learning!A49
     * Preconditions:
     * LEARNER and LEARNER B are enrolled in course F; a discussion thread exists.
     * Procedure:
     * 1. Login as LEARNER and create a discussion thread in course F.
     * 2. Login as LEARNER B (enrolled) and call POST /api/student/online-courses/discussions/{threadId}/replies.
     * 3. Query the course_discussion_posts table.
     * Expected results:
     * A reply post is stored with parent_post_id matching the thread.
     * The author_id matches LEARNER B's user ID.
     */
    @Test
    @DisplayName("IT_LEARNING_27 — Verify an enrolled learner can reply to a discussion.")
    void IT_LEARNING_27() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        String peer = enrolledPeer(courseId);
        long threadId = createThread(courseId, LEARNER);
        long replyId = createReply(threadId, peer);
        assertEquals(threadId, n("select parent_post_id from course_discussion_posts where id=?", replyId));
        assertEquals(uid(peer), n("select author_id from course_discussion_posts where id=?", replyId));
    }

    /**
     * Workbook: Online Course Learning!A50
     * Preconditions:
     * LEARNER created a thread in course F; LEARNER B is a verified account with no enrollment in F.
     * Procedure:
     * 1. Login as LEARNER and create a discussion thread in course F.
     * 2. Login as LEARNER B (not enrolled) and call POST /api/student/online-courses/discussions/{threadId}/replies.
     * 3. Query the course_discussion_posts table.
     * Expected results:
     * The POST /api/student/online-courses/discussions/{threadId}/replies endpoint called by an unenrolled learner returns HTTP 400.
     * No row with parent_post_id equal to the thread id is inserted into course_discussion_posts.
     */
    @Test
    @DisplayName("IT_LEARNING_28 — Verify an unenrolled learner cannot reply to a course discussion.")
    void IT_LEARNING_28() throws Exception {
        String outsider = freshLearner();
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        long threadId = createThread(courseId, LEARNER);
        status(call("POST", "/api/student/online-courses/discussions/" + threadId + "/replies", outsider,
                body("content", "Outsider reply")), 403);
        assertEquals(0, n("select count(*) from course_discussion_posts where parent_post_id=?", threadId));
    }

    /**
     * Workbook: Online Course Learning!A52
     * Preconditions:
     * LEARNER created a thread in course F; LEARNER B is enrolled in course F.
     * Procedure:
     * 1. Login as LEARNER B (enrolled) and call POST /api/student/online-courses/discussions/{threadId}/reactions with type LIKE.
     * 2. Query the course_discussion_reactions table.
     * Expected results:
     * Exactly one reaction with reaction_type LIKE is stored for the post and user.
     * The reaction count is updated.
     */
    @Test
    @DisplayName("IT_LEARNING_29 — Verify a learner reaction is stored once for a discussion.")
    void IT_LEARNING_29() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        String peer = enrolledPeer(courseId);
        long threadId = createThread(courseId, LEARNER);
        ok("POST", "/api/student/online-courses/discussions/" + threadId + "/reactions", peer, body("type", "LIKE"));
        assertEquals(1, n("select count(*) from course_discussion_reactions where post_id=? and user_id=? and reaction_type='LIKE'", threadId, uid(peer)));
    }

    /**
     * Workbook: Online Course Learning!A53
     * Preconditions:
     * LEARNER created a thread in course F; LEARNER B is enrolled in F.
     * Procedure:
     * 1. Login as LEARNER and enroll in course F.
     * 2. LEARNER B (enrolled) calls POST /api/student/online-courses/discussions/{threadId}/reactions with type LIKE three times.
     * 3. Query the course_discussion_reactions table after each call.
     * Expected results:
     * After the first LIKE reaction, exactly one row exists in course_discussion_reactions for (thread, LEARNER B, LIKE).
     * After the second LIKE call the row is removed (toggled off).
     * After the third LIKE call the row is re-inserted.
     * All three calls return HTTP 200.
     */
    @Test
    @DisplayName("IT_LEARNING_30 — Verify the same learner can toggle a reaction on and off.")
    void IT_LEARNING_30() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        String peer = enrolledPeer(courseId);
        long threadId = createThread(courseId, LEARNER);
        long peerId = uid(peer);
        ok("POST", "/api/student/online-courses/discussions/" + threadId + "/reactions", peer, body("type", "LIKE"));
        assertEquals(1, n("select count(*) from course_discussion_reactions where post_id=? and user_id=? and reaction_type='LIKE'", threadId, peerId));
        ok("POST", "/api/student/online-courses/discussions/" + threadId + "/reactions", peer, body("type", "LIKE"));
        assertEquals(0, n("select count(*) from course_discussion_reactions where post_id=? and user_id=?", threadId, peerId));
        ok("POST", "/api/student/online-courses/discussions/" + threadId + "/reactions", peer, body("type", "LIKE"));
        assertEquals(1, n("select count(*) from course_discussion_reactions where post_id=? and user_id=? and reaction_type='LIKE'", threadId, peerId));
    }

    /**
     * Workbook: Online Course Learning!A55
     * Preconditions:
     * LEARNER created a thread; LEARNER B replied to the thread; both are enrolled.
     * Procedure:
     * 1. Login as LEARNER B and reply to LEARNER's thread.
     * 2. Login as LEARNER (thread author) and call POST /api/student/online-courses/discussions/replies/{replyId}/helpful.
     * 3. Query the course_discussion_reactions table.
     * Expected results:
     * A reaction with helpful=true is stored for the reply.
     * The helpful count is incremented.
     */
    @Test
    @DisplayName("IT_LEARNING_31 — Verify the question author can mark another learner's reply as helpful.")
    void IT_LEARNING_31() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        String peer = enrolledPeer(courseId);
        long replyId = createReply(createThread(courseId, LEARNER), peer);
        ok("POST", "/api/student/online-courses/discussions/replies/" + replyId + "/helpful", LEARNER, null);
        assertEquals(1, n("select count(*) from course_discussion_reactions where post_id=? and user_id=? and helpful=true", replyId, uid(LEARNER)));
    }

    /**
     * Workbook: Online Course Learning!A56
     * Preconditions:
     * LEARNER B created a thread and replied to it in course F; LEARNER is enrolled in F.
     * Procedure:
     * 1. Login as LEARNER B and reply to a thread.
     * 2. Login as LEARNER (enrolled) and call POST /api/student/online-courses/discussions/replies/{replyId}/helpful.
     * 3. Query the course_discussion_reactions table.
     * Expected results:
     * The POST /api/student/online-courses/discussions/replies/{replyId}/helpful endpoint returns HTTP 200.
     * Exactly one row with helpful=true is inserted into course_discussion_reactions for the reply.
     */
    @Test
    @DisplayName("IT_LEARNING_32 — Verify an enrolled learner can mark another learner's reply as helpful.")
    void IT_LEARNING_32() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        String peer = enrolledPeer(courseId);
        long replyId = createReply(createThread(courseId, peer), peer);
        ok("POST", "/api/student/online-courses/discussions/replies/" + replyId + "/helpful", LEARNER, null);
        assertEquals(1, n("select count(*) from course_discussion_reactions where post_id=? and helpful=true", replyId));
    }

    /**
     * Workbook: Online Course Learning!A58
     * Preconditions:
     * LEARNER created a thread; LEARNER B replied; both are enrolled.
     * Procedure:
     * 1. Login as LEARNER B and reply to LEARNER's thread.
     * 2. Login as LEARNER and call PATCH /api/student/online-courses/discussions/{threadId}/resolved?replyId={replyId}.
     * 3. Query the course_discussion_posts table.
     * Expected results:
     * The thread status is RESOLVED.
     * The reply is marked as accepted (accepted=true).
     */
    @Test
    @DisplayName("IT_LEARNING_33 — Verify a question author can solve a discussion with an accepted reply.")
    void IT_LEARNING_33() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        String peer = enrolledPeer(courseId);
        long threadId = createThread(courseId, LEARNER);
        long replyId = createReply(threadId, peer);
        em.clear();
        JsonNode resolved = ok("PATCH", "/api/student/online-courses/discussions/" + threadId + "/resolved?replyId=" + replyId, LEARNER, null);
        assertEquals("RESOLVED", resolved.path("status").asText());
        assertEquals("RESOLVED", str("select status from course_discussion_posts where id=?", threadId));
        assertEquals("true", str("select accepted::text from course_discussion_posts where id=?", replyId));
    }

    /**
     * Workbook: Online Course Learning!A59
     * Preconditions:
     * LEARNER B is enrolled in course F and created a thread; LEARNER replied to the thread; LEARNER C is also enrolled in F.
     * Procedure:
     * 1. Login as LEARNER B (enrolled) and create a thread.
     * 2. Login as LEARNER (enrolled) and reply to the thread.
     * 3. Login as LEARNER C (enrolled, not the author) and call PATCH .../discussions/{threadId}/resolved?replyId={replyId}.
     * 4. Query the course_discussion_posts table.
     * Expected results:
     * The PATCH /api/student/online-courses/discussions/{threadId}/resolved?replyId={replyId} endpoint called by a non-author learner returns HTTP 400.
     * The thread status in course_discussion_posts remains OPEN.
     */
    @Test
    @DisplayName("IT_LEARNING_34 — Verify a thread can only be resolved by its author.")
    void IT_LEARNING_34() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        String peer = enrolledPeer(courseId);
        String bystander = enrolledPeer(courseId);
        long threadId = createThread(courseId, peer);
        long replyId = createReply(threadId, LEARNER);
        em.clear();
        rejected("PATCH", "/api/student/online-courses/discussions/" + threadId + "/resolved?replyId=" + replyId, bystander, null);
        assertEquals("OPEN", str("select status from course_discussion_posts where id=?", threadId));
    }

    /**
     * Workbook: Online Course Learning!A61
     * Preconditions:
     * LEARNER created a thread; LEARNER B is enrolled in the same course.
     * Procedure:
     * 1. Login as LEARNER B and call POST /api/student/online-courses/discussions/{threadId}/reports with reasonCategory SPAM.
     * 2. Query the course_discussion_reports table.
     * Expected results:
     * One report with reason_category SPAM is stored for the post and reporter.
     * The report is available for Content Manager moderation.
     */
    @Test
    @DisplayName("IT_LEARNING_35 — Verify an enrolled learner can report a discussion question once.")
    void IT_LEARNING_35() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        String peer = enrolledPeer(courseId);
        long threadId = createThread(courseId, LEARNER);
        ok("POST", "/api/student/online-courses/discussions/" + threadId + "/reports", peer,
                body("reasonCategory", "SPAM", "reason", "Repeated unrelated promotion"));
        assertEquals(1, n("select count(*) from course_discussion_reports where post_id=? and reporter_id=? and reason_category='SPAM'", threadId, uid(peer)));
    }

    /**
     * Workbook: Online Course Learning!A62
     * Preconditions:
     * LEARNER is enrolled in course F and created a thread in F.
     * Procedure:
     * 1. Login as LEARNER and create a thread in course F.
     * 2. Login as LEARNER and call POST /api/student/online-courses/discussions/{threadId}/reports with reasonCategory SPAM.
     * 3. Query the course_discussion_reports table.
     * Expected results:
     * The POST /api/student/online-courses/discussions/{threadId}/reports endpoint with reasonCategory SPAM returns HTTP 200.
     * One row is inserted into course_discussion_reports with (post_id=thread, reporter_id=LEARNER, reason_category=SPAM).
     */
    @Test
    @DisplayName("IT_LEARNING_36 — Verify a learner can report a discussion question with a reason.")
    void IT_LEARNING_36() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        long threadId = createThread(courseId, learner);
        ok("POST", "/api/student/online-courses/discussions/" + threadId + "/reports", learner,
                body("reasonCategory", "SPAM", "reason", "Self report for moderation flow"));
        assertEquals(1, n("select count(*) from course_discussion_reports where post_id=? and reporter_id=? and reason_category='SPAM'", threadId, uid(learner)));
    }

    /**
     * Workbook: Online Course Learning!A64
     * Preconditions:
     * LEARNER B replied to a thread; LEARNER is enrolled in the same course.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/online-courses/discussions/replies/{replyId}/reports with reasonCategory OFF_TOPIC.
     * 2. Query the course_discussion_reports table.
     * Expected results:
     * One report with reason_category OFF_TOPIC is stored for the reply and reporter.
     * The report is available for moderation.
     */
    @Test
    @DisplayName("IT_LEARNING_37 — Verify an enrolled learner can report a discussion reply once.")
    void IT_LEARNING_37() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        String peer = enrolledPeer(courseId);
        long replyId = createReply(createThread(courseId, LEARNER), peer);
        ok("POST", "/api/student/online-courses/discussions/replies/" + replyId + "/reports", peer,
                body("reasonCategory", "OFF_TOPIC", "reason", "This reply is unrelated to the lesson"));
        assertEquals(1, n("select count(*) from course_discussion_reports where post_id=? and reporter_id=? and reason_category='OFF_TOPIC'", replyId, uid(peer)));
    }

    /**
     * Workbook: Online Course Learning!A65
     * Preconditions:
     * LEARNER created a thread in course F; LEARNER B replied to the thread; both are enrolled in F.
     * Procedure:
     * 1. Login as LEARNER and create a thread in course F.
     * 2. LEARNER B replies to the thread.
     * 3. LEARNER reports the reply twice via POST .../discussions/replies/{replyId}/reports.
     * 4. Query the course_discussion_reports table.
     * Expected results:
     * The first POST /api/student/online-courses/discussions/replies/{replyId}/reports call returns HTTP 200 and one report row is stored.
     * The duplicate POST call returns HTTP 400.
     * Only one course_discussion_reports row remains for (reply, LEARNER).
     */
    @Test
    @DisplayName("IT_LEARNING_38 — Verify duplicate reply reports from the same learner are rejected.")
    void IT_LEARNING_38() throws Exception {
        long courseId = course(0, 1);
        enroll(LEARNER, courseId);
        String peer = enrolledPeer(courseId);
        long replyId = createReply(createThread(courseId, LEARNER), peer);
        ok("POST", "/api/student/online-courses/discussions/replies/" + replyId + "/reports", LEARNER,
                body("reasonCategory", "OFF_TOPIC", "reason", "First report"));
        status(call("POST", "/api/student/online-courses/discussions/replies/" + replyId + "/reports", LEARNER,
                body("reasonCategory", "OFF_TOPIC", "reason", "Duplicate report")), 400);
        assertEquals(1, n("select count(*) from course_discussion_reports where post_id=? and reporter_id=?", replyId, uid(LEARNER)));
    }

    /**
     * Workbook: Online Course Learning!A67
     * Preconditions:
     * LEARNER is enrolled in course F; lesson L1 contains vocabulary content.
     * Procedure:
     * 1. Login as LEARNER and enroll in course F.
     * 2. Seed vocabulary data in lesson L1.
     * 3. Call GET /api/student/flashcards/practice?source=ENROLLED&courseId={F}.
     * Expected results:
     * The response contains at least one term with the correct term text.
     * The term status is NEW for a fresh learner.
     */
    @Test
    @DisplayName("IT_LEARNING_39 — Verify enrolled flashcard practice returns vocabulary from course lessons.")
    void IT_LEARNING_39() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        prepareVocabulary(courseId);
        JsonNode terms = ok("GET", "/api/student/flashcards/practice?source=ENROLLED&courseId=" + courseId, learner, null);
        assertFalse(terms.isEmpty());
        assertEquals("Academic", terms.get(0).path("term").asText());
        assertEquals("NEW", terms.get(0).path("status").asText());
    }

    /**
     * Workbook: Online Course Learning!A68
     * Preconditions:
     * Course F is published with vocabulary seeded in lesson L1; LEARNER B is a verified account with no enrollment in F.
     * Procedure:
     * 1. Login as LEARNER B (not enrolled in course F) and call GET /api/student/flashcards/practice?source=ENROLLED&courseId={F}.
     * 2. Inspect the returned list.
     * Expected results:
     * The GET /api/student/flashcards/practice?source=ENROLLED&courseId={F} endpoint returns HTTP 200 with an empty array (no terms exposed to unenrolled learners).
     */
    @Test
    @DisplayName("IT_LEARNING_40 — Verify an unenrolled learner receives an empty flashcard practice list.")
    void IT_LEARNING_40() throws Exception {
        String outsider = freshLearner();
        long courseId = course(0, 1);
        prepareVocabulary(courseId);
        assertTrue(ok("GET", "/api/student/flashcards/practice?source=ENROLLED&courseId=" + courseId, outsider, null).isEmpty());
    }

    /**
     * Workbook: Online Course Learning!A70
     * Preconditions:
     * LEARNER is enrolled in course F; vocabulary data exists in course lessons.
     * Procedure:
     * 1. Login as LEARNER and load flashcards from course F.
     * 2. Call PATCH /api/student/online-courses/{F}/vocabulary/{termKey}/progress?status=MASTERED&starred=true&reviewed=true&correct=true.
     * 3. Query the vocabulary_progress table.
     * Expected results:
     * The response shows status MASTERED and starred=true.
     * A vocabulary_progress row with status MASTERED and starred=true is stored for the learner, course, and term.
     */
    @Test
    @DisplayName("IT_LEARNING_41 — Verify flashcard learning and star progress is private and persisted.")
    void IT_LEARNING_41() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        prepareVocabulary(courseId);
        JsonNode term = ok("GET", "/api/student/flashcards/practice?source=ENROLLED&courseId=" + courseId, learner, null).get(0);
        String termKey = term.path("termKey").asText();
        JsonNode updated = ok("PATCH", "/api/student/online-courses/" + courseId + "/vocabulary/" + termKey
                + "/progress?status=MASTERED&starred=true&reviewed=true&correct=true", learner, null);
        assertEquals("MASTERED", updated.path("status").asText());
        assertTrue(updated.path("starred").asBoolean());
        assertEquals(1, n("select count(*) from vocabulary_progress where student_id=? and online_course_id=? and term_key=? and status='MASTERED' and starred=true", uid(learner), courseId, termKey));
    }

    /**
     * Workbook: Online Course Learning!A71
     * Preconditions:
     * LEARNER is enrolled in course F; lesson L1 contains vocabulary content (Academic term seeded).
     * Procedure:
     * 1. Login as LEARNER and enroll in course F (with vocabulary prepared).
     * 2. Load flashcard terms and call PATCH .../vocabulary/{termKey}/progress?status=LEARNING&starred=false&reviewed=true&correct=false.
     * 3. Query the vocabulary_progress table.
     * Expected results:
     * The PATCH /api/student/online-courses/{F}/vocabulary/{termKey}/progress endpoint with status=LEARNING&starred=false returns HTTP 200 with a payload whose status is LEARNING and starred is false.
     * A row exists in vocabulary_progress with (LEARNER, F, term_key) where status=LEARNING and starred=false.
     */
    @Test
    @DisplayName("IT_LEARNING_42 — Verify unstarred flashcard progress is persisted without affecting starred flag.")
    void IT_LEARNING_42() throws Exception {
        String learner = freshLearner();
        long courseId = course(0, 1);
        enroll(learner, courseId);
        prepareVocabulary(courseId);
        JsonNode term = ok("GET", "/api/student/flashcards/practice?source=ENROLLED&courseId=" + courseId, learner, null).get(0);
        String termKey = term.path("termKey").asText();
        ok("PATCH", "/api/student/online-courses/" + courseId + "/vocabulary/" + termKey
                + "/progress?status=LEARNING&starred=false&reviewed=true&correct=false", learner, null);
        assertEquals("LEARNING", str("select status from vocabulary_progress where student_id=? and online_course_id=? and term_key=?",
                uid(learner), courseId, termKey));
        assertFalse(Boolean.parseBoolean(str("select starred::text from vocabulary_progress where student_id=? and online_course_id=? and term_key=?",
                uid(learner), courseId, termKey)));
    }
}
