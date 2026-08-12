package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import fu.sep490.g23.backend.repository.AuthTokenRepository;
import fu.sep490.g23.backend.repository.UserRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Manage User Accounts
 * Excel sheet: IT_ADMIN | SRS: UC-42b View User Accounts, UC-42d Lock/Unlock User Account
 * Chạy: mvnw -Dtest=AdminIT test
 */
@EnglishLabIT
public class AdminIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Test
    @DisplayName("IT_ADMIN_01")
    void itAdmin01() throws Exception {
        String token = login(mockMvc, ADMIN, PASSWORD);
        MvcResult result = mockMvc.perform(get("/api/admin/users")
                        .param("page", "0").param("size", "200")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode page = json(result);
        assertEquals(userRepository.count(), page.path("totalElements").asLong());
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(learnerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_ADMIN_02")
    void itAdmin02() throws Exception {
        String email = registerVerifiedLearner(mockMvc, userRepository, authTokenRepository, "admin-lock");
        long uid = userRepository.findByEmail(email).orElseThrow().getId();
        String token = login(mockMvc, ADMIN, PASSWORD);
        mockMvc.perform(patch("/api/admin/users/" + uid + "/status")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        assertFalse(userRepository.findById(uid).orElseThrow().isEmailVerified());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(patch("/api/admin/users/" + uid + "/status")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
        login(mockMvc, email, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(patch("/api/admin/users/" + uid + "/status")
                        .header("Authorization", bearer(learnerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
    }
}
