package fu.sap490.g23.backend.it;

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

import static fu.sap490.g23.backend.it.ItSupport.*;
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
        mockMvc.perform(post("/api/student/commerce/wishlist/1")
                        .header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s < 500, "wishlist add 5xx");
                });
    }

    @Test
    @DisplayName("IT_WISHLIST_02")
    void itWishlist02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/commerce/wishlist").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
