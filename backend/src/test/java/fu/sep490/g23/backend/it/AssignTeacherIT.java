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
 * Integration Test – Assign Teacher to Classroom
 * Excel sheet: IT_ASNTEACH | SRS: UC-37 Assign Teacher to Classroom
 * Chạy: mvnw -Dtest=AssignTeacherIT test
 */
@EnglishLabIT
public class AssignTeacherIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ASNTEACH_01")
    void itAsnteach01() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        MvcResult list = mockMvc.perform(get("/api/staff/classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(list.getResponse().getContentAsString());
        Assumptions.assumeTrue(items.size() > 0);
        long oid = items.get(0).path("id").asLong();
        MvcResult teachers = mockMvc.perform(get("/api/staff/classrooms/teachers")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode tarr = mapper().readTree(teachers.getResponse().getContentAsString());
        Assumptions.assumeTrue(tarr.size() > 0);
        long tid = tarr.get(0).path("id").asLong();
        mockMvc.perform(post("/api/staff/classrooms/" + oid + "/teachers/" + tid + "/assign")
                        .param("role", "PRIMARY")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ASNTEACH_02")
    void itAsnteach02() throws Exception {
        String staff = login(mockMvc, STAFF, PASSWORD);
        MvcResult list = mockMvc.perform(get("/api/staff/classrooms").header("Authorization", bearer(staff)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(list.getResponse().getContentAsString());
        Assumptions.assumeTrue(items.size() > 0);
        long oid = items.get(0).path("id").asLong();
        String learner = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(post("/api/staff/classrooms/" + oid + "/teachers/28/assign")
                        .header("Authorization", bearer(learner)))
                .andExpect(status().isForbidden());
    }
}
