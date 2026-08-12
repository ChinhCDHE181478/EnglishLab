package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.notification.AppNotification;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.notification.AppNotificationRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppNotificationRepository notificationRepository;

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
        long nid = createUnreadNotification("IT_NOTIF_02").getId();
        mockMvc.perform(patch("/api/student/notifications/" + nid + "/read")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
        AppNotification saved = notificationRepository.findById(nid).orElseThrow();
        assertTrue(saved.isRead());
        assertTrue(saved.getReadAt() != null);
    }

    @Test
    @DisplayName("IT_NOTIF_03")
    void itNotif03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        User learner = userRepository.findByEmail(LEARNER).orElseThrow();
        createUnreadNotification("IT_NOTIF_03_A");
        createUnreadNotification("IT_NOTIF_03_B");
        mockMvc.perform(patch("/api/student/notifications/read-all").header("Authorization", bearer(token)))
                .andExpect(status().is2xxSuccessful());
        assertEquals(0, notificationRepository.countByUserIdAndReadFalse(learner.getId()));
    }

    @Test
    @DisplayName("IT_NOTIF_04")
    void itNotif04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(put("/api/user/me/notification-preferences")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailEnabled":false,
                                  "inAppEnabled":true,
                                  "classReminderEnabled":true,
                                  "studyAlertEnabled":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.inAppEnabled").value(true));
        mockMvc.perform(get("/api/user/me/notification-preferences")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classReminderEnabled").value(true))
                .andExpect(jsonPath("$.studyAlertEnabled").value(false));
    }

    @Test
    @DisplayName("IT_NOTIF_05")
    void itNotif05() throws Exception {
        mockMvc.perform(get("/api/student/notifications")).andExpect(status().is4xxClientError());
    }

    private AppNotification createUnreadNotification(String key) {
        User learner = userRepository.findByEmail(LEARNER).orElseThrow();
        return notificationRepository.saveAndFlush(AppNotification.builder()
                .user(learner)
                .type("INTEGRATION_TEST")
                .title(key)
                .body("Integration test notification")
                .deduplicationKey(key + "-" + UUID.randomUUID())
                .read(false)
                .build());
    }
}
