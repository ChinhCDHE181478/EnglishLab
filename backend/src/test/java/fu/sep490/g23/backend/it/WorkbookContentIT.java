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

/** Canonical workbook scenarios for Content Management. */
@EnglishLabIT
@RecordApplicationEvents
@Tag("workbook")
public class WorkbookContentIT extends WorkbookTestSupport {

    /**
     * Workbook: Content Management!A19
     * Preconditions:
     * Published program P has no UPCOMING or ACTIVE classroom; CM, STAFF and LEARNER accounts exist; LEARNER has no request for P.
     * Procedure:
     * 1. Login as CM and call DELETE /api/content-manager/instructor-led-courses/{P}.
     * 2. Call GET /api/content-manager/instructor-led-courses/{P}.
     * 3. Login as STAFF and call GET /api/staff/classrooms/instructor-led-courses.
     * 4. Login as LEARNER and call POST /api/student/course-enrollment-requests for P; query the CourseRegistrationRequest rows for P.
     * Expected results:
     * The archive succeeds with 204 and the status of P is ARCHIVED.
     * P is not in the Staff list.
     * The registration is rejected because the program is not published and no request row is created.
     */
    @Test
    @DisplayName("IT_CONTENT_06 — Verify archiving an unused program removes it from Staff selection and blocks registration.")
    void IT_CONTENT_06() throws Exception {
        long p=cloneRow("instructor_led_courses",program(),"code","R1-"+UUID.randomUUID().toString().substring(0,8),"title","Round1 unused program");status(call("DELETE","/api/content-manager/instructor-led-courses/"+p,CM,null),204);assertEquals("ARCHIVED",str("select publication_status from instructor_led_courses where id=?",p));ok("GET","/api/content-manager/instructor-led-courses/"+p,CM,null);assertTrue(row(ok("GET","/api/staff/classrooms/instructor-led-courses",STAFF,null),"id",p).isMissingNode());String u=freshLearner();rejected("POST","/api/student/course-enrollment-requests",u,requestBody(u,p));assertEquals(0,n("select count(*) from course_registration_requests where course_offering_id=?",p));
    }

    /**
     * Workbook: Content Management!A26
     * Preconditions:
     * CM account exists.
     * Course X has thumbnail T1 uploaded through POST /api/content-manager/online-courses/thumbnail.
     * The object store (S3/R2) is stubbed so keys can be listed; the update transaction is allowed to commit.
     * Procedure:
     * 1. Login as CM and call POST /api/content-manager/online-courses/thumbnail with a new image and take the URL T2.
     * 2. Call PUT /api/content-manager/online-courses/{X} with thumbnailUrl T2.
     * 3. Call GET /api/content-manager/online-courses/{X}.
     * 4. After the transaction commits, list the thumbnail keys in the object store.
     * Expected results:
     * The course detail returns T2.
     * A thumbnail-replaced event is published once with the key of T1.
     * T1 no longer exists in the object store and T2 still exists.
     */
    @Test
    @DisplayName("IT_CONTENT_10 — Verify replacing a thumbnail deletes the old file from storage.")
    void IT_CONTENT_10() throws Exception {
        long c=course(100000,0);String first=upload("/api/content-manager/online-courses/thumbnail",CM).path("url").asText();sql("update online_courses set thumbnail_url=? where id=?",first,c);commit();String second=upload("/api/content-manager/online-courses/thumbnail",CM).path("url").asText();ok("PUT","/api/content-manager/online-courses/"+c,CM,body("title",str("select title from online_courses where id=?",c),"category","IELTS","level","BEGINNER","targetBand",6.5,"targetScore","6.5","targetOutcome","Read English at IELTS 6.5","status","DRAFT","price",100000,"thumbnailUrl",second,"modules",List.of()));assertEquals(second,ok("GET","/api/content-manager/online-courses/"+c,CM,null).path("thumbnailUrl").asText());assertEquals(1,events.stream(CourseThumbnailReplacedEvent.class).count());commit();String old=first.replace("https://storage.example.test/","");verify(store,timeout(5000).times(1)).delete(old);assertFalse(stored.containsKey(old));assertTrue(stored.containsKey(second.replace("https://storage.example.test/","")));
    }

