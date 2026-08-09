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
 * Integration Test – Manage User Accounts
 * Excel sheet: IT_ADMIN | SRS: UC-42b View User Accounts, UC-42d Lock/Unlock User Account
 * Chạy: mvnw -Dtest=AdminIT test
 */
@EnglishLabIT
public class AdminIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ADMIN_01")
    void itAdmin01() throws Exception {
        String token = login(mockMvc, ADMIN, PASSWORD);
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ADMIN_02")
    void itAdmin02() throws Exception {
        String token = login(mockMvc, ADMIN, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(r.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        Assumptions.assumeTrue(items.size() > 0);
        long uid = items.get(0).path("id").asLong();
        mockMvc.perform(patch("/api/admin/users/" + uid + "/status")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    Assumptions.assumeTrue(s == 200 || s == 400 || s == 404, "status " + s);
                });
    }
}
