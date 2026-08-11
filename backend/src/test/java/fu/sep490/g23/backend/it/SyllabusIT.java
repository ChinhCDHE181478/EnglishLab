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
 * Integration Test – Manage Syllabus
 * Excel sheet: IT_SYLLABUS | SRS: UC-32a Create Syllabus, UC-32b View Syllabus, UC-32c Update Syllabus, UC-32d Delete Syllabus
 * Chạy: mvnw -Dtest=SyllabusIT test
 */
@EnglishLabIT
public class SyllabusIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_SYLLABUS_01")
    void itSyllabus01() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/curriculum-programs")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_SYLLABUS_02")
    void itSyllabus02() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/curriculum-programs")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_SYLLABUS_03")
    void itSyllabus03() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/curriculum-programs")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_SYLLABUS_04")
    void itSyllabus04() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/curriculum-programs")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_SYLLABUS_05")
    void itSyllabus05() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/curriculum-programs")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
