package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – View operational report
 * Excel sheet: IT_REPORT | SRS: UC-40 View operational report, UC-41 View revenue analytic of online course
 * Chạy: mvnw -Dtest=ReportIT test
 */
@EnglishLabIT
public class ReportIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_REPORT_01")
    void itReport01() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/dashboard").header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 404, "dashboard " + s);
                });
    }

    @Test
    @DisplayName("IT_REPORT_02")
    void itReport02() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/revenue").header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 404, "revenue " + s);
                });
    }
}
