package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
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
 * Integration Test – Assign Teacher to Classroom
 * Excel sheet: IT_ASNTEACH | SRS: UC-37 Assign Teacher to Classroom
 * Chạy: mvnw -Dtest=AssignTeacherIT test
 */
@EnglishLabIT
public class AssignTeacherIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClassroomTeacherAssignmentRepository assignmentRepository;

    @Test
    @DisplayName("IT_ASNTEACH_01")
    void itAsnteach01() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        MvcResult list = mockMvc.perform(get("/api/staff/classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(list.getResponse().getContentAsString());
        assertFalse(items.isEmpty(), "A classroom fixture is required");
        long oid = items.get(0).path("id").asLong();
        MvcResult teachers = mockMvc.perform(get("/api/staff/classrooms/teachers")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode tarr = mapper().readTree(teachers.getResponse().getContentAsString());
        assertFalse(tarr.isEmpty(), "A teacher fixture is required");
        long tid = tarr.get(0).path("id").asLong();
        mockMvc.perform(post("/api/staff/classrooms/" + oid + "/teachers/" + tid + "/assign")
                        .param("role", "SUBSTITUTE")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(tid))
                .andExpect(jsonPath("$.role").value("SUBSTITUTE"));
        assertFalse(assignmentRepository.findAllByClassSectionIdAndTeacherId(oid, tid).isEmpty());
    }

    @Test
    @DisplayName("IT_ASNTEACH_02")
    void itAsnteach02() throws Exception {
        String staff = login(mockMvc, STAFF, PASSWORD);
        MvcResult list = mockMvc.perform(get("/api/staff/classrooms").header("Authorization", bearer(staff)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(list.getResponse().getContentAsString());
        assertFalse(items.isEmpty(), "A classroom fixture is required");
        long oid = items.get(0).path("id").asLong();
        JsonNode teachers = json(mockMvc.perform(get("/api/staff/classrooms/teachers")
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isOk()).andReturn());
        assertFalse(teachers.isEmpty(), "A teacher fixture is required");
        long teacherId = teachers.get(0).path("id").asLong();
        long before = assignmentRepository.count();
        String learner = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(post("/api/staff/classrooms/" + oid + "/teachers/" + teacherId + "/assign")
                        .header("Authorization", bearer(learner)))
                .andExpect(status().isForbidden());
        assertEquals(before, assignmentRepository.count());
    }
}
