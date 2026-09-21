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

    /**
     * Workbook: Online Course Learning!A16
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
    @DisplayName("IT_LEARNING_04 — Verify un-completing a lesson rolls the progress back.")
    void IT_LEARNING_04() throws Exception {
        String u=freshLearner();long c=course(0,2);enroll(u,c);long l=lesson(c,0);ok("PATCH",progress(c,l,true),u,null);long e=enrollment(u,c),id=n("select id from lesson_progress where online_course_enrollment_id=? and online_lesson_id=?",e,l);ok("PATCH",progress(c,l,false),u,null);assertEquals(id,n("select id from lesson_progress where online_course_enrollment_id=? and online_lesson_id=?",e,l));assertEquals("IN_PROGRESS",str("select status from lesson_progress where id=?",id));assertNull(str("select completed_at::text from lesson_progress where id=?",id));assertEquals(0,n("select progress_percent::bigint from online_course_enrollments where id=?",e));assertEquals(0,ok("GET","/api/student/online-courses/my-enrollments",u,null).get(0).path("progressPercent").asInt());
    }

    /**
     * Workbook: Online Course Learning!A17
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
    @DisplayName("IT_LEARNING_05 — Verify completing all required lessons completes the enrollment and issues a verifiable certificate.")
    void IT_LEARNING_05() throws Exception {
        String u=freshLearner();long c=course(0,2);enroll(u,c);ok("PATCH",progress(c,lesson(c,0),true),u,null);ok("PATCH",progress(c,lesson(c,1),true),u,null);var done=ok("GET","/api/student/online-courses/"+c+"/completion",u,null);System.out.println("ROUND1_COMPLETION "+done);assertEquals("COMPLETED",str("select status from online_course_enrollments where id=?",enrollment(u,c)));assertEquals(100,ok("GET","/api/student/online-courses/my-enrollments",u,null).get(0).path("progressPercent").asInt());var cert=ok("GET","/api/student/online-courses/"+c+"/certificate",u,null);String code=cert.path("verificationCode").asText();assertFalse(code.isBlank());assertFalse(cert.path("verificationUrl").asText().isBlank());var pub=ok("GET","/api/online-courses/certificates/"+code,null,null);assertEquals(cert.path("courseTitle"),pub.path("courseTitle"));assertEquals(cert.path("learnerName"),pub.path("learnerName"));
    }

    /**
     * Workbook: Online Course Learning!A18
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
    @DisplayName("IT_LEARNING_06 — Verify a lesson of another course cannot be marked complete.")
    void IT_LEARNING_06() throws Exception {
        String u=freshLearner();long c=course(0,2),other=course(0,1);enroll(u,c);var before=ok("GET","/api/student/online-courses/my-enrollments",u,null);rejected("PATCH",progress(c,lesson(other,0),true),u,null);assertEquals(0,n("select count(*) from lesson_progress where online_course_enrollment_id=?",enrollment(u,c)));assertEquals(before,ok("GET","/api/student/online-courses/my-enrollments",u,null));
    }
}
