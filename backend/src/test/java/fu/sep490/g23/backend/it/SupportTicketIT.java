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
 * Integration Test – Submit Support Ticket
 * Excel sheet: IT_SUPPORT | SRS: UC-07 Submit Support Ticket, UC-44 Resolve Support Tickets
 * Chạy: mvnw -Dtest=SupportTicketIT test
 */
@EnglishLabIT
public class SupportTicketIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_SUPPORT_01")
    void itSupport01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult created = createTicket(token, "IT ticket create")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        long ticketId = json(created).path("id").asLong();
        mockMvc.perform(get("/api/student/support-tickets/" + ticketId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("IT ticket create"));
    }

    @Test
    @DisplayName("IT_SUPPORT_02")
    void itSupport02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        long ticketId = json(createTicket(token, "IT ticket own list")
                .andExpect(status().isOk()).andReturn()).path("id").asLong();
        mockMvc.perform(get("/api/student/support-tickets").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + ticketId + ")]").exists());
    }

    @Test
    @DisplayName("IT_SUPPORT_03")
    void itSupport03() throws Exception {
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long ticketId = json(createTicket(learnerToken, "IT ticket staff queue")
                .andExpect(status().isOk()).andReturn()).path("id").asLong();
        String staffToken = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/support-tickets")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + ticketId + ")]").exists());
    }

    @Test
    @DisplayName("IT_SUPPORT_04")
    void itSupport04() throws Exception {
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long ticketId = json(createTicket(learnerToken, "IT ticket resolve")
                .andExpect(status().isOk()).andReturn()).path("id").asLong();
        String staffToken = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(patch("/api/staff/support-tickets/" + ticketId)
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        mockMvc.perform(get("/api/staff/support-tickets/" + ticketId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").exists());
    }

    private org.springframework.test.web.servlet.ResultActions createTicket(String token, String subject)
            throws Exception {
        return mockMvc.perform(post("/api/student/support-tickets")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"subject":"%s","message":"Integration test support message","category":"OTHER"}
                        """.formatted(subject)));
    }
}
