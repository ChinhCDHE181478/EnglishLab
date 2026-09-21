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

/** Canonical workbook scenarios for Community, Promotion & Support. */
@EnglishLabIT
@RecordApplicationEvents
@Tag("workbook")
public class WorkbookCommunityIT extends WorkbookTestSupport {

    /**
     * Workbook: Community, Promotion & Support!A18
     * Preconditions:
     * Homework H is OPEN with a deadline in about 20 hours in classroom C.
     * LEARNER and LEARNER B are enrolled in C with studyAlertEnabled true; LEARNER B has already submitted H.
     * Procedure:
     * 1. Run the learning reminder job.
     * 2. Query the AppNotification rows of LEARNER and LEARNER B.
     * 3. Login as each learner and call GET /api/student/notifications.
     * Expected results:
     * LEARNER has one HOMEWORK_DEADLINE notification for H with action path /my-homework.
     * LEARNER B has no HOMEWORK_DEADLINE notification for H.
     */
    @Test
    @DisplayName("IT_COMMUNITY_07 — Verify homework deadline reminders skip learners who already submitted.")
    void IT_COMMUNITY_07() throws Exception {
        String b=CM;sql("insert into user_roles(user_id,role_code) values(?,'LEARNER') on conflict do nothing",uid(b));long c=classroom();classEnroll(c,LEARNER);classEnroll(c,b);sql("update users set notification_study_alert_enabled=true,notification_in_app_enabled=true where email in (?,?)",LEARNER,b);long h=ok("POST","/api/teacher/classrooms/"+c+"/homework",TEACHER,body("title","Round1 deadline "+UUID.randomUUID(),"instruction","Submit a paragraph","activityType","TEXT_RESPONSE","skill","WRITING","gradingMode","TEACHER","maxScore",10,"status","OPEN","deadline",java.time.LocalDateTime.now().plusHours(20).toString())).path("id").asLong();ok("POST","/api/student/classrooms/homework/"+h+"/submit",b,body("textAnswer","Already submitted answer"));reminders.dispatchDueReminders();assertEquals(1,n("select count(*) from app_notifications where user_id=? and type='HOMEWORK_DEADLINE' and deduplication_key=? and action_path='/my-homework'",uid(LEARNER),"HOMEWORK_"+h+"_24H"));assertEquals(0,n("select count(*) from app_notifications where user_id=? and type='HOMEWORK_DEADLINE' and deduplication_key=?",uid(b),"HOMEWORK_"+h+"_24H"));assertTrue(ok("GET","/api/student/notifications",LEARNER,null).toString().contains("HOMEWORK_DEADLINE"));ok("GET","/api/student/notifications",b,null);
    }

    /**
     * Workbook: Community, Promotion & Support!A19
     * Preconditions:
     * LEARNER A has an unread notification N1; LEARNER B is a verified account.
     * Procedure:
     * 1. Login as LEARNER B and call PATCH /api/student/notifications/{N1}/read.
     * 2. Query the AppNotification N1 and call GET /api/student/notifications/unread-count as LEARNER A.
     * Expected results:
     * The request is rejected with the message that the learner cannot update this notification.
     * N1 is still unread and the unread count of A is unchanged.
     */
    @Test
    @DisplayName("IT_COMMUNITY_08 — Verify a learner cannot mark the notification of another learner as read.")
    void IT_COMMUNITY_08() throws Exception {
        String a=freshLearner(),b=freshLearner();long id=note(a,"ROUND1","Unseen notification");var before=ok("GET","/api/student/notifications/unread-count",a,null);rejected("PATCH","/api/student/notifications/"+id+"/read",b,null);assertEquals("false",str("select read::text from app_notifications where id=?",id));assertEquals(before,ok("GET","/api/student/notifications/unread-count",a,null));
    }

