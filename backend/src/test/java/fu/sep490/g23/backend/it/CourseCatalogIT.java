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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – View public courses
 * Excel sheet: IT_COURSE | SRS: UC-02 View public courses
 * Chạy: mvnw -Dtest=CourseCatalogIT test
 */
@EnglishLabIT
public class CourseCatalogIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_COURSE_01")
    void itCourse01() throws Exception {
        mockMvc.perform(get("/api/online-courses")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_COURSE_02")
    void itCourse02() throws Exception {
        MvcResult all = mockMvc.perform(get("/api/online-courses"))
                .andExpect(status().isOk()).andReturn();
        JsonNode courses = items(json(all));
        assertFalse(courses.isEmpty(), "Published course fixture is required");
        String title = courses.get(0).path("title").asText();
        String keyword = title.substring(0, Math.min(title.length(), 5));
        MvcResult filtered = mockMvc.perform(get("/api/online-courses").param("keyword", keyword))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode course : items(json(filtered))) {
            assertTrue(course.path("title").asText().toLowerCase().contains(keyword.toLowerCase()));
        }
    }

    @Test
    @DisplayName("IT_COURSE_03")
    void itCourse03() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/online-courses")
                        .param("keyword", "__no_such_course_xyz__"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(items(json(result)).isEmpty());
    }

    @Test
    @DisplayName("IT_COURSE_04")
    void itCourse04() throws Exception {
        mockMvc.perform(get("/api/online-courses/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_COURSE_05")
    void itCourse05() throws Exception {
        MvcResult list = mockMvc.perform(get("/api/online-courses"))
                .andExpect(status().isOk()).andReturn();
        JsonNode courses = items(json(list));
        assertFalse(courses.isEmpty(), "Published course fixture is required");
        JsonNode first = courses.get(0);
        String key = first.path("slug").asText();
        if (key.isBlank()) key = first.path("id").asText();
        mockMvc.perform(get("/api/online-courses/" + key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(first.path("title").asText()));
        mockMvc.perform(get("/api/online-courses/__unknown_course_slug__"))
                .andExpect(status().is4xxClientError());
    }
}
