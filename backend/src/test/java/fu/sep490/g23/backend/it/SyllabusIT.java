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
        MvcResult created = createProgram(token, "IT syllabus create")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        long programId = json(created).path("id").asLong();
        mockMvc.perform(get("/api/content-manager/curriculum-programs/" + programId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT syllabus create"));
    }

    @Test
    @DisplayName("IT_SYLLABUS_03")
    void itSyllabus03() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        long programId = json(createProgram(token, "IT syllabus before")
                .andExpect(status().isOk()).andReturn()).path("id").asLong();
        mockMvc.perform(put("/api/content-manager/curriculum-programs/" + programId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(programBody("IT syllabus updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT syllabus updated"));
        mockMvc.perform(get("/api/content-manager/curriculum-programs/" + programId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT syllabus updated"));
    }

    @Test
    @DisplayName("IT_SYLLABUS_04")
    void itSyllabus04() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        long programId = json(createProgram(token, "IT syllabus units")
                .andExpect(status().isOk()).andReturn()).path("id").asLong();
        MvcResult unit = mockMvc.perform(post("/api/content-manager/curriculum-programs/" + programId + "/units")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayOrder":1,"title":"IT Unit 1","description":"Integrated unit","sessionPlan":"Practice"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("IT Unit 1"))
                .andReturn();
        long unitId = json(unit).path("id").asLong();
        mockMvc.perform(get("/api/content-manager/curriculum-programs/" + programId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.units[?(@.id == " + unitId + ")]").exists());
    }

    @Test
    @DisplayName("IT_SYLLABUS_05")
    void itSyllabus05() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        long programId = json(createProgram(token, "IT syllabus archive")
                .andExpect(status().isOk()).andReturn()).path("id").asLong();
        mockMvc.perform(delete("/api/content-manager/curriculum-programs/" + programId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/content-manager/curriculum-programs/" + programId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    private org.springframework.test.web.servlet.ResultActions createProgram(String token, String title)
            throws Exception {
        return mockMvc.perform(post("/api/content-manager/curriculum-programs")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(programBody(title)));
    }

    private String programBody(String title) {
        return """
                {
                  "title":"%s",
                  "code":"IT-%s",
                  "examCategory":"IELTS",
                  "focusSkills":"LISTENING,READING",
                  "targetBand":6.5,
                  "entryLevel":"4.0",
                  "outcomes":"Integration-tested outcome",
                  "teacherGuide":"Integration-tested guide",
                  "totalSessions":10,
                  "status":"DRAFT",
                  "displayOrder":99
                }
                """.formatted(title, UUID.randomUUID().toString().substring(0, 8));
    }
}
