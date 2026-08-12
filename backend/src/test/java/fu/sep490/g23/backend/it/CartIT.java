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
 * Integration Test – Add Courses to Cart
 * Excel sheet: IT_CART | SRS: UC-46 Add Courses to Cart
 * Chạy: mvnw -Dtest=CartIT test
 */
@EnglishLabIT
public class CartIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_CART_01")
    void itCart01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(delete("/api/student/commerce/cart").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        long courseId = payableCourseId(token);
        mockMvc.perform(post("/api/student/commerce/cart/" + courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId));
        MvcResult cart = mockMvc.perform(get("/api/student/commerce/cart")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertTrue(containsCourse(items(json(cart)), courseId));
    }

    @Test
    @DisplayName("IT_CART_02")
    void itCart02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(delete("/api/student/commerce/cart").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        long courseId = payableCourseId(token);
        mockMvc.perform(post("/api/student/commerce/cart/" + courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/student/commerce/cart/" + courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        MvcResult cart = mockMvc.perform(get("/api/student/commerce/cart")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertFalse(containsCourse(items(json(cart)), courseId));
    }

    private long payableCourseId(String token) throws Exception {
        JsonNode enrolled = items(json(mockMvc.perform(get("/api/student/online-courses/my-enrollments")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn()));
        JsonNode published = items(json(mockMvc.perform(get("/api/online-courses"))
                .andExpect(status().isOk()).andReturn()));
        for (JsonNode course : published) {
            long id = course.path("id").asLong();
            if (!containsCourse(enrolled, id)) return id;
        }
        throw new AssertionError("A published course not yet enrolled by the learner is required");
    }

    private boolean containsCourse(JsonNode rows, long courseId) {
        for (JsonNode row : rows) {
            if (row.path("courseId").asLong(row.path("id").asLong()) == courseId) return true;
        }
        return false;
    }
}