    /**
     * Workbook: Content Management!A27
     * Preconditions:
     * CM account exists.
     * A DRAFT course X with no modules exists.
     * Procedure:
     * 1. Login as CM and call PATCH /api/content-manager/online-courses/{X}/publish.
     * 2. Query the OnlineCourse X and its OnlineCourseVersion rows.
     * 3. Without a JWT call GET /api/online-courses?keyword={titleOfX}.
     * Expected results:
     * The publish is rejected because the course is not publishable.
     * The status of X is still DRAFT and its version is not published.
     * X is not in the public catalog.
     */
    @Test
    @DisplayName("IT_CONTENT_11 — Verify a course without modules or lessons cannot be published.")
    void IT_CONTENT_11() throws Exception {
        long c=course(100000,0);rejected("PATCH","/api/content-manager/online-courses/"+c+"/publish",CM,null);assertEquals("DRAFT",str("select status from online_courses where id=?",c));assertEquals(0,n("select count(*) from online_course_versions where online_course_id=? and status='PUBLISHED'",c));String slug=str("select slug from online_courses where id=?",c);rejected("GET","/api/online-courses/"+slug,null,null);
    }

    /**
     * Workbook: Content Management!A29
     * Preconditions:
     * Program P is published with unit U; classroom C is linked to P; LEARNER is enrolled in C.
     * Exercise E2 exists in the exercise bank with status DRAFT; CM account exists.
     * Procedure:
     * 1. Login as CM and call POST /api/content-manager/curriculum-units/{U}/exercises with {"resourceId":E2,"displayOrder":1}.
     * 2. Query the CourseUnitContentRef rows of U for E2.
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/practice.
     * Expected results:
     * The attachment is rejected with the message that only a published exercise can be used.
     * No CourseUnitContentRef exists for E2 and U.
     * E2 is not in the learner practice list.
     */
    @Test
    @DisplayName("IT_CONTENT_12 — Verify an unpublished exercise cannot be attached to a program unit.")
    void IT_CONTENT_12() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long e=exercise("DRAFT");rejected("POST","/api/content-manager/curriculum-units/"+unit(c)+"/exercises",CM,body("resourceId",e,"displayOrder",1));assertEquals(0,n("select count(*) from course_unit_content_refs where course_unit_id=? and content_bank_item_id=?",unit(c),e));assertTrue(row(ok("GET",practice(c),LEARNER,null),"exerciseId",e).isMissingNode());
    }

    /**
     * Workbook: Content Management!A31
     * Preconditions:
     * Program P has lesson L1 and classroom C is linked to P with a session S that uses L1.
     * LEARNER is enrolled in C; CM account exists.
     * Procedure:
     * 1. Login as LEARNER and call GET /api/student/classrooms/{C}/sessions; read the lesson title of S.
     * 2. Login as CM and call PUT /api/content-manager/curriculum-session-plans/{L1} with sessionNumber, the new title and the other fields unchanged.
     * 3. Query the CourseLesson L1.
     * 4. As LEARNER call GET /api/student/classrooms/{C}/sessions again.
     * Expected results:
     * Before the update S shows the old lesson title.
     * The CourseLesson row holds the new title.
     * After the update S shows the new lesson title and no session is duplicated.
     */
    @Test
    @DisplayName("IT_CONTENT_13 — Verify a renamed lesson title is shown in the class sessions of the learner.")
    void IT_CONTENT_13() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long s=classSession(c),l=ok("POST","/api/content-manager/curriculum-units/"+unit(c)+"/session-plans",CM,body("sessionNumber",1,"title","Round1 original lesson","description","Fixture lesson","learningObjectives","Read and write English")).path("id").asLong();sql("update class_schedules set course_lesson_id=? where id=?",l,s);String old=str("select title from course_lessons where id=?",l);assertEquals(old,row(ok("GET","/api/student/classrooms/"+c+"/sessions",LEARNER,null),"id",s).path("courseLessonTitle").asText());String title="Round1 renamed lesson";ok("PUT","/api/content-manager/curriculum-session-plans/"+l,CM,body("sessionNumber",n("select sequence_number from course_lessons where id=?",l),"title",title,"description",str("select description from course_lessons where id=?",l),"learningObjectives",str("select learning_objectives from course_lessons where id=?",l)));assertEquals(title,str("select title from course_lessons where id=?",l));assertEquals(title,row(ok("GET","/api/student/classrooms/"+c+"/sessions",LEARNER,null),"id",s).path("courseLessonTitle").asText());assertEquals(1,n("select count(*) from class_schedules where class_section_id=?",c));
    }

    /**
     * Workbook: Content Management — Create Online Course
     * Preconditions:
     * CM account exists.
     * Procedure:
     * 1. Login as CM and call POST /api/content-manager/online-courses with valid fields.
     * 2. Query the OnlineCourse row.
     * Expected results:
     * The course is created with status DRAFT and the correct title and price.
     */
    @Test
    @DisplayName("IT_CONTENT_14 — Verify creating a valid draft course persists it with correct fields.")
    void IT_CONTENT_14() throws Exception {
        String title="Round1 valid course "+UUID.randomUUID();long c=ok("POST","/api/content-manager/online-courses",CM,body("title",title,"category","IELTS","level","BEGINNER","targetBand",6.5,"targetScore","6.5","targetOutcome","Read English","status","DRAFT","price",50000,"modules",List.of())).path("id").asLong();assertEquals("DRAFT",str("select status from online_courses where id=?",c));assertEquals(title,str("select title from online_courses where id=?",c));assertEquals(50000,n("select price from online_courses where id=?",c));
    }

    /**
     * Workbook: Content Management — View Online Course
     * Preconditions:
     * LEARNER account exists.
     * Procedure:
     * 1. Login as LEARNER and call GET /api/content-manager/online-courses.
     * Expected results:
     * The request is rejected with 403.
     */
    @Test
    @DisplayName("IT_CONTENT_15 — Verify a learner cannot access CM course management endpoints.")
    void IT_CONTENT_15() throws Exception {
        String u=freshLearner();status(call("GET","/api/content-manager/online-courses",u,null),403);
    }

    /**
     * Workbook: Content Management — Attach Practice Exercise to Course Unit
     * Preconditions:
     * Program P is published with unit U; classroom C is linked to P; LEARNER is enrolled in C.
     * Exercise E is PUBLISHED in the exercise bank; CM account exists.
     * Procedure:
     * 1. Login as CM and call POST /api/content-manager/curriculum-units/{U}/exercises with {\"resourceId\":E,\"displayOrder\":1}.
     * 2. Query the CourseUnitContentRef rows of U for E.
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/practice.
     * Expected results:
     * A CourseUnitContentRef row exists for U and E.
     * E appears in the learner practice list.
     */
    @Test
    @DisplayName("IT_CONTENT_16 — Verify a published exercise is attached and visible in learner practice.")
    void IT_CONTENT_16() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long e=exercise("PUBLISHED");ok("POST","/api/content-manager/curriculum-units/"+unit(c)+"/exercises",CM,body("resourceId",e,"displayOrder",1));assertEquals(1,n("select count(*) from course_unit_content_refs where course_unit_id=? and content_bank_item_id=?",unit(c),e));assertFalse(row(ok("GET",practice(c),LEARNER,null),"exerciseId",e).isMissingNode());
    }

    /**
     * Workbook: Content Management — Manage Course Lesson Session Plans
     * Preconditions:
     * Program P has lesson L in unit U; classroom C is linked to P with a session S using L; LEARNER is enrolled in C; CM account exists.
     * Procedure:
     * 1. Login as CM and call DELETE /api/content-manager/curriculum-session-plans/{L}.
     * 2. Query the CourseLesson rows for L.
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/sessions.
     * Expected results:
     * The CourseLesson row L is removed.
     * The session S no longer references L.
     */
    @Test
    @DisplayName("IT_CONTENT_17 — Verify deleting a lesson plan removes its reference from the class sessions.")
    void IT_CONTENT_17() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long s=classSession(c),l=ok("POST","/api/content-manager/curriculum-units/"+unit(c)+"/session-plans",CM,body("sessionNumber",1,"title","Round1 delete lesson","description","Fixture","learningObjectives","Read")).path("id").asLong();sql("update class_schedules set course_lesson_id=? where id=?",l,s);ok("DELETE","/api/content-manager/curriculum-session-plans/"+l,CM,null);assertEquals(0,n("select count(*) from course_lessons where id=?",l));var sess=row(ok("GET","/api/student/classrooms/"+c+"/sessions",LEARNER,null),"id",s);assertTrue(sess.path("courseLessonTitle").isNull()||sess.path("courseLessonTitle").asText().isEmpty()||sess.path("courseLessonTitle").isMissingNode());
    }
}
