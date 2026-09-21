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
        String u=freshLearner();String before=str("select title from placement_test_definitions where id=(select max(id) from placement_test_definitions)");status(call("PUT","/api/content-manager/placement-test",u,body("title","Hacked title","listeningConfigJson","{}","readingConfigJson","{}","writingConfigJson","{}","speakingConfigJson","{}")),403);assertEquals(before,str("select title from placement_test_definitions where id=(select max(id) from placement_test_definitions)"));
    }
}
