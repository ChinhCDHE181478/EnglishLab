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
 * Integration Test – Assign Learner to Classroom
 * Excel sheet: IT_ASSIGN | SRS: UC-38 Assign Learner to Classroom
 * Chạy: mvnw -Dtest=AssignLearnerIT test
 */
@EnglishLabIT
public class AssignLearnerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ASSIGN_01")
    void itAssign01() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/enrollment-requests").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ASSIGN_02")
    void itAssign02() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/enrollment-requests").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ASSIGN_03")
    void itAssign03() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ASSIGN_04")
    void itAssign04() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/enrollment-requests").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
