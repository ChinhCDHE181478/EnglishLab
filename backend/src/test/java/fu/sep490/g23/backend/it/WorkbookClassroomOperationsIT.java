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

/** Canonical workbook scenarios for Classroom Operations. */
@EnglishLabIT
@RecordApplicationEvents
@Tag("workbook")
public class WorkbookClassroomOperationsIT extends WorkbookTestSupport {

    /**
     * Workbook: Classroom Operations!A12
     * Preconditions:
     * STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call POST /api/staff/classroom-proposals without capacity and with an empty weekdays list.
     * 2. Query the ClassroomProposal rows created during the test.
     * Expected results:
     * The request is rejected with a validation error (400).
     * No ClassroomProposal row exists.
     */
    @Test
    @DisplayName("IT_CLASSOPS_01 — Verify a proposal with missing required fields is rejected.")
    void IT_CLASSOPS_01() throws Exception {
        long before=n("select count(*) from classroom_proposals");status(call("POST","/api/staff/classroom-proposals",STAFF,body("title","Round1 invalid proposal","courseOfferingId",program(),"weekdays",List.of(),"plannedStartDate",monday().toString(),"endDate",monday().plusWeeks(4).toString(),"sessionStartTime","18:00","sessionEndTime","20:00","deliveryType","VIRTUAL")),400);assertEquals(before,n("select count(*) from classroom_proposals"));
    }

    /**
     * Workbook: Classroom Operations!A14
     * Preconditions:
     * A proposal Q is REJECTED with a review note; STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call PUT /api/staff/classroom-proposals/{Q} with a corrected plan.
     * 2. Call PATCH /api/staff/classroom-proposals/{Q}/submit.
     * 3. Query the ClassroomProposal Q.
     * Expected results:
     * Q has approvalStatus PENDING_APPROVAL with a new submittedAt.
     * reviewNote, reviewer and reviewedAt are cleared.
     */
    @Test
    @DisplayName("IT_CLASSOPS_02 — Verify a rejected proposal can be corrected and resubmitted for approval.")
    void IT_CLASSOPS_02() throws Exception {
        long t=uid(otherTeacher()),q=proposal(t);send(q);ok("PATCH","/api/manager/classroom-proposals/"+q+"/reject",MANAGER,body("reason","Room not available"));ok("PUT","/api/staff/classroom-proposals/"+q,STAFF,proposalBody(t));send(q);assertEquals("PENDING_APPROVAL",proposalStatus(q));assertNull(str("select review_note from classroom_proposals where id=?",q));assertNull(str("select reviewed_by_id::text from classroom_proposals where id=?",q));assertNull(str("select reviewed_at::text from classroom_proposals where id=?",q));assertNotNull(str("select submitted_at::text from classroom_proposals where id=?",q));
    }

    /**
     * Workbook: Classroom Operations!A15
     * Preconditions:
     * Published program; teacher with one overlapping SCHEDULED session; valid STAFF account.
     * Procedure:
     * 1. Seed a teacher session on a future Monday 18:00-20:00.
     * 2. POST /api/staff/classroom-proposals for the same teacher, overlapping future Monday/Wednesday 18:00-20:00.
     * 3. Compare proposal count, classrooms and schedule rows.
     * Expected results:
     * HTTP 409 CLASSROOM_SCHEDULE_CONFLICT. No proposal or classroom is created and existing schedules are unchanged.
     */
    @Test
    @DisplayName("IT_CLASSOPS_03 — Verify an overlapping teacher proposal is rejected at creation.")
    void IT_CLASSOPS_03() throws Exception {
        long c=classroom(),s=classSession(c);sql("update class_schedules set session_date=?,start_time='18:00',end_time='20:00' where id=?",monday(),s);
         String before=snapshot("class_schedules");long classes=n("select count(*) from class_sections"),proposals=n("select count(*) from classroom_proposals");
         var r=call("POST","/api/staff/classroom-proposals",STAFF,proposalBody(uid(TEACHER)));status(r,409);
         assertTrue(r.getResponse().getContentAsString().contains("CLASSROOM_SCHEDULE_CONFLICT"));
         assertEquals(proposals,n("select count(*) from classroom_proposals"));assertEquals(before,snapshot("class_schedules"));assertEquals(classes,n("select count(*) from class_sections"));
    }

