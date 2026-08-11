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
 * Integration Test – View Teaching Schedule
 * Excel sheet: IT_SCHEDULE | SRS: UC-22 View Teaching Schedule
 * Chạy: mvnw -Dtest=TeachingScheduleIT test
 */
@EnglishLabIT
public class TeachingScheduleIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_SCHEDULE_01")
    void itSchedule01() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        mockMvc.perform(get("/api/teacher/classrooms/assigned").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_SCHEDULE_02")
    void itSchedule02() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/teacher/classrooms/assigned").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(r.getResponse().getContentAsString());
        Assumptions.assumeTrue(items.size() > 0);
        long id = items.get(0).path("id").asLong();
        mockMvc.perform(get("/api/teacher/classrooms/" + id + "/sessions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
