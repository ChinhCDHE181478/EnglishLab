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

/** Canonical workbook scenarios for Classroom Learning. */
@EnglishLabIT
@RecordApplicationEvents
@Tag("workbook")
public class WorkbookClassroomLearningIT extends WorkbookTestSupport {

    /**
     * Workbook: Classroom Learning!A12
     * Preconditions:
     * Fresh learner L; classrooms A/B with sessions; STAFF account. No enrollment exists for L in A or B.
     * Procedure:
     * 1. Create two classroom fixtures A and B with sessions; learner has no enrollment in either.
     * 2. POST /api/staff/classrooms/{A}/transfer-student with studentId L and targetClassSectionId B.
     * 3. Compare all enrollment rows; read learner classrooms and attempt to read sessions of A/B.
     * Expected results:
     * HTTP 400 states that the learner has no valid source registration. Enrollment rows are unchanged, the learner class list is empty, and neither class grants session access.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_01 — Verify transfer is rejected when the learner has no valid source-class registration.")
    void IT_CLASSLEARN_01() throws Exception {
        String u=freshLearner();long a=classroom(),b=classroom();classSession(a);classSession(b);
         String before=snapshot("class_enrollments");
         var r=call("POST","/api/staff/classrooms/"+a+"/transfer-student",STAFF,body("studentId",uid(u),"targetClassSectionId",b));
         status(r,400);assertTrue(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("không có đăng ký hợp lệ"));
         assertEquals(before,snapshot("class_enrollments"));assertTrue(ok("GET","/api/student/classrooms/my-classrooms",u,null).isEmpty());
         rejected("GET","/api/student/classrooms/"+a+"/sessions",u,null);rejected("GET","/api/student/classrooms/"+b+"/sessions",u,null);
    }

    /**
     * Workbook: Classroom Learning!A13
     * Preconditions:
     * Classroom C is full; LEARNER holds a seat; learner W is WAITLIST for C (staff enrolled W while C was full); STAFF account exists.
     * Procedure:
     * 1. Login as W and call GET /api/student/notifications and GET /api/student/notifications/unread-count.
     * 2. Login as STAFF and call POST /api/staff/classrooms/{C}/students/{L}/remove.
     * 3. Query ClassEnrollment(L, C).
     * 4. Login as LEARNER and call GET /api/student/classrooms/{C}/sessions.
     * 5. As W call GET /api/student/notifications and GET /api/student/notifications/unread-count.
     * Expected results:
     * Before the removal W has no CLASSROOM_SLOT_AVAILABLE notification.
     * ClassEnrollment(L, C) is CANCELLED.
     * The removed learner is rejected and receives no session data.
     * W has exactly one unread CLASSROOM_SLOT_AVAILABLE notification for C and the unread count increased by 1.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_02 — Verify removing a learner from a full class notifies the first waitlisted learner.")
    void IT_CLASSLEARN_02() throws Exception {
        String waiter=CM;sql("insert into user_roles(user_id,role_code) values(?,'LEARNER') on conflict do nothing",uid(waiter));long c=classroom();classSession(c);classEnroll(c,LEARNER);sql("update class_sections set capacity=1 where id=?",c);long waitId=classEnroll(c,waiter);sql("update class_enrollments set registration_status='WAITLIST',waitlist_priority=1 where id=?",waitId);assertEquals("WAITLIST",str("select registration_status from class_enrollments where class_section_id=? and student_id=?",c,uid(waiter)));long before=n("select count(*) from app_notifications where user_id=? and read=false",uid(waiter));ok("GET","/api/student/notifications",waiter,null);ok("POST","/api/staff/classrooms/"+c+"/students/"+uid(LEARNER)+"/remove",STAFF,null);assertEquals("CANCELLED",str("select registration_status from class_enrollments where class_section_id=? and student_id=?",c,uid(LEARNER)));rejected("GET","/api/student/classrooms/"+c+"/sessions",LEARNER,null);assertEquals(before+1,n("select count(*) from app_notifications where user_id=? and read=false",uid(waiter)));assertTrue(ok("GET","/api/student/notifications",waiter,null).toString().contains("CLASSROOM_SLOT_AVAILABLE"));
    }

    /**
     * Workbook: Classroom Learning!A14
     * Preconditions:
     * Fresh learner; classroom C with a session. An existing CANCELLED enrollment with gradebook fields is seeded before the test.
     * Procedure:
     * 1. Confirm learner cannot read C sessions and C is absent from my-classrooms.
     * 2. POST /api/staff/classrooms/{C}/enroll with studentId L.
     * 3. Inspect enrollment and read learner classrooms/sessions.
     * Expected results:
     * Existing enrollment becomes ASSIGNED; C appears in my-classrooms and its session is returned.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_03 — Verify reactivating a cancelled enrollment restores learner classroom access.")
    void IT_CLASSLEARN_03() throws Exception {
        String u=freshLearner();long c=classroom(),s=classSession(c);inactive(c,u);rejected("GET","/api/student/classrooms/"+c+"/sessions",u,null);assertTrue(ok("GET","/api/student/classrooms/my-classrooms",u,null).isEmpty());ok("POST","/api/staff/classrooms/"+c+"/enroll",STAFF,body("studentId",uid(u)));assertEquals("ASSIGNED",str("select registration_status from class_enrollments where class_section_id=? and student_id=?",c,uid(u)));assertFalse(row(ok("GET","/api/student/classrooms/my-classrooms",u,null),"id",c).isMissingNode());assertEquals(s,ok("GET","/api/student/classrooms/"+c+"/sessions",u,null).get(0).path("id").asLong());
    }

    /**
     * Workbook: Classroom Learning!A19
     * Preconditions:
     * Test data (seeded in the database): ClassSection C is VIRTUAL with googleMeetStatus READY and googleMeetUrl "https://meet.google.com/abc-defg-hij"; the Google Meet service is replaced by a stub so no call to Google is made.
     * Test data: session S2 belongs to C and has deliveryModeOverride OFFLINE (with a room), status SCHEDULED.
     * LEARNER has a ClassEnrollment(C) with class access.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/classrooms/sessions/{S2}/join.
     * 2. Query the ClassroomAttendance rows of S2 for LEARNER.
     * Expected results:
     * The request is rejected with the message that the session is not an online class.
     * No ClassroomAttendance row is created.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_07 — Verify an offline session cannot be joined as an online class.")
    void IT_CLASSLEARN_07() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long s=classSession(c);sql("update class_schedules set delivery_mode_override='OFFLINE',room_id=(select id from rooms limit 1) where id=?",s);rejected("POST","/api/student/classrooms/sessions/"+s+"/join",LEARNER,null);assertEquals(0,n("select count(*) from classroom_attendance_records where session_id=?",s));
    }

    /**
     * Workbook: Classroom Learning!A21
     * Preconditions:
     * Test data (seeded in the database): ClassSection C is VIRTUAL with googleMeetStatus READY and googleMeetUrl "https://meet.google.com/abc-defg-hij"; the Google Meet service is replaced by a stub so no call to Google is made.
     * ClassSchedule S belongs to C, has sessionDate today, a start time within the next hour, status SCHEDULED and no delivery-mode override, so it is an online session.
     * LEARNER has a ClassEnrollment(C) with class access; STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call POST /api/staff/classrooms/{C}/students/{L}/remove.
     * 2. Query ClassEnrollment(L, C).
     * 3. Login as LEARNER and call POST /api/student/classrooms/sessions/{S}/join.
     * Expected results:
     * ClassEnrollment(L, C) is CANCELLED.
     * The join is rejected with the message that the learner has no access to the class.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_09 — Verify a removed learner cannot join an online session.")
    void IT_CLASSLEARN_09() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long s=classSession(c);ok("POST","/api/staff/classrooms/"+c+"/students/"+uid(LEARNER)+"/remove",STAFF,null);assertEquals("CANCELLED",str("select registration_status from class_enrollments where class_section_id=? and student_id=?",c,uid(LEARNER)));rejected("POST","/api/student/classrooms/sessions/"+s+"/join",LEARNER,null);
    }

    /**
     * Workbook: Classroom Learning!A22
     * Preconditions:
     * Test data (seeded in the database): ClassSection C is VIRTUAL with googleMeetStatus READY and googleMeetUrl "https://meet.google.com/abc-defg-hij"; the Google Meet service is replaced by a stub so no call to Google is made.
     * ClassSchedule S belongs to C, has sessionDate today, a start time within the next hour, status SCHEDULED and no delivery-mode override, so it is an online session.
     * LEARNER has a ClassEnrollment(C) with class access; STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call PUT /api/staff/classrooms/sessions/{S} with status CANCELLED.
     * 2. Query the ClassSchedule S.
     * 3. Login as LEARNER and call POST /api/student/classrooms/sessions/{S}/join.
     * Expected results:
     * The stored ClassSchedule status of S is CANCELLED.
     * The join is rejected with the message that the session was cancelled.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_10 — Verify a cancelled session cannot be joined.")
    void IT_CLASSLEARN_10() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long s=classSession(c);ok("PUT","/api/staff/classrooms/sessions/"+s,STAFF,body("sessionDate",java.time.LocalDate.now().plusDays(10).toString(),"startTime","10:00","endTime","11:00","status","CANCELLED","teacherId",uid(TEACHER),"sessionContent","Round1 cancelled session","sessionContent","Round1 cancelled session","sessionContent","Round1 cancelled session","sessionContent","Round1 cancelled session"));assertEquals("CANCELLED",str("select status from class_schedules where id=?",s));rejected("POST","/api/student/classrooms/sessions/"+s+"/join",LEARNER,null);
    }

    /**
     * Workbook: Classroom Learning!A28
     * Preconditions:
     * Homework H (allowResubmission false, future deadline, OPEN) exists in classroom C taught by TEACHER.
     * LEARNER is enrolled in C and has not submitted.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/classrooms/homework/{H}/submit with {"textAnswer":"v1"}, then again with {"textAnswer":"v2"}.
     * 2. Query the ClassroomHomeworkSubmission rows of H.
     * 3. Login as TEACHER and call GET /api/teacher/classrooms/homework/{H}/submissions.
     * Expected results:
     * Both submit calls succeed.
     * Exactly one ClassroomHomeworkSubmission exists for H and L with status SUBMITTED and answer "v2".
     * The teacher list shows one submission from L with answer "v2".
     */
    @Test
    @DisplayName("IT_CLASSLEARN_14 — Verify a resubmitted answer before grading updates the single submission.")
    void IT_CLASSLEARN_14() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long h=homework(c,"OPEN");submit(h,"v1");submit(h,"v2");assertEquals(1,n("select count(*) from classroom_homework_submissions where homework_id=?",h));assertEquals("v2",str("select text_answer from classroom_homework_submissions where homework_id=?",h));assertEquals("SUBMITTED",str("select status from classroom_homework_submissions where homework_id=?",h));var list=ok("GET","/api/teacher/classrooms/homework/"+h+"/submissions",TEACHER,null);assertEquals(1,list.size());assertEquals("v2",list.get(0).path("textAnswer").asText());
    }

    /**
     * Workbook: Classroom Learning!A29
     * Preconditions:
     * Homework H has allowResubmission false; LEARNER has a GRADED submission with answer "v2".
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/classrooms/homework/{H}/submit with {"textAnswer":"v3"}.
     * 2. Query the ClassroomHomeworkSubmission of L for H.
     * Expected results:
     * The request is rejected with the message that graded homework cannot be resubmitted.
     * The submission is still GRADED and its answer is still "v2".
     */
    @Test
    @DisplayName("IT_CLASSLEARN_15 — Verify resubmitting after grading is rejected when resubmission is disabled.")
    void IT_CLASSLEARN_15() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long h=homework(c,"OPEN");submit(h,"v2");
          ok("POST","/api/teacher/classrooms/homework/"+h+"/students/"+uid(LEARNER)+"/grade",TEACHER,body("score",8.5,"teacherFeedback","Good"));
          var r=rejected("POST","/api/student/classrooms/homework/"+h+"/submit",LEARNER,body("textAnswer","v3"));
          System.out.println("RETEST_RESPONSE "+caseId+" "+r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
          assertEquals("GRADED",str("select status from classroom_homework_submissions where homework_id=?",h));
          assertEquals("v2",str("select text_answer from classroom_homework_submissions where homework_id=?",h));
    }

    /**
     * Workbook: Classroom Learning!A30
     * Preconditions:
     * Homework H1 was created by TEACHER with status OPEN and a deadline in the past (the create API accepts a past deadline); LEARNER is enrolled in the classroom taught by TEACHER.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/classrooms/homework/{H1}/submit with a text answer.
     * 2. Query the ClassroomHomeworkSubmission rows of H1.
     * 3. Login as TEACHER and call GET /api/teacher/classrooms/homework/{H1}/submissions.
     * Expected results:
     * The late submission is rejected. No stored submission exists. The teacher roster may contain the enrolled learner as a placeholder: submitted=false, id=null, submissionTiming=NOT_SUBMITTED.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_16 — Verify a submission after the deadline is rejected.")
    void IT_CLASSLEARN_16() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);
          long h=ok("POST","/api/teacher/classrooms/"+c+"/homework",TEACHER,hwBody("OPEN",java.time.LocalDateTime.now().minusDays(1).toString())).path("id").asLong();
          var r=rejected("POST","/api/student/classrooms/homework/"+h+"/submit",LEARNER,body("textAnswer","late answer"));
          System.out.println("RETEST_RESPONSE "+caseId+" "+r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
          assertEquals(0,n("select count(*) from classroom_homework_submissions where homework_id=?",h));
          var entry=row(ok("GET","/api/teacher/classrooms/homework/"+h+"/submissions",TEACHER,null),"studentId",uid(LEARNER));
          assertFalse(entry.isMissingNode());assertFalse(entry.path("submitted").asBoolean());assertTrue(entry.path("id").isNull());
          assertEquals("NOT_SUBMITTED",entry.path("submissionTiming").asText());
    }

    /**
     * Workbook: Classroom Learning!A31
     * Preconditions:
     * Homework H2 is OPEN with a future deadline; LEARNER is enrolled in its classroom.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/classrooms/homework/{H2}/submit with an empty body.
     * 2. Query the ClassroomHomeworkSubmission rows of H2 and LEARNER.
     * Expected results:
     * The request is rejected with the message that an answer or attachment is required.
     * No submission row exists.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_17 — Verify a submission without answer or attachment is rejected.")
    void IT_CLASSLEARN_17() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long h=homework(c,"OPEN");
          var r=rejected("POST","/api/student/classrooms/homework/"+h+"/submit",LEARNER,"{}");
          System.out.println("RETEST_RESPONSE "+caseId+" "+r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
          assertEquals(0,n("select count(*) from classroom_homework_submissions where homework_id=?",h));
    }

    /**
     * Workbook: Classroom Learning!A32
     * Preconditions:
     * Homework H3 exists with a status other than OPEN; LEARNER is enrolled in its classroom.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/classrooms/homework/{H3}/submit with a text answer.
     * 2. Query the ClassroomHomeworkSubmission rows of H3 and LEARNER.
     * Expected results:
     * The request is rejected with the message that the homework is not open for submission.
     * No submission row exists.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_18 — Verify a submission to a homework that is not open is rejected.")
    void IT_CLASSLEARN_18() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long h=homework(c,"DRAFT");rejected("POST","/api/student/classrooms/homework/"+h+"/submit",LEARNER,body("textAnswer","Round1 answer"));assertEquals(0,n("select count(*) from classroom_homework_submissions where homework_id=?",h));
    }

    /**
     * Workbook: Classroom Learning!A35
     * Preconditions:
     * Fresh learner; published program and classroom C with session. FULLY_PAID enrollment seeded with existing gradebook fields; external mail stubbed.
     * Procedure:
     * 1. Seed FULLY_PAID enrollment E for learner L and classroom C; verify no classroom access yet.
     * 2. Submit a course request; staff schedules and completes the placement test as eligible.
     * 3. POST /api/staff/classrooms/enrollments/{E}/assign.
     * 4. Re-login and read learner classrooms, sessions and request history.
     * Expected results:
     * Request history is SUBMITTED, TEST_SCHEDULED, WAITING_FOR_CLASS (3 entries). E becomes ASSIGNED and learner can read C and its session. The separate course request remains WAITING_FOR_CLASS; this API does not finalize it.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_20 — Verify registration through placement review and classroom access through the paid-enrollment assignment API.")
    void IT_CLASSLEARN_20() throws Exception {
        String u=freshLearner();long c=classroom(),s=classSession(c),e=paid(c,u);
         assertTrue(ok("GET","/api/student/classrooms/my-classrooms",u,null).isEmpty());long r=registration(u,program());eligible(r);
         assertEquals("WAITING_FOR_CLASS",regStatus(r));assertEquals(3,historyCount(r));assignPaid(e);
         assertEquals("ASSIGNED",str("select registration_status from class_enrollments where id=?",e));tokens.remove(u);
         assertFalse(row(ok("GET","/api/student/classrooms/my-classrooms",u,null),"id",c).isMissingNode());
         assertEquals(s,ok("GET","/api/student/classrooms/"+c+"/sessions",u,null).get(0).path("id").asLong());
         assertEquals("WAITING_FOR_CLASS",regStatus(r));assertEquals(3,historyCount(r));
         var my=row(ok("GET","/api/student/course-enrollment-requests/my",u,null),"id",r);List<String> states=new ArrayList<>();for(var x:my.path("history"))states.add(x.path("toStatus").asText());assertEquals(List.of("SUBMITTED","TEST_SCHEDULED","WAITING_FOR_CLASS"),states);
    }

    /**
     * Workbook: Classroom Learning!A36
     * Preconditions:
     * Published program P exists.
     * LEARNER has an open (SUBMITTED) request for P.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/course-enrollment-requests for P again.
     * 2. Query the CourseRegistrationRequest and EnrollmentRequestStatusHistory rows for LEARNER and P.
     * Expected results:
     * The second request is rejected with the message that a request is already being processed.
     * Exactly one request row and one history row exist.
     * The first request is unchanged.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_21 — Verify a second open request for the same program is rejected.")
    void IT_CLASSLEARN_21() throws Exception {
        String u=freshLearner();long p=program(),r=registration(u,p);String before=str("select to_jsonb(t)::text from course_registration_requests t where id=?",r);rejected("POST","/api/student/course-enrollment-requests",u,requestBody(u,p));assertEquals(1,n("select count(*) from course_registration_requests where learner_id=? and course_offering_id=?",uid(u),p));assertEquals(1,historyCount(r));assertEquals(before,str("select to_jsonb(t)::text from course_registration_requests t where id=?",r));
    }

    /**
     * Workbook: Classroom Learning!A37
     * Preconditions:
     * CM and LEARNER accounts exist.
     * LEARNER has no request for the new program.
     * Procedure:
     * 1. Login as CM and call POST /api/content-manager/instructor-led-courses with title, a unique code and status DRAFT to create program D.
     * 2. Login as LEARNER and call POST /api/student/course-enrollment-requests with courseOfferingId D.
     * 3. Query the CourseRegistrationRequest and EnrollmentRequestStatusHistory rows for LEARNER and D.
     * Expected results:
     * Program D is stored with publication status DRAFT.
     * The registration is rejected because the program is not published.
     * No request row and no history row exist for LEARNER and D.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_22 — Verify registering for an unpublished program is rejected.")
    void IT_CLASSLEARN_22() throws Exception {
        String u=freshLearner();long p=ok("POST","/api/content-manager/instructor-led-courses",CM,body("title","Round1 draft program","code","R1-"+UUID.randomUUID().toString().substring(0,8),"status","DRAFT","examCategory","IELTS","entryLevel","4.0","focusSkills","READING,WRITING","outcomes","Develop English reading and writing skills","targetBand",6.5)).path("id").asLong();assertEquals("DRAFT",str("select publication_status from instructor_led_courses where id=?",p));rejected("POST","/api/student/course-enrollment-requests",u,requestBody(u,p));assertEquals(0,n("select count(*) from course_registration_requests where learner_id=?",uid(u)));
    }

    /**
     * Workbook: Classroom Learning!A38
     * Preconditions:
     * Published program P exists.
     * LEARNER is logged in.
     * Procedure:
     * 1. Call POST /api/student/course-enrollment-requests for P without contactName and contactPhone.
     * 2. Query the CourseRegistrationRequest and EnrollmentRequestStatusHistory rows for LEARNER.
     * Expected results:
     * The request is rejected with a validation error (400).
     * No request row and no history row are created.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_23 — Verify a registration request with missing contact fields is rejected.")
    void IT_CLASSLEARN_23() throws Exception {
        String u=freshLearner();long before=n("select count(*) from system_audit_logs");status(call("POST","/api/student/course-enrollment-requests",u,body("courseOfferingId",program(),"contactEmail",u,"consultationTrack","IELTS")),400);assertEquals(0,n("select count(*) from course_registration_requests where learner_id=?",uid(u)));assertEquals(before,n("select count(*) from system_audit_logs"));
    }

    /**
     * Workbook: Classroom Learning!A41
     * Preconditions:
     * Exercise E is PUBLISHED and attached to unit U of program P; classroom C is linked to P; LEARNER is enrolled in C.
     * CM account exists.
     * Procedure:
     * 1. Login as LEARNER and call GET /api/student/classrooms/{C}/practice; E is listed.
     * 2. Login as CM and call PUT /api/content-manager/exercise-bank/{E} with the same title, skill, prompt and answerKey and status DRAFT.
     * 3. Query the exercise E and the CourseUnitContentRef rows of U.
     * 4. As LEARNER call GET /api/student/classrooms/{C}/practice again.
     * Expected results:
     * E has status DRAFT and the CourseUnitContentRef of U still exists.
     * E is no longer in the learner practice list.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_25 — Verify an attached exercise changed to DRAFT disappears from the class practice list.")
    void IT_CLASSLEARN_25() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long e=exercise("PUBLISHED");attach(c,e);assertFalse(row(ok("GET",practice(c),LEARNER,null),"exerciseId",e).isMissingNode());ok("PUT","/api/content-manager/exercise-bank/"+e,CM,exerciseBody("DRAFT"));assertEquals("DRAFT",str("select status from content_bank_items where id=?",e));assertEquals(1,n("select count(*) from course_unit_content_refs where course_unit_id=? and content_bank_item_id=?",unit(c),e));assertTrue(row(ok("GET",practice(c),LEARNER,null),"exerciseId",e).isMissingNode());
    }

    /**
     * Workbook: Classroom Learning!A42
     * Preconditions:
     * Classroom C has practice exercises; LEARNER is enrolled in C; LEARNER B is not enrolled in C.
     * Procedure:
     * 1. Login as LEARNER B and call GET /api/student/classrooms/{C}/practice.
     * 2. Login as LEARNER and call GET /api/student/classrooms/{C}/practice.
     * Expected results:
     * LEARNER B is rejected with the message that the learner is not in the class and receives no exercise data.
     * LEARNER receives the exercises of C, so the difference comes only from the enrollment record.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_26 — Verify a learner who is not enrolled cannot open the class practice list.")
    void IT_CLASSLEARN_26() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long e=exercise("PUBLISHED");attach(c,e);var bad=rejected("GET",practice(c),freshLearner(),null);assertFalse(bad.getResponse().getContentAsString().contains("answerKey"));assertFalse(row(ok("GET",practice(c),LEARNER,null),"exerciseId",e).isMissingNode());
    }

    /**
     * Workbook: Classroom Learning!A44
     * Preconditions:
     * Exercise E (answerKey {"q1":"A","q2":"B","q3":"C","q4":"D"}) is attached to program P and PUBLISHED; classroom C is linked to P; LEARNER is enrolled in C.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/classrooms/{C}/practice/{E}/attempts with answersJson {"q1":"A","q2":"B","q3":"X","q4":"X"} and durationSeconds 60.
     * 2. Call the same endpoint again with answersJson {"q1":"A","q2":"B","q3":"C","q4":"D"}.
     * 3. Query the ClassroomPracticeAttemptHistory rows and call GET /api/student/classrooms/{C}/practice/{E}/attempts.
     * Expected results:
     * Two ClassroomPracticeAttemptHistory rows exist with attemptNumber 1 and 2.
     * The first attempt has correctAnswers 2, totalQuestions 4 and scorePercent 50; the second attempt has correctAnswers 4, totalQuestions 4 and scorePercent 100.
     * The attempts endpoint returns both attempts with their scores.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_27 — Verify practice attempts are scored against the answer key and numbered.")
    void IT_CLASSLEARN_27() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long e=exercise("PUBLISHED");attach(c,e);String p=practice(c)+"/"+e+"/attempts";var first=ok("POST",p,LEARNER,answers(false));assertEquals(1,first.path("attemptNumber").asInt());assertEquals(2,first.path("correctAnswers").asInt());assertEquals(4,first.path("totalQuestions").asInt());assertEquals(50,first.path("scorePercent").asInt());var second=ok("POST",p,LEARNER,answers(true));assertEquals(2,second.path("attemptNumber").asInt());assertEquals(4,second.path("correctAnswers").asInt());assertEquals(100,second.path("scorePercent").asInt());assertEquals(2,n("select count(*) from classroom_practice_attempt_history where class_section_id=? and exercise_content_bank_item_id=? and student_id=?",c,e,uid(LEARNER)));assertEquals(2,ok("GET",p,LEARNER,null).size());
    }

    /**
     * Workbook: Classroom Learning!A45
     * Preconditions:
     * Exercise E (answerKey {"q1":"A","q2":"B","q3":"C","q4":"D"}) is attached to program P and PUBLISHED; classroom C is linked to P; LEARNER is enrolled in C and has no attempt yet.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/classrooms/{C}/practice/{E}/complete with answersJson {"q1":"A","q2":"B","q3":"C","q4":"D"}.
     * 2. Query the ClassroomPracticeAttemptHistory rows of E and LEARNER.
     * 3. Call GET /api/student/classrooms/{C}/practice.
     * Expected results:
     * One attempt with attemptNumber 1 and scorePercent 100 is stored and returned by the complete call.
     * The practice list shows E with the latest score.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_28 — Verify completing an exercise stores the latest attempt and shows it in the practice list.")
    void IT_CLASSLEARN_28() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long e=exercise("PUBLISHED");attach(c,e);var result=ok("POST",practice(c)+"/"+e+"/complete",LEARNER,answers(true));assertEquals(100,result.path("lastScorePercent").asInt());assertEquals(1,n("select count(*) from classroom_practice_attempt_history where class_section_id=? and exercise_content_bank_item_id=?",c,e));assertEquals(100,row(ok("GET",practice(c),LEARNER,null),"exerciseId",e).path("lastScorePercent").asInt());
    }

    /**
     * Workbook: Classroom Learning!A46
     * Preconditions:
     * Exercise E is attached to program P; classroom C is linked to P; LEARNER is enrolled in C.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/classrooms/{C}/practice/{E}/attempts with no answers.
     * 2. Query the ClassroomPracticeAttemptHistory rows of E and LEARNER.
     * Expected results:
     * The request is rejected with the message that the exercise must be completed first.
     * No attempt row is created.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_29 — Verify a practice attempt without answers is rejected.")
    void IT_CLASSLEARN_29() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long e=exercise("PUBLISHED");attach(c,e);rejected("POST",practice(c)+"/"+e+"/attempts",LEARNER,"{}");assertEquals(0,n("select count(*) from classroom_practice_attempt_history where class_section_id=? and exercise_content_bank_item_id=?",c,e));
    }

    /**
     * Workbook: Classroom Learning!A47
     * Preconditions:
     * Exercise E3 is PUBLISHED but is not attached to any unit of the program of classroom C; LEARNER is enrolled in C.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/classrooms/{C}/practice/{E3}/attempts with valid answers.
     * 2. Query the ClassroomPracticeAttemptHistory rows of E3 and LEARNER.
     * Expected results:
     * The request is rejected with the message that the exercise does not belong to the class syllabus.
     * No attempt row is created.
     */
    @Test
    @DisplayName("IT_CLASSLEARN_30 — Verify an attempt for an exercise outside the class program is rejected.")
    void IT_CLASSLEARN_30() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long e=exercise("PUBLISHED");rejected("POST",practice(c)+"/"+e+"/attempts",LEARNER,answers(true));assertEquals(0,n("select count(*) from classroom_practice_attempt_history where class_section_id=? and exercise_content_bank_item_id=?",c,e));
    }
}
