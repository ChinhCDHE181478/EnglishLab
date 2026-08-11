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
 * Integration Test – View Timetable
 * Excel sheet: IT_TIMETABLE | SRS: UC-09 View Timetable
 * Chạy: mvnw -Dtest=TimetableIT test
 */
@EnglishLabIT
public class TimetableIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_TIMETABLE_01")
    void itTimetable01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/classrooms/my-classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_TIMETABLE_02")
    void itTimetable02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/student/classrooms/my-classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(r.getResponse().getContentAsString());
        Assumptions.assumeTrue(items.size() > 0, "Learner chưa có lớp");
        long id = items.get(0).path("id").asLong();
        mockMvc.perform(get("/api/student/classrooms/" + id + "/sessions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
