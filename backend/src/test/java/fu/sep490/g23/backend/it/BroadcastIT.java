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
 * Integration Test – Manage System Notifications
 * Excel sheet: IT_BROADCAST | SRS: UC-43a Create System Notification, UC-43b View System Notifications, UC-43c Update System Notification, UC-43d Delete System Notification
 * Chạy: mvnw -Dtest=BroadcastIT test
 */
@EnglishLabIT
public class BroadcastIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_BROADCAST_01")
    void itBroadcast01() throws Exception {
        String token = login(mockMvc, ADMIN, PASSWORD);
        String body = """
                {"title":"IT Broadcast %s","message":"Integration test broadcast","sendInApp":true,"sendEmail":false}
                """.formatted(System.currentTimeMillis());
        mockMvc.perform(post("/api/admin/broadcasts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_BROADCAST_02")
    void itBroadcast02() throws Exception {
        String token = login(mockMvc, ADMIN, PASSWORD);
        mockMvc.perform(get("/api/admin/broadcasts").param("page", "0").param("size", "10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_BROADCAST_03")
    void itBroadcast03() throws Exception {
        String token = login(mockMvc, ADMIN, PASSWORD);
        mockMvc.perform(post("/api/admin/broadcasts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"IT Broadcast upd src","message":"x","sendInApp":true,"sendEmail":false}
                                """))
                .andExpect(status().isOk());
        MvcResult list = mockMvc.perform(get("/api/admin/broadcasts").param("page", "0").param("size", "10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(list.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        Assumptions.assumeTrue(items.size() > 0);
        long bid = items.get(0).path("id").asLong();
        mockMvc.perform(put("/api/admin/broadcasts/" + bid)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"IT Broadcast updated","message":"updated","sendInApp":true,"sendEmail":false}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_BROADCAST_04")
    void itBroadcast04() throws Exception {
        String token = login(mockMvc, ADMIN, PASSWORD);
        MvcResult created = mockMvc.perform(post("/api/admin/broadcasts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"IT Broadcast cancel","message":"x","sendInApp":true,"sendEmail":false}
                                """))
                .andExpect(status().isOk()).andReturn();
        long bid = mapper().readTree(created.getResponse().getContentAsString()).path("id").asLong();
        String when = LocalDateTime.now().plusMinutes(5).withNano(0).toString();
        mockMvc.perform(post("/api/admin/broadcasts/" + bid + "/schedule")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"" + when + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/broadcasts/" + bid + "/cancel")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
