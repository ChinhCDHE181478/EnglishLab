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
 * Integration Test – Manage Online Courses
 * Excel sheet: IT_ONLINE | SRS: UC-33a Create Online Course, UC-33b View Online Courses, UC-33c Update Online Course, UC-33d Deactivate Online Course
 * Chạy: mvnw -Dtest=OnlineCourseCmIT test
 */
@EnglishLabIT
public class OnlineCourseCmIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ONLINE_01")
    void itOnline01() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/online-courses").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ONLINE_02")
    void itOnline02() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        String title = "IT Online " + UUID.randomUUID().toString().substring(0, 8);
        MvcResult created = mockMvc.perform(post("/api/content-manager/online-courses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseBody(title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andReturn();
        long courseId = json(created).path("id").asLong();
        mockMvc.perform(get("/api/content-manager/online-courses/" + courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title));
    }

    @Test
    @DisplayName("IT_ONLINE_03")
    void itOnline03() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        MvcResult created = mockMvc.perform(post("/api/content-manager/online-courses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseBody("IT Online before")))
                .andExpect(status().isCreated()).andReturn();
        long id = json(created).path("id").asLong();
        mockMvc.perform(put("/api/content-manager/online-courses/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseBody("IT Online updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT Online updated"));
        mockMvc.perform(get("/api/content-manager/online-courses/" + id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT Online updated"));
    }

    @Test
    @DisplayName("IT_ONLINE_04")
    void itOnline04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/content-manager/online-courses").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    private String courseBody(String title) {
        return """
                {
                  "title":"%s",
                  "shortDescription":"Integration-tested course",
                  "description":"Created by OnlineCourseCmIT",
                  "category":"IELTS",
                  "level":"BEGINNER",
                  "status":"DRAFT",
                  "targetScore":"6.5",
                  "targetBand":6.5,
                  "price":100000,
                  "totalLessons":0,
                  "totalHours":0,
                  "displayOrder":99,
                  "featured":false,
                  "modules":[]
                }
                """.formatted(title);
    }
}
