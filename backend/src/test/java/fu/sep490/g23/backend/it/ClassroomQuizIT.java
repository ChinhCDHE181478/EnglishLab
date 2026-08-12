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
import java.util.Map;

import static fu.sep490.g23.backend.it.ItSupport.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = sharedClassroomId(mockMvc, teacherToken, learnerToken);
        QuizFixture quiz = createQuiz(teacherToken, classroomId);
        mockMvc.perform(get("/api/teacher/classrooms/" + classroomId + "/quizzes")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + quiz.quizId() + ")]").exists());
    }

    @Test
    @DisplayName("IT_QUIZ_02")
    void itQuiz02() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = sharedClassroomId(mockMvc, teacherToken, learnerToken);
        QuizFixture quiz = createQuiz(teacherToken, classroomId);
        mockMvc.perform(delete("/api/teacher/quizzes/" + quiz.quizId())
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/teacher/classrooms/" + classroomId + "/quizzes")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + quiz.quizId() + ")]").isEmpty());
    }

    @Test
    @DisplayName("IT_QUIZ_03")
    void itQuiz03() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = sharedClassroomId(mockMvc, teacherToken, learnerToken);
        QuizFixture quiz = createQuiz(teacherToken, classroomId);
        openQuiz(teacherToken, quiz.quizId());
        MvcResult learnerQuizzes = mockMvc.perform(get("/api/student/classrooms/quizzes")
                        .header("Authorization", bearer(learnerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + quiz.quizId() + ")]").exists())
                .andReturn();
        JsonNode listedQuiz = null;
        for (JsonNode row : items(json(learnerQuizzes))) {
            if (row.path("id").asLong() == quiz.quizId()) listedQuiz = row;
        }
        assertTrue(listedQuiz != null
                        && listedQuiz.path("questions").get(0).path("correctAnswer").isNull(),
                "Learner quiz payload must not expose the answer key");
    }

    @Test
    @DisplayName("IT_QUIZ_04")
    void itQuiz04() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = sharedClassroomId(mockMvc, teacherToken, learnerToken);
        QuizFixture quiz = createQuiz(teacherToken, classroomId);
        openQuiz(teacherToken, quiz.quizId());
        String answersJson = mapper().writeValueAsString(Map.of(String.valueOf(quiz.questionId()), "A"));
        String requestBody = mapper().writeValueAsString(Map.of("answersJson", answersJson));
        mockMvc.perform(post("/api/student/quizzes/" + quiz.quizId() + "/submit")
                        .header("Authorization", bearer(learnerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submitted").value(true))
                .andExpect(jsonPath("$.myScore").value(10.0));
    }

    private QuizFixture createQuiz(String teacherToken, long classroomId) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/teacher/classrooms/" + classroomId + "/quizzes")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"IT Quiz %s",
                                  "description":"Integration quiz",
                                  "timeLimitMinutes":15,
                                  "passingScore":50,
                                  "dueAt":"%s",
                                  "questions":[{
                                    "sortOrder":1,
                                    "prompt":"Choose A",
                                    "optionsJson":"[\\\"A\\\",\\\"B\\\"]",
                                    "correctAnswer":"A",
                                    "explanation":"A is correct"
                                  }]
                                }
                                """.formatted(UUID.randomUUID(), LocalDateTime.now().plusDays(1).withNano(0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        JsonNode body = json(created);
        return new QuizFixture(body.path("id").asLong(), body.path("questions").get(0).path("id").asLong());
    }

    private void openQuiz(String teacherToken, long quizId) throws Exception {
        mockMvc.perform(patch("/api/teacher/quizzes/" + quizId + "/open")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    private record QuizFixture(long quizId, long questionId) {
    }
}
