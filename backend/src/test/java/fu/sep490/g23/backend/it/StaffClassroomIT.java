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
 * Integration Test – Manage Classrooms
 * Excel sheet: IT_CLASS | SRS: UC-36a Create Classroom, UC-36b View Classrooms, UC-36c Update Classroom
 * Chạy: mvnw -Dtest=StaffClassroomIT test
 */
@EnglishLabIT
public class StaffClassroomIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_CLASS_01")
    void itClass01() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CLASS_02")
    void itClass02() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        MvcResult progs = mockMvc.perform(get("/api/staff/classrooms/training-programs")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode arr = mapper().readTree(progs.getResponse().getContentAsString());
        Assumptions.assumeTrue(arr.isArray() && arr.size() > 0, "Cần training program");
        long pid = arr.get(0).path("id").asLong();
        LocalDate start = LocalDate.now().plusDays(21);
        LocalDate end = start.plusDays(28);
        String body = """
                {
                  "title":"IT Class Proposal %s",
                  "courseOfferingId":%d,
                  "capacity":20,
                  "plannedStartDate":"%s",
                  "plannedEndDate":"%s",
                  "weekdays":["MONDAY","WEDNESDAY"],
                  "sessionStartTime":"18:00:00",
                  "sessionEndTime":"20:00:00",
                  "note":"IT_CLASS_02"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), pid, start, end);
        mockMvc.perform(post("/api/staff/classroom-proposals")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CLASS_03")
    void itClass03() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        MvcResult list = mockMvc.perform(get("/api/staff/classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(list.getResponse().getContentAsString());
        Assumptions.assumeTrue(items.isArray() && items.size() > 0);
        long oid = items.get(0).path("id").asLong();
        for (JsonNode o : items) {
            MvcResult d = mockMvc.perform(get("/api/staff/classrooms/" + o.path("id").asLong())
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk()).andReturn();
            JsonNode detail = mapper().readTree(d.getResponse().getContentAsString());
            if ("OFFLINE".equals(detail.path("deliveryMode").asText())
                    && detail.path("trainingProgramId").isMissingNode()) {
                oid = o.path("id").asLong();
                break;
            }
        }
        MvcResult detailR = mockMvc.perform(get("/api/staff/classrooms/" + oid)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode detail = mapper().readTree(detailR.getResponse().getContentAsString());
        String title = detail.path("title").asText("IT Class");
        String mode = detail.path("deliveryMode").asText("OFFLINE");
        int cap = detail.path("maxCapacity").asInt(20);
        String body = """
                {"title":"%s","deliveryMode":"%s","maxCapacity":%d,"price":0,"shortDescription":"IT update"}
                """.formatted(title, mode, cap);
        mockMvc.perform(put("/api/staff/classrooms/" + oid)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
