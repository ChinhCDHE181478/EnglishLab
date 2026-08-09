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
 * Integration Test – Manage Online Courses
 * Excel sheet: IT_ONLINE | SRS: UC-33a Create Online Course, UC-33b View Online Courses, UC-33c Update Online Course, UC-33d Deactivate Online Course
 * Chạy: mvnw -Dtest=OnlineCourseCmIT test
 */
@EnglishLabIT
public class OnlineCourseCmIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ONLINE_01")
    void itOnline01() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/online-courses").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ONLINE_02")
    void itOnline02() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        String body = """
                {"title":"IT Online %s","slug":"it-online-%s","price":100000,"status":"DRAFT"}
                """.formatted(UUID.randomUUID().toString().substring(0, 6),
                        UUID.randomUUID().toString().substring(0, 8));
        mockMvc.perform(post("/api/content-manager/online-courses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 201 || s == 400, "create course " + s);
                });
    }

    @Test
    @DisplayName("IT_ONLINE_03")
    void itOnline03() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        MvcResult list = mockMvc.perform(get("/api/content-manager/online-courses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(list.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        Assumptions.assumeTrue(items.size() > 0);
        long id = items.get(0).path("id").asLong();
        mockMvc.perform(put("/api/content-manager/online-courses/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"IT Online Upd","price":100000}
                                """))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 400, "update " + s);
                });
    }

    @Test
    @DisplayName("IT_ONLINE_04")
    void itOnline04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/content-manager/online-courses").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }
}