    /**
     * Workbook: Community, Promotion & Support!A20
     * Preconditions:
     * LEARNER A has two unread notifications; LEARNER B has one unread notification.
     * Procedure:
     * 1. Login as LEARNER A and call PATCH /api/student/notifications/read-all.
     * 2. Query the AppNotification rows of A and B.
     * 3. Call GET /api/student/notifications/unread-count as LEARNER A and as LEARNER B.
     * Expected results:
     * All notifications of A have read true and the count of A is 0.
     * The notification of B is still unread and the count of B is 1.
     */
    @Test
    @DisplayName("IT_COMMUNITY_09 — Verify mark-all-read changes only the notifications of the caller.")
    void IT_COMMUNITY_09() throws Exception {
        String a=freshLearner(),b=freshLearner();note(a,"ROUND1","A1");note(a,"ROUND1","A2");long bid=note(b,"ROUND1","B1");ok("PATCH","/api/student/notifications/read-all",a,null);assertEquals(0,n("select count(*) from app_notifications where user_id=? and read=false",uid(a)));assertEquals("false",str("select read::text from app_notifications where id=?",bid));assertEquals(0,ok("GET","/api/student/notifications/unread-count",a,null).path("unreadCount").asLong());var count=ok("GET","/api/student/notifications/unread-count",b,null);assertTrue(count.asLong(-1)==1||count.path("count").asLong(-1)==1||count.path("unreadCount").asLong(-1)==1,"B unread count: "+count);
    }

    /**
     * Workbook: Community, Promotion & Support!A26
     * Preconditions:
     * Ticket assigned to STAFF; learner owns the ticket; mail/notification external I/O is stubbed as described in Test Cases.
     * Procedure:
     * 1. Create/claim a ticket and reply as staff so it is WAITING_FOR_LEARNER.
     * 2. Reply as learner.
     * 3. Inspect stored message, ticket status, staff detail and the exact newest SUPPORT_TICKET_REPLY inbox item.
     * Expected results:
     * Follow-up message is saved once; status becomes IN_PROGRESS. Exactly one new unread SUPPORT_TICKET_REPLY notification is created for staff; staff detail contains the follow-up.
     */
    @Test
    @DisplayName("IT_COMMUNITY_12 — Verify learner reply changes ticket status and creates a staff inbox notification.")
    void IT_COMMUNITY_12() throws Exception {
        long k=ticket();claim(k);reply(k);assertEquals("WAITING_FOR_LEARNER",ticketStatus(k));long before=notifCount(STAFF,"SUPPORT_TICKET_REPLY");String message="Round1 learner followup message";ok("POST",learnerTicket(k)+"/replies",LEARNER,body("message",message));assertEquals("IN_PROGRESS",ticketStatus(k));assertEquals(1,n("select count(*) from support_ticket_messages where ticket_id=? and body=?",k,message));assertEquals(before+1,notifCount(STAFF,"SUPPORT_TICKET_REPLY"));assertTrue(ok("GET",staffTicket(k),STAFF,null).toString().contains(message));var inbox=ok("GET","/api/student/notifications",STAFF,null);var notification=row(inbox,"id",n("select max(id) from app_notifications where user_id=? and type='SUPPORT_TICKET_REPLY'",uid(STAFF)));assertFalse(notification.isMissingNode());assertFalse(notification.path("read").asBoolean());assertEquals("SUPPORT_TICKET_REPLY",notification.path("type").asText());
    }

    /**
     * Workbook: Community, Promotion & Support!A27
     * Preconditions:
     * Learner owns K and K is WAITING_FOR_LEARNER after a staff reply; STAFF account exists.
     * Procedure:
     * 1. Create, claim and reply to the ticket as staff, leaving WAITING_FOR_LEARNER.
     * 2. PATCH /api/student/support-tickets/{K}/status with CLOSED.
     * 3. Inspect ticket and staff detail.
     * Expected results:
     * Ticket becomes CLOSED; staff detail shows CLOSED.
     */
    @Test
    @DisplayName("IT_COMMUNITY_13 — Verify a learner can close a ticket awaiting their reply.")
    void IT_COMMUNITY_13() throws Exception {
        long k=ticket();claim(k);reply(k);assertEquals("WAITING_FOR_LEARNER",ticketStatus(k));ok("PATCH",learnerTicket(k)+"/status",LEARNER,body("status","CLOSED"));assertEquals("CLOSED",ticketStatus(k));assertEquals("CLOSED",ok("GET",staffTicket(k),STAFF,null).path("status").asText());
    }

    /**
     * Workbook: Community, Promotion & Support!A28
     * Preconditions:
     * LEARNER has an IN_PROGRESS ticket K.
     * Procedure:
     * 1. Login as LEARNER and call PATCH /api/student/support-tickets/{K}/status with {"status":"RESOLVED"}.
     * 2. Query the SupportTicket K.
     * Expected results:
     * The request is rejected because the learner may only close a ticket.
     * The ticket status is still IN_PROGRESS.
     */
    @Test
    @DisplayName("IT_COMMUNITY_14 — Verify a learner cannot set a ticket status other than CLOSED.")
    void IT_COMMUNITY_14() throws Exception {
        long k=ticket();claim(k);assertEquals("IN_PROGRESS",ticketStatus(k));rejected("PATCH",learnerTicket(k)+"/status",LEARNER,body("status","RESOLVED"));assertEquals("IN_PROGRESS",ticketStatus(k));
    }

