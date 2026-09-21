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

/** Canonical workbook scenarios for Teaching Operations. */
@EnglishLabIT
@RecordApplicationEvents
@Tag("workbook")
public class WorkbookTeachingIT extends WorkbookTestSupport {

    /**
     * Workbook: Teaching Operations!A15
     * Preconditions:
     * Classroom C is taught by TEACHER; session S is SCHEDULED in the future.
     * A PENDING RESCHEDULE_SESSION request exists for S.
     * STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call POST /api/staff/requests/{requestId}/reject with {"reviewNote":"Room is booked"}.
     * 2. Query the ClassroomChangeRequest, ClassSchedule S and the AppNotification rows of TEACHER.
     * Expected results:
     * The request status is REJECTED with reviewNote and reviewer.
     * TEACHER has a CLASSROOM_CHANGE_REQUEST_REJECTED notification whose body is "Room is booked".
     * Session S keeps its original date and time.
     */
    @Test
    @DisplayName("IT_TEACHING_04 — Verify rejecting a reschedule request keeps the session and notifies the teacher.")
    void IT_TEACHING_04() throws Exception {
        long c=classroom(),s=classSession(c),r=change(c,s);String before=str("select to_jsonb(t)::text from class_schedules t where id=?",s);ok("POST","/api/staff/requests/"+r+"/reject",STAFF,body("reviewNote","Room is booked"));assertEquals("REJECTED",str("select status from classroom_change_requests where id=?",r));assertEquals("Room is booked",str("select review_note from classroom_change_requests where id=?",r));assertEquals(uid(STAFF),n("select reviewer_id from classroom_change_requests where id=?",r));assertEquals(before,str("select to_jsonb(t)::text from class_schedules t where id=?",s));assertTrue(n("select count(*) from app_notifications where user_id=? and type='CLASSROOM_CHANGE_REQUEST_REJECTED' and body='Room is booked'",uid(TEACHER))>=1);
    }

    /**
     * Workbook: Teaching Operations!A16
     * Preconditions:
     * Session S has a PENDING RESCHEDULE_SESSION request created by TEACHER.
     * Procedure:
     * 1. Login as TEACHER and call POST /api/teacher/classrooms/requests again for S with the same requestType.
     * 2. Query the PENDING ClassroomChangeRequest rows for S and the AppNotification rows of the training staff.
     * Expected results:
     * The request is rejected with the message that a pending request of the same type already exists.
     * Exactly one PENDING request exists for S and no second pending-request notification is created.
     */
    @Test
    @DisplayName("IT_TEACHING_05 — Verify a second pending reschedule request for the same session is rejected.")
    void IT_TEACHING_05() throws Exception {
        long c=classroom(),s=classSession(c);change(c,s);long before=n("select count(*) from app_notifications");rejected("POST","/api/teacher/classrooms/requests",TEACHER,body("requestType","RESCHEDULE_SESSION","classSectionId",c,"targetSessionId",s,"newValuesJson",body("sessionDate",java.time.LocalDate.now().plusDays(12).toString(),"startTime","13:00","endTime","14:00"),"reason","Second schedule request"));assertEquals(1,n("select count(*) from classroom_change_requests where target_session_id=? and status='PENDING'",s));assertEquals(before,n("select count(*) from app_notifications"));
    }

    /**
     * Workbook: Teaching Operations!A17
     * Preconditions:
     * Classroom C is taught by T1; teacher T2 is not assigned to C.
     * STAFF account exists.
     * Procedure:
     * 1. Login as T2 and call GET /api/teacher/classrooms/assigned and GET /api/teacher/classrooms/{C}/sessions.
     * 2. Login as STAFF and call POST /api/staff/classrooms/{C}/teachers/{T2}/assign.
     * 3. As T2 repeat the first request.
     * Expected results:
     * Before the assignment C is not listed and the sessions request is rejected with the message that T2 is not assigned to the class.
     * A new active ClassroomTeacherAssignment exists for T2.
     * After the assignment C is listed and the sessions of C are returned.
     */
    @Test
    @DisplayName("IT_TEACHING_06 — Verify a teacher sees the schedule of a class only after being assigned to it.")
    void IT_TEACHING_06() throws Exception {
        long c=classroom();classSession(c);String t=otherTeacher();assertTrue(row(ok("GET","/api/teacher/classrooms/assigned",t,null),"id",c).isMissingNode());rejected("GET","/api/teacher/classrooms/"+c+"/sessions",t,null);ok("POST","/api/staff/classrooms/"+c+"/teachers/"+uid(t)+"/assign",STAFF,null);assertEquals(uid(t),n("select primary_teacher_id from class_sections where id=?",c));assertFalse(row(ok("GET","/api/teacher/classrooms/assigned",t,null),"id",c).isMissingNode());assertEquals(1,ok("GET","/api/teacher/classrooms/"+c+"/sessions",t,null).size());
    }

