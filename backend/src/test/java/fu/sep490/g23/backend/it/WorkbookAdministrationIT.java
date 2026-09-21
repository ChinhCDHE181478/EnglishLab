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

/** Canonical workbook scenarios for Administration & Reports. */
@EnglishLabIT
@RecordApplicationEvents
@Tag("workbook")
public class WorkbookAdministrationIT extends WorkbookTestSupport {

    /**
     * Workbook: Administration & Reports!A26
     * Preconditions:
     * ADMIN account exists.
     * A verified user U has only the LEARNER role.
     * Procedure:
     * 1. Login as ADMIN and call GET /api/admin/users?role=TEACHER and check that U is not listed.
     * 2. Call PATCH /api/admin/users/{U}/roles with {"roles":["LEARNER","TEACHER"]}.
     * 3. Call GET /api/admin/users?role=TEACHER again.
     * 4. Query the roles of U.
     * Expected results:
     * Before the change U is not in the TEACHER-filtered list.
     * U now has the roles LEARNER and TEACHER in the database.
     * The TEACHER-filtered list returns U with both roles.
     */
    @Test
    @DisplayName("IT_ADMIN_11 — Verify the admin user list filtered by role shows the newly assigned TEACHER.")
    void IT_ADMIN_11() throws Exception {
        String u=freshLearner();long id=uid(u);assertTrue(row(ok("GET","/api/admin/users?role=TEACHER&page=0&size=200",ADMIN,null),"id",id).isMissingNode());ok("PATCH","/api/admin/users/"+id+"/roles",ADMIN,body("roles",List.of("LEARNER","TEACHER")));assertEquals(2,n("select count(*) from user_roles where user_id=? and role_code in ('LEARNER','TEACHER')",id));var user=row(ok("GET","/api/admin/users?role=TEACHER&page=0&size=200",ADMIN,null),"id",id);assertFalse(user.isMissingNode());assertTrue(user.path("roles").toString().contains("TEACHER")&&user.path("roles").toString().contains("LEARNER"));
    }

    /**
     * Workbook: Administration & Reports!A32
     * Preconditions:
     * ADMIN exists; LEARNER is disabled through PATCH /api/admin/users/{learnerId}/status with enabled false; LEARNER B is enabled.
     * Procedure:
     * 1. Login as ADMIN and create and send a broadcast with targetRole LEARNER.
     * 2. Read the recipient count and in-app success count of the broadcast and query the AppNotification rows.
     * 3. Enable LEARNER again, login as LEARNER and as LEARNER B and call GET /api/student/notifications.
     * Expected results:
     * The counts include LEARNER B and exclude LEARNER.
     * LEARNER has no notification for this broadcast; LEARNER B has one.
     */
    @Test
    @DisplayName("IT_ADMIN_16 — Verify disabled accounts are excluded from the broadcast recipients.")
    void IT_ADMIN_16() throws Exception {
        String a=LEARNER,b=CM;sql("insert into user_roles(user_id,role_code) values(?,'LEARNER') on conflict do nothing",uid(b));ok("PATCH","/api/admin/users/"+uid(a)+"/status",ADMIN,body("enabled",false));long expected=n("select count(distinct u.id) from users u join user_roles r on r.user_id=u.id where u.email_verified=true and r.role_code='LEARNER'");long id=broadcast(true,false,"/dashboard");var sent=ok("POST","/api/admin/broadcasts/"+id+"/send",ADMIN,null);assertEquals(expected,sent.path("recipientCount").asLong());assertEquals(expected,sent.path("inAppSuccessCount").asLong());assertEquals(0,n("select count(*) from app_notifications where user_id=? and deduplication_key=?",uid(a),"ADMIN_BROADCAST_"+id));assertEquals(1,n("select count(*) from app_notifications where user_id=? and deduplication_key=?",uid(b),"ADMIN_BROADCAST_"+id));ok("PATCH","/api/admin/users/"+uid(a)+"/status",ADMIN,body("enabled",true));assertFalse(ok("GET","/api/student/notifications",a,null).toString().contains("ADMIN_BROADCAST_"+id));assertTrue(ok("GET","/api/student/notifications",b,null).toString().contains("ADMIN_BROADCAST"));
    }

    /**
     * Workbook: Administration & Reports!A33
     * Preconditions:
     * LEARNER has emailEnabled true; LEARNER B has emailEnabled false; the mail sender is stubbed.
     * ADMIN account exists.
     * Procedure:
     * 1. Login as ADMIN and create a broadcast with sendEmail true, sendInApp false and targetRole LEARNER, then send it.
     * 2. Read the e-mail queued count of the broadcast and check the mail sender.
     * 3. Query the AppNotification rows of LEARNER and LEARNER B.
     * Expected results:
     * The e-mail queued count counts LEARNER and not LEARNER B.
     * The mail sender received one message, addressed to LEARNER.
     * No in-app notification is created.
     */
    @Test
    @DisplayName("IT_ADMIN_17 — Verify an e-mail broadcast reaches only learners who allow e-mail.")
    void IT_ADMIN_17() throws Exception {
        String a=freshLearner(),b=freshLearner();sql("update users set notification_email_enabled=false where id<>?",uid(a));long before=n("select count(*) from app_notifications");long id=broadcast(false,true,"/dashboard");clearInvocations(mail);var sent=ok("POST","/api/admin/broadcasts/"+id+"/send",ADMIN,null);assertEquals(1,sent.path("emailQueuedCount").asLong());var cap=ArgumentCaptor.forClass(MimeMessage.class);verify(mail,timeout(5000).times(1)).send(cap.capture());assertEquals(a,((jakarta.mail.internet.InternetAddress)cap.getValue().getAllRecipients()[0]).getAddress());assertEquals(before,n("select count(*) from app_notifications"));
    }

