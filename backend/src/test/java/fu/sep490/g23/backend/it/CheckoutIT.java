package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.payment.PaymentOrder;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.service.payment.PayosProperties;
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
import java.util.LinkedHashMap;
import java.util.Map;
import vn.payos.PayOS;
import vn.payos.core.ClientOptions;

import static fu.sep490.g23.backend.it.ItSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private PayosProperties payosProperties;

    @Test
    @DisplayName("IT_CHECKOUT_01")
    void itCheckout01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        long courseId = payableCourseId(token);
        mockMvc.perform(post("/api/student/payments/quote")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseIds\":[" + courseId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").isNumber());
    }

    @Test
    @DisplayName("IT_CHECKOUT_02")
    void itCheckout02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(delete("/api/student/commerce/cart").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        long before = paymentOrderRepository.count();
        mockMvc.perform(post("/api/student/payments/payos/link")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseIds\":[],\"classroomOfferingIds\":[]}"))
                .andExpect(status().is4xxClientError());
        assertEquals(before, paymentOrderRepository.count());
    }

    @Test
    @DisplayName("IT_CHECKOUT_03")
    void itCheckout03() throws Exception {
        User learner = userRepository.findByEmail(LEARNER).orElseThrow();
        long orderCode = 8_000_000_000L + Math.abs(UUID.randomUUID().getMostSignificantBits() % 900_000_000L);
        PaymentOrder order = paymentOrderRepository.saveAndFlush(PaymentOrder.builder()
                .orderCode(orderCode)
                .student(learner)
                .amount(10_000L)
                .originalAmount(10_000L)
                .systemDiscountAmount(0L)
                .couponDiscountAmount(0L)
                .couponReservationReleased(true)
                .description("ITWEBHOOK")
                .status(PaymentOrderStatus.PENDING)
                .build());

        boolean oldEnabled = payosProperties.isEnabled();
        String oldClientId = payosProperties.getClientId();
        String oldApiKey = payosProperties.getApiKey();
        String oldChecksum = payosProperties.getChecksumKey();
        try {
            payosProperties.setEnabled(true);
            payosProperties.setClientId("it-client");
            payosProperties.setApiKey("it-api-key");
            payosProperties.setChecksumKey("it-checksum-key");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderCode", orderCode);
            data.put("code", "00");
            data.put("reference", "IT-REF-" + orderCode);
            PayOS client = new PayOS(ClientOptions.builder()
                    .clientId(payosProperties.getClientId())
                    .apiKey(payosProperties.getApiKey())
                    .checksumKey(payosProperties.getChecksumKey())
                    .build());
            String signature = client.getCrypto().createSignatureFromObj(data, payosProperties.getChecksumKey());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", true);
            payload.put("code", "00");
            payload.put("data", data);
            payload.put("signature", signature);
            String body = mapper().writeValueAsString(payload);
            mockMvc.perform(post("/api/payos/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
            assertEquals(PaymentOrderStatus.PAID,
                    paymentOrderRepository.findById(order.getId()).orElseThrow().getStatus());
            mockMvc.perform(post("/api/payos/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        } finally {
            payosProperties.setEnabled(oldEnabled);
            payosProperties.setClientId(oldClientId);
            payosProperties.setApiKey(oldApiKey);
            payosProperties.setChecksumKey(oldChecksum);
        }
    }

    @Test
    @DisplayName("IT_CHECKOUT_04")
    void itCheckout04() throws Exception {
        long before = paymentOrderRepository.count();
        mockMvc.perform(post("/api/student/payments/payos/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseIds\":[1]}"))
                .andExpect(status().is4xxClientError());
        assertEquals(before, paymentOrderRepository.count());
    }

    @Test
    @DisplayName("IT_CHECKOUT_05")
    void itCheckout05() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/payments/orders")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private long payableCourseId(String token) throws Exception {
        JsonNode enrolled = items(json(mockMvc.perform(get("/api/student/online-courses/my-enrollments")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn()));
        JsonNode published = items(json(mockMvc.perform(get("/api/online-courses"))
                .andExpect(status().isOk()).andReturn()));
        for (JsonNode course : published) {
            long id = course.path("id").asLong();
            boolean alreadyEnrolled = false;
            for (JsonNode enrollment : enrolled) {
                if (enrollment.path("courseId").asLong() == id) alreadyEnrolled = true;
            }
            if (!alreadyEnrolled) return id;
        }
        throw new AssertionError("A published course not yet enrolled by the learner is required");
    }
}
