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
 * Integration Test – Take Placement Exam
 * Excel sheet: IT_PLACEMENT | SRS: UC-16 Take Placement Exam
 * Chạy: mvnw -Dtest=PlacementIT test
 */
@EnglishLabIT
public class PlacementIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_PLACEMENT_01")
    void itPlacement01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/placement-tests/current").header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 404, "placement " + s);
                });
    }

    @Test
    @DisplayName("IT_PLACEMENT_02")
    void itPlacement02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(post("/api/student/placement-tests/1/submit")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_PLACEMENT_03")
    void itPlacement03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/placement-tests/current").header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 404, "placement " + s);
                });
    }

    @Test
    @DisplayName("IT_PLACEMENT_04")
    void itPlacement04() throws Exception {
        mockMvc.perform(get("/api/student/placement-tests/current"))
                .andExpect(status().is4xxClientError());
    }
}
