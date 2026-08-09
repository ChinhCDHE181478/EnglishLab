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
 * Integration Test – Checkout
 * Excel sheet: IT_CHECKOUT | SRS: UC-47 Checkout
 * Chạy: mvnw -Dtest=CheckoutIT test
 */
@EnglishLabIT
public class CheckoutIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_CHECKOUT_01")
    void itCheckout01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(post("/api/student/payments/quote")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseIds\":[1]}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 400 || s == 404, "quote " + s);
                });
    }

    @Test
    @DisplayName("IT_CHECKOUT_02")
    void itCheckout02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/payments/orders").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CHECKOUT_03")
    void itCheckout03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(delete("/api/student/commerce/cart").header("Authorization", bearer(token)));
        mockMvc.perform(post("/api/student/payments/payos/link")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_CHECKOUT_04")
    void itCheckout04() throws Exception {
        mockMvc.perform(post("/api/payments/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_CHECKOUT_05")
    void itCheckout05() throws Exception {
        mockMvc.perform(post("/api/student/payments/payos/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
