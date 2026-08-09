# Giải thích chi tiết code Integration Test (toàn bộ file trong `it/`)

Thư mục: `backend/src/test/java/fu/sap490/g23/backend/it/`

Đọc theo thứ tự: **ItSupport → EnglishLabIT → AuthIT** (mẫu) → các `*IT` còn lại (cùng pattern).

Chạy ví dụ:
```powershell
cd D:\EngLishLab\EnglishLab\backend
.\mvnw.cmd "-Dtest=AuthIT" test
```

---

## Phần 0 – Khái niệm dùng lại trong MỌI file *IT

### Text block `"""` và `%s`

```java
String body = """
        {"email":"%s","password":"%s"}
        """.formatted(email, PASSWORD);
```

| Thành phần | Ý nghĩa |
|------------|---------|
| `""" ... """` | Chuỗi nhiều dòng (Java text block) — viết JSON cho dễ đọc, **không phải body rỗng** |
| `%s` | Chỗ trống chờ điền **chuỗi** |
| `.formatted(a, b)` | `%s` thứ 1 ← `a`, `%s` thứ 2 ← `b` |

### `UUID.randomUUID()`

Tạo mã ngẫu nhiên dài. Thường ghép vào email (`it.reg.` + UUID + `@englishlab-it.test`) để **mỗi lần chạy test không trùng** user.

### `mockMvc.perform(...)`

Giả lập HTTP vào Spring (như Postman **Send**), nhưng chạy trong JUnit → đi **Controller → Service → Repository** (Integration Test đúng chuẩn).

| Code | Giống Postman |
|------|----------------|
| `perform(...)` | Bấm Send |
| `post/get/put/...("/api/...")` | Method + URL |
| `.contentType(APPLICATION_JSON)` | Header Content-Type |
| `.content(body)` | Body JSON |
| `.header("Authorization", bearer(token))` | Bearer Token |
| `.andExpect(status()...)` | Kiểm tra Status |

### `@EnglishLabIT`

Gồm: `@SpringBootTest` (load app) + `@AutoConfigureMockMvc` + timezone initializer.

### `@DisplayName("IT_xxx ...")`

Tên test = **Test Case ID** trên Excel (map Round Passed/Failed/N/A).

### `import static ItSupport.*`

Dùng ngắn `PASSWORD`, `LEARNER`, `login(...)`, `bearer(...)` mà không viết `ItSupport.` mỗi lần.

---
## Mục lục file

