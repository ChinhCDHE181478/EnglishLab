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
 * Integration tests for managing instructor-led courses.
 * Run with: mvnw -Dtest=InstructorLedCourseIT test
 */
@EnglishLabIT
public class InstructorLedCourseIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_INSTRUCTOR_LED_COURSE_01")
    void listsInstructorLedCourses() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/instructor-led-courses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_INSTRUCTOR_LED_COURSE_02")
    void createsAndReadsInstructorLedCourse() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        MvcResult created = createInstructorLedCourse(token, "IT instructor-led course create")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        long instructorLedCourseId = json(created).path("id").asLong();
        mockMvc.perform(get("/api/content-manager/instructor-led-courses/" + instructorLedCourseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT instructor-led course create"));
    }

    @Test
    @DisplayName("IT_INSTRUCTOR_LED_COURSE_03")
    void updatesInstructorLedCourse() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        long instructorLedCourseId = json(createInstructorLedCourse(token, "IT instructor-led course before")
                .andExpect(status().isOk()).andReturn()).path("id").asLong();
        mockMvc.perform(put("/api/content-manager/instructor-led-courses/" + instructorLedCourseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseBody("IT instructor-led course updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT instructor-led course updated"));
        mockMvc.perform(get("/api/content-manager/instructor-led-courses/" + instructorLedCourseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT instructor-led course updated"));
    }

    @Test
    @DisplayName("IT_INSTRUCTOR_LED_COURSE_04")
    void createsCourseUnit() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        long instructorLedCourseId = json(createInstructorLedCourse(token, "IT instructor-led course units")
                .andExpect(status().isOk()).andReturn()).path("id").asLong();
        MvcResult unit = mockMvc.perform(post("/api/content-manager/instructor-led-courses/" + instructorLedCourseId + "/units")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayOrder":1,"title":"IT Unit 1","description":"Integrated unit","sessionPlan":"Practice"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT Unit 1"))
                .andReturn();
        long unitId = json(unit).path("id").asLong();
        mockMvc.perform(get("/api/content-manager/instructor-led-courses/" + instructorLedCourseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.units[?(@.id == " + unitId + ")]").exists());
    }

    @Test
    @DisplayName("IT_INSTRUCTOR_LED_COURSE_05")
    void archivesInstructorLedCourse() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        long instructorLedCourseId = json(createInstructorLedCourse(token, "IT instructor-led course archive")
                .andExpect(status().isOk()).andReturn()).path("id").asLong();
        mockMvc.perform(delete("/api/content-manager/instructor-led-courses/" + instructorLedCourseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/content-manager/instructor-led-courses/" + instructorLedCourseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    private org.springframework.test.web.servlet.ResultActions createInstructorLedCourse(String token, String title)
            throws Exception {
        return mockMvc.perform(post("/api/content-manager/instructor-led-courses")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(courseBody(title)));
    }

    private String courseBody(String title) {
        return """
                {
                  "title":"%s",
                  "code":"IT-%s",
                  "examCategory":"IELTS",
                  "focusSkills":"LISTENING,READING",
                  "targetBand":6.5,
                  "entryLevel":"4.0",
                  "outcomes":"Integration-tested outcome",
                  "teacherGuide":"Integration-tested guide",
                  "totalSessions":10,
                  "status":"DRAFT",
                  "displayOrder":99
                }
                """.formatted(title, UUID.randomUUID().toString().substring(0, 8));
    }
}