    /**
     * Workbook: Teaching Operations!A19
     * Preconditions:
     * Teacher owns classroom C/session S. Learner has an existing FULLY_PAID enrollment E with gradebook fields and has no attendance record.
     * Procedure:
     * 1. GET /api/teacher/classrooms/sessions/{S}/attendance and verify L is absent.
     * 2. POST /api/staff/classrooms/enrollments/{E}/assign.
     * 3. Read the teacher attendance sheet and query stored attendance rows.
     * Expected results:
     * Learner appears in the sheet with null attendance status after assignment. No attendance record has been inserted for L/S.
     */
    @Test
    @DisplayName("IT_TEACHING_07 — Verify a newly assigned paid learner appears in the teacher attendance sheet.")
    void IT_TEACHING_07() throws Exception {
        String u=freshLearner();long c=classroom(),s=classSession(c),e=paid(c,u);
         assertTrue(row(ok("GET","/api/teacher/classrooms/sessions/"+s+"/attendance",TEACHER,null),"studentId",uid(u)).isMissingNode());assignPaid(e);
         var record=row(ok("GET","/api/teacher/classrooms/sessions/"+s+"/attendance",TEACHER,null),"studentId",uid(u));assertFalse(record.isMissingNode());assertTrue(record.path("status").isNull());
         assertEquals(0,n("select count(*) from classroom_attendance_records where session_id=? and student_id=?",s,uid(u)));
    }

