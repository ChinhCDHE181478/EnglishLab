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
 * Integration Test – Manage profile
 * Excel sheet: IT_USER | SRS: UC-05 Manage profile
 * Chạy: mvnw -Dtest=UserIT test
 */
@EnglishLabIT
public class UserIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_USER_01")
    void itUser01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/user/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_USER_02")
    void itUser02() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_USER_03")
    void itUser03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        String fullName = "IT User " + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(put("/api/user/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"%s",
                                  "phoneNumber":"0900000001",
                                  "targetExam":"IELTS",
                                  "targetScore":"6.5",
                                  "currentBand":6.0,
                                  "studyGoal":"Integration test"
                                }
                                """.formatted(fullName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value(fullName));
        mockMvc.perform(get("/api/user/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value(fullName));
    }

    @Test
    @DisplayName("IT_USER_04")
    void itUser04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult before = mockMvc.perform(get("/api/user/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        String originalName = json(before).path("fullName").asText();
        mockMvc.perform(put("/api/user/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","phoneNumber":"","targetExam":""}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/user/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value(originalName));
    }
}
