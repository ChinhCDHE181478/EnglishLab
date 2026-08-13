package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import fu.sep490.g23.backend.repository.AuthTokenRepository;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
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
 * Integration Test – Take Placement Exam
 * Excel sheet: IT_PLACEMENT | SRS: UC-16 Take Placement Exam
 * Chạy: mvnw -Dtest=PlacementIT test
 */
@EnglishLabIT
public class PlacementIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private PlacementTestAttemptRepository attemptRepository;

    @Test
    @DisplayName("IT_PLACEMENT_01")
    void itPlacement01() throws Exception {
        String token = newLearnerToken("placement-load");
        mockMvc.perform(get("/api/student/placement-tests/current")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCode").isNotEmpty())
                .andExpect(jsonPath("$.sections").isMap());
    }

    @Test
    @DisplayName("IT_PLACEMENT_02")
    void itPlacement02() throws Exception {
        String token = newLearnerToken("placement-submit");
        long before = attemptRepository.count();
        MvcResult submitted = mockMvc.perform(post("/api/student/placement-tests/current/submit")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validToeicSubmission()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();
        long attemptId = json(submitted).path("id").asLong();
        assertEquals(before + 1, attemptRepository.count());
        assertTrue(attemptRepository.findById(attemptId).isPresent());
    }

    @Test
    @DisplayName("IT_PLACEMENT_03")
    void itPlacement03() throws Exception {
        String token = newLearnerToken("placement-result");
        MvcResult submitted = mockMvc.perform(post("/api/student/placement-tests/current/submit")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validToeicSubmission()))
                .andExpect(status().isOk()).andReturn();
        long attemptId = json(submitted).path("id").asLong();
        mockMvc.perform(get("/api/student/placement-tests/current")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestAttempt.id").value(attemptId))
                .andExpect(jsonPath("$.latestAttempt.overallScore").exists());
    }

    @Test
    @DisplayName("IT_PLACEMENT_04")
    void itPlacement04() throws Exception {
        long before = attemptRepository.count();
        mockMvc.perform(get("/api/student/placement-tests/current"))
                .andExpect(status().is4xxClientError());
        assertEquals(before, attemptRepository.count());
    }

    private String newLearnerToken(String prefix) throws Exception {
        String email = registerVerifiedLearner(mockMvc, userRepository, authTokenRepository, prefix);
        return login(mockMvc, email, PASSWORD);
    }

    private String validToeicSubmission() {
        return """
                {
                  "examType":"TOEIC",
                  "listeningAnswers":{},
                  "readingAnswers":{},
                  "deviceCheck":{"completed":true}
                }
                """;
    }
}
