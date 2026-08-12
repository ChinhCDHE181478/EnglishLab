package fu.sep490.g23.backend.it;

import fu.sep490.g23.backend.entity.AuthToken;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.AuthTokenType;
import fu.sep490.g23.backend.repository.AuthTokenRepository;
import fu.sep490.g23.backend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static fu.sep490.g23.backend.it.ItSupport.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Register Account / Login / Reset password
 * Excel sheet: IT_AUTH | SRS: UC-01, UC-03, UC-04
 * OTP: đọc mã 6 số từ bảng auth_tokens (không cần UI/mail).
 * Chạy: mvnw -Dtest=AuthIT test
 */
@EnglishLabIT
public class AuthIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Test
    @DisplayName("IT_AUTH_01")
    void itAuth01() throws Exception {
        String email = "it.reg." + UUID.randomUUID() + "@englishlab-it.test";
        String body = """
                {"email":"%s","password":"%s","fullName":"IT Register User"}
                """.formatted(email, PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
        User saved = userRepository.findByEmail(email).orElseThrow();
        assertNotEquals(PASSWORD, saved.getPassword());
    }

    @Test
    @DisplayName("IT_AUTH_02")
    void itAuth02() throws Exception {
        String body = """
                {"email":"%s","password":"%s","fullName":"Dup"}
                """.formatted(LEARNER, PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_AUTH_03")
    void itAuth03() throws Exception {
        // 1) Register → service lưu OTP EMAIL_VERIFICATION vào auth_tokens (mail có thể skip)
        String email = "it.verify." + UUID.randomUUID() + "@englishlab-it.test";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","fullName":"IT Verify OTP"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().is2xxSuccessful());

        // 2) Đọc OTP mới nhất từ DB (không mở UI / không đọc hộp thư)
        String otp = latestOtp(email, AuthTokenType.EMAIL_VERIFICATION);

        // 3) Gọi API verify qua MockMvc như client thật
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, otp)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_AUTH_04")
    void itAuth04() throws Exception {
        String email = "it.otp." + UUID.randomUUID() + "@englishlab-it.test";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","fullName":"IT OTP Neg"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"000000"}
                                """.formatted(email)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_AUTH_05")
    void itAuth05() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(LEARNER, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("IT_AUTH_06")
    void itAuth06() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/user/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(LEARNER));
    }

    @Test
    @DisplayName("IT_AUTH_07")
    void itAuth07() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"WrongPass999!"}
                                """.formatted(LEARNER)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_AUTH_08")
    void itAuth08() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(LEARNER)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("IT_AUTH_09")
    void itAuth09() throws Exception {
        // Dùng user riêng — không đụng password LEARNER dùng chung các IT khác
        String email = "it.reset." + UUID.randomUUID() + "@englishlab-it.test";
        String oldPassword = PASSWORD;
        String newPassword = "NewPass123!";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","fullName":"IT Reset User"}
                                """.formatted(email, oldPassword)))
                .andExpect(status().is2xxSuccessful());

        String verifyOtp = latestOtp(email, AuthTokenType.EMAIL_VERIFICATION);
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, verifyOtp)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().is2xxSuccessful());

        String resetOtp = latestOtp(email, AuthTokenType.PASSWORD_RESET);
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s","newPassword":"%s"}
                                """.formatted(email, resetOtp, newPassword)))
                .andExpect(status().isOk());

        // Login bằng mật khẩu mới
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, oldPassword)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    /** Đọc OTP mới nhất còn hiệu lực từ auth_tokens theo email + type. */
    private String latestOtp(String email, AuthTokenType type) {
        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElseThrow();
        AuthToken token = authTokenRepository.findTopByUserAndTypeOrderByCreatedAtDesc(user, type).orElseThrow();
        assertFalse(token.isExpired(), "OTP must not be expired");
        assertFalse(token.isUsed(), "OTP must not be used");
        return token.getToken();
    }
}
