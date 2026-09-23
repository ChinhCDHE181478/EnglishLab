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

/** Canonical workbook scenarios for Authentication & Account. */
@EnglishLabIT
@RecordApplicationEvents
@Tag("workbook")
public class WorkbookAuthenticationIT extends WorkbookTestSupport {

    /**
     * Workbook: Authentication & Account!A24
     * Preconditions:
     * A verified learner account with password P1 exists; P2 satisfies the password policy.
     * Procedure:
     * 1. Login with P1 and call PUT /api/user/me/password with currentPassword P1, newPassword P2 and confirmPassword P2.
     * 2. Query the password hash of the user.
     * 3. Call POST /api/auth/login with P1 and with P2.
     * Expected results:
     * The stored password is the encoded P2 and passwordSet is true.
     * Login with P1 is rejected with 401 and login with P2 succeeds.
     */
    @Test
    @DisplayName("IT_AUTH_10 — Verify changing the password with the correct current password replaces the old password.")
    void IT_AUTH_10() throws Exception {
        String u=freshLearner(),p="ChangedPass456!";
          ok("PUT","/api/user/me/password",u,body("currentPassword",PASSWORD,"newPassword",p,"confirmPassword",p));
          assertTrue(encoder.matches(p,str("select password from users where email=?",u)));
          assertEquals("true",str("select password_set::text from users where email=?",u));
          status(call("POST","/api/auth/login",null,body("email",u,"password",PASSWORD)),401);
          assertFalse(ok("POST","/api/auth/login",null,body("email",u,"password",p)).path("accessToken").asText().isBlank());
    }

    /**
     * Workbook: Authentication & Account!A26
     * Preconditions:
     * LEARNER and ADMIN accounts exist.
     * Procedure:
     * 1. Login as LEARNER and call PUT /api/user/me with a new fullName, phoneNumber and the required targetExam.
     * 2. Call GET /api/user/me and query the User row of LEARNER.
     * 3. Login as ADMIN and call GET /api/admin/users?page=0&size=200.
     * Expected results:
     * The profile and the User row hold the new full name and phone number.
     * The row of LEARNER in the admin user list shows the new full name.
     */
    @Test
    @DisplayName("IT_AUTH_11 — Verify a profile update is saved and shown in the Admin user list.")
    void IT_AUTH_11() throws Exception {
        String u=freshLearner(),name="Updated Round1 "+UUID.randomUUID();
          ok("PUT","/api/user/me",u,body("fullName",name,"phoneNumber","0901234567","targetExam","IELTS","targetScore","6.5"));
          var me=ok("GET","/api/user/me",u,null);assertEquals(name,me.path("fullName").asText());assertEquals("0901234567",me.path("phoneNumber").asText());
          assertEquals(name,str("select full_name from users where email=?",u));assertEquals("0901234567",str("select phone_number from users where email=?",u));
          assertEquals(name,row(ok("GET","/api/admin/users?page=0&size=200",ADMIN,null),"id",uid(u)).path("fullName").asText());
    }

    /**
     * Workbook: Authentication & Account!A27
     * Preconditions:
     * LEARNER already has an avatar A1 stored in the object store.
     * The object store (S3/R2) is stubbed so stored keys can be listed; the update transaction is allowed to commit.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/user/me/avatar with a new image file.
     * 2. Call GET /api/user/me.
     * 3. After the transaction commits, list the avatar keys in the object store.
     * Expected results:
     * The profile returns the new avatar URL.
     * An avatar-updated event is published once with the key of A1.
     * A1 no longer exists in the object store and the new avatar file still exists.
     */
    @Test
    @DisplayName("IT_AUTH_12 — Verify uploading a new avatar deletes the previous avatar file from storage.")
    void IT_AUTH_12() throws Exception {
        String u=freshLearner();String first=upload("/api/user/me/avatar",u).path("avatarUrl").asText();commit();String old=first.replace("https://storage.example.test/","");assertTrue(stored.containsKey(old));String second=upload("/api/user/me/avatar",u).path("avatarUrl").asText();assertNotEquals(first,second);assertEquals(second,ok("GET","/api/user/me",u,null).path("avatarUrl").asText());assertEquals(1,events.stream(AvatarUpdatedEvent.class).count());commit();verify(store,timeout(5000).times(1)).delete(old);assertFalse(stored.containsKey(old));assertTrue(stored.containsKey(second.replace("https://storage.example.test/","")));
    }

    /**
     * Workbook: Authentication & Account — Log In
     * Preconditions:
     * A verified learner account with password P1 exists.
     * Procedure:
     * 1. Call POST /api/auth/login with email and a wrong password.
     * 2. Call POST /api/auth/login with the correct password P1.
     * Expected results:
     * The wrong-password login is rejected with 401.
     * The correct-password login returns a valid access token.
     */
    @Test
    @DisplayName("IT_AUTH_13 — Verify login with a wrong password is rejected and the correct password still works.")
    void IT_AUTH_13() throws Exception {
        String u=freshLearner();status(call("POST","/api/auth/login",null,body("email",u,"password","WrongPassword999!")),401);assertFalse(ok("POST","/api/auth/login",null,body("email",u,"password",PASSWORD)).path("accessToken").asText().isBlank());
    }
}
