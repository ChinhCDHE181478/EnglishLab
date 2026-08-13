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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Test
    @DisplayName("IT_ASSIGN_01")
    void itAssign01() throws Exception {
        String staffToken = login(mockMvc, STAFF, PASSWORD);
        long requestId = createEnrollmentRequest(staffToken, "assign-list");
        mockMvc.perform(get("/api/staff/enrollment-requests")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + requestId + ")]").exists());
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/staff/enrollment-requests")
                        .header("Authorization", bearer(learnerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT_ASSIGN_02")
    void itAssign02() throws Exception {
        String staffToken = login(mockMvc, STAFF, PASSWORD);
        long requestId = createEnrollmentRequest(staffToken, "assign-filter");
        MvcResult result = mockMvc.perform(get("/api/staff/enrollment-requests")
                        .param("status", "SUBMITTED")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk()).andReturn();
        JsonNode rows = items(json(result));
        assertFalse(rows.isEmpty());
        boolean found = false;
        for (JsonNode row : rows) {
            assertTrue("SUBMITTED".equals(row.path("status").asText()));
            if (row.path("id").asLong() == requestId) found = true;
        }
        assertTrue(found, "The seeded SUBMITTED request must be returned");
    }

    @Test
    @DisplayName("IT_ASSIGN_03")
    void itAssign03() throws Exception {
        String staffToken = login(mockMvc, STAFF, PASSWORD);
        long requestId = createEnrollmentRequest(staffToken, "assign-class");
        moveToWaitingForClass(staffToken, requestId);
        MvcResult available = mockMvc.perform(get("/api/staff/enrollment-requests/" + requestId + "/available-classrooms")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk()).andReturn();
        JsonNode classroomIds = json(available);
        assertFalse(classroomIds.isEmpty(), "An assignable classroom fixture is required");
        long classroomId = classroomIds.get(0).asLong();
        mockMvc.perform(patch("/api/staff/enrollment-requests/" + requestId + "/assign-class")
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classroomId\":" + classroomId + ",\"note\":\"IT assignment\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLASS_ASSIGNED"))
                .andExpect(jsonPath("$.assignedClassroomId").value(classroomId));
    }

    @Test
    @DisplayName("IT_ASSIGN_04")
    void itAssign04() throws Exception {
        String staffToken = login(mockMvc, STAFF, PASSWORD);
        long requestId = createEnrollmentRequest(staffToken, "assign-reject");
        mockMvc.perform(patch("/api/staff/enrollment-requests/" + requestId + "/reject")
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Not eligible for this intake\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Not eligible for this intake"));
        mockMvc.perform(patch("/api/staff/enrollment-requests/" + requestId + "/assign-class")
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classroomId\":1}"))
                .andExpect(status().is4xxClientError());
    }

    private long createEnrollmentRequest(String staffToken, String prefix) throws Exception {
        String email = registerVerifiedLearner(mockMvc, userRepository, authTokenRepository, prefix);
        String learnerToken = login(mockMvc, email, PASSWORD);
        JsonNode programs = items(json(mockMvc.perform(get("/api/staff/classrooms/training-programs")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk()).andReturn()));
        assertFalse(programs.isEmpty(), "A published training program fixture is required");
        long programId = programs.get(0).path("id").asLong();
        MvcResult created = mockMvc.perform(post("/api/student/course-enrollment-requests")
                        .header("Authorization", bearer(learnerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId":%d,
                                  "contactName":"IT Assignment Learner",
                                  "contactEmail":"%s",
                                  "contactPhone":"0900000003",
                                  "consultationTrack":"IELTS"
                                }
                                """.formatted(programId, email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn();
        return json(created).path("id").asLong();
    }

    private void moveToWaitingForClass(String staffToken, long requestId) throws Exception {
        mockMvc.perform(patch("/api/staff/enrollment-requests/" + requestId + "/schedule-test")
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"appointmentAt":"%s","location":"EnglishLab center","note":"IT schedule"}
                                """.formatted(LocalDateTime.now().plusDays(1).withNano(0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TEST_SCHEDULED"));
        mockMvc.perform(patch("/api/staff/enrollment-requests/" + requestId + "/complete-test")
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eligible\":true,\"placementLevel\":\"INTERMEDIATE\",\"note\":\"Qualified\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_CLASS"));
    }
}
