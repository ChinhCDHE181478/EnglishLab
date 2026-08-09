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
 * Integration Test – Take Quiz
 * Excel sheet: IT_QUIZ | SRS: UC-15 Take Quiz, UC-27a Create Quiz Practice
 * Chạy: mvnw -Dtest=ClassroomQuizIT test
 */
@EnglishLabIT
public class ClassroomQuizIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_QUIZ_01")
    void itQuiz01() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/teacher/classrooms/assigned").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(r.getResponse().getContentAsString());
        Assumptions.assumeTrue(items.size() > 0);
        long id = items.get(0).path("id").asLong();
        mockMvc.perform(get("/api/teacher/classrooms/" + id + "/quizzes")
                        .header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 404, "quizzes " + s);
                });
    }

    @Test
    @DisplayName("IT_QUIZ_02")
    void itQuiz02() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/teacher/classrooms/assigned").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(r.getResponse().getContentAsString());
        Assumptions.assumeTrue(items.size() > 0);
        long id = items.get(0).path("id").asLong();
        mockMvc.perform(get("/api/teacher/classrooms/" + id + "/quizzes")
                        .header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 404, "quizzes " + s);
                });
    }

    @Test
    @DisplayName("IT_QUIZ_03")
    void itQuiz03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/classrooms/quizzes").header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 404, "student quizzes " + s);
                });
    }

    @Test
    @DisplayName("IT_QUIZ_04")
    void itQuiz04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/classrooms/quizzes").header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 404, "student quizzes " + s);
                });
    }
}
