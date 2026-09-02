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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Test
    @DisplayName("IT_ENROLL_01")
    void itEnroll01() throws Exception {
        String email = registerVerifiedLearner(mockMvc, userRepository, authTokenRepository, "enroll");
        String token = login(mockMvc, email, PASSWORD);
        String staffToken = login(mockMvc, STAFF, PASSWORD);
        MvcResult programsResult = mockMvc.perform(get("/api/staff/classrooms/instructor-led-courses")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk()).andReturn();
        JsonNode programs = items(json(programsResult));
        if (programs.isEmpty()) throw new AssertionError("A published training program is required");
        long programId = programs.get(0).path("id").asLong();
        MvcResult created = mockMvc.perform(post("/api/student/course-enrollment-requests")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId":%d,
                                  "contactName":"IT Enrollment Learner",
                                  "contactEmail":"%s",
                                  "contactPhone":"0900000002",
                                  "consultationTrack":"IELTS",
                                  "studyWorkGoal":"Integration testing"
                                }
                                """.formatted(programId, email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn();
        long requestId = json(created).path("id").asLong();
        mockMvc.perform(get("/api/student/course-enrollment-requests/my")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + requestId + ")]").exists());
        mockMvc.perform(post("/api/student/course-enrollment-requests")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/student/course-enrollment-requests")
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
