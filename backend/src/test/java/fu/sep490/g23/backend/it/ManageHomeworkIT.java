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
 * Integration Test – Manage Homework
 * Excel sheet: IT_MNGHW | SRS: UC-26a Create Homework, UC-26 Manage Homework
 * Chạy: mvnw -Dtest=ManageHomeworkIT test
 */
@EnglishLabIT
public class ManageHomeworkIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_MNGHW_01")
    void itMnghw01() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = sharedClassroomId(mockMvc, teacherToken, learnerToken);
        MvcResult created = createHomework(teacherToken, classroomId, LocalDateTime.now().plusDays(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        long homeworkId = json(created).path("id").asLong();
        mockMvc.perform(get("/api/teacher/classrooms/" + classroomId + "/homework")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + homeworkId + ")]").exists());
    }

    @Test
    @DisplayName("IT_MNGHW_02")
    void itMnghw02() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = sharedClassroomId(mockMvc, teacherToken, learnerToken);
        long studentId = currentUserId(mockMvc, learnerToken);
        MvcResult created = createHomework(teacherToken, classroomId, LocalDateTime.now().plusDays(2))
                .andExpect(status().isOk()).andReturn();
        long homeworkId = json(created).path("id").asLong();
        mockMvc.perform(post("/api/student/classrooms/homework/" + homeworkId + "/submit")
                        .header("Authorization", bearer(learnerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textAnswer\":\"IT learner submission\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(studentId));
        mockMvc.perform(post("/api/teacher/classrooms/homework/" + homeworkId + "/students/" + studentId + "/grade")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":8.5,\"teacherFeedback\":\"Good integration result\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(8.5))
                .andExpect(jsonPath("$.teacherFeedback").value("Good integration result"))
                .andExpect(jsonPath("$.status").value("GRADED"));
    }

    private org.springframework.test.web.servlet.ResultActions createHomework(
            String teacherToken, long classroomId, LocalDateTime deadline
    ) throws Exception {
        return mockMvc.perform(post("/api/teacher/classrooms/" + classroomId + "/homework")
                .header("Authorization", bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title":"IT Homework %s",
                          "instruction":"Complete the integration exercise",
                          "deadline":"%s",
                          "maxScore":10,
                          "allowResubmission":false,
                          "status":"OPEN",
                          "activityType":"TEXT_RESPONSE",
                          "gradingMode":"TEACHER"
                        }
                        """.formatted(UUID.randomUUID(), deadline.withNano(0))));
    }
}