    /**
     * Workbook: Administration & Reports!A34
     * Preconditions:
     * ADMIN account exists.
     * Procedure:
     * 1. Login as ADMIN and call POST /api/admin/broadcasts with sendInApp false and sendEmail false.
     * 2. Query the AdminBroadcast rows and call GET /api/admin/broadcasts?page=0&size=10.
     * Expected results:
     * The request is rejected with the message that at least one channel must be selected.
     * No new broadcast is stored or listed.
     */
    @Test
    @DisplayName("IT_ADMIN_18 — Verify a broadcast without any channel is rejected.")
    void IT_ADMIN_18() throws Exception {
        String before=snapshot("admin_broadcasts");rejected("POST","/api/admin/broadcasts",ADMIN,body("title","Round1 invalid channel","message","Fixture message","sendInApp",false,"sendEmail",false));assertEquals(before,snapshot("admin_broadcasts"));ok("GET","/api/admin/broadcasts?page=0&size=10",ADMIN,null);
    }

    /**
     * Workbook: Administration & Reports!A35
     * Preconditions:
     * ADMIN account exists.
     * Procedure:
     * 1. Login as ADMIN and call POST /api/admin/broadcasts with actionPath "https://evil.example".
     * 2. Query the AdminBroadcast rows and call GET /api/admin/broadcasts?page=0&size=10.
     * Expected results:
     * The request is rejected with the message that the action path must be an internal path starting with a single slash.
     * No new broadcast is stored or listed.
     */
    @Test
    @DisplayName("IT_ADMIN_19 — Verify a broadcast with an external action path is rejected.")
    void IT_ADMIN_19() throws Exception {
        String before=snapshot("admin_broadcasts");rejected("POST","/api/admin/broadcasts",ADMIN,body("title","Round1 invalid path","message","Fixture message","sendInApp",true,"sendEmail",false,"actionPath","https://evil.example"));assertEquals(before,snapshot("admin_broadcasts"));ok("GET","/api/admin/broadcasts?page=0&size=10",ADMIN,null);
    }

    /**
     * Workbook: Administration & Reports!A37
     * Preconditions:
     * ADMIN account exists; one SENT, one CANCELLED and one DRAFT broadcast exist.
     * Procedure:
     * 1. Login as ADMIN and call GET /api/admin/broadcasts?page=0&size=10.
     * 2. Call GET /api/admin/broadcasts?status=SENT&page=0&size=10.
     * 3. Call GET /api/admin/broadcasts?status=CANCELLED&page=0&size=10.
     * Expected results:
     * The unfiltered page contains the three broadcasts.
     * The SENT filter returns only SENT broadcasts and includes the sent one.
     * The CANCELLED filter returns only CANCELLED broadcasts and includes the cancelled one.
     */
    @Test
    @DisplayName("IT_ADMIN_20 — Verify the broadcast list and its status filter show each broadcast under its status.")
    void IT_ADMIN_20() throws Exception {
        long sent=broadcast(true,false,null),cancel=broadcast(true,false,null),draft=broadcast(true,false,null);ok("POST","/api/admin/broadcasts/"+sent+"/send",ADMIN,null);ok("POST","/api/admin/broadcasts/"+cancel+"/schedule",ADMIN,body("scheduledAt",java.time.LocalDateTime.now().plusDays(1).toString()));ok("POST","/api/admin/broadcasts/"+cancel+"/cancel",ADMIN,null);var all=ok("GET","/api/admin/broadcasts?page=0&size=10",ADMIN,null);for(long id:new long[]{sent,cancel,draft})assertFalse(row(all,"id",id).isMissingNode());for(String status:List.of("SENT","CANCELLED")){var page=ok("GET","/api/admin/broadcasts?status="+status+"&page=0&size=10",ADMIN,null);for(var x:items(page))assertEquals(status,x.path("status").asText());assertFalse(row(page,"id",status.equals("SENT")?sent:cancel).isMissingNode());}
    }

    /**
     * Workbook: Administration & Reports — View User Account
     * Preconditions:
     * LEARNER account exists.
     * Procedure:
     * 1. Login as LEARNER and call GET /api/admin/users?page=0&size=10.
     * Expected results:
     * The request is rejected with 403.
     */
    @Test
    @DisplayName("IT_ADMIN_21 — Verify a non-admin cannot access the admin user list.")
    void IT_ADMIN_21() throws Exception {
        String u=freshLearner();status(call("GET","/api/admin/users?page=0&size=10",u,null),403);
    }

    /**
     * Workbook: Administration & Reports — View System Announcement
     * Preconditions:
     * LEARNER account exists.
     * Procedure:
     * 1. Login as LEARNER and call GET /api/admin/broadcasts?page=0&size=10.
     * Expected results:
     * The request is rejected with 403.
     */
    @Test
    @DisplayName("IT_ADMIN_22 — Verify a non-admin cannot access the broadcast admin list.")
    void IT_ADMIN_22() throws Exception {
        String u=freshLearner();status(call("GET","/api/admin/broadcasts?page=0&size=10",u,null),403);
    }
}
