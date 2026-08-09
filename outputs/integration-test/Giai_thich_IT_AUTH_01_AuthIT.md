# Giải thích chi tiết đoạn code `IT_AUTH_01` (AuthIT)

File: `backend/src/test/java/fu/sap490/g23/backend/it/AuthIT.java`  
Case Excel: **IT_AUTH_01** – đăng ký tài khoản mới  
Công cụ: Spring Boot Test + **MockMvc** (Integration Test)

```java
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
```

---

## 1. `String email = "it.reg." + UUID.randomUUID() + "@englishlab-it.test";`

**Mục đích:** tạo một **email mới, không trùng** mỗi lần chạy test.

| Phần | Nghĩa |
|------|--------|
| `"it.reg."` | Tiền tố tự đặt = “integration test – register”. Nhìn log/DB biết đây là email test, không phải user thật. |
| `UUID.randomUUID()` | Hàm Java tạo **chuỗi ngẫu nhiên dài** (kiểu `a1b2c3d4-e5f6-...`). Mỗi lần chạy khác nhau → tránh lỗi “email đã tồn tại”. |
| `"@englishlab-it.test"` | Đuôi email giả. Domain `.test` chỉ dùng trong test, không gửi mail thật. |

**Ví dụ sau khi chạy:**

`it.reg.550e8400-e29b-41d4-a716-446655440000@englishlab-it.test`

Nếu viết cố định `"abc@gmail.com"` rồi chạy lần 2 → thường fail vì trùng email.

---

## 2. `UUID.randomUUID()` là gì?

- **UUID** = Universally Unique Identifier (mã định danh gần như không trùng).
- **`randomUUID()`** = nhờ Java **random** ra một UUID.
- Ở đây dùng như “chuỗi ngẫu nhiên” để **ghép vào email**, không liên quan mật khẩu.

---

## 3. Tại sao `String body = """ ... """` trông như “để trống”?

Đó **không phải body rỗng**. Đó là **Java text block** (dấu `"""`):

```java
String body = """
        {"email":"%s","password":"%s","fullName":"IT Register User"}
        """.formatted(email, PASSWORD);
```

- `"""` … `"""` = chuỗi nhiều dòng, viết JSON cho dễ đọc.
- Bên trong vẫn có JSON đầy đủ: `email`, `password`, `fullName`.
- Dòng trống quanh `"""` chỉ là format code, **không làm body trống**.

Sau `.formatted(...)`, `body` trở thành ví dụ:

```json
{"email":"it.reg.xxx@englishlab-it.test","password":"Password123!","fullName":"IT Register User"}
```

---

## 4. Vì sao email / password lại là `%s`?

`%s` = **chỗ trống chờ điền chuỗi** (string placeholder).

```java
""".formatted(email, PASSWORD);
```

| Thứ tự `%s` | Được thay bằng |
|-------------|----------------|
| `%s` thứ 1 | biến `email` (vừa tạo ở trên) |
| `%s` thứ 2 | hằng `PASSWORD` trong `ItSupport` (= `"Password123!"`) |

**Vì sao không viết cứng email vào JSON?**  
Vì email mỗi lần chạy khác nhau (do UUID). `%s` + `.formatted` = **ghép động** email/password vào JSON.

`fullName` viết sẵn `"IT Register User"` vì không cần đổi mỗi lần.

---

## 5. `mockMvc.perform(...)` là gì?

**MockMvc** = công cụ Spring Test **giả lập HTTP** gọi vào app (giống Postman), nhưng **trong test Java**, đi qua:

`Controller → Service → Repository` (đúng Integration Test).

```java
mockMvc.perform(
        post("/api/auth/register")                 // 1. POST URL register
            .contentType(MediaType.APPLICATION_JSON) // 2. Báo body là JSON
            .content(body)                           // 3. Gửi chuỗi JSON vừa ghép
)
.andExpect(status().is2xxSuccessful());            // 4. Kỳ vọng Status 200–299
```

| Câu lệnh | Giống Postman |
|----------|----------------|
| `perform(...)` | Bấm **Send** |
| `post("/api/auth/register")` | Method POST + URL |
| `.contentType(JSON)` | Header Content-Type |
| `.content(body)` | Ô Body |
| `.andExpect(status().is2xxSuccessful())` | Kiểm tra Status xanh (2xx) |

Không cần mở trình duyệt/Postman — JUnit tự gửi request vào Spring đang chạy trong test.

---

## 6. Các annotation quanh method

| Code | Ý nghĩa |
|------|---------|
| `@Test` | Đánh dấu đây là 1 test case JUnit sẽ chạy |
| `@DisplayName("IT_AUTH_01 register")` | Tên hiển thị = mã **IT_AUTH_01** trên Excel |
| `throws Exception` | Cho phép ném lỗi I/O / MockMvc; JUnit bắt nếu fail |

---

## 7. Tóm tắt 1 mạch

1. Tạo email random (`it.reg.` + UUID + `@englishlab-it.test`)
2. Nhét email + password vào JSON bằng `%s` / `.formatted`
3. `mockMvc.perform(post register)` gửi JSON vào `AuthController`
4. Kỳ vọng đăng ký thành công (`2xx`) → case **Passed**

Luồng tích hợp tương ứng Excel Procedure:

`POST /api/auth/register` → `AuthController.register()` → `AuthService.register()` → `UserRepository` / `AuthTokenRepository`
