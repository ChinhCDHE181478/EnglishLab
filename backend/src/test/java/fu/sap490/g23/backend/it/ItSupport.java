package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.TimeZone;

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
}
