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
        MvcResult list = mockMvc.perform(get("/api/online-courses")).andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(list.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        Assumptions.assumeTrue(items.size() > 0, "Cần >=1 public course");
        String id = items.get(0).path("id").asText(null);
        String slug = items.get(0).path("slug").asText(null);
        String key = (slug != null && !slug.isBlank()) ? slug : id;
        mockMvc.perform(get("/api/online-courses/" + key)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_COURSE_03")
    void itCourse03() throws Exception {
        mockMvc.perform(get("/api/online-courses").param("keyword", "IELTS"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_COURSE_04")
    void itCourse04() throws Exception {
        mockMvc.perform(get("/api/online-courses").param("keyword", "__no_such_course_xyz__"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_COURSE_05")
    void itCourse05() throws Exception {
        mockMvc.perform(get("/api/online-courses/categories")).andExpect(status().isOk());
    }
}
