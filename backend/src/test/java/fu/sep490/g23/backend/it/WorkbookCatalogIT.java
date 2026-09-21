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

/** Canonical workbook scenarios for Public Catalog. */
@EnglishLabIT
@RecordApplicationEvents
@Tag("workbook")
public class WorkbookCatalogIT extends WorkbookTestSupport {

    /**
     * Workbook: Public Catalog — View Online Course Details
     * Preconditions:
     * CM account exists; a DRAFT course X with a unique slug exists.
     * Procedure:
     * 1. Login as CM and create a DRAFT course X.
     * 2. Without a JWT call GET /api/online-courses/{slugOfX}.
     * 3. Query the OnlineCourse X to confirm it is DRAFT.
     * Expected results:
     * The public detail returns 404.
     * The course exists in the database with status DRAFT.
     */
    @Test
    @DisplayName("IT_CATALOG_05 — Verify an unpublished course detail returns 404 to the public.")
    void IT_CATALOG_05() throws Exception {
        long c=course(100000,0);String slug=str("select slug from online_courses where id=?",c);status(call("GET","/api/online-courses/"+slug,null,null),404);assertEquals("DRAFT",str("select status from online_courses where id=?",c));
    }
}
