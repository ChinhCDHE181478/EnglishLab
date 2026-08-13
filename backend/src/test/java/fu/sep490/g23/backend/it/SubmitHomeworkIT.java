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
 * Integration Test – Submit Homework
 * Excel sheet: IT_HOMEWORK | SRS: UC-13 Submit Homework
 * Chạy: mvnw -Dtest=SubmitHomeworkIT test
 */
@EnglishLabIT
public class SubmitHomeworkIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_HOMEWORK_01")
    void itHomework01() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = sharedClassroomId(mockMvc, teacherToken, learnerToken);
        long homeworkId = createHomework(teacherToken, classroomId, LocalDateTime.now().plusDays(2));
        mockMvc.perform(post("/api/student/classrooms/homework/" + homeworkId + "/submit")
                        .header("Authorization", bearer(learnerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textAnswer\":\"IT_HOMEWORK_01 answer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeworkId").value(homeworkId))
                .andExpect(jsonPath("$.submitted").value(true))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("IT_HOMEWORK_02")
    void itHomework02() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = sharedClassroomId(mockMvc, teacherToken, learnerToken);
        long homeworkId = createHomework(teacherToken, classroomId, LocalDateTime.now().plusDays(2));
        mockMvc.perform(post("/api/student/classrooms/homework/" + homeworkId + "/submit")
                        .header("Authorization", bearer(learnerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textAnswer\":\"Persisted own submission\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/student/classrooms/my-homework")
                        .header("Authorization", bearer(learnerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + homeworkId + ")].mySubmission.textAnswer")
                        .value("Persisted own submission"));
    }

    @Test
    @DisplayName("IT_HOMEWORK_03")
    void itHomework03() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = sharedClassroomId(mockMvc, teacherToken, learnerToken);
        long studentId = currentUserId(mockMvc, learnerToken);
        long homeworkId = createHomework(teacherToken, classroomId, LocalDateTime.now().minusMinutes(1));
        mockMvc.perform(post("/api/student/classrooms/homework/" + homeworkId + "/submit")
                        .header("Authorization", bearer(learnerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textAnswer\":\"Late submission must fail\"}"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/teacher/classrooms/homework/" + homeworkId + "/submissions")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.studentId == " + studentId + ")].submitted").value(false));
    }

    private long createHomework(String teacherToken, long classroomId, LocalDateTime deadline) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/classrooms/" + classroomId + "/homework")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"IT learner homework %s",
                                  "instruction":"Submit a text answer",
                                  "deadline":"%s",
                                  "maxScore":10,
                                  "allowResubmission":false,
                                  "status":"OPEN",
                                  "activityType":"TEXT_RESPONSE",
                                  "gradingMode":"TEACHER"
                                }
                                """.formatted(UUID.randomUUID(), deadline.withNano(0))))
                .andExpect(status().isOk()).andReturn();
        return json(result).path("id").asLong();
    }
}
