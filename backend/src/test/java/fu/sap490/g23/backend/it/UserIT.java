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
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(put("/api/user/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"IT User Updated"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_USER_03")
    void itUser03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(put("/api/user/me/password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"WrongOld!","newPassword":"Password123!"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_USER_04")
    void itUser04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        // endpoint wired — multipart thường 400 nếu thiếu file
        mockMvc.perform(post("/api/user/me/avatar").header("Authorization", bearer(token)))
                .andExpect(status().is4xxClientError());
    }
}
