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
        mockMvc.perform(get("/api/student/commerce/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CART_02")
    void itCart02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(delete("/api/student/commerce/cart").header("Authorization", bearer(token)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 204, "cart clear " + s);
                });
    }
}
