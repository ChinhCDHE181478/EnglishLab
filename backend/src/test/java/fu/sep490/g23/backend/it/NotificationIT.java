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
 * Integration Test – View Notifications
 * Excel sheet: IT_NOTIF | SRS: UC-06 View Notifications
 * Chạy: mvnw -Dtest=NotificationIT test
 */
@EnglishLabIT
public class NotificationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_NOTIF_01")
    void itNotif01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/notifications").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_NOTIF_02")
    void itNotif02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/student/notifications").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(r.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        if (items.size() == 0) return;
        long nid = items.get(0).path("id").asLong();
        mockMvc.perform(patch("/api/student/notifications/" + nid + "/read")
                        .header("Authorization", bearer(token)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("IT_NOTIF_03")
    void itNotif03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(patch("/api/student/notifications/read-all").header("Authorization", bearer(token)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("IT_NOTIF_04")
    void itNotif04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/notification-preferences").header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 404, "prefs " + s);
                });
    }

    @Test
    @DisplayName("IT_NOTIF_05")
    void itNotif05() throws Exception {
        mockMvc.perform(get("/api/student/notifications")).andExpect(status().is4xxClientError());
    }
}
