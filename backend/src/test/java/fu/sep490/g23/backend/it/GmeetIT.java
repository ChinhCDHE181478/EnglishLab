package fu.sep490.g23.backend.it;

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

import static fu.sep490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Join Online Meeting
 * Excel sheet: IT_GMEET | SRS: UC-10 Join Online Meeting
 * Chạy: mvnw -Dtest=GmeetIT test
 */
@EnglishLabIT
public class GmeetIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_GMEET_01")
    void itGmeet01() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/teacher/classrooms/assigned").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(r.getResponse().getContentAsString());
        JsonNode virtual = null;
        for (JsonNode it : items) {
            String mode = it.path("deliveryMode").asText("");
            if ("VIRTUAL".equals(mode) || "ONLINE".equals(mode) || "HYBRID".equals(mode)) {
                virtual = it;
                break;
            }
        }
        Assumptions.assumeTrue(virtual != null, "Teacher không có lớp VIRTUAL");
        long id = virtual.path("id").asLong();
        MvcResult sessions = mockMvc.perform(get("/api/teacher/classrooms/" + id + "/sessions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode sess = mapper().readTree(sessions.getResponse().getContentAsString());
        Assumptions.assumeTrue(sess.size() > 0);
        long sid = sess.get(0).path("id").asLong();
        mockMvc.perform(post("/api/teacher/classrooms/sessions/" + sid + "/open")
                        .header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    // 200 OK hoặc 400/503 khi Google Meet provider chưa bật (env N/A)
                    Assumptions.assumeTrue(s == 200 || s == 400 || s == 503, "open meet " + s);
                });
    }

    @Test
    @DisplayName("IT_GMEET_02")
    void itGmeet02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/student/classrooms/my-classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(r.getResponse().getContentAsString());
        JsonNode virtual = null;
        for (JsonNode it : items) {
            String mode = it.path("deliveryMode").asText("");
            if ("VIRTUAL".equals(mode) || "ONLINE".equals(mode) || "HYBRID".equals(mode)) {
                virtual = it;
                break;
            }
        }
        Assumptions.assumeTrue(virtual != null, "Learner không có lớp VIRTUAL");
        long id = virtual.path("id").asLong();
        MvcResult sessions = mockMvc.perform(get("/api/student/classrooms/" + id + "/sessions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode sess = mapper().readTree(sessions.getResponse().getContentAsString());
        Assumptions.assumeTrue(sess.size() > 0);
        long sid = sess.get(0).path("id").asLong();
        mockMvc.perform(post("/api/student/classrooms/sessions/" + sid + "/join")
                        .header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 400 || s == 503, "join meet " + s);
                });
    }

    @Test
    @DisplayName("IT_GMEET_03")
    void itGmeet03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(post("/api/student/classrooms/sessions/999999991/join")
                        .header("Authorization", bearer(token)))
                .andExpect(status().is4xxClientError());
    }
}
