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
 * Integration Test – Access Classroom Learning Materials
 * Excel sheet: IT_MATERIAL | SRS: UC-11 Access Classroom Learning Materials
 * Chạy: mvnw -Dtest=ClassroomMaterialIT test
 */
@EnglishLabIT
public class ClassroomMaterialIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_MATERIAL_01")
    void itMaterial01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/student/classrooms/my-classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(r.getResponse().getContentAsString());
        Assumptions.assumeTrue(items.size() > 0);
        long id = items.get(0).path("id").asLong();
        mockMvc.perform(get("/api/student/classrooms/" + id + "/materials")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
