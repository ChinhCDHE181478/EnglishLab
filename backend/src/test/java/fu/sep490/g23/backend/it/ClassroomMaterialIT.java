package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import fu.sep490.g23.backend.repository.AuthTokenRepository;
import fu.sep490.g23.backend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static fu.sep490.g23.backend.it.ItSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test - View Class Learning Materials
 * Excel sheet: IT_MATERIAL | SRS: UC-49 View Class Learning Materials
 * Run: mvnw -Dtest=ClassroomMaterialIT test
 */
@EnglishLabIT
public class ClassroomMaterialIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    /** An enrolled learner can read a material posted by the classroom teacher. */
    @Test
    @DisplayName("IT_MATERIAL_01")
    void itMaterial01() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        String learnerToken = login(mockMvc, LEARNER, PASSWORD);
        long classroomId = ItClassroomFixture.sharedClassroomId(mockMvc, teacherToken);

        String title = "IT_MATERIAL_01 " + System.currentTimeMillis();
        long materialId = createMaterial(teacherToken, classroomId, title);

        JsonNode material = findMaterial(learnerToken, classroomId, materialId);
        assertEquals(title, material.path("title").asText(), "Learner must receive the posted material");
        assertEquals("https://englishlab.test/it-material.pdf", material.path("fileUrl").asText(),
                "Learner must receive the material file URL");
    }

    /** A learner who is not enrolled in the classroom cannot view its materials. */
    @Test
    @DisplayName("IT_MATERIAL_02")
    void itMaterial02() throws Exception {
        String teacherToken = login(mockMvc, TEACHER, PASSWORD);
        long classroomId = ItClassroomFixture.sharedClassroomId(mockMvc, teacherToken);
        String title = "IT_MATERIAL_02 " + System.currentTimeMillis();
        createMaterial(teacherToken, classroomId, title);

        String outsiderEmail = registerVerifiedLearner(mockMvc, userRepository, authTokenRepository, "material");
        String outsiderToken = login(mockMvc, outsiderEmail, PASSWORD);
        MvcResult outsider = mockMvc.perform(get("/api/student/classrooms/" + classroomId + "/materials")
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(!outsider.getResponse().getContentAsString().contains(title),
                "The access-denied response must not expose classroom material data");
    }

    private long createMaterial(String teacherToken, long classroomId, String title) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/teacher/classrooms/" + classroomId + "/materials")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","fileUrl":"https://englishlab.test/it-material.pdf","fileType":"PDF",
                                 "description":"Material for integration test","materialType":"DOCUMENT"}
                                """.formatted(title)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        long materialId = json(created).path("id").asLong();
        assertTrue(materialId > 0, "The created material must have an id");
        return materialId;
    }

    private JsonNode findMaterial(String learnerToken, long classroomId, long materialId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/student/classrooms/" + classroomId + "/materials")
                        .header("Authorization", bearer(learnerToken)))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode material : items(json(result))) {
            if (material.path("id").asLong() == materialId) {
                return material;
            }
        }
        throw new IllegalStateException("Material id " + materialId + " was not returned for classroom " + classroomId);
    }
}
