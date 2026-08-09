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
 * Integration Test – Manage Class Attendance
 * Excel sheet: IT_ATTEND | SRS: UC-23a View Class Attendance, UC-23b Record Class Attendance
 * Chạy: mvnw -Dtest=ClassAttendanceIT test
 */
@EnglishLabIT
public class ClassAttendanceIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ATTEND_01")
    void itAttend01() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/teacher/classrooms/assigned").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(r.getResponse().getContentAsString());
        Assumptions.assumeTrue(items.size() > 0);
        long id = items.get(0).path("id").asLong();
        MvcResult sessions = mockMvc.perform(get("/api/teacher/classrooms/" + id + "/sessions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode sess = mapper().readTree(sessions.getResponse().getContentAsString());
        Assumptions.assumeTrue(sess.isArray() && sess.size() > 0, "Cần session");
        long sid = sess.get(0).path("id").asLong();
        mockMvc.perform(get("/api/teacher/classrooms/sessions/" + sid + "/attendance")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ATTEND_02")
    void itAttend02() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/teacher/classrooms/assigned").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(r.getResponse().getContentAsString());
        Assumptions.assumeTrue(items.size() > 0);
        long id = items.get(0).path("id").asLong();
        MvcResult sessions = mockMvc.perform(get("/api/teacher/classrooms/" + id + "/sessions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode sess = mapper().readTree(sessions.getResponse().getContentAsString());
        Assumptions.assumeTrue(sess.isArray() && sess.size() > 0, "Cần session");
        long sid = sess.get(0).path("id").asLong();
        mockMvc.perform(get("/api/teacher/classrooms/sessions/" + sid + "/attendance")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