    /**
     * Workbook: Classroom Operations!A16
     * Preconditions:
     * STAFF account; valid future Monday/Wednesday window 18:00-20:00; teacher with no existing session conflict.
     * Procedure:
     * 1. Create and submit proposal A for an otherwise unoccupied teacher.
     * 2. POST another proposal for the same teacher and time window.
     * 3. Query proposal count and A status.
     * Expected results:
     * HTTP 409 CLASSROOM_SCHEDULE_CONFLICT for B creation. A stays PENDING_APPROVAL and no B row is created.
     */
    @Test
    @DisplayName("IT_CLASSOPS_04 — Verify creating a proposal that overlaps a pending proposal is rejected.")
    void IT_CLASSOPS_04() throws Exception {
        long t=uid(otherTeacher()),a=proposal(t);send(a);long before=n("select count(*) from classroom_proposals");
         var r=call("POST","/api/staff/classroom-proposals",STAFF,proposalBody(t));status(r,409);
         assertTrue(r.getResponse().getContentAsString().contains("CLASSROOM_SCHEDULE_CONFLICT"));
         assertEquals("PENDING_APPROVAL",proposalStatus(a));assertEquals(before,n("select count(*) from classroom_proposals"));
    }

    /**
     * Workbook: Classroom Operations!A29
     * Preconditions:
     * A published instructor-led program P exists.
     * LEARNER has no open request for P; STAFF account exists.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/course-enrollment-requests for program P.
     * 2. Query the CourseRegistrationRequest and EnrollmentRequestStatusHistory rows of LEARNER.
     * 3. Login as STAFF and call GET /api/staff/enrollment-requests and GET /api/staff/enrollment-requests?status=SUBMITTED.
     * 4. As LEARNER call GET /api/student/course-enrollment-requests/my.
     * Expected results:
     * A CourseRegistrationRequest with status SUBMITTED and source ONLINE exists together with one status history row.
     * The Staff queue returns the request with the learner contact data.
     * The status filter returns only SUBMITTED requests and includes this one.
     * The learner list returns the same request.
     */
    @Test
    @DisplayName("IT_CLASSOPS_12 — Verify a submitted class registration request appears in the Staff request queue.")
    void IT_CLASSOPS_12() throws Exception {
        String u=freshLearner();long p=n("select instructor_led_course_id from class_sections where id=?",shared());
          long r=ok("POST","/api/student/course-enrollment-requests",u,body("courseOfferingId",p,"contactName","Retest learner","contactEmail",u,"contactPhone","0901234567","consultationTrack","IELTS")).path("id").asLong();
          assertEquals("SUBMITTED",str("select status from course_registration_requests where id=?",r));
          assertEquals("ONLINE",str("select request_source from course_registration_requests where id=?",r));
          assertEquals(1,n("select count(*) from system_audit_logs where target_type='COURSE_REGISTRATION_REQUEST' and target_id=?",String.valueOf(r)));
          for(String path:List.of("/api/staff/enrollment-requests","/api/staff/enrollment-requests?status=SUBMITTED")){
           var all=ok("GET",path,STAFF,null);var item=row(all,"id",r);assertFalse(item.isMissingNode());assertEquals(u,item.path("contactEmail").asText());
           if(path.contains("?"))for(var x:items(all))assertEquals("SUBMITTED",x.path("status").asText());
          }
          assertFalse(row(ok("GET","/api/student/course-enrollment-requests/my",u,null),"id",r).isMissingNode());
    }