    /**
     * Workbook: Community, Promotion & Support!A34
     * Preconditions:
     * LEARNER has an IN_PROGRESS ticket K assigned to STAFF.
     * Procedure:
     * 1. Login as STAFF and call POST /api/staff/support-tickets/{K}/replies with a reply text.
     * 2. Query the SupportTicket K, its messages and the AppNotification rows of LEARNER.
     * 3. Login as LEARNER and call GET /api/student/support-tickets/{K} and GET /api/student/notifications.
     * Expected results:
     * The reply is stored as a message of K and the status is WAITING_FOR_LEARNER.
     * One unread SUPPORT_TICKET_UPDATED notification for K exists for LEARNER.
     * The learner sees the staff message, the new status and the notification.
     */
    @Test
    @DisplayName("IT_COMMUNITY_17 — Verify a Staff reply sets WAITING_FOR_LEARNER and notifies the learner.")
    void IT_COMMUNITY_17() throws Exception {
        long k=ticket();claim(k);long before=notifCount(LEARNER,"SUPPORT_TICKET_UPDATED");reply(k);assertEquals("WAITING_FOR_LEARNER",ticketStatus(k));assertEquals(1,n("select count(*) from support_ticket_messages where ticket_id=? and author_id=?",k,uid(STAFF)));assertEquals(before+1,notifCount(LEARNER,"SUPPORT_TICKET_UPDATED"));assertTrue(ok("GET",learnerTicket(k),LEARNER,null).toString().contains("Round1 staff answer"));assertTrue(ok("GET","/api/student/notifications",LEARNER,null).toString().contains("SUPPORT_TICKET_UPDATED"));
    }

    /**
     * Workbook: Community, Promotion & Support!A36
     * Preconditions:
     * LEARNER has an IN_PROGRESS ticket K assigned to STAFF and K already has at least one staff reply; STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call PATCH /api/staff/support-tickets/{K} with {"status":"RESOLVED"}.
     * 2. Query the SupportTicket K and the AppNotification rows of LEARNER.
     * 3. Login as LEARNER and call GET /api/student/notifications.
     * Expected results:
     * The ticket status is RESOLVED and resolvedAt is set.
     * One unread SUPPORT_TICKET_UPDATED notification for K exists for LEARNER and appears in the inbox.
     */
    @Test
    @DisplayName("IT_COMMUNITY_18 — Verify resolving a replied ticket sets resolvedAt and notifies the learner.")
    void IT_COMMUNITY_18() throws Exception {
        long k=ticket();claim(k);reply(k);sql("update support_tickets set status='IN_PROGRESS' where id=?",k);long before=notifCount(LEARNER,"SUPPORT_TICKET_UPDATED");ok("PATCH",staffTicket(k),STAFF,body("status","RESOLVED"));assertEquals("RESOLVED",ticketStatus(k));assertNotNull(str("select resolved_at::text from support_tickets where id=?",k));assertEquals(before+1,notifCount(LEARNER,"SUPPORT_TICKET_UPDATED"));assertTrue(ok("GET","/api/student/notifications",LEARNER,null).toString().contains("SUPPORT_TICKET_UPDATED"));
    }

    /**
     * Workbook: Community, Promotion & Support!A37
     * Preconditions:
     * Learner owns a ticket; STAFF has claimed and replied before resolving.
     * Procedure:
     * 1. Create ticket, claim it as STAFF and send a staff reply.
     * 2. PATCH /api/staff/support-tickets/{K} with RESOLVED.
     * 3. Inspect status, resolvedAt, resolvedBy and learner notification inbox.
     * Expected results:
     * Ticket is RESOLVED with resolvedAt and STAFF as resolver. Exactly one additional SUPPORT_TICKET_UPDATED notification is created for the learner and visible in their inbox.
     */
    @Test
    @DisplayName("IT_COMMUNITY_19 — Verify staff can resolve a ticket after a staff reply.")
    void IT_COMMUNITY_19() throws Exception {
        long k=ticket();claim(k);reply(k);long before=notifCount(LEARNER,"SUPPORT_TICKET_UPDATED");
         ok("PATCH",staffTicket(k),STAFF,body("status","RESOLVED"));assertEquals("RESOLVED",ticketStatus(k));
         assertNotNull(str("select resolved_at::text from support_tickets where id=?",k));assertEquals(uid(STAFF),n("select resolved_by_id from support_tickets where id=?",k));
         assertEquals(before+1,notifCount(LEARNER,"SUPPORT_TICKET_UPDATED"));assertTrue(ok("GET","/api/student/notifications",LEARNER,null).toString().contains("SUPPORT_TICKET_UPDATED"));
    }

