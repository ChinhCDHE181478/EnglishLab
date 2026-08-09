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
 * Integration Test – Enroll in Course
 * Excel sheet: IT_ENROLL | SRS: UC-08 Enroll in Course
 * Chạy: mvnw -Dtest=EnrollCourseIT test
 */
@EnglishLabIT
public class EnrollCourseIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ENROLL_01")
    void itEnroll01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(post("/api/student/enrollment-requests")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_ENROLL_02")
    void itEnroll02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/online-courses/my-enrollments")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