    /**
     * Workbook: Classroom Operations!A31
     * Preconditions:
     * Request R is SUBMITTED.
     * STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call PATCH /api/staff/enrollment-requests/{R}/schedule-test with appointmentAt one hour in the past.
     * 2. Query the status and status history of R and the mail sender.
     * Expected results:
     * The request is rejected with the message that the appointment must be in the future.
     * R is still SUBMITTED and no history row is added.
     * No appointment e-mail is sent.
     */
    @Test
    @DisplayName("IT_CLASSOPS_13 — Verify scheduling an entrance test in the past is rejected.")
    void IT_CLASSOPS_13() throws Exception {
        String u=freshLearner();long r=registration(u,program());long h=historyCount(r);clearInvocations(mail);rejected("PATCH",reqPath(r)+"/schedule-test",STAFF,body("appointmentAt",LocalDateTime.now().minusHours(1).toString(),"location","Round1 room"));assertEquals("SUBMITTED",regStatus(r));assertEquals(h,historyCount(r));verify(mail,never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    /**
     * Workbook: Classroom Operations!A32
     * Preconditions:
     * Request R is SUBMITTED.
     * LEARNER notification preference emailEnabled is false (set through PUT /api/user/me/notification-preferences).
     * STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call PATCH /api/staff/enrollment-requests/{R}/schedule-test with a future appointmentAt.
     * 2. Query the status and status history of R and the mail sender.
     * Expected results:
     * R is TEST_SCHEDULED and the history row exists.
     * No appointment e-mail is sent to the learner because the e-mail preference is off.
     */
    @Test
    @DisplayName("IT_CLASSOPS_14 — Verify scheduling a test sends no e-mail when the learner turned e-mail notifications off.")
    void IT_CLASSOPS_14() throws Exception {
        String u=freshLearner();long r=registration(u,program());ok("PUT","/api/user/me/notification-preferences",u,body("emailEnabled",false,"inAppEnabled",true,"classReminderEnabled",true,"studyAlertEnabled",true));clearInvocations(mail);schedule(r);assertEquals("TEST_SCHEDULED",regStatus(r));assertEquals(2,historyCount(r));verify(mail,never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    /**
     * Workbook: Classroom Operations!A34
     * Preconditions:
     * A published program P exists.
     * LEARNER has no open request for P; request R for P is TEST_SCHEDULED.
     * STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call PATCH /api/staff/enrollment-requests/{R}/complete-test with {"eligible":false,"note":"Level too low for this intake"}.
     * 2. Query the CourseRegistrationRequest R and its EnrollmentRequestStatusHistory.
     * 3. Login as LEARNER and call GET /api/student/course-enrollment-requests/my.
     * Expected results:
     * R has status REJECTED with rejectionReason "Level too low for this intake".
     * A history row TEST_SCHEDULED to REJECTED exists.
     * The learner sees status REJECTED and the same reason.
     * No ClassEnrollment is created for the learner.
     */
    @Test
    @DisplayName("IT_CLASSOPS_15 — Verify an ineligible test result rejects the request with a reason.")
    void IT_CLASSOPS_15() throws Exception {
        String u=freshLearner();long r=registration(u,program());schedule(r);String reason="Level too low for this intake";ok("PATCH",reqPath(r)+"/complete-test",STAFF,body("eligible",false,"note",reason));assertEquals("REJECTED",regStatus(r));assertEquals(reason,str("select rejection_reason from course_registration_requests where id=?",r));assertEquals(3,historyCount(r));var mine=row(ok("GET","/api/student/course-enrollment-requests/my",u,null),"id",r);assertEquals("REJECTED",mine.path("status").asText());assertEquals(reason,mine.path("rejectionReason").asText());assertEquals(0,n("select count(*) from class_enrollments where student_id=?",uid(u)));
    }

    /**
     * Workbook: Classroom Operations!A36
     * Preconditions:
     * Request R is SUBMITTED for LEARNER and program P.
     * STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call PATCH /api/staff/enrollment-requests/{R}/reject with {"reason":"Not eligible for this intake"}.
     * 2. Query the CourseRegistrationRequest R and its EnrollmentRequestStatusHistory.
     * 3. Login as LEARNER and call GET /api/student/course-enrollment-requests/my.
     * Expected results:
     * R has status REJECTED with the given reason and reviewer.
     * A history row SUBMITTED to REJECTED exists.
     * The learner sees REJECTED and the reason.
     */
    @Test
    @DisplayName("IT_CLASSOPS_16 — Verify Staff can reject a request directly with a reason.")
    void IT_CLASSOPS_16() throws Exception {
        String u=freshLearner();long r=registration(u,program());String reason="Not eligible for this intake";ok("PATCH",reqPath(r)+"/reject",STAFF,body("reason",reason));assertEquals("REJECTED",regStatus(r));assertEquals(reason,str("select rejection_reason from course_registration_requests where id=?",r));assertEquals(uid(STAFF),n("select reviewed_by_id from course_registration_requests where id=?",r));assertEquals(2,historyCount(r));assertEquals(reason,row(ok("GET","/api/student/course-enrollment-requests/my",u,null),"id",r).path("rejectionReason").asText());
    }

    /**
     * Workbook: Classroom Operations!A37
     * Preconditions:
     * Request R for LEARNER and program P is REJECTED.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/course-enrollment-requests for program P.
     * 2. Query the CourseRegistrationRequest rows for LEARNER and P.
     * Expected results:
     * The new request is accepted with status SUBMITTED.
     * Two rows exist: R (REJECTED, unchanged) and the new request (SUBMITTED).
     */
    @Test
    @DisplayName("IT_CLASSOPS_17 — Verify a rejected request no longer blocks a new request for the same program.")
    void IT_CLASSOPS_17() throws Exception {
        String u=freshLearner();long p=program(),r=registration(u,p);ok("PATCH",reqPath(r)+"/reject",STAFF,body("reason","Not eligible for this intake"));String before=str("select to_jsonb(t)::text from course_registration_requests t where id=?",r);long next=registration(u,p);assertEquals("SUBMITTED",regStatus(next));assertEquals(2,n("select count(*) from course_registration_requests where learner_id=? and course_offering_id=?",uid(u),p));assertEquals(before,str("select to_jsonb(t)::text from course_registration_requests t where id=?",r));
    }

    /**
     * Workbook: Classroom Operations!A39
     * Preconditions:
     * Fresh learner; classroom C with free seats and session. Existing FULLY_PAID enrollment with gradebook fields is seeded.
     * Procedure:
     * 1. POST /api/staff/classrooms/enrollments/{E}/assign with assignmentNote.
     * 2. Inspect enrollment, assignedBy, assignedAt, gradebook fields and CLASSROOM_ASSIGNED notifications.
     * 3. Read learner my-classrooms.
     * Expected results:
     * Exactly one enrollment E remains and becomes ASSIGNED, with STAFF as assigner and a non-null assignedAt. Gradebook fields exist. One CLASSROOM_ASSIGNED notification is added and classroom C is visible.
     */
    @Test
    @DisplayName("IT_CLASSOPS_18 — Verify assigning an existing tuition-paid enrollment grants access and creates an in-app notification.")
    void IT_CLASSOPS_18() throws Exception {
        String u=freshLearner();long c=classroom();classSession(c);long e=paid(c,u);
         long before=n("select count(*) from app_notifications where user_id=? and type='CLASSROOM_ASSIGNED'",uid(u));assignPaid(e);
         assertEquals("ASSIGNED",str("select registration_status from class_enrollments where id=?",e));
         assertEquals(uid(STAFF),n("select assigned_by_id from class_enrollments where id=?",e));assertNotNull(str("select assigned_at::text from class_enrollments where id=?",e));
         assertEquals(1,n("select count(*) from class_enrollments where class_section_id=? and student_id=?",c,uid(u)));assertNotNull(str("select gradebook_status from class_enrollments where id=?",e));
         assertEquals(before+1,n("select count(*) from app_notifications where user_id=? and type='CLASSROOM_ASSIGNED'",uid(u)));
         assertFalse(row(ok("GET","/api/student/classrooms/my-classrooms",u,null),"id",c).isMissingNode());
    }

    /**
     * Workbook: Classroom Operations!A40
     * Preconditions:
     * Class FULL capacity 1 already has an ASSIGNED learner. Request learner has an existing WAITLIST enrollment with required tuition data; request R is WAITING_FOR_CLASS.
     * Procedure:
     * 1. GET /api/staff/enrollment-requests/{R}/available-classrooms.
     * 2. PATCH /api/staff/enrollment-requests/{R}/assign-class with classroomId FULL.
     * 3. Inspect request history, enrollment status, learner class list and mocked mail sender.
     * Expected results:
     * FULL is absent from available classrooms. Assignment returns HTTP 400 with the class-full message. R remains WAITING_FOR_CLASS with unchanged history, enrollment remains WAITLIST, no ASSIGNED enrollment/access exists and no assignment email is sent.
     */
    @Test
    @DisplayName("IT_CLASSOPS_19 — Verify a waiting learner cannot gain access to a full class.")
    void IT_CLASSOPS_19() throws Exception {
        String u=freshLearner();long c=classroom();classSession(c);classEnroll(c,LEARNER);sql("update class_sections set capacity=1 where id=?",c);
         long e=inactive(c,u);sql("update class_enrollments set registration_status='WAITLIST' where id=?",e);
         long r=registration(u,program());eligible(r);long h=historyCount(r);
         for(var x:ok("GET",reqPath(r)+"/available-classrooms",STAFF,null))assertNotEquals(c,x.asLong());clearInvocations(mail);
         var response=call("PATCH",reqPath(r)+"/assign-class",STAFF,body("classroomId",c));status(response,400);
         assertTrue(response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("đủ chỗ"));
         assertEquals("WAITING_FOR_CLASS",regStatus(r));assertEquals(h,historyCount(r));assertEquals("WAITLIST",str("select registration_status from class_enrollments where id=?",e));
         assertEquals(0,n("select count(*) from class_enrollments where class_section_id=? and student_id=? and registration_status='ASSIGNED'",c,uid(u)));
         assertTrue(ok("GET","/api/student/classrooms/my-classrooms",u,null).isEmpty());verify(mail,never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    /**
     * Workbook: Classroom Operations!A41
     * Preconditions:
     * Learner is ASSIGNED to C1. E2 is an existing FULLY_PAID enrollment in C2. Both classrooms have SCHEDULED sessions on the same date 10:00-11:00.
     * Procedure:
     * 1. POST /api/staff/classrooms/enrollments/{E2}/assign.
     * 2. Compare E2 data and notification count.
     * 3. Read learner my-classrooms.
     * Expected results:
     * HTTP 409 with LEARNER_SCHEDULE conflict. E2 remains unchanged/FULLY_PAID, no notification is added and only C1 is visible to the learner.
     */
    @Test
    @DisplayName("IT_CLASSOPS_20 — Verify paid-enrollment assignment rejects a learner timetable conflict.")
    void IT_CLASSOPS_20() throws Exception {
        String u=freshLearner();long a=classroom(),b=classroom();classSession(a);classSession(b);classEnroll(a,u);long e=paid(b,u);
         String before=str("select to_jsonb(t)::text from class_enrollments t where id=?",e);long notices=n("select count(*) from app_notifications where user_id=?",uid(u));
         var response=call("POST","/api/staff/classrooms/enrollments/"+e+"/assign",STAFF,body("assignmentNote","Conflicting class"));status(response,409);
         assertTrue(response.getResponse().getContentAsString().contains("LEARNER_SCHEDULE"));assertEquals(before,str("select to_jsonb(t)::text from class_enrollments t where id=?",e));
         assertEquals(notices,n("select count(*) from app_notifications where user_id=?",uid(u)));var mine=ok("GET","/api/student/classrooms/my-classrooms",u,null);assertEquals(1,mine.size());assertEquals(a,mine.get(0).path("id").asLong());
    }

    /**
     * Workbook: Classroom Operations!A42
     * Preconditions:
     * Request R was just submitted and is SUBMITTED.
     * An assignable classroom C exists.
     * STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call GET /api/staff/enrollment-requests/{R}/available-classrooms.
     * 2. Call PATCH /api/staff/enrollment-requests/{R}/assign-class with {"classroomId":C}.
     * 3. Query the ClassEnrollment rows for the learner and C, the status history of R and the mail sender.
     * Expected results:
     * The available list is empty.
     * The assignment is rejected because the applicant has not passed the test yet.
     * The request is still SUBMITTED; no ClassEnrollment, no new history row and no e-mail exist.
     */
    @Test
    @DisplayName("IT_CLASSOPS_21 — Verify a request that has not passed the test cannot be assigned to a class.")
    void IT_CLASSOPS_21() throws Exception {
        String u=freshLearner();long c=classroom();classSession(c);long r=registration(u,program());assertTrue(ok("GET",reqPath(r)+"/available-classrooms",STAFF,null).isEmpty());clearInvocations(mail);rejected("PATCH",reqPath(r)+"/assign-class",STAFF,body("classroomId",c));assertEquals("SUBMITTED",regStatus(r));assertEquals(1,historyCount(r));assertEquals(0,n("select count(*) from class_enrollments where class_section_id=? and student_id=?",c,uid(u)));verify(mail,never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    /**
     * Workbook: Classroom Operations!A43
     * Preconditions:
     * Fresh learner with email notifications enabled; request R is SUBMITTED. Explicit test-only mail host/from values; JavaMailSender is mocked and throws on send.
     * Procedure:
     * 1. Submit a learner course-registration request.
     * 2. Configure JavaMailSender.send to throw MailSendException.
     * 3. PATCH /api/staff/enrollment-requests/{R}/schedule-test with a valid future appointment.
     * 4. Verify an actual send attempt, request status, history and learner request list.
     * Expected results:
     * The schedule request succeeds despite the simulated send failure. Exactly one send attempt occurs. R is TEST_SCHEDULED with two history entries and the learner sees TEST_SCHEDULED. Persistence is checked within the rolled-back integration transaction.
     */
    @Test
    @DisplayName("IT_CLASSOPS_22 — Verify scheduling a placement appointment succeeds when the email sender fails.")
    void IT_CLASSOPS_22() throws Exception {
        String u=freshLearner();long r=registration(u,program());clearInvocations(mail);
         doThrow(new org.springframework.mail.MailSendException("Retest simulated SMTP failure")).when(mail).send(any(jakarta.mail.internet.MimeMessage.class));
         schedule(r);verify(mail,times(1)).send(any(jakarta.mail.internet.MimeMessage.class));
         assertEquals("TEST_SCHEDULED",regStatus(r));assertEquals(2,historyCount(r));
         assertEquals("TEST_SCHEDULED",row(ok("GET","/api/student/course-enrollment-requests/my",u,null),"id",r).path("status").asText());
    }

    /**
     * Workbook: Classroom Operations!A45
     * Preconditions:
     * A published program P exists.
     * STAFF and MANAGER accounts exist; teacher T exists.
     * plannedStartDate is a future Monday and the weekdays [MONDAY, WEDNESDAY] include it; deliveryType VIRTUAL so no room is required; teacher T has no ClassSchedule in that slot.
     * Procedure:
     * 1. Login as STAFF and call POST /api/staff/classroom-proposals with courseOfferingId P, primaryTeacherId T, capacity 20, plannedStartDate, endDate, weekdays [MONDAY, WEDNESDAY], deliveryType VIRTUAL and session times.
     * 2. Call PATCH /api/staff/classroom-proposals/{proposalId}/submit.
     * 3. Login as MANAGER and call GET /api/manager/classroom-proposals/pending, then PATCH /api/manager/classroom-proposals/{proposalId}/approve.
     * 4. Query the ClassroomProposal, ClassSection and ClassSchedule rows and call GET /api/staff/classrooms/{newClassroomId}/sessions as STAFF.
     * Expected results:
     * The proposal moves from DRAFT to PENDING_APPROVAL to APPROVED.
     * One ClassSection for program P with teacher T and capacity 20 is created.
     * ClassSchedule rows exist only on Monday and Wednesday between the start and end dates at the planned time with status SCHEDULED.
     * The Staff sessions list returns the same sessions and no duplicate classroom is created.
     */
    @Test
    @DisplayName("IT_CLASSOPS_23 — Verify approving a proposal creates the classroom and its sessions.")
    void IT_CLASSOPS_23() throws Exception {
        long t=uid(otherTeacher()),before=n("select count(*) from class_sections"),q=proposal(t);assertEquals("DRAFT",proposalStatus(q));send(q);assertFalse(row(ok("GET","/api/manager/classroom-proposals/pending",MANAGER,null),"id",q).isMissingNode());var approved=ok("PATCH","/api/manager/classroom-proposals/"+q+"/approve",MANAGER,null);assertEquals("APPROVED",proposalStatus(q));long c=approved.path("approvedClassroomId").asLong();assertEquals(before+1,n("select count(*) from class_sections"));assertEquals(t,n("select primary_teacher_id from class_sections where id=?",c));assertEquals(20,n("select capacity from class_sections where id=?",c));var sessions=ok("GET","/api/staff/classrooms/"+c+"/sessions",STAFF,null);assertTrue(sessions.size()>0);assertEquals(n("select count(*) from class_schedules where class_section_id=?",c),sessions.size());for(var s:sessions){LocalDate d=LocalDate.parse(s.path("sessionDate").asText());assertTrue(d.getDayOfWeek()==DayOfWeek.MONDAY||d.getDayOfWeek()==DayOfWeek.WEDNESDAY);assertFalse(d.isBefore(monday())||d.isAfter(monday().plusWeeks(4)));assertEquals("SCHEDULED",s.path("status").asText());}
    }

    /**
     * Workbook: Classroom Operations!A47
     * Preconditions:
     * A proposal Q is PENDING_APPROVAL.
     * MANAGER account exists.
     * Procedure:
     * 1. Login as MANAGER and call PATCH /api/manager/classroom-proposals/{Q}/reject with {"reason":"Room not available"}.
     * 2. Query the ClassroomProposal Q and count ClassSection and ClassSchedule rows before and after the call.
     * Expected results:
     * Q has approvalStatus REJECTED, reviewNote "Room not available", reviewer and reviewedAt.
     * The ClassSection and ClassSchedule counts are unchanged.
     */
    @Test
    @DisplayName("IT_CLASSOPS_24 — Verify rejecting a proposal creates no classroom and stores the reason.")
    void IT_CLASSOPS_24() throws Exception {
        long q=proposal(uid(otherTeacher()));send(q);long classes=n("select count(*) from class_sections"),sessions=n("select count(*) from class_schedules");ok("PATCH","/api/manager/classroom-proposals/"+q+"/reject",MANAGER,body("reason","Room not available"));assertEquals("REJECTED",proposalStatus(q));assertEquals("Room not available",str("select review_note from classroom_proposals where id=?",q));assertEquals(uid(MANAGER),n("select reviewed_by_id from classroom_proposals where id=?",q));assertNotNull(str("select reviewed_at::text from classroom_proposals where id=?",q));assertEquals(classes,n("select count(*) from class_sections"));assertEquals(sessions,n("select count(*) from class_schedules"));
    }

    /**
     * Workbook: Classroom Operations — Create Classroom Proposal
     * Preconditions:
     * STAFF account exists; teacher T has no conflicting sessions; published program P exists.
     * Procedure:
     * 1. Login as STAFF and call POST /api/staff/classroom-proposals with all required fields.
     * 2. Query the ClassroomProposal row.
     * Expected results:
     * The proposal is stored with status DRAFT and the correct fields.
     */
    @Test
    @DisplayName("IT_CLASSOPS_25 — Verify a valid proposal is persisted with correct fields.")
    void IT_CLASSOPS_25() throws Exception {
        long t=uid(otherTeacher()),before=n("select count(*) from classroom_proposals"),q=proposal(t);assertEquals(before+1,n("select count(*) from classroom_proposals"));assertEquals("DRAFT",proposalStatus(q));assertEquals(t,n("select primary_teacher_id from classroom_proposals where id=?",q));assertEquals(20,n("select capacity from classroom_proposals where id=?",q));
    }

    /**
     * Workbook: Classroom Operations — View Classroom
     * Preconditions:
     * Classroom C exists; LEARNER is not enrolled in C.
     * Procedure:
     * 1. Login as LEARNER and call GET /api/student/classrooms/{C}/sessions.
     * Expected results:
     * The request is rejected (403 or 400) because the learner is not enrolled.
     */
    @Test
    @DisplayName("IT_CLASSOPS_26 — Verify a learner not enrolled in the class cannot view its sessions.")
    void IT_CLASSOPS_26() throws Exception {
        String u=freshLearner();long c=classroom();var r=call("GET","/api/student/classrooms/"+c+"/sessions",u,null);assertTrue(r.getResponse().getStatus()>=400,"Expected rejection: HTTP="+r.getResponse().getStatus());
    }

    /**
     * Workbook: Classroom Operations — Enroll Learner in Classroom
     * Preconditions:
     * Classroom C exists; learner U is already ASSIGNED to C.
     * Procedure:
     * 1. Login as STAFF and call POST /api/staff/classrooms/enrollments/{E}/assign again.
     * 2. Query the ClassEnrollment rows for U and C.
     * Expected results:
     * The request is rejected because the learner is already assigned.
     * Only one enrollment row exists.
     */
    @Test
    @DisplayName("IT_CLASSOPS_27 — Verify assigning an already-assigned learner is rejected.")
    void IT_CLASSOPS_27() throws Exception {
        String u=freshLearner();long c=classroom();classSession(c);long e=paid(c,u);assignPaid(e);assertEquals("ASSIGNED",str("select registration_status from class_enrollments where id=?",e));var r=call("POST","/api/staff/classrooms/enrollments/"+e+"/assign",STAFF,body("assignmentNote","Duplicate attempt"));assertTrue(r.getResponse().getStatus()>=400);assertEquals(1,n("select count(*) from class_enrollments where class_section_id=? and student_id=?",c,uid(u)));
    }

    /**
     * Workbook: Classroom Operations — View Class Registration Requests
     * Preconditions:
     * LEARNER account exists; STAFF has enrollment requests in the queue.
     * Procedure:
     * 1. Login as LEARNER and call GET /api/staff/enrollment-requests.
     * Expected results:
     * The request is rejected with 403 because the learner has no Staff role.
     */
    @Test
    @DisplayName("IT_CLASSOPS_28 — Verify a learner cannot access the Staff enrollment request queue.")
    void IT_CLASSOPS_28() throws Exception {
        String u=freshLearner();status(call("GET","/api/staff/enrollment-requests",u,null),403);
    }

    /**
     * Workbook: Classroom Operations — Record Applicant Test Result
     * Preconditions:
     * Request R is TEST_SCHEDULED.
     * STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call PATCH /api/staff/enrollment-requests/{R}/complete-test with {\"eligible\":true,\"placementLevel\":\"INTERMEDIATE\"}.
     * 2. Query the CourseRegistrationRequest R.
     * Expected results:
     * R has status WAITING_FOR_CLASS.
     * placementLevel is INTERMEDIATE.
     */
    @Test
    @DisplayName("IT_CLASSOPS_29 — Verify an eligible test result advances the request to WAITING_FOR_CLASS.")
    void IT_CLASSOPS_29() throws Exception {
        String u=freshLearner();long r=registration(u,program());schedule(r);ok("PATCH",reqPath(r)+"/complete-test",STAFF,body("eligible",true,"placementLevel","INTERMEDIATE"));assertEquals("WAITING_FOR_CLASS",regStatus(r));assertEquals("INTERMEDIATE",str("select placement_level from course_registration_requests where id=?",r));
    }

    /**
     * Workbook: Classroom Operations — Approve Classroom Proposal
     * Preconditions:
     * A proposal Q is already APPROVED.
     * MANAGER account exists.
     * Procedure:
     * 1. Login as MANAGER and call PATCH /api/manager/classroom-proposals/{Q}/approve.
     * Expected results:
     * The request is rejected because Q is already approved.
     */
    @Test
    @DisplayName("IT_CLASSOPS_30 — Verify approving an already-approved proposal is rejected.")
    void IT_CLASSOPS_30() throws Exception {
        long t=uid(otherTeacher()),q=proposal(t);send(q);ok("PATCH","/api/manager/classroom-proposals/"+q+"/approve",MANAGER,null);assertEquals("APPROVED",proposalStatus(q));rejected("PATCH","/api/manager/classroom-proposals/"+q+"/approve",MANAGER,null);assertEquals("APPROVED",proposalStatus(q));
    }

    /**
     * Workbook: Classroom Operations — Reject Classroom Proposal
     * Preconditions:
     * A proposal Q is already REJECTED.
     * MANAGER account exists.
     * Procedure:
     * 1. Login as MANAGER and call PATCH /api/manager/classroom-proposals/{Q}/reject with a reason.
     * Expected results:
     * The request is rejected because Q is already rejected.
     */
    @Test
    @DisplayName("IT_CLASSOPS_31 — Verify rejecting an already-rejected proposal is rejected.")
    void IT_CLASSOPS_31() throws Exception {
        long q=proposal(uid(otherTeacher()));send(q);ok("PATCH","/api/manager/classroom-proposals/"+q+"/reject",MANAGER,body("reason","First rejection"));assertEquals("REJECTED",proposalStatus(q));rejected("PATCH","/api/manager/classroom-proposals/"+q+"/reject",MANAGER,body("reason","Second rejection"));assertEquals("REJECTED",proposalStatus(q));
    }
}