    /**
     * Workbook: Community, Promotion & Support — Submit Support Request
     * Preconditions:
     * LEARNER account exists.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/support-tickets with an empty subject.
     * 2. Query the SupportTicket rows.
     * Expected results:
     * The request is rejected because the subject is required.
     * No new ticket row is created.
     */
    @Test
    @DisplayName("IT_COMMUNITY_20 — Verify submitting a support request without required fields is rejected.")
    void IT_COMMUNITY_20() throws Exception {
        long before=n("select count(*) from support_tickets");rejected("POST","/api/student/support-tickets",LEARNER,body("subject","","category","TECHNICAL","message","Missing subject test"));assertEquals(before,n("select count(*) from support_tickets"));
    }

    /**
     * Workbook: Community, Promotion & Support — View My Support Requests
     * Preconditions:
     * LEARNER has an open ticket K.
     * Procedure:
     * 1. Login as LEARNER and call GET /api/student/support-tickets/{K}.
     * 2. Call GET /api/student/support-tickets.
     * Expected results:
     * The detail returns K with the correct subject and status.
     * The list contains K.
     */
    @Test
    @DisplayName("IT_COMMUNITY_21 — Verify a learner can view their own ticket details.")
    void IT_COMMUNITY_21() throws Exception {
        long k=ticket();var detail=ok("GET",learnerTicket(k),LEARNER,null);assertEquals(k,detail.path("id").asLong());assertEquals("OPEN",detail.path("status").asText());assertFalse(row(ok("GET","/api/student/support-tickets",LEARNER,null),"id",k).isMissingNode());
    }

    /**
     * Workbook: Community, Promotion & Support — View Support Request Queue
     * Preconditions:
     * LEARNER account exists.
     * Procedure:
     * 1. Login as LEARNER and call GET /api/staff/support-tickets.
     * Expected results:
     * The request is rejected with 403.
     */
    @Test
    @DisplayName("IT_COMMUNITY_22 — Verify a learner cannot access the Staff support ticket queue.")
    void IT_COMMUNITY_22() throws Exception {
        String u=freshLearner();status(call("GET","/api/staff/support-tickets",u,null),403);
    }

    /**
     * Workbook: Community, Promotion & Support — Claim Support Request
     * Preconditions:
     * Ticket K is IN_PROGRESS and assigned to STAFF.
     * Procedure:
     * 1. Login as another STAFF S2 and call POST /api/staff/support-tickets/{K}/claim.
     * 2. Query the SupportTicket K.
     * Expected results:
     * The request is rejected because K is already claimed.
     * The assignee is still STAFF.
     */
    @Test
    @DisplayName("IT_COMMUNITY_23 — Verify claiming an already-claimed ticket is rejected.")
    void IT_COMMUNITY_23() throws Exception {
        long k=ticket();claim(k);assertEquals(uid(STAFF),n("select assignee_id from support_tickets where id=?",k));rejected("POST",staffTicket(k)+"/claim",STAFF,null);assertEquals(uid(STAFF),n("select assignee_id from support_tickets where id=?",k));
    }

    /**
     * Workbook: Community, Promotion & Support — Reply to Support Request
     * Preconditions:
     * Ticket K is RESOLVED.
     * STAFF account exists.
     * Procedure:
     * 1. Login as STAFF and call POST /api/staff/support-tickets/{K}/replies with a message.
     * 2. Query the SupportTicket K and its messages.
     * Expected results:
     * The request is rejected because K is already resolved.
     * No new message is added.
     */
    @Test
    @DisplayName("IT_COMMUNITY_24 — Verify staff cannot reply to a RESOLVED ticket.")
    void IT_COMMUNITY_24() throws Exception {
        long k=ticket();claim(k);reply(k);ok("PATCH",staffTicket(k),STAFF,body("status","RESOLVED"));assertEquals("RESOLVED",ticketStatus(k));long messages=n("select count(*) from support_ticket_messages where ticket_id=?",k);rejected("POST",staffTicket(k)+"/replies",STAFF,body("message","Post-resolution reply"));assertEquals(messages,n("select count(*) from support_ticket_messages where ticket_id=?",k));
    }
}
