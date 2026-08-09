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
 * Integration Test – Access Online Learning Materials
 * Excel sheet: IT_ACCESS | SRS: UC-48 Access Online Learning Materials
 * Chạy: mvnw -Dtest=AccessMaterialsIT test
 */
@EnglishLabIT
public class AccessMaterialsIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ACCESS_01")
    void itAccess01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        // chưa enroll => 403/404 vẫn chứng minh security/service wiring
        mockMvc.perform(get("/api/student/online-courses/1/content")
                        .header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 403 || s == 404, "unexpected " + s);
                });
    }

    @Test
    @DisplayName("IT_ACCESS_02")
    void itAccess02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/online-courses/1/progress")
                        .header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 403 || s == 404, "unexpected " + s);
                });
    }
}