    /**
     * Workbook: Teaching Operations!A20
     * Preconditions:
     * Classroom C is taught by TEACHER; LEARNER is enrolled in C and has a saved PRESENT record for session S.
     * STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call POST /api/staff/classrooms/{C}/students/{L}/remove.
     * 2. Query ClassEnrollment(L, C).
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/attendance/me.
     * 4. Login as TEACHER and call GET /api/teacher/classrooms/sessions/{S}/attendance.
     * Expected results:
     * ClassEnrollment(L, C) is CANCELLED.
     * The learner request is rejected with the message that the learner has no access to the class.
     * The teacher sheet still returns the saved PRESENT record of L.
     */
    @Test
    @DisplayName("IT_TEACHING_08 — Verify a removed learner loses attendance access while the saved record stays visible to the teacher.")
    void IT_TEACHING_08() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long s=classSession(c);attended(c,s,"PRESENT");ok("POST","/api/staff/classrooms/"+c+"/students/"+uid(LEARNER)+"/remove",STAFF,null);assertEquals("CANCELLED",str("select registration_status from class_enrollments where class_section_id=? and student_id=?",c,uid(LEARNER)));rejected("GET","/api/student/classrooms/"+c+"/attendance/me",LEARNER,null);assertEquals("PRESENT",row(ok("GET","/api/teacher/classrooms/sessions/"+s+"/attendance",TEACHER,null),"studentId",uid(LEARNER)).path("status").asText());
    }

    /**
     * Workbook: Teaching Operations!A23
     * Preconditions:
     * Classroom C is taught by T1; teacher T2 is not assigned to C.
     * LEARNER is enrolled in C; session S belongs to C.
     * Procedure:
     * 1. Login as T2 and call POST /api/teacher/classrooms/attendance for session S and learner L.
     * 2. Query the ClassroomAttendance rows of S.
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/attendance/me.
     * Expected results:
     * The request is rejected with the message that T2 is not assigned to the class.
     * No ClassroomAttendance row exists for S.
     * The learner list has no entry for S.
     */
    @Test
    @DisplayName("IT_TEACHING_10 — Verify a teacher who is not assigned to the class cannot record attendance.")
    void IT_TEACHING_10() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long s=classSession(c);
          var r=rejected("POST","/api/teacher/classrooms/attendance",otherTeacher(),body("sessionId",s,"records",List.of(Map.of("studentId",uid(LEARNER),"status","PRESENT"))));
          System.out.println("RETEST_RESPONSE "+caseId+" "+r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
          assertEquals(0,n("select count(*) from classroom_attendance_records where session_id=?",s));
          assertTrue(row(ok("GET","/api/student/classrooms/"+c+"/attendance/me",LEARNER,null),"sessionId",s).isMissingNode());
    }

    /**
     * Workbook: Teaching Operations!A26
     * Preconditions:
     * A ClassroomAttendance(S, L) with status PRESENT exists in classroom C taught by TEACHER.
     * Procedure:
     * 1. Login as TEACHER and call POST /api/teacher/classrooms/attendance for S with status ABSENT for L.
     * 2. Query the ClassroomAttendance rows of S.
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/attendance/me.
     * Expected results:
     * Exactly one ClassroomAttendance row exists for S and L and its status is ABSENT.
     * The learner list returns one row for S with status ABSENT.
     */
    @Test
    @DisplayName("IT_TEACHING_12 — Verify saving attendance again updates the existing record.")
    void IT_TEACHING_12() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long s=classSession(c),a=attended(c,s,"PRESENT");attended(c,s,"ABSENT");assertEquals(1,n("select count(*) from classroom_attendance_records where session_id=? and student_id=?",s,uid(LEARNER)));assertEquals("ABSENT",str("select status from classroom_attendance_records where id=?",a));assertEquals("ABSENT",row(ok("GET","/api/student/classrooms/"+c+"/attendance/me",LEARNER,null),"sessionId",s).path("status").asText());
    }

    /**
     * Workbook: Teaching Operations!A27
     * Preconditions:
     * Classroom C is taught by TEACHER; LEARNER has ClassroomAttendance A for session S with status ABSENT.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/attendance/{A}/disputes with {"reason":"I attended via the online link"}.
     * 2. Login as TEACHER and call GET /api/teacher/attendance-disputes/pending.
     * 3. Call POST /api/teacher/attendance-disputes/{disputeId}/review with {"status":"APPROVED","attendanceStatus":"PRESENT","reviewNote":"Confirmed"}.
     * 4. Query the ClassroomAttendanceDispute and ClassroomAttendance A; as LEARNER call GET /api/student/attendance/disputes and GET /api/student/classrooms/{C}/attendance/me.
     * Expected results:
     * A dispute with status PENDING is created for A and listed in the teacher pending queue.
     * After the review the dispute is APPROVED with reviewer and note.
     * ClassroomAttendance A is updated to PRESENT.
     * The learner sees the dispute as APPROVED and the attendance as PRESENT.
     */
    @Test
    @DisplayName("IT_TEACHING_13 — Verify approving an attendance dispute corrects the attendance record.")
    void IT_TEACHING_13() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long s=classSession(c),a=attended(c,s,"ABSENT"),d=dispute(a);assertFalse(row(ok("GET","/api/teacher/attendance-disputes/pending",TEACHER,null),"id",d).isMissingNode());ok("POST","/api/teacher/attendance-disputes/"+d+"/review",TEACHER,body("status","APPROVED","attendanceStatus","PRESENT","reviewNote","Confirmed"));assertEquals("APPROVED",str("select dispute_status from classroom_attendance_records where id=?",a));assertEquals("PRESENT",str("select status from classroom_attendance_records where id=?",a));assertEquals(uid(TEACHER),n("select dispute_reviewed_by_id from classroom_attendance_records where id=?",a));assertEquals("APPROVED",row(ok("GET","/api/student/attendance/disputes",LEARNER,null),"id",d).path("status").asText());assertEquals("PRESENT",row(ok("GET","/api/student/classrooms/"+c+"/attendance/me",LEARNER,null),"sessionId",s).path("status").asText());
    }

    /**
     * Workbook: Teaching Operations!A28
     * Preconditions:
     * A PENDING dispute D exists for ClassroomAttendance A (ABSENT) of classroom C taught by TEACHER.
     * Procedure:
     * 1. Login as TEACHER and call POST /api/teacher/attendance-disputes/{D}/review with {"status":"REJECTED"} and no reviewNote.
     * 2. Query the ClassroomAttendanceDispute D and the ClassroomAttendance A.
     * Expected results:
     * The review is rejected with the message that a reason is required.
     * D is still PENDING and A is still ABSENT.
     */
    @Test
    @DisplayName("IT_TEACHING_14 — Verify rejecting an attendance dispute without a note is refused.")
    void IT_TEACHING_14() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long a=attended(c,classSession(c),"ABSENT"),d=dispute(a);rejected("POST","/api/teacher/attendance-disputes/"+d+"/review",TEACHER,body("status","REJECTED"));assertEquals("PENDING",str("select dispute_status from classroom_attendance_records where id=?",a));assertEquals("ABSENT",str("select status from classroom_attendance_records where id=?",a));
    }

    /**
     * Workbook: Teaching Operations!A29
     * Preconditions:
     * LEARNER already has a PENDING dispute for ClassroomAttendance A.
     * TEACHER account exists.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/attendance/{A}/disputes again.
     * 2. Query the dispute rows of A and call GET /api/teacher/attendance-disputes/pending as TEACHER.
     * Expected results:
     * The request is rejected with the message that a pending dispute already exists.
     * Exactly one dispute row exists for A and the teacher queue lists it once.
     */
    @Test
    @DisplayName("IT_TEACHING_15 — Verify a second pending dispute for the same attendance record is rejected.")
    void IT_TEACHING_15() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long a=attended(c,classSession(c),"ABSENT"),d=dispute(a);rejected("POST","/api/student/attendance/"+a+"/disputes",LEARNER,body("reason","I attended via the online link"));assertEquals(1,n("select count(*) from classroom_attendance_records where id=? and dispute_status='PENDING'",a));long count=0;for(var x:ok("GET","/api/teacher/attendance-disputes/pending",TEACHER,null))if(x.path("id").asLong()==d)count++;assertEquals(1,count);
    }

    /**
     * Workbook: Teaching Operations!A33
     * Preconditions:
     * Classroom C is taught by T1; teacher T2 is not assigned to C; LEARNER is enrolled in C.
     * Procedure:
     * 1. Login as T2 and call POST /api/teacher/classrooms/{C}/homework with a valid body.
     * 2. Query the ClassroomHomework rows of C and, as LEARNER, call GET /api/student/classrooms/{C}/homework.
     * Expected results:
     * The request is rejected with the message that T2 is not assigned to the class.
     * No row is created and the learner list is unchanged.
     */
    @Test
    @DisplayName("IT_TEACHING_18 — Verify a teacher who is not assigned to the class cannot create homework.")
    void IT_TEACHING_18() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);String t=otherTeacher();var before=ok("GET","/api/student/classrooms/"+c+"/homework",LEARNER,null);rejected("POST","/api/teacher/classrooms/"+c+"/homework",t,hwBody("OPEN",java.time.LocalDateTime.now().plusDays(3).toString()));assertEquals(0,n("select count(*) from classroom_homework where class_section_id=?",c));assertEquals(before,ok("GET","/api/student/classrooms/"+c+"/homework",LEARNER,null));
    }

    /**
     * Workbook: Teaching Operations!A35
     * Preconditions:
     * Homework H is OPEN in classroom C with deadline D1; LEARNER is enrolled in C.
     * TEACHER account exists.
     * Procedure:
     * 1. Login as TEACHER and call PUT /api/teacher/classrooms/homework/{H} with a later deadline D2.
     * 2. Query the ClassroomHomework row H.
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/homework.
     * Expected results:
     * The same ClassroomHomework row now holds deadline D2.
     * The learner sees homework H with deadline D2.
     */
    @Test
    @DisplayName("IT_TEACHING_19 — Verify extending a homework deadline updates the learner homework list.")
    void IT_TEACHING_19() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long h=homework(c,"OPEN");String deadline=java.time.LocalDateTime.now().plusDays(6).withNano(0).toString();ok("PUT","/api/teacher/classrooms/homework/"+h,TEACHER,hwBody("OPEN",deadline));em.flush();assertEquals(java.time.LocalDateTime.parse(deadline),db.queryForObject("select deadline from classroom_homework where id=?",java.time.LocalDateTime.class,h));assertEquals(java.time.LocalDateTime.parse(deadline),java.time.LocalDateTime.parse(row(ok("GET","/api/student/classrooms/"+c+"/homework",LEARNER,null),"id",h).path("deadline").asText()));
    }

    /**
     * Workbook: Teaching Operations!A37
     * Preconditions:
     * Homework H is OPEN in classroom C; LEARNER is enrolled in C and has not submitted.
     * TEACHER account exists.
     * Procedure:
     * 1. Login as TEACHER and call DELETE /api/teacher/classrooms/homework/{H}.
     * 2. Login as LEARNER and call GET /api/student/classrooms/my-homework.
     * 3. As LEARNER call POST /api/student/classrooms/homework/{H}/submit with a text answer.
     * 4. Query the ClassroomHomework and ClassroomHomeworkSubmission rows for H.
     * Expected results:
     * The ClassroomHomework row is removed.
     * H is not in the learner list.
     * The submit request is rejected with the message that the homework was not found.
     * No submission row exists for H.
     */
    @Test
    @DisplayName("IT_TEACHING_20 — Verify deleting a homework removes it and blocks later submissions.")
    void IT_TEACHING_20() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long h=homework(c,"OPEN");ok("DELETE","/api/teacher/classrooms/homework/"+h,TEACHER,null);assertEquals(0,n("select count(*) from classroom_homework where id=?",h));assertTrue(row(ok("GET","/api/student/classrooms/my-homework",LEARNER,null),"id",h).isMissingNode());rejected("POST","/api/student/classrooms/homework/"+h+"/submit",LEARNER,body("textAnswer","Round1 answer"));assertEquals(0,n("select count(*) from classroom_homework_submissions where homework_id=?",h));
    }

    /**
     * Workbook: Teaching Operations!A39
     * Preconditions:
     * Classroom C is taught by TEACHER; LEARNER is enrolled in C.
     * Homework H (teacher grading, maxScore 10) is OPEN and LEARNER has a SUBMITTED submission for it.
     * Procedure:
     * 1. Login as TEACHER and call POST /api/teacher/classrooms/homework/{H}/students/{L}/grade with {"score":8.5,"teacherFeedback":"Good"}.
     * 2. Query the ClassroomHomeworkSubmission and the ClassroomGradebookEntry of L.
     * 3. Call GET /api/teacher/classrooms/{C}/gradebook.
     * 4. Login as LEARNER and call GET /api/student/classrooms/{C}/gradebook/me.
     * Expected results:
     * The submission is GRADED with score 8.5 and the feedback.
     * The ClassroomGradebookEntry of L has homeworkScore 8.5 and status GRADED.
     * The teacher gradebook shows the same score.
     * The learner gradebook returns no data because the entry is not PUBLISHED.
     */
    @Test
    @DisplayName("IT_TEACHING_21 — Verify grading a submission updates the gradebook entry while it stays hidden from the learner.")
    void IT_TEACHING_21() throws Exception {
        long h=gradedFixture(8.5),c=n("select class_section_id from classroom_homework where id=?",h);assertEquals("GRADED",str("select status from classroom_homework_submissions where homework_id=?",h));assertEquals(8.5,Double.parseDouble(str("select score::text from classroom_homework_submissions where homework_id=?",h)));assertEquals(8.5,db.queryForObject("select homework_score from class_enrollments where class_section_id=? and student_id=?",Double.class,c,uid(LEARNER)));assertEquals("GRADED",str("select gradebook_status from class_enrollments where class_section_id=? and student_id=?",c,uid(LEARNER)));assertEquals(8.5,row(ok("GET","/api/teacher/classrooms/"+c+"/gradebook",TEACHER,null),"studentId",uid(LEARNER)).path("homeworkAverage").asDouble());assertTrue(ok("GET","/api/student/classrooms/"+c+"/gradebook/me",LEARNER,null).isNull());
    }

    /**
     * Workbook: Teaching Operations!A40
     * Preconditions:
     * LEARNER has a SUBMITTED submission for homework H (maxScore 10) in classroom C taught by TEACHER.
     * Procedure:
     * 1. Login as TEACHER and call POST /api/teacher/classrooms/homework/{H}/students/{L}/grade with {"score":9,"teacherFeedback":"Well done"}.
     * 2. Query the ClassroomHomeworkSubmission of L for H.
     * 3. Login as LEARNER and call GET /api/student/classrooms/my-homework.
     * Expected results:
     * The submission is GRADED with score 9 and the feedback.
     * The learner list shows homework H with score 9, feedback "Well done" and status GRADED.
     */
    @Test
    @DisplayName("IT_TEACHING_22 — Verify a graded score and feedback appear in the learner homework list.")
    void IT_TEACHING_22() throws Exception {
        long h=gradedFixture(9);assertEquals("GRADED",str("select status from classroom_homework_submissions where homework_id=?",h));assertEquals("Well done",str("select teacher_feedback from classroom_homework_submissions where homework_id=?",h));var row=row(ok("GET","/api/student/classrooms/my-homework",LEARNER,null),"id",h);System.out.println("ROUND1_HOMEWORK "+row);assertTrue(row.toString().contains("Well done"));assertTrue(row.toString().contains("GRADED"));
    }

    /**
     * Workbook: Teaching Operations!A41
     * Preconditions:
     * LEARNER has a SUBMITTED submission for homework H with maxScore 10.
     * TEACHER account exists.
     * Procedure:
     * 1. Login as TEACHER and call POST /api/teacher/classrooms/homework/{H}/students/{L}/grade with {"score":15}.
     * 2. Query the ClassroomHomeworkSubmission and the ClassroomGradebookEntry of L.
     * Expected results:
     * The request is rejected because the score is above maxScore.
     * The submission is still SUBMITTED and the gradebook homeworkScore is unchanged.
     */
    @Test
    @DisplayName("IT_TEACHING_23 — Verify a score above the maximum is rejected.")
    void IT_TEACHING_23() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long h=homework(c,"OPEN");submit(h,"Round1 submitted answer");String before=str("select homework_score::text from class_enrollments where class_section_id=? and student_id=?",c,uid(LEARNER));rejected("POST","/api/teacher/classrooms/homework/"+h+"/students/"+uid(LEARNER)+"/grade",TEACHER,body("score",15));assertEquals("SUBMITTED",str("select status from classroom_homework_submissions where homework_id=?",h));assertEquals(before,str("select homework_score::text from class_enrollments where class_section_id=? and student_id=?",c,uid(LEARNER)));
    }

    /**
     * Workbook: Teaching Operations!A43
     * Preconditions:
     * Classroom C is taught by TEACHER; the ClassroomGradebookEntry of LEARNER has homeworkScore 8.5 and status GRADED.
     * Procedure:
     * 1. Login as TEACHER and call POST /api/teacher/classrooms/{C}/gradebook/publish.
     * 2. Query the ClassroomGradebookEntry rows of C.
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/gradebook/me.
     * Expected results:
     * Every entry of C has status PUBLISHED.
     * The learner receives the gradebook with homeworkScore 8.5.
     */
    @Test
    @DisplayName("IT_TEACHING_24 — Verify publishing the gradebook makes the graded scores visible to the learner.")
    void IT_TEACHING_24() throws Exception {
        long h=gradedFixture(8.5),c=n("select class_section_id from classroom_homework where id=?",h);ok("POST","/api/teacher/classrooms/"+c+"/gradebook/publish",TEACHER,null);assertEquals(0,n("select count(*) from class_enrollments where class_section_id=? and gradebook_status<>'PUBLISHED'",c));assertEquals(8.5,ok("GET","/api/student/classrooms/"+c+"/gradebook/me",LEARNER,null).path("homeworkAverage").asDouble());
    }

    /**
     * Workbook: Teaching Operations!A45
     * Preconditions:
     * A ClassroomMaterial M exists in classroom C taught by TEACHER; LEARNER is enrolled in C.
     * Procedure:
     * 1. Login as TEACHER and call DELETE /api/teacher/classrooms/materials/{M}.
     * 2. Query the ClassroomMaterial rows of C.
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/materials.
     * 4. As TEACHER call GET /api/teacher/classrooms/{C}/materials.
     * Expected results:
     * The ClassroomMaterial row M is removed.
     * M is not in the learner list and not in the teacher list.
     */
    @Test
    @DisplayName("IT_TEACHING_25 — Verify deleting a material removes it from the teacher and learner lists.")
    void IT_TEACHING_25() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long m=material(c);ok("DELETE","/api/teacher/classrooms/materials/"+m,TEACHER,null);assertEquals(0,n("select count(*) from class_resources where id=?",m));for(String role:List.of("student","teacher"))assertTrue(row(ok("GET","/api/"+role+"/classrooms/"+c+"/materials",role.equals("student")?LEARNER:TEACHER,null),"id",m).isMissingNode());
    }

    /**
     * Workbook: Teaching Operations!A46
     * Preconditions:
     * A ClassroomMaterial M exists in classroom C taught by T1; teacher T2 is not assigned to C; LEARNER is enrolled in C.
     * Procedure:
     * 1. Login as T2 and call DELETE /api/teacher/classrooms/materials/{M}.
     * 2. Query the ClassroomMaterial rows of C.
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/materials.
     * Expected results:
     * The request is rejected with the message that T2 is not assigned to the class.
     * The ClassroomMaterial row M still exists.
     * M is still in the learner list.
     */
    @Test
    @DisplayName("IT_TEACHING_26 — Verify a teacher who is not assigned to the class cannot delete a material.")
    void IT_TEACHING_26() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long m=material(c);rejected("DELETE","/api/teacher/classrooms/materials/"+m,otherTeacher(),null);assertEquals(1,n("select count(*) from class_resources where id=?",m));assertFalse(row(ok("GET","/api/student/classrooms/"+c+"/materials",LEARNER,null),"id",m).isMissingNode());
    }

    /**
     * Workbook: Teaching Operations!A49
     * Preconditions:
     * Test data (seeded in the database): ClassSection C is VIRTUAL with googleMeetStatus READY and googleMeetUrl "https://meet.google.com/abc-defg-hij"; the Google Meet service is replaced by a stub so no call to Google is made.
     * Session S is OPEN (status changed by the teacher open action).
     * TEACHER has an active ClassroomTeacherAssignment for C; LEARNER has a ClassEnrollment(C) with class access.
     * Procedure:
     * 1. Login as TEACHER and call POST /api/teacher/classrooms/sessions/{S}/close.
     * 2. Query the ClassSchedule S.
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/sessions.
     * Expected results:
     * The stored ClassSchedule status of S is COMPLETED.
     * The learner sees S as COMPLETED.
     */
    @Test
    @DisplayName("IT_TEACHING_28 — Verify closing an online session marks it COMPLETED in the learner timetable.")
    void IT_TEACHING_28() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long s=classSession(c);ok("POST","/api/teacher/classrooms/sessions/"+s+"/open",TEACHER,null);ok("POST","/api/teacher/classrooms/sessions/"+s+"/close",TEACHER,null);assertEquals("COMPLETED",str("select status from class_schedules where id=?",s));assertEquals("COMPLETED",row(ok("GET","/api/student/classrooms/"+c+"/sessions",LEARNER,null),"id",s).path("status").asText());
    }

    /**
     * Workbook: Teaching Operations!A50
     * Preconditions:
     * Test data (seeded in the database): ClassSection C is VIRTUAL with googleMeetStatus NOT_CREATED and no googleMeetUrl; the Google Meet service is replaced by a stub.
     * ClassSchedule S belongs to C, has sessionDate today, a start time within the next hour, status SCHEDULED and no delivery-mode override, so it is an online session.
     * TEACHER has an active ClassroomTeacherAssignment for C.
     * Procedure:
     * 1. Login as TEACHER and call POST /api/teacher/classrooms/sessions/{S}/open.
     * 2. Query the ClassSchedule S.
     * Expected results:
     * The request is rejected with 409 and the message that Staff has not created the Google Meet link.
     * S is still SCHEDULED.
     */
    @Test
    @DisplayName("IT_TEACHING_29 — Verify opening an online session is refused when the Google Meet link is not ready.")
    void IT_TEACHING_29() throws Exception {
        long c=classroom(),s=classSession(c);sql("update class_sections set google_meet_status='NOT_CREATED',google_meet_url=null where id=?",c);status(call("POST","/api/teacher/classrooms/sessions/"+s+"/open",TEACHER,null),409);assertEquals("SCHEDULED",str("select status from class_schedules where id=?",s));
    }

    /**
     * Workbook: Teaching Operations!A51
     * Preconditions:
     * Test data (seeded in the database): ClassSection C is VIRTUAL with googleMeetStatus READY and googleMeetUrl "https://meet.google.com/abc-defg-hij"; the Google Meet service is replaced by a stub so no call to Google is made.
     * Test data: session S2 belongs to C and has deliveryModeOverride OFFLINE (with a room), status SCHEDULED.
     * TEACHER has an active ClassroomTeacherAssignment for C.
     * Procedure:
     * 1. Login as TEACHER and call POST /api/teacher/classrooms/sessions/{S2}/open.
     * 2. Query the ClassSchedule S2.
     * Expected results:
     * The request is rejected with the message that only online sessions can open a virtual room.
     * S2 status is unchanged.
     */
    @Test
    @DisplayName("IT_TEACHING_30 — Verify an offline session cannot be opened as an online room.")
    void IT_TEACHING_30() throws Exception {
        long c=classroom(),s=classSession(c);sql("update class_schedules set delivery_mode_override='OFFLINE',room_id=(select id from rooms limit 1) where id=?",s);rejected("POST","/api/teacher/classrooms/sessions/"+s+"/open",TEACHER,null);assertEquals("SCHEDULED",str("select status from class_schedules where id=?",s));
    }

    /**
     * Workbook: Teaching Operations!A52
     * Preconditions:
     * VIRTUAL classroom with READY Google Meet URL and future SCHEDULED session. T2 is neither the assigned classroom teacher nor the session teacher. No Google call is made.
     * Procedure:
     * 1. POST /api/teacher/classrooms/sessions/{S}/open as unassigned teacher T2.
     * 2. Inspect the response and stored session status.
     * Expected results:
     * HTTP 400 contains the message that the teacher is not assigned to the classroom. S remains SCHEDULED.
     */
    @Test
    @DisplayName("IT_TEACHING_31 — Verify an unassigned teacher cannot open an online session (current HTTP contract).")
    void IT_TEACHING_31() throws Exception {
        long c=classroom(),s=classSession(c);var response=call("POST","/api/teacher/classrooms/sessions/"+s+"/open",otherTeacher(),null);System.out.println("RETEST_RESPONSE "+caseId+" "+response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));status(response,400);assertTrue(response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("phân công"));assertEquals("SCHEDULED",str("select status from class_schedules where id=?",s));
    }

    /**
     * Workbook: Teaching Operations — Update Homework
     * Preconditions:
     * Homework H is OPEN in classroom C taught by TEACHER; teacher T2 is not assigned to C.
     * Procedure:
     * 1. Login as T2 and call PUT /api/teacher/classrooms/homework/{H} with a new deadline.
     * 2. Query the ClassroomHomework H.
     * Expected results:
     * The request is rejected because T2 is not assigned to the class.
     * The homework deadline is unchanged.
     */
    @Test
    @DisplayName("IT_TEACHING_32 — Verify an unassigned teacher cannot update homework.")
    void IT_TEACHING_32() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long h=homework(c,"OPEN");String before=str("select deadline::text from classroom_homework where id=?",h);String t2=otherTeacher();rejected("PUT","/api/teacher/classrooms/homework/"+h,t2,hwBody("OPEN",java.time.LocalDateTime.now().plusDays(10).withNano(0).toString()));assertEquals(before,str("select deadline::text from classroom_homework where id=?",h));
    }

    /**
     * Workbook: Teaching Operations — Delete Homework
     * Preconditions:
     * Homework H is OPEN in classroom C taught by TEACHER; teacher T2 is not assigned to C.
     * Procedure:
     * 1. Login as T2 and call DELETE /api/teacher/classrooms/homework/{H}.
     * 2. Query the ClassroomHomework H.
     * Expected results:
     * The request is rejected because T2 is not assigned to the class.
     * The homework row still exists.
     */
    @Test
    @DisplayName("IT_TEACHING_33 — Verify an unassigned teacher cannot delete homework.")
    void IT_TEACHING_33() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);long h=homework(c,"OPEN");String t2=otherTeacher();rejected("DELETE","/api/teacher/classrooms/homework/"+h,t2,null);assertEquals(1,n("select count(*) from classroom_homework where id=?",h));
    }

    /**
     * Workbook: Teaching Operations — Publish Gradebook
     * Preconditions:
     * Classroom C is taught by TEACHER; LEARNER is enrolled in C with gradebook status PENDING (no grading done).
     * Procedure:
     * 1. Login as TEACHER and call POST /api/teacher/classrooms/{C}/gradebook/publish.
     * 2. Query the ClassroomGradebookEntry rows of C.
     * 3. Login as LEARNER and call GET /api/student/classrooms/{C}/gradebook/me.
     * Expected results:
     * The publish succeeds and all entries become PUBLISHED.
     * The learner can see their gradebook (even with null scores).
     */
    @Test
    @DisplayName("IT_TEACHING_34 — Verify publishing the gradebook when no homework is graded still marks entries PUBLISHED.")
    void IT_TEACHING_34() throws Exception {
        long c=classroom();classEnroll(c,LEARNER);ok("POST","/api/teacher/classrooms/"+c+"/gradebook/publish",TEACHER,null);assertEquals(0,n("select count(*) from class_enrollments where class_section_id=? and gradebook_status<>'PUBLISHED'",c));ok("GET","/api/student/classrooms/"+c+"/gradebook/me",LEARNER,null);
    }
}
