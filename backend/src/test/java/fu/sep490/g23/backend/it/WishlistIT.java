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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Wishlist Courses
 * Excel sheet: IT_WISHLIST | SRS: UC-45 Wishlist Courses
 * Chạy: mvnw -Dtest=WishlistIT test
 */
@EnglishLabIT
public class WishlistIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_WISHLIST_01")
    void itWishlist01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        long courseId = availableCourseId(token);
        mockMvc.perform(delete("/api/student/commerce/wishlist/" + courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/student/commerce/wishlist/" + courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId));
        MvcResult wishlist = mockMvc.perform(get("/api/student/commerce/wishlist")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertTrue(containsCourse(items(json(wishlist)), courseId));
    }

    @Test
    @DisplayName("IT_WISHLIST_02")
    void itWishlist02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        long courseId = availableCourseId(token);
        mockMvc.perform(delete("/api/student/commerce/wishlist/" + courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/student/commerce/wishlist/" + courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        MvcResult result = mockMvc.perform(get("/api/student/commerce/wishlist")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertTrue(containsCourse(items(json(result)), courseId));
    }

    private long availableCourseId(String token) throws Exception {
        JsonNode enrolled = items(json(mockMvc.perform(get("/api/student/online-courses/my-enrollments")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn()));
        JsonNode published = items(json(mockMvc.perform(get("/api/online-courses"))
                .andExpect(status().isOk()).andReturn()));
        for (JsonNode course : published) {
            long id = course.path("id").asLong();
            boolean owned = false;
            for (JsonNode enrollment : enrolled) {
                if (enrollment.path("courseId").asLong() == id) owned = true;
            }
            if (!owned) return id;
        }
        throw new AssertionError("A published course not yet enrolled by the learner is required");
    }

    private boolean containsCourse(JsonNode rows, long courseId) {
        for (JsonNode row : rows) {
            if (row.path("id").asLong() == courseId) return true;
        }
        return false;
    }
}
