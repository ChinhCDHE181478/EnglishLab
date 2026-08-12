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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Access Online Learning Materials
 * Excel sheet: IT_ACCESS | SRS: UC-48 Access Online Learning Materials
 * Chạy: mvnw -Dtest=AccessMaterialsIT test
 */
@EnglishLabIT
public class AccessMaterialsIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ACCESS_01")
    void itAccess01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        // chưa enroll => 403/404 vẫn chứng minh security/service wiring
        long courseId = enrolledCourseId(token);
        mockMvc.perform(get("/api/student/online-courses/" + courseId + "/content")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId))
                .andExpect(jsonPath("$.modules").isArray());
        mockMvc.perform(get("/api/student/online-courses/" + courseId + "/content"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_ACCESS_02")
    void itAccess02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        long courseId = enrolledCourseId(token);
        MvcResult content = mockMvc.perform(get("/api/student/online-courses/" + courseId + "/content")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        long lessonId = 0;
        for (JsonNode module : json(content).path("modules")) {
            if (!module.path("lessons").isEmpty()) {
                lessonId = module.path("lessons").get(0).path("id").asLong();
                break;
            }
        }
        if (lessonId == 0) throw new AssertionError("An enrolled course with a lesson is required");
        mockMvc.perform(patch("/api/student/online-courses/" + courseId + "/lessons/" + lessonId + "/progress")
                        .param("completed", "true")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedLessonIds").isArray())
                .andExpect(jsonPath("$.completedLessonIds[?(@ == " + lessonId + ")]").exists());
    }

    private long enrolledCourseId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/student/online-courses/my-enrollments")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode enrollments = items(json(result));
        assertFalse(enrollments.isEmpty(), "An enrolled learner fixture is required");
        return enrollments.get(0).path("courseId").asLong();
    }
}
