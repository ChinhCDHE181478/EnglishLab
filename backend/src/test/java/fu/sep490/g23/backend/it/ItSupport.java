package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.entity.AuthToken;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.AuthTokenType;
import fu.sep490.g23.backend.repository.AuthTokenRepository;
import fu.sep490.g23.backend.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.TimeZone;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Helper dùng chung cho các Integration Test (MockMvc).
 * Không có @Test — không chạy file này.
 */
public final class ItSupport {

    static {
        // IDE Test Runner không dùng surefire argLine → phải set sớm trước khi pgjdbc đọc ZoneId.
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static final String PASSWORD = "Password123!";
    public static final String LEARNER = "0386852628z@gmail.com";
    public static final String TEACHER = "classroom.teacher1@englishlab.vn";
    public static final String TM = "training.manager@englishlab.vn";
    public static final String STAFF = "staff@englishlab.vn";
    public static final String MANAGER = "classroom.manager@englishlab.vn";
    public static final String CM = "content.manager@englishlab.vn";
    public static final String ADMIN = "classroom.admin@englishlab.vn";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ItSupport() {
    }

    public static String login(MockMvc mockMvc, String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = MAPPER.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static JsonNode json(MvcResult result) throws Exception {
        return MAPPER.readTree(result.getResponse().getContentAsString());
    }

    public static JsonNode items(JsonNode body) {
        return body.isArray() ? body : body.path("content");
    }

    public static long currentUserId(MockMvc mockMvc, String token) throws Exception {
        MvcResult me = mockMvc.perform(get("/api/user/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return json(me).path("id").asLong();
    }

    public static long sharedClassroomId(MockMvc mockMvc, String teacherToken, String learnerToken) throws Exception {
        JsonNode teacherClasses = items(json(mockMvc.perform(get("/api/teacher/classrooms/assigned")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk()).andReturn()));
        JsonNode learnerClasses = items(json(mockMvc.perform(get("/api/student/classrooms/my-classrooms")
                        .header("Authorization", bearer(learnerToken)))
                .andExpect(status().isOk()).andReturn()));
        Set<Long> learnerClassIds = new HashSet<>();
        learnerClasses.forEach(row -> learnerClassIds.add(row.path("id").asLong()));
        for (JsonNode row : teacherClasses) {
            long id = row.path("id").asLong();
            if (learnerClassIds.contains(id)) return id;
        }
        throw new AssertionError("A classroom shared by the teacher and learner fixtures is required");
    }

    public static long firstSessionId(MockMvc mockMvc, String teacherToken, long classroomId) throws Exception {
        JsonNode sessions = items(json(mockMvc.perform(get("/api/teacher/classrooms/" + classroomId + "/sessions")
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk()).andReturn()));
        if (sessions.isEmpty()) throw new AssertionError("A classroom session fixture is required");
        return sessions.get(0).path("id").asLong();
    }

    public static String uniqueEmail(String prefix) {
        return "it." + prefix + "." + UUID.randomUUID() + "@englishlab-it.test";
    }

    public static String registerVerifiedLearner(
            MockMvc mockMvc,
            UserRepository userRepository,
            AuthTokenRepository authTokenRepository,
            String prefix
    ) throws Exception {
        String email = uniqueEmail(prefix);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","fullName":"IT Fixture Learner"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().is2xxSuccessful());

        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElseThrow();
        AuthToken otp = authTokenRepository
                .findTopByUserAndTypeOrderByCreatedAtDesc(user, AuthTokenType.EMAIL_VERIFICATION)
                .orElseThrow();
        assertTrue(!otp.isExpired() && !otp.isUsed(), "The new verification token must be usable");

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, otp.getToken())))
                .andExpect(status().isOk());
        return email;
    }
}