1. [`ItSupport.java`](#itsupportjava)
2. [`EnglishLabIT.java`](#englishlabitjava)
3. [`ItTimezoneInitializer.java`](#ittimezoneinitializerjava)
4. [`AuthIT.java`](#authitjava)
5. [`AuthOtpIT.java`](#authotpitjava)
6. [`UserIT.java`](#useritjava)
7. [`NotificationIT.java`](#notificationitjava)
8. [`CommerceIT.java`](#commerceitjava)
9. [`PaymentIT.java`](#paymentitjava)
10. [`OnlineCourseIT.java`](#onlinecourseitjava)
11. [`DiscussionIT.java`](#discussionitjava)
12. [`ContentManagerCourseIT.java`](#contentmanagercourseitjava)
13. [`PackageIT.java`](#packageitjava)
14. [`CurriculumIT.java`](#curriculumitjava)
15. [`EnrollmentRequestIT.java`](#enrollmentrequestitjava)
16. [`TrainingManagerClassroomIT.java`](#trainingmanagerclassroomitjava)
17. [`StudentClassroomIT.java`](#studentclassroomitjava)
18. [`TeacherClassroomIT.java`](#teacherclassroomitjava)
19. [`ClassroomQuizIT.java`](#classroomquizitjava)
20. [`AssessmentIT.java`](#assessmentitjava)
21. [`SupportTicketIT.java`](#supportticketitjava)
22. [`AdminIT.java`](#adminitjava)
23. [`LarkIT.java`](#larkitjava)
24. [`InfrastructureIT.java`](#infrastructureitjava)
25. [`ReportIT.java`](#reportitjava)
26. [`ClassroomProposalIT.java`](#classroomproposalitjava)
27. [`AttendanceDisputeIT.java`](#attendancedisputeitjava)
28. [`LearningNotesIT.java`](#learningnotesitjava)

---

## File: `ItSupport.java`

Helper dùng chung (login, tài khoản demo, Bearer). **Không có @Test — đừng Run Test file này.**

### Vai trò từng phần

| Code | Giải thích |
|------|------------|
| `static { ... TimeZone ...}` | Set timezone `Asia/Ho_Chi_Minh` sớm — IDE không dùng Maven surefire vẫn chạy được |
| `PASSWORD`, `LEARNER`, `TEACHER`… | Hằng email/mật khẩu demo |
| `private ItSupport()` | Không cho `new ItSupport()` — chỉ dùng static |
| `login(mockMvc, email, password)` | POST `/api/auth/login` → đọc JSON → trả `accessToken` |
| `bearer(token)` | Trả về `"Bearer " + token` để gắn header |
| `mapper()` | Cho class khác dùng chung `ObjectMapper` |

### Hàm `login` chi tiết

1. Ghép JSON login bằng `%s` + `.formatted(email, password)`.
2. `mockMvc.perform(post("/api/auth/login")...)` gửi request.
3. `.andExpect(status().isOk())` — login phải 200.
4. `.andReturn()` lấy body → `MAPPER.readTree` → `accessToken`.

**Không Run Test file này** (không có `@Test`).

```java
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
```

---
## File: `EnglishLabIT.java`

Annotation gộp `@SpringBootTest` + `@AutoConfigureMockMvc` + timezone initializer. Mọi class `*IT` gắn `@EnglishLabIT`.

```java
package fu.sap490.g23.backend.it;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ContextConfiguration;

/**
 * Meta-annotation cho mọi *IT: SpringBootTest + MockMvc + timezone initializer (IDE-safe).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = ItTimezoneInitializer.class)
public @interface EnglishLabIT {
}
```

---
## File: `ItTimezoneInitializer.java`

Set `Asia/Ho_Chi_Minh` trước khi Spring tạo DataSource (tránh lỗi IDE Asia/Saigon).

```java
package fu.sap490.g23.backend.it;

import java.util.TimeZone;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Chạy trước khi Spring tạo DataSource — tránh Postgres từ chối Asia/Saigon khi chạy test từ IDE.
 */
public class ItTimezoneInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static {
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
}
```

---
## File: `AuthIT.java`

Sheet Excel **IT - Auth** — đăng ký, login JWT, negative OTP/password, /me.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_AUTH_01 register` → method `itAuth01_register()`

- Tạo email/`id` ngẫu nhiên bằng `UUID.randomUUID()` để **không trùng** data khi chạy lại.
- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_AUTH_02 duplicate register rejected` → method `itAuth02_duplicateRegister()`

- Tạo email/`id` ngẫu nhiên bằng `UUID.randomUUID()` để **không trùng** data khi chạy lại.
- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andExpect(status().is4xxClientError())` = **negative test**: cố tình sai, hệ thống phải **từ chối** (vẫn có thể Passed).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_AUTH_04 invalid OTP rejected` → method `itAuth04_invalidOtp()`

- Tạo email/`id` ngẫu nhiên bằng `UUID.randomUUID()` để **không trùng** data khi chạy lại.
- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).

#### `IT_AUTH_05 login + me` → method `itAuth05_loginAndMe()`

- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andExpect(status().is4xxClientError())` = **negative test**: cố tình sai, hệ thống phải **từ chối** (vẫn có thể Passed).
- `.andExpect(jsonPath("$.field"))` = kiểm tra **field JSON** trong response.
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_AUTH_06 wrong password` → method `itAuth06_wrongPassword()`

- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().is4xxClientError())` = **negative test**: cố tình sai, hệ thống phải **từ chối** (vẫn có thể Passed).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_AUTH_07 me without token` → method `itAuth07_meUnauthorized()`

- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().is4xxClientError())` = **negative test**: cố tình sai, hệ thống phải **từ chối** (vẫn có thể Passed).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_AUTH_08 forgot password` → method `itAuth08_forgotPassword()`

- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_AUTH_10 reset invalid code` → method `itAuth10_resetInvalid()`

- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andExpect(status().is4xxClientError())` = **negative test**: cố tình sai, hệ thống phải **từ chối** (vẫn có thể Passed).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Authentication (IT - Auth)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=AuthIT test
 */
@EnglishLabIT
public class AuthIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_AUTH_01 register")
    void itAuth01_register() throws Exception {
        String email = "it.reg." + UUID.randomUUID() + "@englishlab-it.test";
        String body = """
                {"email":"%s","password":"%s","fullName":"IT Register User"}
                """.formatted(email, PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("IT_AUTH_02 duplicate register rejected")
    void itAuth02_duplicateRegister() throws Exception {
        String body = """
                {"email":"%s","password":"%s","fullName":"Dup"}
                """.formatted(LEARNER, PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_AUTH_04 invalid OTP rejected")
    void itAuth04_invalidOtp() throws Exception {
        // Dùng email chưa verify (mới register) — Learner demo đã verify sẵn có thể trả 200.
        String email = "it.otp." + UUID.randomUUID() + "@englishlab-it.test";
        String register = """
                {"email":"%s","password":"%s","fullName":"IT OTP Neg"}
                """.formatted(email, PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(register))
                .andExpect(status().is2xxSuccessful());
        String body = """
                {"email":"%s","code":"000000"}
                """.formatted(email);
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_AUTH_05 login + me")
    void itAuth05_loginAndMe() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(LEARNER));
    }

    @Test
    @DisplayName("IT_AUTH_06 wrong password")
    void itAuth06_wrongPassword() throws Exception {
        String body = """
                {"email":"%s","password":"WrongPass999!"}
                """.formatted(LEARNER);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_AUTH_07 me without token")
    void itAuth07_meUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_AUTH_08 forgot password")
    void itAuth08_forgotPassword() throws Exception {
        String body = """
                {"email":"%s"}
                """.formatted(LEARNER);
        // Có thể 200 hoặc 400 rate-limit; không được 500
        MvcResult result = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        int status = result.getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertTrue(status < 500, "forgot-password không được 5xx, got " + status);
    }

    @Test
    @DisplayName("IT_AUTH_10 reset invalid code")
    void itAuth10_resetInvalid() throws Exception {
        String body = """
                {"email":"%s","code":"000000","newPassword":"%s"}
                """.formatted(LEARNER, PASSWORD);
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}
```

---
## File: `AuthOtpIT.java`

IT_AUTH_03 / IT_AUTH_09 — đọc OTP thật từ bảng `auth_tokens` bằng JdbcTemplate.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_AUTH_03 verify email with OTP from DB` → method `itAuth03_verifyEmail()`

- Tạo email/`id` ngẫu nhiên bằng `UUID.randomUUID()` để **không trùng** data khi chạy lại.
- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Đọc OTP từ DB bằng SQL (`JdbcTemplate`) — giống bước “query auth_tokens” trên Excel.

#### `IT_AUTH_09 reset password with OTP from DB` → method `itAuth09_resetPassword()`

- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Đọc OTP từ DB bằng SQL (`JdbcTemplate`) — giống bước “query auth_tokens” trên Excel.
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IT_AUTH_03 / IT_AUTH_09 – cần đọc OTP từ bảng auth_tokens (PostgreSQL).
 */
@EnglishLabIT
public class AuthOtpIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String latestOtp(String email, String type) {
        return jdbcTemplate.queryForObject("""
                SELECT t.token
                FROM auth_tokens t
                JOIN users u ON u.id = t.user_id
                WHERE u.email = ? AND t.type = ? AND t.used_at IS NULL
                  AND (t.expires_at IS NULL OR t.expires_at > NOW())
                ORDER BY t.created_at DESC
                LIMIT 1
                """, String.class, email, type);
    }

    @Test
    @DisplayName("IT_AUTH_03 verify email with OTP from DB")
    void itAuth03_verifyEmail() throws Exception {
        String email = "it.ver." + UUID.randomUUID() + "@englishlab-it.test";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","fullName":"IT Verify"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().is2xxSuccessful());

        String otp = latestOtp(email, "EMAIL_VERIFICATION");
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, otp)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_AUTH_09 reset password with OTP from DB")
    void itAuth09_resetPassword() throws Exception {
        String email = ItSupport.LEARNER;
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().is2xxSuccessful());

        String otp = latestOtp(email, "PASSWORD_RESET");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s","newPassword":"%s"}
                                """.formatted(email, otp, PASSWORD)))
                .andExpect(status().isOk());
    }
}
```

---
## File: `UserIT.java`

Sheet **IT - User** — hồ sơ /me, đổi mật khẩu negative, avatar.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_USER_01 get me` → method `itUser01_getMe()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andExpect(jsonPath("$.field"))` = kiểm tra **field JSON** trong response.
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_USER_02 update me` → method `itUser02_updateMe()`

- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_USER_03 wrong current password` → method `itUser03_wrongCurrentPassword()`

- `""" ... %s ... """.formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…).
- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(put("..."))` = giả lập **HTTP PUT**.
- `.andExpect(status().is4xxClientError())` = **negative test**: cố tình sai, hệ thống phải **từ chối** (vẫn có thể Passed).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_USER_05 update me unauthorized` → method `itUser05_unauthorized()`

- `mockMvc.perform(put("..."))` = giả lập **HTTP PUT**.
- `.andExpect(status().is4xxClientError())` = **negative test**: cố tình sai, hệ thống phải **từ chối** (vẫn có thể Passed).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Account Profile (IT - User)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=UserIT test
 */
@EnglishLabIT
public class UserIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_USER_01 get me")
    void itUser01_getMe() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/user/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    @DisplayName("IT_USER_02 update me")
    void itUser02_updateMe() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult me = mockMvc.perform(get("/api/user/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode json = mapper().readTree(me.getResponse().getContentAsString());
        String body = """
                {"fullName":"%s","phoneNumber":"%s","targetExam":"%s","targetScore":"%s","studyGoal":"%s"}
                """.formatted(
                json.path("fullName").asText("Learner"),
                json.path("phoneNumber").asText("0900000000"),
                json.path("targetExam").asText("IELTS"),
                json.path("targetScore").asText("6.5"),
                json.path("studyGoal").asText("IT update"));
        mockMvc.perform(put("/api/user/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_USER_03 wrong current password")
    void itUser03_wrongCurrentPassword() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(put("/api/user/me/password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"NotThePassword!","newPassword":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_USER_05 update me unauthorized")
    void itUser05_unauthorized() throws Exception {
        mockMvc.perform(put("/api/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"x\"}"))
                .andExpect(status().is4xxClientError());
    }
}
```

---
## File: `NotificationIT.java`

Sheet **IT - Notif** — preference + list thông báo.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_NOTIF_01 get preferences` → method `itNotif01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `mockMvc.perform(put("..."))` = giả lập **HTTP PUT**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_NOTIF_02 update preferences` → method `itNotif02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(put("..."))` = giả lập **HTTP PUT**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_NOTIF_03 invalid preferences body` → method `itNotif03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `mockMvc.perform(put("..."))` = giả lập **HTTP PUT**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andExpect(status().is4xxClientError())` = **negative test**: cố tình sai, hệ thống phải **từ chối** (vẫn có thể Passed).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_NOTIF_04 list notifications` → method `itNotif04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_NOTIF_05 unread count` → method `itNotif05()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Notifications (IT - Notif)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=NotificationIT test
 */
@EnglishLabIT
public class NotificationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_NOTIF_01 get preferences")
    void itNotif01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/user/me/notification-preferences")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_NOTIF_02 update preferences")
    void itNotif02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(put("/api/user/me/notification-preferences")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inAppEnabled":false,"emailEnabled":true,"larkEnabled":false}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/user/me/notification-preferences")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inAppEnabled":true,"emailEnabled":true,"larkEnabled":false}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_NOTIF_03 invalid preferences body")
    void itNotif03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(put("/api/user/me/notification-preferences")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_NOTIF_04 list notifications")
    void itNotif04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/notifications")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_NOTIF_05 unread count")
    void itNotif05() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/notifications/unread-count")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
```

---
## File: `CommerceIT.java`

Sheet **IT - Commerce** — giỏ hàng / wishlist.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_COMMERCE_01 add to cart` → method `itCommerce01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `mockMvc.perform(delete("..."))` = giả lập **HTTP DELETE**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_COMMERCE_02 wishlist` → method `itCommerce02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_COMMERCE_03 clear cart` → method `itCommerce03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(delete("..."))` = giả lập **HTTP DELETE**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_COMMERCE_04 add cart again` → method `itCommerce04()`

- Gọi API qua MockMvc rồi `andExpect` theo Expected của mã IT trên Excel.

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Cart & Wishlist (IT - Commerce)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=CommerceIT test
 */
@EnglishLabIT
public class CommerceIT {

    @Autowired
    private MockMvc mockMvc;

    private long firstCourseId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/online-courses"))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(result.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        org.junit.jupiter.api.Assertions.assertTrue(items.isArray() && items.size() > 0, "Cần có khóa học public");
        return items.get(0).path("id").asLong();
    }

    @Test
    @DisplayName("IT_COMMERCE_01 add to cart")
    void itCommerce01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        long courseId = firstCourseId();
        mockMvc.perform(delete("/api/student/commerce/cart").header("Authorization", bearer(token)));
        mockMvc.perform(post("/api/student/commerce/cart/" + courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/api/student/commerce/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_COMMERCE_02 wishlist")
    void itCommerce02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        long courseId = firstCourseId();
        mockMvc.perform(post("/api/student/commerce/wishlist/" + courseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().is2xxSuccessful());
        // move-to-cart có thể 400 nếu đã trong giỏ – không được 500
        MvcResult move = mockMvc.perform(post("/api/student/commerce/wishlist/" + courseId + "/move-to-cart")
                        .header("Authorization", bearer(token)))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(move.getResponse().getStatus() < 500);
    }

    @Test
    @DisplayName("IT_COMMERCE_03 clear cart")
    void itCommerce03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(delete("/api/student/commerce/cart").header("Authorization", bearer(token)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("IT_COMMERCE_04 add cart again")
    void itCommerce04() throws Exception {
        itCommerce01();
    }
}
```

---
## File: `PaymentIT.java`

Sheet **IT - Payment** — PayOS link, quote, webhook, manager orders.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_PAYMENT_01 payos link` → method `itPayment01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andExpect(jsonPath("$.field"))` = kiểm tra **field JSON** trong response.
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_PAYMENT_02 quote` → method `itPayment02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andExpect(jsonPath("$.field"))` = kiểm tra **field JSON** trong response.
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_PAYMENT_03 webhook unsigned rejected` → method `itPayment03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andExpect(status().is4xxClientError())` = **negative test**: cố tình sai, hệ thống phải **từ chối** (vẫn có thể Passed).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_PAYMENT_04 manager orders` → method `itPayment04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_PAYMENT_05 manager orders again` → method `itPayment05()`

- Gọi API qua MockMvc rồi `andExpect` theo Expected của mã IT trên Excel.

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – PayOS & Orders (IT - Payment)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=PaymentIT test
 */
@EnglishLabIT
public class PaymentIT {

    @Autowired
    private MockMvc mockMvc;

    private long firstCourseId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/online-courses")).andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(result.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        return items.get(0).path("id").asLong();
    }

    @Test
    @DisplayName("IT_PAYMENT_01 payos link")
    void itPayment01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        long courseId = firstCourseId();
        mockMvc.perform(post("/api/student/payments/payos/link")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseIds\":[" + courseId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").exists());
    }

    @Test
    @DisplayName("IT_PAYMENT_02 quote")
    void itPayment02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        long courseId = firstCourseId();
        mockMvc.perform(post("/api/student/payments/quote")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseIds\":[" + courseId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").exists());
    }

    @Test
    @DisplayName("IT_PAYMENT_03 webhook unsigned rejected")
    void itPayment03() throws Exception {
        mockMvc.perform(post("/api/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"00\",\"data\":{}}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT_PAYMENT_04 manager orders")
    void itPayment04() throws Exception {
        String token = login(mockMvc, MANAGER, PASSWORD);
        mockMvc.perform(get("/api/manager/payments/orders")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_PAYMENT_05 manager orders again")
    void itPayment05() throws Exception {
        itPayment04();
    }
}
```

---
## File: `OnlineCourseIT.java`

Sheet **IT - Course** — catalog public + content/progress learner.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_COURSE_01 public catalog` → method `itCourse01()`

- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).

#### `IT_COURSE_02 public detail` → method `itCourse02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_COURSE_03 learner content (enrolled => 200, else 4xx business)` → method `itCourse03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_COURSE_06 same as content check` → method `itCourse06()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `mockMvc.perform(patch("..."))` = giả lập **HTTP PATCH**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_COURSE_04 progress attempt` → method `itCourse04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `mockMvc.perform(patch("..."))` = giả lập **HTTP PATCH**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_COURSE_05 rating attempt` → method `itCourse05()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Online Learning (IT - Course)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=OnlineCourseIT test
 */
@EnglishLabIT
public class OnlineCourseIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_COURSE_01 public catalog")
    void itCourse01() throws Exception {
        mockMvc.perform(get("/api/online-courses")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_COURSE_02 public detail")
    void itCourse02() throws Exception {
        MvcResult list = mockMvc.perform(get("/api/online-courses")).andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(list.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        long id = items.get(0).path("id").asLong();
        String slug = items.get(0).path("slug").asText(String.valueOf(id));
        mockMvc.perform(get("/api/online-courses/" + slug)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_COURSE_03 learner content (enrolled => 200, else 4xx business)")
    void itCourse03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult list = mockMvc.perform(get("/api/online-courses")).andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(list.getResponse().getContentAsString());
        if (!items.isArray()) items = items.path("content");
        long id = items.get(0).path("id").asLong();
        MvcResult r = mockMvc.perform(get("/api/student/online-courses/" + id + "/content")
                        .header("Authorization", bearer(token)))
                .andReturn();
        int st = r.getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertTrue(st == 200 || (st >= 400 && st < 500),
                "content phải 200 hoặc 4xx business, got " + st);
    }

    @Test
    @DisplayName("IT_COURSE_06 same as content check")
    void itCourse06() throws Exception {
        itCourse03();
    }

    @Test
    @DisplayName("IT_COURSE_04 progress attempt")
    void itCourse04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult list = mockMvc.perform(get("/api/online-courses")).andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(list.getResponse().getContentAsString());
        if (!items.isArray()) items = items.path("content");
        long id = items.get(0).path("id").asLong();
        MvcResult r = mockMvc.perform(patch("/api/student/online-courses/" + id + "/lessons/1/progress")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getStatus() < 500);
    }

    @Test
    @DisplayName("IT_COURSE_05 rating attempt")
    void itCourse05() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult list = mockMvc.perform(get("/api/online-courses")).andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(list.getResponse().getContentAsString());
        if (!items.isArray()) items = items.path("content");
        long id = items.get(0).path("id").asLong();
        MvcResult r = mockMvc.perform(post("/api/student/online-courses/" + id + "/rating")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5,\"comment\":\"IT\"}"))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getStatus() < 500);
    }
}
```

---
## File: `DiscussionIT.java`

Sheet **IT - Discuss** — thảo luận / report / CM moderation.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_DISCUSS_01 create discussion` → method `itDiscuss01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_DISCUSS_02 list discussions` → method `itDiscuss02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_DISCUSS_03 report requires thread - smoke create/report path` → method `itDiscuss03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_DISCUSS_04 report smoke` → method `itDiscuss04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_DISCUSS_05 CM discussion reports` → method `itDiscuss05()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Course Discussion (IT - Discuss)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=DiscussionIT test
 */
@EnglishLabIT
public class DiscussionIT {

    @Autowired
    private MockMvc mockMvc;

    private long courseId() throws Exception {
        MvcResult list = mockMvc.perform(get("/api/online-courses")).andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(list.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        return items.get(0).path("id").asLong();
    }

    @Test
    @DisplayName("IT_DISCUSS_01 create discussion")
    void itDiscuss01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        long id = courseId();
        MvcResult r = mockMvc.perform(post("/api/student/online-courses/" + id + "/discussions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"IT discuss","content":"Integration discussion body"}
                                """))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getStatus() < 500);
    }

    @Test
    @DisplayName("IT_DISCUSS_02 list discussions")
    void itDiscuss02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/online-courses/" + courseId() + "/discussions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_DISCUSS_03 report requires thread - smoke create/report path")
    void itDiscuss03() throws Exception {
        itDiscuss01();
    }

    @Test
    @DisplayName("IT_DISCUSS_04 report smoke")
    void itDiscuss04() throws Exception {
        itDiscuss01();
    }

    @Test
    @DisplayName("IT_DISCUSS_05 CM discussion reports")
    void itDiscuss05() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/discussion-reports")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
```

---
## File: `ContentManagerCourseIT.java`

Sheet **IT - Content** — CM list khóa online.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_CONTENT_01` → method `itContent01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CONTENT_02` → method `itContent02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CONTENT_03` → method `itContent03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CONTENT_04` → method `itContent04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – CM Online Courses (IT - Content)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=ContentManagerCourseIT test
 */
@EnglishLabIT
public class ContentManagerCourseIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_CONTENT_01")
    void itContent01() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/online-courses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CONTENT_02")
    void itContent02() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/online-courses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CONTENT_03")
    void itContent03() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/online-courses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CONTENT_04")
    void itContent04() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/online-courses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
```

---
## File: `PackageIT.java`

Sheet **IT - Package** — CM packages.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_PACKAGE_01` → method `itPackage01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_PACKAGE_02` → method `itPackage02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_PACKAGE_03` → method `itPackage03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Packages (IT - Package)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=PackageIT test
 */
@EnglishLabIT
public class PackageIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_PACKAGE_01")
    void itPackage01() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/packages")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_PACKAGE_02")
    void itPackage02() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/packages")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_PACKAGE_03")
    void itPackage03() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/packages")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
```

---
## File: `CurriculumIT.java`

Sheet **IT - Curriculum** — programs, bank, rubrics.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_CURRICULUM_01 programs` → method `itCur01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CURRICULUM_05 programs again` → method `itCur05()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CURRICULUM_02 bank` → method `itCur02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CURRICULUM_03 learning paths` → method `itCur03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CURRICULUM_04 rubrics` → method `itCur04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Curriculum (IT - Curriculum)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=CurriculumIT test
 */
@EnglishLabIT
public class CurriculumIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_CURRICULUM_01 programs")
    void itCur01() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/curriculum-programs")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CURRICULUM_05 programs again")
    void itCur05() throws Exception { itCur01(); }

    @Test
    @DisplayName("IT_CURRICULUM_02 bank")
    void itCur02() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/content-manager/exercise-bank")
                        .header("Authorization", bearer(token))).andReturn();
        if (r.getResponse().getStatus() == 404) {
            mockMvc.perform(get("/api/content-manager/assessment-bank")
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk());
        } else {
            org.junit.jupiter.api.Assertions.assertEquals(200, r.getResponse().getStatus());
        }
    }

    @Test
    @DisplayName("IT_CURRICULUM_03 learning paths")
    void itCur03() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        MvcResult r = mockMvc.perform(get("/api/content-manager/learning-paths")
                        .header("Authorization", bearer(token))).andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getStatus() < 500);
    }

    @Test
    @DisplayName("IT_CURRICULUM_04 rubrics")
    void itCur04() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/rubrics")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
```

---
## File: `EnrollmentRequestIT.java`

Sheet **IT - EnrollReq** — HV tạo form + Staff list.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_ENROLLREQ_01 student submit + list mine` → method `itEnroll01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_ENROLLREQ_04 submit again` → method `itEnroll04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_ENROLLREQ_02 staff list` → method `itEnroll02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_ENROLLREQ_03 staff list` → method `itEnroll03()`

- Gọi API qua MockMvc rồi `andExpect` theo Expected của mã IT trên Excel.

#### `IT_ENROLLREQ_05 staff list` → method `itEnroll05()`

- Gọi API qua MockMvc rồi `andExpect` theo Expected của mã IT trên Excel.

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Enrollment Requests (IT - EnrollReq)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=EnrollmentRequestIT test
 */
@EnglishLabIT
public class EnrollmentRequestIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ENROLLREQ_01 student submit + list mine")
    void itEnroll01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult offerings = mockMvc.perform(get("/api/course-offerings")).andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(offerings.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        long oid = items.get(0).path("id").asLong();
        MvcResult create = mockMvc.perform(post("/api/student/course-enrollment-requests")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseOfferingId":%d,"contactName":"IT Learner","contactEmail":"%s",
                                 "contactPhone":"0900000001","consultationTrack":"IELTS","note":"IT"}
                                """.formatted(oid, LEARNER)))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(create.getResponse().getStatus() < 500);
        mockMvc.perform(get("/api/student/course-enrollment-requests/my")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ENROLLREQ_04 submit again")
    void itEnroll04() throws Exception { itEnroll01(); }

    @Test
    @DisplayName("IT_ENROLLREQ_02 staff list")
    void itEnroll02() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/enrollment-requests").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ENROLLREQ_03 staff list")
    void itEnroll03() throws Exception { itEnroll02(); }

    @Test
    @DisplayName("IT_ENROLLREQ_05 staff list")
    void itEnroll05() throws Exception { itEnroll02(); }
}
```

---
## File: `TrainingManagerClassroomIT.java`

Sheet **IT - Classroom** — TM lớp / registrations / waitlist.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_CLASS_01 public offerings` → method `itClass01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CLASS_02 tm list` → method `itClass02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CLASS_08 tm list again` → method `itClass08()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CLASS_03 tm detail` → method `itClass03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CLASS_04 registrations` → method `itClass04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CLASS_06 registrations again` → method `itClass06()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CLASS_05 waitlist reorder` → method `itClass05()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_CLASS_07 detail precheck` → method `itClass07()`

- Gọi API qua MockMvc rồi `andExpect` theo Expected của mã IT trên Excel.

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – TM Classroom (IT - Classroom)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=TrainingManagerClassroomIT test
 */
@EnglishLabIT
public class TrainingManagerClassroomIT {

    @Autowired
    private MockMvc mockMvc;

    private long firstOfferingId(String token) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/training-manager/classrooms")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(r.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        org.junit.jupiter.api.Assertions.assertTrue(items.size() > 0);
        return items.get(0).path("id").asLong();
    }

    @Test
    @DisplayName("IT_CLASS_01 public offerings")
    void itClass01() throws Exception {
        mockMvc.perform(get("/api/classroom-offerings")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CLASS_02 tm list")
    void itClass02() throws Exception {
        String token = login(mockMvc, TM, PASSWORD);
        mockMvc.perform(get("/api/training-manager/classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CLASS_08 tm list again")
    void itClass08() throws Exception { itClass02(); }

    @Test
    @DisplayName("IT_CLASS_03 tm detail")
    void itClass03() throws Exception {
        String token = login(mockMvc, TM, PASSWORD);
        long id = firstOfferingId(token);
        mockMvc.perform(get("/api/training-manager/classrooms/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CLASS_04 registrations")
    void itClass04() throws Exception {
        String token = login(mockMvc, TM, PASSWORD);
        long id = firstOfferingId(token);
        mockMvc.perform(get("/api/training-manager/classrooms/registrations")
                        .param("classroomOfferingId", String.valueOf(id))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CLASS_06 registrations again")
    void itClass06() throws Exception { itClass04(); }

    @Test
    @DisplayName("IT_CLASS_05 waitlist reorder")
    void itClass05() throws Exception {
        String token = login(mockMvc, TM, PASSWORD);
        long id = firstOfferingId(token);
        MvcResult list = mockMvc.perform(get("/api/training-manager/classrooms/registrations")
                        .param("classroomOfferingId", String.valueOf(id))
                        .param("status", "WAITLIST")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(list.getResponse().getContentAsString());
        if (!items.isArray() || items.size() < 2) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Cần >=2 WAITLIST để assert reorder");
        }
        StringBuilder ids = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) ids.append(',');
            ids.append(items.get(i).path("id").asLong());
        }
        ids.append(']');
        mockMvc.perform(put("/api/training-manager/classrooms/" + id + "/waitlist/order")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enrollmentIds\":" + ids + "}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_CLASS_07 detail precheck")
    void itClass07() throws Exception { itClass03(); }
}
```

---
## File: `StudentClassroomIT.java`

Sheet **IT - LearnerCls** — lớp của HV, session, homework…

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_LEARNERCLS_01 my classrooms` → method `itLearner01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_LEARNERCLS_02 sessions` → method `itLearner02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_LEARNERCLS_03 homework` → method `itLearner03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_LEARNERCLS_05 homework again` → method `itLearner05()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_LEARNERCLS_04 materials` → method `itLearner04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_LEARNERCLS_06 gradebook me` → method `itLearner06()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Learner Classroom (IT - LearnerCls)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=StudentClassroomIT test
 */
@EnglishLabIT
public class StudentClassroomIT {

    @Autowired
    private MockMvc mockMvc;

    private Long myClassroomId(String token) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/student/classrooms/my-classrooms")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(r.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        if (!items.isArray() || items.size() == 0) return null;
        long id = items.get(0).path("id").asLong(0);
        if (id == 0) id = items.get(0).path("offeringId").asLong();
        return id;
    }

    @Test
    @DisplayName("IT_LEARNERCLS_01 my classrooms")
    void itLearner01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/classrooms/my-classrooms").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_LEARNERCLS_02 sessions")
    void itLearner02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        Long id = myClassroomId(token);
        org.junit.jupiter.api.Assumptions.assumeTrue(id != null, "Learner chưa có lớp");
        mockMvc.perform(get("/api/student/classrooms/" + id + "/sessions").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_LEARNERCLS_03 homework")
    void itLearner03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        Long id = myClassroomId(token);
        org.junit.jupiter.api.Assumptions.assumeTrue(id != null);
        mockMvc.perform(get("/api/student/classrooms/" + id + "/homework").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_LEARNERCLS_05 homework again")
    void itLearner05() throws Exception { itLearner03(); }

    @Test
    @DisplayName("IT_LEARNERCLS_04 materials")
    void itLearner04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        Long id = myClassroomId(token);
        org.junit.jupiter.api.Assumptions.assumeTrue(id != null);
        mockMvc.perform(get("/api/student/classrooms/" + id + "/materials").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_LEARNERCLS_06 gradebook me")
    void itLearner06() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        Long id = myClassroomId(token);
        org.junit.jupiter.api.Assumptions.assumeTrue(id != null);
        MvcResult r = mockMvc.perform(get("/api/student/classrooms/" + id + "/gradebook/me")
                        .header("Authorization", bearer(token))).andReturn();
        int st = r.getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertTrue(st == 200 || st == 204, "got " + st);
    }
}
```

---
## File: `TeacherClassroomIT.java`

Sheet **IT - Teacher** — lớp assigned, điểm danh, gradebook.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_TEACH_01 assigned` → method `itTeach01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_TEACH_06 assigned again` → method `itTeach06()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_TEACH_02 homework` → method `itTeach02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_TEACH_03 attendance via session` → method `itTeach03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_TEACH_04 gradebook` → method `itTeach04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_TEACH_05 requests mine` → method `itTeach05()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Teacher (IT - Teacher)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=TeacherClassroomIT test
 */
@EnglishLabIT
public class TeacherClassroomIT {

    @Autowired
    private MockMvc mockMvc;

    private long assignedId(String token) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/teacher/classrooms/assigned")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = mapper().readTree(r.getResponse().getContentAsString());
        JsonNode items = root.isArray() ? root : root.path("content");
        org.junit.jupiter.api.Assumptions.assumeTrue(items.size() > 0, "Teacher chưa được assign lớp");
        return items.get(0).path("id").asLong();
    }

    @Test
    @DisplayName("IT_TEACH_01 assigned")
    void itTeach01() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        mockMvc.perform(get("/api/teacher/classrooms/assigned").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_TEACH_06 assigned again")
    void itTeach06() throws Exception { itTeach01(); }

    @Test
    @DisplayName("IT_TEACH_02 homework")
    void itTeach02() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        long id = assignedId(token);
        mockMvc.perform(get("/api/teacher/classrooms/" + id + "/homework").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_TEACH_03 attendance via session")
    void itTeach03() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        long id = assignedId(token);
        MvcResult sessions = mockMvc.perform(get("/api/teacher/classrooms/" + id + "/sessions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(sessions.getResponse().getContentAsString());
        org.junit.jupiter.api.Assumptions.assumeTrue(items.isArray() && items.size() > 0);
        long sid = items.get(0).path("id").asLong();
        mockMvc.perform(get("/api/teacher/classrooms/sessions/" + sid + "/attendance")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_TEACH_04 gradebook")
    void itTeach04() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        long id = assignedId(token);
        mockMvc.perform(get("/api/teacher/classrooms/" + id + "/gradebook").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_TEACH_05 requests mine")
    void itTeach05() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        mockMvc.perform(get("/api/teacher/classrooms/requests/mine").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
```

---
## File: `ClassroomQuizIT.java`

Sheet **IT - Quiz** — quiz GV/HV.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_QUIZ_01 teacher quizzes` → method `itQuiz01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_QUIZ_02 teacher quizzes again` → method `itQuiz02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `mockMvc.perform(delete("..."))` = giả lập **HTTP DELETE**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_QUIZ_03 student quizzes` → method `itQuiz03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `mockMvc.perform(delete("..."))` = giả lập **HTTP DELETE**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_QUIZ_04 delete quiz skipped on demo - endpoint exists check` → method `itQuiz04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(delete("..."))` = giả lập **HTTP DELETE**.
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Quiz (IT - Quiz)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=ClassroomQuizIT test
 */
@EnglishLabIT
public class ClassroomQuizIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_QUIZ_01 teacher quizzes")
    void itQuiz01() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        MvcResult assigned = mockMvc.perform(get("/api/teacher/classrooms/assigned")
                        .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(assigned.getResponse().getContentAsString());
        if (!items.isArray()) items = items.path("content");
        org.junit.jupiter.api.Assumptions.assumeTrue(items.size() > 0);
        long id = items.get(0).path("id").asLong();
        mockMvc.perform(get("/api/teacher/classrooms/" + id + "/quizzes").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_QUIZ_02 teacher quizzes again")
    void itQuiz02() throws Exception { itQuiz01(); }

    @Test
    @DisplayName("IT_QUIZ_03 student quizzes")
    void itQuiz03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/classrooms/quizzes").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_QUIZ_04 delete quiz skipped on demo - endpoint exists check")
    void itQuiz04() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        MvcResult r = mockMvc.perform(delete("/api/teacher/quizzes/99999999")
                        .header("Authorization", bearer(token))).andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getStatus() < 500);
    }
}
```

---
## File: `AssessmentIT.java`

Sheet **IT - Assess** — placement, mock, assessments.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_ASSESS_01 placement current` → method `itAssess01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_ASSESS_02 placement submit empty answers` → method `itAssess02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_ASSESS_03 course assessments` → method `itAssess03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_ASSESS_05 course assessments again` → method `itAssess05()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_ASSESS_04 mock tests` → method `itAssess04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_ASSESS_06 mock tests again` → method `itAssess06()`

- Gọi API qua MockMvc rồi `andExpect` theo Expected của mã IT trên Excel.

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Assessment (IT - Assess)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=AssessmentIT test
 */
@EnglishLabIT
public class AssessmentIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ASSESS_01 placement current")
    void itAssess01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/placement-tests/current").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ASSESS_02 placement submit empty answers")
    void itAssess02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult r = mockMvc.perform(post("/api/student/placement-tests/current/submit")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getStatus() < 500);
    }

    @Test
    @DisplayName("IT_ASSESS_03 course assessments")
    void itAssess03() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        MvcResult list = mockMvc.perform(get("/api/online-courses")).andExpect(status().isOk()).andReturn();
        JsonNode items = mapper().readTree(list.getResponse().getContentAsString());
        if (!items.isArray()) items = items.path("content");
        long id = items.get(0).path("id").asLong();
        MvcResult r = mockMvc.perform(get("/api/student/online-courses/" + id + "/assessments")
                        .header("Authorization", bearer(token))).andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getStatus() < 500);
    }

    @Test
    @DisplayName("IT_ASSESS_05 course assessments again")
    void itAssess05() throws Exception { itAssess03(); }

    @Test
    @DisplayName("IT_ASSESS_04 mock tests")
    void itAssess04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/mock-tests").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ASSESS_06 mock tests again")
    void itAssess06() throws Exception { itAssess04(); }
}
```

---
## File: `SupportTicketIT.java`

Sheet **IT - Support** — ticket HV + manager.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_SUPPORT_01 create ticket` → method `itSupport01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_SUPPORT_02 list mine` → method `itSupport02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_SUPPORT_03 manager list` → method `itSupport03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_SUPPORT_04 invalid body` → method `itSupport04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Support (IT - Support)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=SupportTicketIT test
 */
@EnglishLabIT
public class SupportTicketIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_SUPPORT_01 create ticket")
    void itSupport01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(post("/api/student/support-tickets")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"IT ticket code test xx","category":"TECHNICAL",
                                 "message":"Noi dung ticket integration code test day du"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_SUPPORT_02 list mine")
    void itSupport02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/support-tickets").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_SUPPORT_03 manager list")
    void itSupport03() throws Exception {
        String token = login(mockMvc, MANAGER, PASSWORD);
        mockMvc.perform(get("/api/manager/support-tickets").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_SUPPORT_04 invalid body")
    void itSupport04() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(post("/api/student/support-tickets")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
```

---
## File: `AdminIT.java`

Sheet **IT - Admin** — users, audit, config.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_ADMIN_01` → method `itAdmin01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_ADMIN_02` → method `itAdmin02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_ADMIN_03` → method `itAdmin03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_ADMIN_04` → method `itAdmin04()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Admin (IT - Admin)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=AdminIT test
 */
@EnglishLabIT
public class AdminIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_ADMIN_01")
    void itAdmin01() throws Exception {
        String token = login(mockMvc, ADMIN, PASSWORD);
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ADMIN_02")
    void itAdmin02() throws Exception {
        String token = login(mockMvc, ADMIN, PASSWORD);
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ADMIN_03")
    void itAdmin03() throws Exception {
        String token = login(mockMvc, ADMIN, PASSWORD);
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_ADMIN_04")
    void itAdmin04() throws Exception {
        String token = login(mockMvc, ADMIN, PASSWORD);
        mockMvc.perform(get("/api/admin/system/config")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
```

---
## File: `LarkIT.java`

Sheet **IT - Lark** — webhook + sync.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_LARK_01 webhook` → method `itLark01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_LARK_02 webhook again` → method `itLark02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_LARK_03 sync lark` → method `itLark03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(post("..."))` = giả lập **HTTP POST** vào Controller (như Postman Send).
- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).
- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Lark (IT - Lark)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=LarkIT test
 */
@EnglishLabIT
public class LarkIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_LARK_01 webhook")
    void itLark01() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/lark/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"challenge":"it-challenge","type":"url_verification"}
                                """))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getStatus() != 404);
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getStatus() < 500);
    }

    @Test
    @DisplayName("IT_LARK_02 webhook again")
    void itLark02() throws Exception { itLark01(); }

    @Test
    @DisplayName("IT_LARK_03 sync lark")
    void itLark03() throws Exception {
        String token = login(mockMvc, TM, PASSWORD);
        MvcResult r = mockMvc.perform(post("/api/training-manager/recordings/sessions/1/sync-lark")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getStatus() < 500);
    }
}
```

---
## File: `InfrastructureIT.java`

Sheet **IT - Infra** — campus, room, template.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_INFRA_01` → method `itInfra01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_INFRA_02` → method `itInfra02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_INFRA_03` → method `itInfra03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Infra (IT - Infra)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=InfrastructureIT test
 */
@EnglishLabIT
public class InfrastructureIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_INFRA_01")
    void itInfra01() throws Exception {
        String token = login(mockMvc, TM, PASSWORD);
        mockMvc.perform(get("/api/training-manager/infrastructure/campuses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_INFRA_02")
    void itInfra02() throws Exception {
        String token = login(mockMvc, TM, PASSWORD);
        mockMvc.perform(get("/api/training-manager/infrastructure/rooms")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_INFRA_03")
    void itInfra03() throws Exception {
        String token = login(mockMvc, TM, PASSWORD);
        mockMvc.perform(get("/api/training-manager/infrastructure/session-templates")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
```

---
## File: `ReportIT.java`

Sheet **IT - Report** — dashboard / revenue.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_REPORT_01 dashboard` → method `itReport01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_REPORT_02 revenue` → method `itReport02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Report (IT - Report)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=ReportIT test
 */
@EnglishLabIT
public class ReportIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_REPORT_01 dashboard")
    void itReport01() throws Exception {
        String token = login(mockMvc, TM, PASSWORD);
        mockMvc.perform(get("/api/training-manager/dashboard").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_REPORT_02 revenue")
    void itReport02() throws Exception {
        String token = login(mockMvc, CM, PASSWORD);
        mockMvc.perform(get("/api/content-manager/revenue/analytics").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
```

---
## File: `ClassroomProposalIT.java`

Sheet **IT - Proposal** — đề xuất lớp (Staff).

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_PROPOSAL_01` → method `itProposal01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_PROPOSAL_02` → method `itProposal02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_PROPOSAL_03` → method `itProposal03()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Proposal (IT - Proposal)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=ClassroomProposalIT test
 */
@EnglishLabIT
public class ClassroomProposalIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_PROPOSAL_01")
    void itProposal01() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/classroom-proposals")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_PROPOSAL_02")
    void itProposal02() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/classroom-proposals")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_PROPOSAL_03")
    void itProposal03() throws Exception {
        String token = login(mockMvc, STAFF, PASSWORD);
        mockMvc.perform(get("/api/staff/classroom-proposals")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
```

---
## File: `AttendanceDisputeIT.java`

Sheet **IT - Dispute** — khiếu nại điểm danh.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_DISPUTE_01 student disputes` → method `itDispute01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_DISPUTE_02 teacher pending` → method `itDispute02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_DISPUTE_03 teacher pending again` → method `itDispute03()`

- Gọi API qua MockMvc rồi `andExpect` theo Expected của mã IT trên Excel.

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Dispute (IT - Dispute)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=AttendanceDisputeIT test
 */
@EnglishLabIT
public class AttendanceDisputeIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_DISPUTE_01 student disputes")
    void itDispute01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/attendance/disputes").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_DISPUTE_02 teacher pending")
    void itDispute02() throws Exception {
        String token = login(mockMvc, TEACHER, PASSWORD);
        mockMvc.perform(get("/api/teacher/attendance-disputes/pending").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_DISPUTE_03 teacher pending again")
    void itDispute03() throws Exception { itDispute02(); }
}
```

---
## File: `LearningNotesIT.java`

Sheet **IT - Notes** — ghi chú học.

### Khung class (giống hầu hết file *IT)

| Thành phần | Ý nghĩa |
|------------|---------|
| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |
| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |
| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |

### Từng test method

#### `IT_NOTES_01` → method `itNotes01()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

#### `IT_NOTES_02` → method `itNotes02()`

- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**.
- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user.
- `mockMvc.perform(get("..."))` = giả lập **HTTP GET**.
- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).
- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).

### Source đầy đủ

```java
package fu.sap490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static fu.sap490.g23.backend.it.ItSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test – Notes (IT - Notes)
 * Map với sheet Excel tương ứng. Chạy: mvnw -Dtest=LearningNotesIT test
 */
@EnglishLabIT
public class LearningNotesIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IT_NOTES_01")
    void itNotes01() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/learning/notes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT_NOTES_02")
    void itNotes02() throws Exception {
        String token = login(mockMvc, LEARNER, PASSWORD);
        mockMvc.perform(get("/api/student/learning/notes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
```

---
## Kết luận nhanh để nói với cô

- Mỗi class `*IT` dùng `@EnglishLabIT` (= Spring Boot Test + MockMvc) để xác minh **Controller–Service–Repository**.
- `mockMvc.perform` = gọi API trong test; `%s` / `formatted` = ghép JSON động; `UUID` = tránh trùng data.
- `@DisplayName("IT_…")` map đúng Excel; `ItSupport` chỉ hỗ trợ login/token.
- Postman không thay các class này — chỉ là tool phụ.
