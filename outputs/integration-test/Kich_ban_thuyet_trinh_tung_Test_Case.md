# Kịch bản thuyết trình Integration Test — từng Test Case

Dự án: **EnglishLab (SEP490_G23)** · Người trình bày: **phongdx**

Tài liệu này để **học thuộc ý** rồi nói với cô. Mỗi test case có 2 mức:

- **Nói ngắn (15 giây)** — dùng khi cô hỏi lướt nhiều case.
- **Nói đầy đủ (45–60 giây)** — dùng khi cô yêu cầu giải thích sâu 1 case.

---

## Phần A — Mở đầu (thuộc nguyên văn, 30 giây)

> Dạ em trình bày phần Integration Test. Integration Test là kiểm thử **tương tác giữa các thành phần**: Controller gọi Service, Service gọi Repository và các Service gọi lẫn nhau. Em thiết kế test case trên file Excel Report 5.2 gồm 24 module và 111 case, và thực thi bằng **@SpringBootTest kết hợp MockMvc** trong `backend/src/test/java/fu/sap490/g23/backend/it/`. Postman em chỉ dùng để quan sát thủ công, không dùng thay Integration Test.

## Phần B — 6 câu nền tảng (cô hay hỏi chen ngang)

| Cô hỏi | Em trả lời (thuộc ý) |
|---|---|
| Integration Test là gì? | Kiểm thử tích hợp nhiều tầng chạy thật với nhau: Controller → Service → Repository, không mock hết như Unit Test. |
| Vì sao không chỉ dùng Postman? | Postman không nạp Spring context, không chạy trong JUnit nên không chứng minh được các bean thật nối nhau. |
| MockMvc là gì? | Công cụ của Spring Test giả lập HTTP đi thẳng vào Controller thật trong context test. |
| @SpringBootTest để làm gì? | Khởi động gần như toàn bộ ứng dụng để Controller, Service, Repository là bean thật. |
| Code map với Excel thế nào? | Mỗi method test có `@DisplayName` chứa đúng mã `IT_...` trên Excel. |
| Passed / Failed / N/A khác nhau? | Passed là actual khớp expected, kể cả negative bị chặn đúng; Failed là sai expected hoặc lỗi 500; N/A là thiếu tiền điều kiện nên chưa kết luận. |

## Phần C — Cách chạy để demo tại chỗ

```powershell
cd D:\EngLishLab\EnglishLab\backend
.\mvnw.cmd "-Dtest=AuthIT" test
.\mvnw.cmd "-Dtest=*IT" test
```

> Em chạy `mvnw -Dtest=AuthIT test`; Maven in ra số test chạy và kết quả, em đối chiếu với cột Round trên Excel.

---

## Phần D — Kịch bản theo từng Test Case

## Module 1. Authentication — sheet `IT - Auth`

**Câu dẫn cho cả module:**

> Module Authentication nằm ở sheet `IT - Auth`, chạy bằng class `AuthIT (riêng IT_AUTH_03/09 nằm ở AuthOtpIT)`. Vai trò sử dụng: không cần token (trừ /me). Thành phần tích hợp chính: AuthController, AuthService, UserRepository, AuthTokenRepository, JwtAuthenticationFilter.

**Nhóm: Register & verify**

### IT_AUTH_01

**Nói ngắn (15 giây):**

> IT_AUTH_01: Kiểm tra đăng ký tài khoản mới: Controller → Service → lưu users + OTP. Em gọi `POST /api/auth/register`, luồng AuthController → AuthService → UserRepository / AuthTokenRepository (đi qua JwtAuthenticationFilter trước khi vào Controller). Mong đợi 200/201. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra đăng ký tài khoản mới: Controller → Service → lưu users + OTP.
2. **Tiền điều kiện:** database đang chạy; email chưa được dùng; mail được stub.
3. **Bước thực hiện:** gọi `POST /api/auth/register` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AuthController → AuthService → UserRepository / AuthTokenRepository (đi qua JwtAuthenticationFilter trước khi vào Controller).
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200/201 thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Response is 200/201 with a success message
   - One users row is inserted with hashed password (not plaintext)
   - One verification auth_tokens row is linked to that user
   - No access token is required for this call
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_AUTH_02

**Nói ngắn (15 giây):**

> IT_AUTH_02: Kiểm tra từ chối đăng ký trùng email (không tạo user thứ 2). Em gọi `POST /api/auth/register`, luồng AuthController → AuthService → UserRepository / AuthTokenRepository (đi qua JwtAuthenticationFilter trước khi vào Controller). Mong đợi 4xx. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra từ chối đăng ký trùng email (không tạo user thứ 2).
2. **Tiền điều kiện:** An active user with the target email đã tồn tại sẵn.
3. **Bước thực hiện:** gọi `POST /api/auth/register` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AuthController → AuthService → UserRepository / AuthTokenRepository (đi qua JwtAuthenticationFilter trước khi vào Controller).
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 4xx, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 4xx with a duplicate-email business error
   - Exactly one users row remains for the email
   - No extra verification token is created for a second account
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_AUTH_03

**Nói ngắn (15 giây):**

> IT_AUTH_03: Kiểm tra xác thực email bằng OTP: kích hoạt tài khoản. Em gọi `POST /api/auth/verify-email`, luồng AuthController → AuthService → AuthTokenRepository / UserRepository (đi qua JwtAuthenticationFilter trước khi vào Controller). Mong đợi 200. Vòng cũ Failed, sau đó em bổ sung AuthOtpIT đọc OTP thật từ DB để chạy lại.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra xác thực email bằng OTP: kích hoạt tài khoản.
2. **Tiền điều kiện:** A pending verification user and valid OTP exist.
3. **Bước thực hiện:** gọi `POST /api/auth/verify-email` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AuthController → AuthService → AuthTokenRepository / UserRepository (đi qua JwtAuthenticationFilter trước khi vào Controller).
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Response is 200 OK
   - users.email_verified becomes true
   - Verification token is consumed/expired per AuthService rules
   - Subsequent login with the password succeeds
6. **Kết quả thực tế:** Kết quả trên Excel: **Failed** ở vòng chạy cũ. Sau đó em bổ sung `AuthOtpIT` đọc OTP thật từ bảng `auth_tokens` nên case này chạy lại được; nếu chạy lại đạt thì cập nhật Round thành Passed.

### IT_AUTH_04

**Nói ngắn (15 giây):**

> IT_AUTH_04: Kiểm tra OTP sai: không kích hoạt tài khoản. Em gọi `POST /api/auth/verify-email`, luồng AuthController → AuthService → AuthTokenRepository / UserRepository (đi qua JwtAuthenticationFilter trước khi vào Controller). Mong đợi 4xx. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra OTP sai: không kích hoạt tài khoản.
2. **Tiền điều kiện:** đã có user chưa xác thực.
3. **Bước thực hiện:** gọi `POST /api/auth/verify-email` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AuthController → AuthService → AuthTokenRepository / UserRepository (đi qua JwtAuthenticationFilter trước khi vào Controller).
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 4xx, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 4xx
   - email_verified remains false
   - Account is not activated
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

**Nhóm: Login & security**

### IT_AUTH_05

**Nói ngắn (15 giây):**

> IT_AUTH_05: Kiểm tra login cấp JWT và /me đọc đúng user qua Security Filter. Em gọi `POST /api/auth/login`, luồng AuthController / UserController → AuthService → UserRepository / AuthTokenRepository (đi qua JwtAuthenticationFilter trước khi vào Controller). Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra login cấp JWT và /me đọc đúng user qua Security Filter.
2. **Tiền điều kiện:** đã có tài khoản LEARNER đã xác thực.
3. **Bước thực hiện:** gọi `POST /api/auth/login`, `GET /api/user/me` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AuthController / UserController → AuthService → UserRepository / AuthTokenRepository (đi qua JwtAuthenticationFilter trước khi vào Controller).
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và body chứa đúng dữ liệu cần lấy (kèm thông tin phân trang nếu có).
   Nguyên văn trên Excel:
   - Login response is 200 OK and contains accessToken plus user payload
   - GET /api/user/me returns 200 with the same email/id
   - JWT subject matches the authenticated user
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_AUTH_06

**Nói ngắn (15 giây):**

> IT_AUTH_06: Kiểm tra login sai mật khẩu: không cấp token. Em gọi `POST /api/auth/login`, luồng AuthController → AuthService → UserRepository / AuthTokenRepository (đi qua JwtAuthenticationFilter trước khi vào Controller). Mong đợi 401/403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra login sai mật khẩu: không cấp token.
2. **Tiền điều kiện:** đã có user đã xác thực.
3. **Bước thực hiện:** gọi `POST /api/auth/login` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AuthController → AuthService → UserRepository / AuthTokenRepository (đi qua JwtAuthenticationFilter trước khi vào Controller).
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 401/403, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 401/403
   - No valid accessToken is returned
   - Protected endpoints remain inaccessible without a token
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_AUTH_07

**Nói ngắn (15 giây):**

> IT_AUTH_07: Kiểm tra gọi /me không JWT: Security chặn trước Controller. Em gọi `GET /api/user/me`, luồng AuthController → UserService / AuthService → UserRepository / AuthTokenRepository (đi qua JwtAuthenticationFilter trước khi vào Controller). Mong đợi 401. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra gọi /me không JWT: Security chặn trước Controller.
2. **Tiền điều kiện:** Spring Security đang bật.
3. **Bước thực hiện:** gọi `GET /api/user/me` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AuthController → UserService / AuthService → UserRepository / AuthTokenRepository (đi qua JwtAuthenticationFilter trước khi vào Controller).
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 401, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 401 or 403
   - No UserResponse body is returned
   - Confirms controller-security integration
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

**Nhóm: Password recovery**

### IT_AUTH_08

**Nói ngắn (15 giây):**

> IT_AUTH_08: Kiểm tra quên mật khẩu tạo token reset. Em gọi `POST /api/auth/forgot-password`, luồng AuthController → AuthService → AuthTokenRepository / UserRepository (đi qua JwtAuthenticationFilter trước khi vào Controller). Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra quên mật khẩu tạo token reset.
2. **Tiền điều kiện:** Verified user exists; mail is stubbed.
3. **Bước thực hiện:** gọi `POST /api/auth/forgot-password`, `POST /api/auth/reset-password` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AuthController → AuthService → AuthTokenRepository / UserRepository (đi qua JwtAuthenticationFilter trước khi vào Controller).
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Forgot-password returns a generic success response
   - Reset succeeds and users.password hash changes
   - Login works only with the new password
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_AUTH_09

**Nói ngắn (15 giây):**

> IT_AUTH_09: Kiểm tra OTP reset sai: không đổi mật khẩu. Em gọi `POST /api/auth/reset-password`, luồng AuthController → AuthService → AuthTokenRepository / UserRepository (đi qua JwtAuthenticationFilter trước khi vào Controller). Mong đợi 4xx. Vòng cũ Failed, sau đó em bổ sung AuthOtpIT đọc OTP thật từ DB để chạy lại.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra OTP reset sai: không đổi mật khẩu.
2. **Tiền điều kiện:** đã có user đã xác thực.
3. **Bước thực hiện:** gọi `POST /api/auth/reset-password` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AuthController → AuthService → AuthTokenRepository / UserRepository (đi qua JwtAuthenticationFilter trước khi vào Controller).
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 4xx, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 4xx
   - Password hash is unchanged
   - Old credentials still authenticate
6. **Kết quả thực tế:** Kết quả trên Excel: **Failed** ở vòng chạy cũ. Sau đó em bổ sung `AuthOtpIT` đọc OTP thật từ bảng `auth_tokens` nên case này chạy lại được; nếu chạy lại đạt thì cập nhật Round thành Passed.

### IT_AUTH_10

**Nói ngắn (15 giây):**

> IT_AUTH_10: Kiểm tra resend verification tạo/xoay OTP. Em gọi `POST /api/auth/resend-verification`, luồng AuthController → AuthService → AuthTokenRepository / UserRepository (đi qua JwtAuthenticationFilter trước khi vào Controller). Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra resend verification tạo/xoay OTP.
2. **Tiền điều kiện:** đã có user chưa xác thực; mail is stubbed.
3. **Bước thực hiện:** gọi `POST /api/auth/resend-verification` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AuthController → AuthService → AuthTokenRepository / UserRepository (đi qua JwtAuthenticationFilter trước khi vào Controller).
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Response is 200 OK
   - An active verification token exists for the user
   - No account is marked verified by this call alone
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 2. Account Profile — sheet `IT - User`

**Câu dẫn cho cả module:**

> Module Account Profile nằm ở sheet `IT - User`, chạy bằng class `UserIT`. Vai trò sử dụng: LEARNER. Thành phần tích hợp chính: UserController, UserService, AvatarStorageService, UserRepository.

**Nhóm: Profile**

### IT_USER_01

**Nói ngắn (15 giây):**

> IT_USER_01: Kiểm tra lấy hồ sơ hiện tại theo JWT. Em gọi `GET /api/user/me`, luồng UserController → UserService / AvatarStorageService → UserRepository. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra lấy hồ sơ hiện tại theo JWT.
2. **Tiền điều kiện:** A LEARNER account exists and a đã có JWT hợp lệ is available.
3. **Bước thực hiện:** gọi `GET /api/user/me` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** UserController → UserService / AvatarStorageService → UserRepository.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và body chứa đúng dữ liệu cần lấy (kèm thông tin phân trang nếu có).
   Nguyên văn trên Excel:
   - Response is 200 OK
   - Returned JSON contains correct id, email, fullName and role
   - Values match the users row for the token subject
   - No other user's data is returned
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_USER_02

**Nói ngắn (15 giây):**

> IT_USER_02: Kiểm tra cập nhật hồ sơ được lưu DB. Em gọi `PUT /api/user/me`, luồng UserController → UserService / AvatarStorageService → UserRepository. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra cập nhật hồ sơ được lưu DB.
2. **Tiền điều kiện:** đã có JWT hợp lệ; request passes validation.
3. **Bước thực hiện:** gọi `PUT /api/user/me`, `GET /api/user/me` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** UserController → UserService / AvatarStorageService → UserRepository.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Response is 200 OK
   - Persisted columns match the request payload
   - GET /api/user/me reflects the updates
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_USER_03

**Nói ngắn (15 giây):**

> IT_USER_03: Kiểm tra đổi mật khẩu (sai current bị từ chối; đúng thì đổi hash). Em gọi `PUT /api/user/me/password`, luồng UserController → UserService / AvatarStorageService → UserRepository. Mong đợi 4xx. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra đổi mật khẩu (sai current bị từ chối; đúng thì đổi hash).
2. **Tiền điều kiện:** Account with a known password exists.
3. **Bước thực hiện:** gọi `PUT /api/user/me/password` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** UserController → UserService / AvatarStorageService → UserRepository.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 4xx, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Wrong current password returns 4xx and hash is unchanged
   - Correct change returns success/204 and hash changes
   - Login succeeds only with the new password
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_USER_04

**Nói ngắn (15 giây):**

> IT_USER_04: Kiểm tra upload avatar. Em gọi `POST /api/user/me/avatar`, luồng UserController → UserService / AvatarStorageService → UserRepository. Mong đợi 200. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra upload avatar.
2. **Tiền điều kiện:** đã có JWT hợp lệ; avatar storage is writable.
3. **Bước thực hiện:** gọi `POST /api/user/me/avatar`, `GET /api/user/avatars/{fileName}` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** UserController → UserService / AvatarStorageService → UserRepository.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Upload response is 200 OK with updated user payload
   - users.avatar_url is set
   - Public avatar GET returns the stored file content type
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_USER_05

**Nói ngắn (15 giây):**

> IT_USER_05: Kiểm tra sửa hồ sơ không token bị chặn. Em gọi `PUT /api/user/me`, luồng UserController → UserService / AvatarStorageService → UserRepository. Mong đợi 401/403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra sửa hồ sơ không token bị chặn.
2. **Tiền điều kiện:** None.
3. **Bước thực hiện:** gọi `PUT /api/user/me` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** UserController → UserService / AvatarStorageService → UserRepository.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 401/403, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 401/403
   - No profile fields are updated in the database
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 3. Notifications — sheet `IT - Notif`

**Câu dẫn cho cả module:**

> Module Notifications nằm ở sheet `IT - Notif`, chạy bằng class `NotificationIT`. Vai trò sử dụng: LEARNER. Thành phần tích hợp chính: UserController, NotificationPreferenceService, StudentNotificationController, AppNotificationService.

**Nhóm: Preferences**

### IT_NOTIF_01

**Nói ngắn (15 giây):**

> IT_NOTIF_01: Kiểm tra đọc preference thông báo. Em gọi `GET /api/user/me/notification-preferences`, luồng UserController / StudentNotificationController → NotificationPreferenceService / AppNotificationService → NotificationPreferenceRepository. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra đọc preference thông báo.
2. **Tiền điều kiện:** Authenticated user without a preference row.
3. **Bước thực hiện:** gọi `GET /api/user/me/notification-preferences` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** UserController / StudentNotificationController → NotificationPreferenceService / AppNotificationService → NotificationPreferenceRepository.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Response is 200 OK
   - emailEnabled=true and inAppEnabled=true
   - GET does not require a pre-inserted preference row
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_NOTIF_02

**Nói ngắn (15 giây):**

> IT_NOTIF_02: Kiểm tra cập nhật preference thông báo. Em gọi `PUT /api/user/me/notification-preferences`, luồng UserController / StudentNotificationController → NotificationPreferenceService / AppNotificationService → NotificationPreferenceRepository / AppNotificationRepository. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra cập nhật preference thông báo.
2. **Tiền điều kiện:** đã có JWT hợp lệ; a notify path is available in the test harness.
3. **Bước thực hiện:** gọi `PUT /api/user/me/notification-preferences` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** UserController / StudentNotificationController → NotificationPreferenceService / AppNotificationService → NotificationPreferenceRepository / AppNotificationRepository.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Preference row is persisted with inAppEnabled=false
   - No new app_notifications row is created while in-app is disabled
   - After re-enable, a new notification row is inserted
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_NOTIF_03

**Nói ngắn (15 giây):**

> IT_NOTIF_03: Kiểm tra body preference thiếu → validation lỗi. Em gọi `PUT /api/user/me/notification-preferences`, luồng UserController / StudentNotificationController → NotificationPreferenceService / AppNotificationService. Mong đợi 400. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra body preference thiếu → validation lỗi.
2. **Tiền điều kiện:** đã có JWT hợp lệ.
3. **Bước thực hiện:** gọi `PUT /api/user/me/notification-preferences` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** UserController / StudentNotificationController → NotificationPreferenceService / AppNotificationService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 400, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 400 Bad Request
   - No partial preference overwrite occurs
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

**Nhóm: In-app list**

### IT_NOTIF_04

**Nói ngắn (15 giây):**

> IT_NOTIF_04: Kiểm tra danh sách thông báo học viên. Em gọi `GET /api/student/notifications`, luồng StudentNotificationController / UserController → AppNotificationService / NotificationPreferenceService → AppNotificationRepository. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra danh sách thông báo học viên.
2. **Tiền điều kiện:** LEARNER JWT; notification rows owned by that user.
3. **Bước thực hiện:** gọi `GET /api/student/notifications`, `PATCH /api/student/notifications/{id}/read`, `GET /api/student/notifications/unread-count` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentNotificationController / UserController → AppNotificationService / NotificationPreferenceService → AppNotificationRepository.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và body chứa đúng dữ liệu cần lấy (kèm thông tin phân trang nếu có).
   Nguyên văn trên Excel:
   - List returns the seeded notifications
   - Mark-read sets read=true and read_at
   - Unread count decreases by one
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_NOTIF_05

**Nói ngắn (15 giây):**

> IT_NOTIF_05: Kiểm tra đếm / đánh dấu đã đọc thông báo. Em gọi `PATCH /api/student/notifications/read-all`, luồng UserController / StudentNotificationController → AppNotificationService / NotificationPreferenceService → AppNotificationRepository. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra đếm / đánh dấu đã đọc thông báo.
2. **Tiền điều kiện:** Two LEARNER accounts with unread notifications.
3. **Bước thực hiện:** gọi `PATCH /api/student/notifications/read-all` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** UserController / StudentNotificationController → AppNotificationService / NotificationPreferenceService → AppNotificationRepository.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - All of A's unread notifications become read
   - B's notifications remain unread
   - No cross-user update occurs
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 4. Cart & Wishlist — sheet `IT - Commerce`

**Câu dẫn cho cả module:**

> Module Cart & Wishlist nằm ở sheet `IT - Commerce`, chạy bằng class `CommerceIT`. Vai trò sử dụng: LEARNER. Thành phần tích hợp chính: StudentCommerceController, StudentCommerceService.

**Nhóm: Cart & wishlist**

### IT_COMMERCE_01

**Nói ngắn (15 giây):**

> IT_COMMERCE_01: Kiểm tra thêm khóa vào giỏ hàng. Em gọi `POST /api/student/commerce/cart/{courseId}`, luồng StudentCommerceController → StudentCommerceService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra thêm khóa vào giỏ hàng.
2. **Tiền điều kiện:** Published course and LEARNER JWT are available.
3. **Bước thực hiện:** gọi `POST /api/student/commerce/cart/{courseId}`, `GET /api/student/commerce/cart` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentCommerceController → StudentCommerceService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và body chứa đúng dữ liệu cần lấy (kèm thông tin phân trang nếu có).
   Nguyên văn trên Excel:
   - Add response is 200 OK
   - GET cart contains the courseId
   - Unauthenticated POST is rejected by security
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_COMMERCE_02

**Nói ngắn (15 giây):**

> IT_COMMERCE_02: Kiểm tra chuyển wishlist sang giỏ. Em gọi `POST /api/student/commerce/wishlist/{courseId}`, luồng StudentCommerceController → StudentCommerceService. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra chuyển wishlist sang giỏ.
2. **Tiền điều kiện:** LEARNER JWT; course is not already owned.
3. **Bước thực hiện:** gọi `POST /api/student/commerce/wishlist/{courseId}`, `POST /api/student/commerce/wishlist/{courseId}/move-to-cart` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentCommerceController → StudentCommerceService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Course appears in wishlist then moves to cart per service rules
   - No course ownership/enrollment is created yet
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_COMMERCE_03

**Nói ngắn (15 giây):**

> IT_COMMERCE_03: Kiểm tra xóa giỏ hàng. Em gọi `DELETE /api/student/commerce/cart`, luồng StudentCommerceController → StudentCommerceService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra xóa giỏ hàng.
2. **Tiền điều kiện:** LEARNER JWT with a non-empty cart.
3. **Bước thực hiện:** gọi `DELETE /api/student/commerce/cart` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentCommerceController → StudentCommerceService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Cart is empty after delete
   - No payment_orders row is inserted by clear-cart
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_COMMERCE_04

**Nói ngắn (15 giây):**

> IT_COMMERCE_04: Kiểm tra thêm lại khóa vào giỏ sau khi xóa. Em gọi `POST /api/student/commerce/cart/{courseId}`, luồng StudentCommerceController → StudentCommerceService. Mong đợi 403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra thêm lại khóa vào giỏ sau khi xóa.
2. **Tiền điều kiện:** TEACHER JWT.
3. **Bước thực hiện:** gọi `POST /api/student/commerce/cart/{courseId}` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentCommerceController → StudentCommerceService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 403 Forbidden
   - No cart mutation is persisted
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 5. PayOS & Orders — sheet `IT - Payment`

**Câu dẫn cho cả module:**

> Module PayOS & Orders nằm ở sheet `IT - Payment`, chạy bằng class `PaymentIT`. Vai trò sử dụng: LEARNER và MANAGER. Thành phần tích hợp chính: StudentPaymentController, PayosWebhookController, PaymentService, payment_orders.

**Nhóm: Checkout**

### IT_PAYMENT_01

**Nói ngắn (15 giây):**

> IT_PAYMENT_01: Kiểm tra tạo link thanh toán PayOS. Em gọi `POST /api/student/payments/payos/link`, luồng StudentPaymentController / PayosWebhookController → PaymentService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra tạo link thanh toán PayOS.
2. **Tiền điều kiện:** LEARNER JWT; PayOS client stubbed.
3. **Bước thực hiện:** gọi `POST /api/student/payments/payos/link` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentPaymentController / PayosWebhookController → PaymentService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Response is 200 OK with checkout URL/orderCode
   - One payment_orders row exists with PENDING (or equivalent) status
   - Amount matches the quoted total
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_PAYMENT_02

**Nói ngắn (15 giây):**

> IT_PAYMENT_02: Kiểm tra quote giá đơn hàng. Em gọi `API tương ứng`, luồng StudentPaymentController / PayosWebhookController → PaymentService. Mong đợi 4xx. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra quote giá đơn hàng.
2. **Tiền điều kiện:** LEARNER with empty cart.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentPaymentController / PayosWebhookController → PaymentService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 4xx, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 4xx business error
   - No new payment_orders row is created
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_PAYMENT_03

**Nói ngắn (15 giây):**

> IT_PAYMENT_03: Kiểm tra webhook thiếu chữ ký bị từ chối. Em gọi `POST /api/payos/webhook`, luồng PayosWebhookController / StudentPaymentController → PaymentService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra webhook thiếu chữ ký bị từ chối.
2. **Tiền điều kiện:** Pending order exists; webhook verification configured for test.
3. **Bước thực hiện:** gọi `POST /api/payos/webhook` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PayosWebhookController / StudentPaymentController → PaymentService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Order status becomes PAID/SUCCESS
   - Learner gains course access exactly once
   - Replay does not create a duplicate enrollment/ownership row
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_PAYMENT_04

**Nói ngắn (15 giây):**

> IT_PAYMENT_04: Kiểm tra Manager xem danh sách orders. Em gọi `GET /api/student/payments/orders`, luồng StudentPaymentController / PayosWebhookController → PaymentService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra Manager xem danh sách orders.
2. **Tiền điều kiện:** Two LEARNER accounts with distinct orders.
3. **Bước thực hiện:** gọi `GET /api/student/payments/orders` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentPaymentController / PayosWebhookController → PaymentService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Response is 200 OK
   - Only learner A's orders are returned
   - No cross-user order leakage
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_PAYMENT_05

**Nói ngắn (15 giây):**

> IT_PAYMENT_05: Kiểm tra chi tiết / lọc orders (nếu có). Em gọi `GET /api/manager/payments/orders`, luồng StudentPaymentController / PayosWebhookController → PaymentService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra chi tiết / lọc orders (nếu có).
2. **Tiền điều kiện:** Seeded orders; MANAGER/CM and LEARNER tokens.
3. **Bước thực hiện:** gọi `GET /api/manager/payments/orders` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentPaymentController / PayosWebhookController → PaymentService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 200, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Staff/CM call returns 200 with order data
   - LEARNER receives 403 on the manager payments path
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 6. Online Learning — sheet `IT - Course`

**Câu dẫn cho cả module:**

> Module Online Learning nằm ở sheet `IT - Course`, chạy bằng class `OnlineCourseIT`. Vai trò sử dụng: public và LEARNER. Thành phần tích hợp chính: PublicOnlineCourseController, StudentOnlineCourseController, OnlineCourseService.

**Nhóm: Catalog**

### IT_COURSE_01

**Nói ngắn (15 giây):**

> IT_COURSE_01: Kiểm tra list khóa học public. Em gọi `GET /api/online-courses`, luồng PublicOnlineCourseController / StudentOnlineCourseController → OnlineCourseService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra list khóa học public.
2. **Tiền điều kiện:** Published and draft courses are seeded.
3. **Bước thực hiện:** gọi `GET /api/online-courses` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicOnlineCourseController / StudentOnlineCourseController → OnlineCourseService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Response is 200 OK
   - Published courses appear
   - Draft courses are hidden according to service rules
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_COURSE_02

**Nói ngắn (15 giây):**

> IT_COURSE_02: Kiểm tra chi tiết khóa public. Em gọi `GET /api/online-courses/{slugOrId}`, luồng PublicOnlineCourseController / StudentOnlineCourseController → OnlineCourseService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra chi tiết khóa public.
2. **Tiền điều kiện:** A published course exists.
3. **Bước thực hiện:** gọi `GET /api/online-courses/{slugOrId}` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicOnlineCourseController / StudentOnlineCourseController → OnlineCourseService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 200, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Published detail returns 200 with course payload
   - Missing course returns 404
   - No authentication is required for public GET
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

**Nhóm: Learner progress**

### IT_COURSE_03

**Nói ngắn (15 giây):**

> IT_COURSE_03: Kiểm tra học viên xem content (cần enroll). Em gọi `GET /api/student/online-courses/{courseId}/content`, luồng PublicOnlineCourseController / StudentOnlineCourseController → OnlineCourseService. Mong đợi 200. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra học viên xem content (cần enroll).
2. **Tiền điều kiện:** Enrolled LEARNER; course contains lessons.
3. **Bước thực hiện:** gọi `GET /api/student/online-courses/{courseId}/content`, `PATCH /api/student/online-courses/{courseId}/lessons/{lessonId}/progress` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicOnlineCourseController / StudentOnlineCourseController → OnlineCourseService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Content response is 200 with modules/lessons
   - Progress patch persists for the lesson
   - Unauthenticated content call is rejected
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_COURSE_04

**Nói ngắn (15 giây):**

> IT_COURSE_04: Kiểm tra cập nhật progress bài học. Em gọi `API tương ứng`, luồng PublicOnlineCourseController / StudentOnlineCourseController → OnlineCourseService. Mong đợi 403/404. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra cập nhật progress bài học.
2. **Tiền điều kiện:** Published course without enrollment for the caller.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicOnlineCourseController / StudentOnlineCourseController → OnlineCourseService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403/404, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 403/404
   - No protected content payload is returned
   - No progress side effect occurs
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_COURSE_05

**Nói ngắn (15 giây):**

> IT_COURSE_05: Kiểm tra rating khóa học. Em gọi `GET /api/student/online-courses/my-enrollments`, luồng PublicOnlineCourseController / StudentOnlineCourseController → OnlineCourseService. Mong đợi 200. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra rating khóa học.
2. **Tiền điều kiện:** Two LEARNER accounts with different enrollments.
3. **Bước thực hiện:** gọi `GET /api/student/online-courses/my-enrollments` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicOnlineCourseController / StudentOnlineCourseController → OnlineCourseService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Response is 200 OK
   - Only learner A courses are returned
   - No cross-user enrollment leakage
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_COURSE_06

**Nói ngắn (15 giây):**

> IT_COURSE_06: Kiểm tra từ chối content khi chưa enroll. Em gọi `POST /api/student/online-courses/{courseId}/rating`, luồng StudentOnlineCourseController / PublicOnlineCourseController → CourseReviewService / OnlineCourseService. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra từ chối content khi chưa enroll.
2. **Tiền điều kiện:** Eligible LEARNER for the target course.
3. **Bước thực hiện:** gọi `POST /api/student/online-courses/{courseId}/rating` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentOnlineCourseController / PublicOnlineCourseController → CourseReviewService / OnlineCourseService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Review is upserted per unique student-course constraint
   - GET returns the saved rating
   - Unauthorized/non-eligible caller is denied
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

---

## Module 7. Course Discussion — sheet `IT - Discuss`

**Câu dẫn cho cả module:**

> Module Course Discussion nằm ở sheet `IT - Discuss`, chạy bằng class `DiscussionIT`. Vai trò sử dụng: LEARNER và CONTENT_MANAGER. Thành phần tích hợp chính: CourseDiscussionController, DiscussionModerationController, CourseDiscussionService, DiscussionModerationService.

**Nhóm: Learner discussion**

### IT_DISCUSS_01

**Nói ngắn (15 giây):**

> IT_DISCUSS_01: Kiểm tra tạo thảo luận (cần enroll). Em gọi `API tương ứng`, luồng CourseDiscussionController / DiscussionModerationController → CourseDiscussionService / DiscussionModerationService. Mong đợi 200. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra tạo thảo luận (cần enroll).
2. **Tiền điều kiện:** Enrolled LEARNER JWT.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** CourseDiscussionController / DiscussionModerationController → CourseDiscussionService / DiscussionModerationService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Response is 200 OK
   - One thread row is inserted for the course
   - Thread owner matches the authenticated learner
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_DISCUSS_02

**Nói ngắn (15 giây):**

> IT_DISCUSS_02: Kiểm tra list thảo luận. Em gọi `API tương ứng`, luồng CourseDiscussionController / DiscussionModerationController → CourseDiscussionService / DiscussionModerationService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra list thảo luận.
2. **Tiền điều kiện:** Existing discussion thread.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** CourseDiscussionController / DiscussionModerationController → CourseDiscussionService / DiscussionModerationService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Reply row is created
   - Reaction/helpful state is persisted consistently
   - Counts in response match database state
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_DISCUSS_03

**Nói ngắn (15 giây):**

> IT_DISCUSS_03: Kiểm tra report thread. Em gọi `GET /api/content-manager/discussion-reports`, luồng CourseDiscussionController / DiscussionModerationController → CourseDiscussionService / DiscussionModerationService. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra report thread.
2. **Tiền điều kiện:** LEARNER and CONTENT_MANAGER accounts.
3. **Bước thực hiện:** gọi `GET /api/content-manager/discussion-reports` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** CourseDiscussionController / DiscussionModerationController → CourseDiscussionService / DiscussionModerationService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Report is visible in CM queue
   - Report references the reported thread/reply
   - Learner cannot call moderation hide/dismiss
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

**Nhóm: Moderation**

### IT_DISCUSS_04

**Nói ngắn (15 giây):**

> IT_DISCUSS_04: Kiểm tra report khi thiếu thread / quyền. Em gọi `API tương ứng`, luồng CourseDiscussionController / DiscussionModerationController → DiscussionModerationService / CourseDiscussionService. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra report khi thiếu thread / quyền.
2. **Tiền điều kiện:** Pending report exists.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** CourseDiscussionController / DiscussionModerationController → DiscussionModerationService / CourseDiscussionService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Report status is updated
   - Thread/reply visibility follows moderation outcome
   - Audit/report row remains queryable
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_DISCUSS_05

**Nói ngắn (15 giây):**

> IT_DISCUSS_05: Kiểm tra CM xem discussion reports. Em gọi `API tương ứng`, luồng CourseDiscussionController / DiscussionModerationController → DiscussionModerationService / CourseDiscussionService. Mong đợi 403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra CM xem discussion reports.
2. **Tiền điều kiện:** LEARNER JWT; pending report id.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** CourseDiscussionController / DiscussionModerationController → DiscussionModerationService / CourseDiscussionService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 403 Forbidden
   - Report status is unchanged
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 8. CM Online Courses — sheet `IT - Content`

**Câu dẫn cho cả module:**

> Module CM Online Courses nằm ở sheet `IT - Content`, chạy bằng class `ContentManagerCourseIT`. Vai trò sử dụng: CONTENT_MANAGER. Thành phần tích hợp chính: ContentManagerOnlineCourseController, OnlineCourseService, version/category services.

**Nhóm: Course CM**

### IT_CONTENT_01

**Nói ngắn (15 giây):**

> IT_CONTENT_01: Kiểm tra CM list khóa online. Em gọi `POST /api/content-manager/online-courses`, luồng ContentManagerOnlineCourseController → OnlineCourseService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra CM list khóa online.
2. **Tiền điều kiện:** CONTENT_MANAGER JWT.
3. **Bước thực hiện:** gọi `POST /api/content-manager/online-courses` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerOnlineCourseController → OnlineCourseService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 200, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - CM create returns 200 and inserts online_courses
   - LEARNER receives 403 on CM API
   - Persisted title/status match request
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CONTENT_02

**Nói ngắn (15 giây):**

> IT_CONTENT_02: Kiểm tra CM tạo/cập nhật khóa (nếu case tạo). Em gọi `GET /api/online-courses`, luồng ContentManagerOnlineCourseController → OnlineCourseService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra CM tạo/cập nhật khóa (nếu case tạo).
2. **Tiền điều kiện:** Draft course owned/managed by CM.
3. **Bước thực hiện:** gọi `GET /api/online-courses` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerOnlineCourseController → OnlineCourseService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Status transitions are persisted
   - Public catalog reflects publish rules
   - Archived/draft course is not publicly listed
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CONTENT_03

**Nói ngắn (15 giây):**

> IT_CONTENT_03: Kiểm tra CM publish/unpublish. Em gọi `POST /api/content-manager/online-courses/{courseId}/versions`, luồng ContentManagerOnlineCourseController → OnlineCourseService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra CM publish/unpublish.
2. **Tiền điều kiện:** CM course exists.
3. **Bước thực hiện:** gọi `POST /api/content-manager/online-courses/{courseId}/versions` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerOnlineCourseController → OnlineCourseService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Version row is created
   - Publish flag/status is consistent
   - Unauthorized role cannot publish versions
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CONTENT_04

**Nói ngắn (15 giây):**

> IT_CONTENT_04: Kiểm tra CM xem chi tiết khóa quản trị. Em gọi `DELETE /api/content-manager/course-categories`, luồng ContentManagerOnlineCourseController → CourseCategoryManagementService / OnlineCourseService. Mong đợi 400. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra CM xem chi tiết khóa quản trị.
2. **Tiền điều kiện:** CONTENT_MANAGER JWT.
3. **Bước thực hiện:** gọi `DELETE /api/content-manager/course-categories` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerOnlineCourseController → CourseCategoryManagementService / OnlineCourseService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 400, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - CRUD operations persist correctly
   - Invalid payload returns 400 without insert
   - LEARNER is forbidden
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 9. Packages & Bundles — sheet `IT - Package`

**Câu dẫn cho cả module:**

> Module Packages & Bundles nằm ở sheet `IT - Package`, chạy bằng class `PackageIT`. Vai trò sử dụng: CONTENT_MANAGER. Thành phần tích hợp chính: ContentManagerPackageController, LearningPackageManagementService, packages.

**Nhóm: Packages**

### IT_PACKAGE_01

**Nói ngắn (15 giây):**

> IT_PACKAGE_01: Kiểm tra CM list packages. Em gọi `POST /api/content-manager/packages`, luồng ContentManagerPackageController → LearningPackageManagementService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra CM list packages.
2. **Tiền điều kiện:** CONTENT_MANAGER JWT.
3. **Bước thực hiện:** gọi `POST /api/content-manager/packages` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerPackageController → LearningPackageManagementService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Response is 200 OK
   - One packages row is inserted with request fields
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_PACKAGE_02

**Nói ngắn (15 giây):**

> IT_PACKAGE_02: Kiểm tra CM chi tiết / tạo package. Em gọi `PUT /api/content-manager/packages/{id}/bundle-items`, luồng ContentManagerPackageController → LearningPackageManagementService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra CM chi tiết / tạo package.
2. **Tiền điều kiện:** Package and candidate courses exist.
3. **Bước thực hiện:** gọi `PUT /api/content-manager/packages/{id}/bundle-items` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerPackageController → LearningPackageManagementService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Relations match the requested course/item set
   - No orphan relation rows remain
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_PACKAGE_03

**Nói ngắn (15 giây):**

> IT_PACKAGE_03: Kiểm tra gắn khóa vào package. Em gọi `API tương ứng`, luồng ContentManagerPackageController → LearningPackageManagementService. Mong đợi 403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra gắn khóa vào package.
2. **Tiền điều kiện:** Package exists; CONTENT_MANAGER JWT.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerPackageController → LearningPackageManagementService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Status is updated for CM caller
   - LEARNER receives 403
   - Database status matches the last successful transition
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 10. Curriculum & Banks — sheet `IT - Curriculum`

**Câu dẫn cho cả module:**

> Module Curriculum & Banks nằm ở sheet `IT - Curriculum`, chạy bằng class `CurriculumIT`. Vai trò sử dụng: CONTENT_MANAGER. Thành phần tích hợp chính: ContentManagerCurriculumController, CurriculumProgramService, ExerciseBankController, AssessmentRubricController, LearningPathController.

**Nhóm: Curriculum**

### IT_CURRICULUM_01

**Nói ngắn (15 giây):**

> IT_CURRICULUM_01: Kiểm tra list curriculum programs. Em gọi `API tương ứng`, luồng ContentManagerCurriculumController / ExerciseBankController → CurriculumProgramService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra list curriculum programs.
2. **Tiền điều kiện:** CONTENT_MANAGER JWT.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerCurriculumController / ExerciseBankController → CurriculumProgramService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Program row is inserted
   - List includes the created program
   - Unauthorized role is rejected
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CURRICULUM_02

**Nói ngắn (15 giây):**

> IT_CURRICULUM_02: Kiểm tra exercise/assessment bank. Em gọi `API tương ứng`, luồng ContentManagerCurriculumController / ExerciseBankController → CurriculumProgramService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra exercise/assessment bank.
2. **Tiền điều kiện:** Program exists.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerCurriculumController / ExerciseBankController → CurriculumProgramService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Unit is saved
   - Foreign key points to the parent program
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CURRICULUM_03

**Nói ngắn (15 giây):**

> IT_CURRICULUM_03: Kiểm tra learning paths. Em gọi `DELETE /api/content-manager/exercise-bank`, luồng ContentManagerCurriculumController / ExerciseBankController → CurriculumProgramService. Mong đợi 400. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra learning paths.
2. **Tiền điều kiện:** CONTENT_MANAGER JWT.
3. **Bước thực hiện:** gọi `DELETE /api/content-manager/exercise-bank` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerCurriculumController / ExerciseBankController → CurriculumProgramService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 400, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - CRUD succeeds for valid payloads
   - Invalid payload returns 400
   - Deleted/archived item is no longer returned by default list rules
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CURRICULUM_04

**Nói ngắn (15 giây):**

> IT_CURRICULUM_04: Kiểm tra rubrics. Em gọi `POST /api/content-manager/learning-paths`, luồng ContentManagerCurriculumController / ExerciseBankController → CurriculumProgramService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra rubrics.
2. **Tiền điều kiện:** CONTENT_MANAGER JWT; courses exist.
3. **Bước thực hiện:** gọi `POST /api/content-manager/learning-paths` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerCurriculumController / ExerciseBankController → CurriculumProgramService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Path is created
   - Course membership and order match the request
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CURRICULUM_05

**Nói ngắn (15 giây):**

> IT_CURRICULUM_05: Kiểm tra chi tiết chương trình / liên kết bank. Em gọi `POST /api/content-manager/rubrics`, luồng ContentManagerCurriculumController / ExerciseBankController → CurriculumProgramService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra chi tiết chương trình / liên kết bank.
2. **Tiền điều kiện:** CONTENT_MANAGER JWT.
3. **Bước thực hiện:** gọi `POST /api/content-manager/rubrics` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerCurriculumController / ExerciseBankController → CurriculumProgramService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Rubric header and criteria rows are inserted
   - Weights/names match the request payload
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 11. Enrollment Requests — sheet `IT - EnrollReq`

**Câu dẫn cho cả module:**

> Module Enrollment Requests nằm ở sheet `IT - EnrollReq`, chạy bằng class `EnrollmentRequestIT`. Vai trò sử dụng: LEARNER và STAFF. Thành phần tích hợp chính: StudentEnrollmentRequestController, StaffEnrollmentRequestController, EnrollmentRequestService.

**Nhóm: Learner request**

### IT_ENROLLREQ_01

**Nói ngắn (15 giây):**

> IT_ENROLLREQ_01: Kiểm tra HV tạo enrollment request. Em gọi `POST /api/student/course-enrollment-requests`, luồng StudentEnrollmentRequestController / StaffEnrollmentRequestController → EnrollmentRequestService. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV tạo enrollment request.
2. **Tiền điều kiện:** LEARNER JWT.
3. **Bước thực hiện:** gọi `POST /api/student/course-enrollment-requests` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentEnrollmentRequestController / StaffEnrollmentRequestController → EnrollmentRequestService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Request row is created for the learner
   - It appears in listMine
   - Initial status matches service defaults
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_ENROLLREQ_02

**Nói ngắn (15 giây):**

> IT_ENROLLREQ_02: Kiểm tra Staff list enrollment requests. Em gọi `API tương ứng`, luồng StudentEnrollmentRequestController / StaffEnrollmentRequestController → EnrollmentRequestService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra Staff list enrollment requests.
2. **Tiền điều kiện:** Open request owned by the learner.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentEnrollmentRequestController / StaffEnrollmentRequestController → EnrollmentRequestService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Request status becomes cancelled
   - A history entry is written
   - Cancelled request cannot be processed as open
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

**Nhóm: Staff processing**

### IT_ENROLLREQ_03

**Nói ngắn (15 giây):**

> IT_ENROLLREQ_03: Kiểm tra Staff xử lý / cập nhật request. Em gọi `API tương ứng`, luồng StaffEnrollmentRequestController / StudentEnrollmentRequestController → EnrollmentRequestService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra Staff xử lý / cập nhật request.
2. **Tiền điều kiện:** Pending request exists.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StaffEnrollmentRequestController / StudentEnrollmentRequestController → EnrollmentRequestService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Status transitions follow the staff workflow
   - Invalid transition is rejected
   - Mail side effects may be stubbed
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_ENROLLREQ_04

**Nói ngắn (15 giây):**

> IT_ENROLLREQ_04: Kiểm tra HV xem request của mình. Em gọi `API tương ứng`, luồng StudentEnrollmentRequestController / StaffEnrollmentRequestController → EnrollmentRequestService. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV xem request của mình.
2. **Tiền điều kiện:** Eligible request and offering with capacity.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentEnrollmentRequestController / StaffEnrollmentRequestController → EnrollmentRequestService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Request reaches assigned/completed state per rules
   - Related classroom enrollment is created or updated
   - Capacity/business rules are enforced
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_ENROLLREQ_05

**Nói ngắn (15 giây):**

> IT_ENROLLREQ_05: Kiểm tra từ chối tạo trùng / validation. Em gọi `API tương ứng`, luồng StudentEnrollmentRequestController / StaffEnrollmentRequestController → EnrollmentRequestService. Mong đợi 403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra từ chối tạo trùng / validation.
2. **Tiền điều kiện:** LEARNER JWT; existing request id.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentEnrollmentRequestController / StaffEnrollmentRequestController → EnrollmentRequestService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 403
   - Request status is unchanged
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 12. TM Classroom Ops — sheet `IT - Classroom`

**Câu dẫn cho cả module:**

> Module TM Classroom Ops nằm ở sheet `IT - Classroom`, chạy bằng class `TrainingManagerClassroomIT`. Vai trò sử dụng: TRAINING_MANAGER. Thành phần tích hợp chính: PublicClassroomController, TrainingManagerClassroomController, ClassroomOfferingService.

**Nhóm: Public & TM offering**

### IT_CLASS_01

**Nói ngắn (15 giây):**

> IT_CLASS_01: Kiểm tra public list classroom offerings. Em gọi `GET /api/classroom-offerings`, luồng PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra public list classroom offerings.
2. **Tiền điều kiện:** At least one public offering exists.
3. **Bước thực hiện:** gọi `GET /api/classroom-offerings` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và body chứa đúng dữ liệu cần lấy (kèm thông tin phân trang nếu có).
   Nguyên văn trên Excel:
   - List/detail return 200
   - Payload maps from classroom_offerings and related package title fields
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CLASS_02

**Nói ngắn (15 giây):**

> IT_CLASS_02: Kiểm tra TM list classrooms. Em gọi `GET /api/training-manager/classrooms`, luồng TrainingManagerClassroomController / PublicClassroomController → ClassroomOfferingService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra TM list classrooms.
2. **Tiền điều kiện:** TM/STAFF JWT; offerings seeded.
3. **Bước thực hiện:** gọi `GET /api/training-manager/classrooms` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** TrainingManagerClassroomController / PublicClassroomController → ClassroomOfferingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 200, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - TM/staff calls return 200
   - LEARNER receives 403 on TM classroom APIs
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

**Nhóm: Enrollment pipeline**

### IT_CLASS_03

**Nói ngắn (15 giây):**

> IT_CLASS_03: Kiểm tra TM xem chi tiết lớp. Em gọi `API tương ứng`, luồng PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra TM xem chi tiết lớp.
2. **Tiền điều kiện:** TM JWT; offering has free capacity.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Final registration_status is ASSIGNED
   - Tuition payment row exists
   - Learner class-access semantics become true
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CLASS_04

**Nói ngắn (15 giây):**

> IT_CLASS_04: Kiểm tra TM xem registrations. Em gọi `API tương ứng`, luồng PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra TM xem registrations.
2. **Tiền điều kiện:** Pending enrollment; TM JWT.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - registration_status=REJECTED
   - No ASSIGNED capacity is consumed
   - Reject reason is stored per service rules
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CLASS_05

**Nói ngắn (15 giây):**

> IT_CLASS_05: Kiểm tra reorder waitlist. Em gọi `API tương ứng`, luồng PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService → ClassroomEnrollmentRepository. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra reorder waitlist.
2. **Tiền điều kiện:** Two WAITLIST rows on the same offering; TM actor.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService → ClassroomEnrollmentRepository.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và body chứa đúng dữ liệu cần lấy (kèm thông tin phân trang nếu có).
   Nguyên văn trên Excel:
   - Priorities are swapped to the requested order
   - Incomplete ID list is rejected
   - If HTTP mapping is missing, document service-level IT coverage of the gap
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_CLASS_06

**Nói ngắn (15 giây):**

> IT_CLASS_06: Kiểm tra lọc registrations theo status. Em gọi `API tương ứng`, luồng PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra lọc registrations theo status.
2. **Tiền điều kiện:** ASSIGNED enrollment and eligible target offering.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Target enrollment reflects the learner
   - Source enrollment is closed/transferred per rules
   - No duplicate active assignments remain
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CLASS_07

**Nói ngắn (15 giây):**

> IT_CLASS_07: Kiểm tra xem lớp trước/không phụ thuộc gán GV. Em gọi `API tương ứng`, luồng PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra xem lớp trước/không phụ thuộc gán GV.
2. **Tiền điều kiện:** TM JWT; teacher user exists.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Teacher is linked as expected
   - Replace removes/supersedes the old assignment cleanly
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_CLASS_08

**Nói ngắn (15 giây):**

> IT_CLASS_08: Kiểm tra TM list có phân trang/filter. Em gọi `API tương ứng`, luồng PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService. Mong đợi 403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra TM list có phân trang/filter.
2. **Tiền điều kiện:** LEARNER JWT; target enrollment id.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PublicClassroomController / TrainingManagerClassroomController → ClassroomOfferingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 403
   - Enrollment row is unchanged
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 13. Learner Classroom — sheet `IT - LearnerCls`

**Câu dẫn cho cả module:**

> Module Learner Classroom nằm ở sheet `IT - LearnerCls`, chạy bằng class `StudentClassroomIT`. Vai trò sử dụng: LEARNER. Thành phần tích hợp chính: StudentClassroomController, ClassroomOfferingService, ClassroomHomeworkService, TuitionProofService, related services.

**Nhóm: Access**

### IT_LEARNERCLS_01

**Nói ngắn (15 giây):**

> IT_LEARNERCLS_01: Kiểm tra HV my-classrooms. Em gọi `GET /api/student/classrooms/my-classrooms`, luồng StudentClassroomController → ClassroomOfferingService / ClassroomHomeworkService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV my-classrooms.
2. **Tiền điều kiện:** ASSIGNED LEARNER JWT.
3. **Bước thực hiện:** gọi `GET /api/student/classrooms/my-classrooms` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentClassroomController → ClassroomOfferingService / ClassroomHomeworkService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Only accessible/ASSIGNED classes are returned
   - Other learners' classes are not included
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_LEARNERCLS_02

**Nói ngắn (15 giây):**

> IT_LEARNERCLS_02: Kiểm tra HV xem sessions. Em gọi `API tương ứng`, luồng StudentClassroomController → ClassroomOfferingService / ClassroomHomeworkService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV xem sessions.
2. **Tiền điều kiện:** Owned session exists.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentClassroomController → ClassroomOfferingService / ClassroomHomeworkService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Owned class returns 200 and join succeeds under rules
   - Foreign class is denied
   - Session data matches classroom_sessions
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_LEARNERCLS_03

**Nói ngắn (15 giây):**

> IT_LEARNERCLS_03: Kiểm tra HV xem homework. Em gọi `POST /api/student/classrooms/homework/attachments`, luồng StudentClassroomController → HomeworkAttachmentStorageService / ClassroomHomeworkService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV xem homework.
2. **Tiền điều kiện:** Open homework; ASSIGNED learner.
3. **Bước thực hiện:** gọi `POST /api/student/classrooms/homework/attachments` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentClassroomController → HomeworkAttachmentStorageService / ClassroomHomeworkService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Attachment metadata/URL is stored
   - Submission row is created for the learner/homework
   - Deadline/eligibility rules are enforced when coded
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_LEARNERCLS_04

**Nói ngắn (15 giây):**

> IT_LEARNERCLS_04: Kiểm tra HV xem materials. Em gọi `API tương ứng`, luồng StudentClassroomController → ClassroomContentService / ClassroomOfferingService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV xem materials.
2. **Tiền điều kiện:** Seeded classroom content.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentClassroomController → ClassroomContentService / ClassroomOfferingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 200, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Member receives 200 with content
   - Non-member receives 403/404
   - Returned items match DB for that offering
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_LEARNERCLS_05

**Nói ngắn (15 giây):**

> IT_LEARNERCLS_05: Kiểm tra HV thao tác homework liên quan. Em gọi `API tương ứng`, luồng StudentClassroomController → TuitionProofService / ClassroomOfferingService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV thao tác homework liên quan.
2. **Tiền điều kiện:** Eligible enrollment; storage writable.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentClassroomController → TuitionProofService / ClassroomOfferingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và body chứa đúng dữ liệu cần lấy (kèm thông tin phân trang nếu có).
   Nguyên văn trên Excel:
   - Proof row is PENDING
   - File URL is stored
   - Proof appears in TM pending list
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_LEARNERCLS_06

**Nói ngắn (15 giây):**

> IT_LEARNERCLS_06: Kiểm tra HV xem gradebook/me. Em gọi `API tương ứng`, luồng StudentClassroomController → ClassroomGradebookService / ClassroomOfferingService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV xem gradebook/me.
2. **Tiền điều kiện:** Gradebook rows and teacher publish capability.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentClassroomController → ClassroomGradebookService / ClassroomOfferingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Unpublished state hides or limits learner scores per rules
   - Published state returns scores
   - DB publish flags match API visibility
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 14. Teacher Operations — sheet `IT - Teacher`

**Câu dẫn cho cả module:**

> Module Teacher Operations nằm ở sheet `IT - Teacher`, chạy bằng class `TeacherClassroomIT`. Vai trò sử dụng: TEACHER. Thành phần tích hợp chính: TeacherClassroomController, ClassroomHomeworkService, ClassroomAttendanceService, ClassroomGradebookService, ClassroomChangeRequestService.

**Nhóm: Teaching ops**

### IT_TEACH_01

**Nói ngắn (15 giây):**

> IT_TEACH_01: Kiểm tra GV list lớp assigned. Em gọi `POST /api/teacher/classrooms/{id}/homework`, luồng TeacherClassroomController → ClassroomHomeworkService / ClassroomAttendanceService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra GV list lớp assigned.
2. **Tiền điều kiện:** TEACHER assignment exists.
3. **Bước thực hiện:** gọi `POST /api/teacher/classrooms/{id}/homework` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** TeacherClassroomController → ClassroomHomeworkService / ClassroomAttendanceService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Homework row is inserted with offering FK
   - Assigned learner can see it when allowed
   - Non-assigned teacher is denied
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_TEACH_02

**Nói ngắn (15 giây):**

> IT_TEACH_02: Kiểm tra GV xem homework lớp. Em gọi `API tương ứng`, luồng TeacherClassroomController → ClassroomHomeworkService / ClassroomAttendanceService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra GV xem homework lớp.
2. **Tiền điều kiện:** Submitted homework exists.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** TeacherClassroomController → ClassroomHomeworkService / ClassroomAttendanceService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Score/feedback are saved
   - Teacher not owning the class is rejected
   - Learner academic views reflect the grade when publish rules allow
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_TEACH_03

**Nói ngắn (15 giây):**

> IT_TEACH_03: Kiểm tra GV xem attendance theo session. Em gọi `POST /api/teacher/classrooms/attendance`, luồng TeacherClassroomController → ClassroomAttendanceService / ClassroomHomeworkService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra GV xem attendance theo session.
2. **Tiền điều kiện:** Session and enrolled students exist.
3. **Bước thực hiện:** gọi `POST /api/teacher/classrooms/attendance` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** TeacherClassroomController → ClassroomAttendanceService / ClassroomHomeworkService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Attendance records are upserted
   - Learner endpoint returns only own attendance
   - Non-teacher is forbidden
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_TEACH_04

**Nói ngắn (15 giây):**

> IT_TEACH_04: Kiểm tra GV xem gradebook lớp. Em gọi `API tương ứng`, luồng TeacherClassroomController → ClassroomGradebookService / ClassroomHomeworkService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra GV xem gradebook lớp.
2. **Tiền điều kiện:** TEACHER JWT; gradebook rows exist.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** TeacherClassroomController → ClassroomGradebookService / ClassroomHomeworkService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Publish makes scores visible to learner
   - Unpublish hides/restricts per service rules
   - Flags in DB match API outcomes
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_TEACH_05

**Nói ngắn (15 giây):**

> IT_TEACH_05: Kiểm tra GV xem change requests của mình. Em gọi `POST /api/teacher/classrooms/requests`, luồng TeacherClassroomController → ClassroomChangeRequestService / AppNotificationService. Mong đợi 400. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra GV xem change requests của mình.
2. **Tiền điều kiện:** TEACHER assigned to offering.
3. **Bước thực hiện:** gọi `POST /api/teacher/classrooms/requests` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** TeacherClassroomController → ClassroomChangeRequestService / AppNotificationService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 400, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - PENDING change request is stored
   - Notification creation respects notification preferences
   - Invalid request payloads return 400
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_TEACH_06

**Nói ngắn (15 giây):**

> IT_TEACH_06: Kiểm tra GV truy cập lớp không được assign (negative nếu có). Em gọi `POST /api/teacher/classrooms/{id}/homework`, luồng TeacherClassroomController → ClassroomHomeworkService / ClassroomAttendanceService. Mong đợi 403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra GV truy cập lớp không được assign (negative nếu có).
2. **Tiền điều kiện:** LEARNER JWT.
3. **Bước thực hiện:** gọi `POST /api/teacher/classrooms/{id}/homework` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** TeacherClassroomController → ClassroomHomeworkService / ClassroomAttendanceService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 403
   - No classroom_homework insert occurs
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 15. Classroom Quiz — sheet `IT - Quiz`

**Câu dẫn cho cả module:**

> Module Classroom Quiz nằm ở sheet `IT - Quiz`, chạy bằng class `ClassroomQuizIT`. Vai trò sử dụng: TEACHER và LEARNER. Thành phần tích hợp chính: ClassroomQuizController, ClassroomQuizService, classroom_quizzes/questions/attempts.

**Nhóm: Quiz lifecycle**

### IT_QUIZ_01

**Nói ngắn (15 giây):**

> IT_QUIZ_01: Kiểm tra GV list quiz theo lớp. Em gọi `POST /api/teacher/classrooms/{offeringId}/quizzes`, luồng ClassroomQuizController → ClassroomQuizService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra GV list quiz theo lớp.
2. **Tiền điều kiện:** TEACHER assigned to class.
3. **Bước thực hiện:** gọi `POST /api/teacher/classrooms/{offeringId}/quizzes` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ClassroomQuizController → ClassroomQuizService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Quiz header and questions are saved
   - Offering FK is correct
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_QUIZ_02

**Nói ngắn (15 giây):**

> IT_QUIZ_02: Kiểm tra GV tạo/xem chi tiết quiz. Em gọi `API tương ứng`, luồng ClassroomQuizController → ClassroomQuizService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra GV tạo/xem chi tiết quiz.
2. **Tiền điều kiện:** Quiz exists; ASSIGNED learner.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ClassroomQuizController → ClassroomQuizService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - OPEN allows attempts
   - CLOSED rejects new attempts
   - Status in DB matches API
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_QUIZ_03

**Nói ngắn (15 giây):**

> IT_QUIZ_03: Kiểm tra HV list quiz. Em gọi `POST /api/student/quizzes/{quizId}/submit`, luồng ClassroomQuizController → ClassroomQuizService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV list quiz.
2. **Tiền điều kiện:** OPEN quiz; ASSIGNED learner.
3. **Bước thực hiện:** gọi `POST /api/student/quizzes/{quizId}/submit` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ClassroomQuizController → ClassroomQuizService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Attempt row is created with score per rules
   - Non-member is denied
   - No duplicate illegal attempts beyond service rules
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_QUIZ_04

**Nói ngắn (15 giây):**

> IT_QUIZ_04: Kiểm tra xóa quiz (destructive / N/A trên demo). Em gọi `DELETE /api/teacher/quizzes/{quizId}`, luồng ClassroomQuizController → ClassroomQuizService. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra xóa quiz (destructive / N/A trên demo).
2. **Tiền điều kiện:** TEACHER owner of quiz.
3. **Bước thực hiện:** gọi `DELETE /api/teacher/quizzes/{quizId}` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ClassroomQuizController → ClassroomQuizService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và body chứa đúng dữ liệu cần lấy (kèm thông tin phân trang nếu có).
   Nguyên văn trên Excel:
   - Quiz is no longer active in teacher list
   - Child questions are cascaded or archived without orphans if cascade is configured
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

---

## Module 16. Assessment & Placement — sheet `IT - Assess`

**Câu dẫn cho cả module:**

> Module Assessment & Placement nằm ở sheet `IT - Assess`, chạy bằng class `AssessmentIT`. Vai trò sử dụng: LEARNER. Thành phần tích hợp chính: PlacementTestController, StudentAssessmentController, StudentMockTestController, PlacementTestService, AiAssessmentService, MockTestService.

**Nhóm: Placement**

### IT_ASSESS_01

**Nói ngắn (15 giây):**

> IT_ASSESS_01: Kiểm tra lấy placement test hiện tại. Em gọi `GET /api/student/placement-tests/current`, luồng PlacementTestController / StudentAssessmentController → PlacementTestService / AiAssessmentService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra lấy placement test hiện tại.
2. **Tiền điều kiện:** LEARNER JWT.
3. **Bước thực hiện:** gọi `GET /api/student/placement-tests/current` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PlacementTestController / StudentAssessmentController → PlacementTestService / AiAssessmentService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - GET returns definition (lazy-seed allowed)
   - Submit creates an attempt with result fields
   - Unauthenticated calls are rejected
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_ASSESS_02

**Nói ngắn (15 giây):**

> IT_ASSESS_02: Kiểm tra submit placement thiếu đáp án. Em gọi `API tương ứng`, luồng PlacementTestController / StudentAssessmentController → PlacementTestService / AiAssessmentService. Mong đợi 400. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra submit placement thiếu đáp án.
2. **Tiền điều kiện:** LEARNER JWT.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PlacementTestController / StudentAssessmentController → PlacementTestService / AiAssessmentService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 400, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 400
   - Attempt count is unchanged
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_ASSESS_03

**Nói ngắn (15 giây):**

> IT_ASSESS_03: Kiểm tra list assessments theo khóa. Em gọi `PUT /api/content-manager/placement-test`, luồng PlacementTestController / StudentAssessmentController → PlacementTestService / AiAssessmentService. Mong đợi 403. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra list assessments theo khóa.
2. **Tiền điều kiện:** CONTENT_MANAGER JWT.
3. **Bước thực hiện:** gọi `PUT /api/content-manager/placement-test` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PlacementTestController / StudentAssessmentController → PlacementTestService / AiAssessmentService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - CM update persists definition fields
   - LEARNER receives 403 on CM path
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

**Nhóm: Course & mock**

### IT_ASSESS_04

**Nói ngắn (15 giây):**

> IT_ASSESS_04: Kiểm tra list mock tests. Em gọi `POST /api/student/assessments/{assessmentId}/submit`, luồng StudentAssessmentController / PlacementTestController → AiAssessmentService / PlacementTestService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra list mock tests.
2. **Tiền điều kiện:** Enrolled LEARNER; assessment configured; AI mocked.
3. **Bước thực hiện:** gọi `POST /api/student/assessments/{assessmentId}/submit` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentAssessmentController / PlacementTestController → AiAssessmentService / PlacementTestService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Submission row is created with status/score per mode
   - Non-enrolled learner is denied
   - AI mock failures are asserted according to actual transactional behavior
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_ASSESS_05

**Nói ngắn (15 giây):**

> IT_ASSESS_05: Kiểm tra assessments khi chưa enroll (negative/N/A). Em gọi `GET /api/student/mock-tests`, luồng StudentMockTestController / PlacementTestController → MockTestService / PlacementTestService. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra assessments khi chưa enroll (negative/N/A).
2. **Tiền điều kiện:** LEARNER JWT; published mock test exists.
3. **Bước thực hiện:** gọi `GET /api/student/mock-tests` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentMockTestController / PlacementTestController → MockTestService / PlacementTestService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Published mock tests are listed
   - Submit inserts an attempt row
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_ASSESS_06

**Nói ngắn (15 giây):**

> IT_ASSESS_06: Kiểm tra chi tiết / start mock test. Em gọi `POST /api/student/placement-tests/current/submit`, luồng PlacementTestController / StudentAssessmentController → PlacementTestService / AiAssessmentService. Mong đợi 403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra chi tiết / start mock test.
2. **Tiền điều kiện:** TEACHER JWT.
3. **Bước thực hiện:** gọi `POST /api/student/placement-tests/current/submit` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** PlacementTestController / StudentAssessmentController → PlacementTestService / AiAssessmentService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 403
   - No placement attempt is created for the teacher user
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 17. Support Tickets — sheet `IT - Support`

**Câu dẫn cho cả module:**

> Module Support Tickets nằm ở sheet `IT - Support`, chạy bằng class `SupportTicketIT`. Vai trò sử dụng: LEARNER và MANAGER. Thành phần tích hợp chính: StudentSupportTicketController, ManagerSupportTicketController, SupportTicketService.

**Nhóm: Tickets**

### IT_SUPPORT_01

**Nói ngắn (15 giây):**

> IT_SUPPORT_01: Kiểm tra HV tạo support ticket. Em gọi `POST /api/student/support-tickets`, luồng StudentSupportTicketController / ManagerSupportTicketController → SupportTicketService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV tạo support ticket.
2. **Tiền điều kiện:** LEARNER JWT.
3. **Bước thực hiện:** gọi `POST /api/student/support-tickets` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentSupportTicketController / ManagerSupportTicketController → SupportTicketService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về 200 thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Response is 200 OK
   - Ticket is owned by the learner
   - Initial message row exists
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_SUPPORT_02

**Nói ngắn (15 giây):**

> IT_SUPPORT_02: Kiểm tra HV list ticket của mình. Em gọi `GET /api/student/support-tickets/{ticketId}`, luồng StudentSupportTicketController / ManagerSupportTicketController → SupportTicketService. Mong đợi 403/404. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV list ticket của mình.
2. **Tiền điều kiện:** Two LEARNER accounts.
3. **Bước thực hiện:** gọi `GET /api/student/support-tickets/{ticketId}` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentSupportTicketController / ManagerSupportTicketController → SupportTicketService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403/404, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 403/404
   - No message content is leaked
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_SUPPORT_03

**Nói ngắn (15 giây):**

> IT_SUPPORT_03: Kiểm tra Manager list tickets. Em gọi `API tương ứng`, luồng ManagerSupportTicketController / StudentSupportTicketController → SupportTicketService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra Manager list tickets.
2. **Tiền điều kiện:** MANAGER or STAFF JWT.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ManagerSupportTicketController / StudentSupportTicketController → SupportTicketService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Assignee is set
   - Staff message is appended
   - Learner can see the staff reply
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_SUPPORT_04

**Nói ngắn (15 giây):**

> IT_SUPPORT_04: Kiểm tra tạo ticket body rỗng → validation. Em gọi `POST /api/student/support-tickets`, luồng StudentSupportTicketController / ManagerSupportTicketController → SupportTicketService. Mong đợi 400. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra tạo ticket body rỗng → validation.
2. **Tiền điều kiện:** LEARNER JWT.
3. **Bước thực hiện:** gọi `POST /api/student/support-tickets` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentSupportTicketController / ManagerSupportTicketController → SupportTicketService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 400, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 400
   - Ticket count is unchanged
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 18. Administration — sheet `IT - Admin`

**Câu dẫn cho cả module:**

> Module Administration nằm ở sheet `IT - Admin`, chạy bằng class `AdminIT`. Vai trò sử dụng: ADMIN. Thành phần tích hợp chính: AdminUserController, AdminSystemController, AdminAuditLogController, AdminUserService, AuditLogService.

**Nhóm: Admin users**

### IT_ADMIN_01

**Nói ngắn (15 giây):**

> IT_ADMIN_01: Kiểm tra Admin list users. Em gọi `POST /api/admin/users`, luồng AdminUserController / AdminSystemController → AdminUserService / AuditLogService. Mong đợi 403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra Admin list users.
2. **Tiền điều kiện:** ADMIN JWT.
3. **Bước thực hiện:** gọi `POST /api/admin/users` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AdminUserController / AdminSystemController → AdminUserService / AuditLogService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - User and roles are created
   - Audit log entry exists
   - Non-admin receives 403
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_ADMIN_02

**Nói ngắn (15 giây):**

> IT_ADMIN_02: Kiểm tra Admin xem/lọc user. Em gọi `PATCH /api/admin/users/{id}/roles`, luồng AdminUserController / AdminSystemController → AdminUserService / AuditLogService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra Admin xem/lọc user.
2. **Tiền điều kiện:** Target user exists; ADMIN JWT.
3. **Bước thực hiện:** gọi `PATCH /api/admin/users/{id}/roles`, `PATCH /api/admin/users/{id}/status` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AdminUserController / AdminSystemController → AdminUserService / AuditLogService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Role set matches the request with no orphans
   - Status change is persisted
   - Audit entries are recorded
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_ADMIN_03

**Nói ngắn (15 giây):**

> IT_ADMIN_03: Kiểm tra Admin audit logs. Em gọi `GET /api/admin/audit-logs`, luồng AdminAuditLogController / AdminUserController → AuditLogService / AdminUserService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra Admin audit logs.
2. **Tiền điều kiện:** Audit rows exist.
3. **Bước thực hiện:** gọi `GET /api/admin/audit-logs` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AdminAuditLogController / AdminUserController → AuditLogService / AdminUserService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 200, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - ADMIN receives 200 with log rows
   - LEARNER receives 403
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_ADMIN_04

**Nói ngắn (15 giây):**

> IT_ADMIN_04: Kiểm tra Admin system config. Em gọi `GET /api/admin/system/config`, luồng AdminSystemController / AdminUserController → AdminUserService / AuditLogService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra Admin system config.
2. **Tiền điều kiện:** ADMIN JWT.
3. **Bước thực hiện:** gọi `GET /api/admin/system/config` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** AdminSystemController / AdminUserController → AdminUserService / AuditLogService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 200, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - ADMIN receives 200
   - Other roles receive 403
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 19. Lark Meetings — sheet `IT - Lark`

**Câu dẫn cho cả module:**

> Module Lark Meetings nằm ở sheet `IT - Lark`, chạy bằng class `LarkIT`. Vai trò sử dụng: webhook công khai và TRAINING_MANAGER. Thành phần tích hợp chính: LarkWebhookController, LarkWebhookService, LarkMeetingService, classroom_sessions.

**Nhóm: Webhook & sync**

### IT_LARK_01

**Nói ngắn (15 giây):**

> IT_LARK_01: Kiểm tra webhook Lark url_verification. Em gọi `POST /api/lark/events`, luồng LarkWebhookController → LarkWebhookService / LarkMeetingService. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra webhook Lark url_verification.
2. **Tiền điều kiện:** None.
3. **Bước thực hiện:** gọi `POST /api/lark/events` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** LarkWebhookController → LarkWebhookService / LarkMeetingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Challenge value is echoed in the response
   - No session mutation occurs
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_LARK_02

**Nói ngắn (15 giây):**

> IT_LARK_02: Kiểm tra webhook thiếu cấu hình/chữ ký. Em gọi `POST /api/lark/events`, luồng LarkWebhookController → LarkWebhookService / LarkMeetingService. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra webhook thiếu cấu hình/chữ ký.
2. **Tiền điều kiện:** Mapped session exists; Lark test mode enabled.
3. **Bước thực hiện:** gọi `POST /api/lark/events` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** LarkWebhookController → LarkWebhookService / LarkMeetingService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Session fields are updated according to the event type
   - Invalid events are rejected safely without corrupt state
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

### IT_LARK_03

**Nói ngắn (15 giây):**

> IT_LARK_03: Kiểm tra sync recording session Lark. Em gọi `API tương ứng`, luồng LarkWebhookController → LarkMeetingService / LarkWebhookService. Mong đợi 403. Kết quả N/A vì môi trường demo thiếu tiền điều kiện.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra sync recording session Lark.
2. **Tiền điều kiện:** STAFF JWT; Lark client mocked.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** LarkWebhookController → LarkMeetingService / LarkWebhookService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Sync status/error fields are updated from the mocked response
   - LEARNER receives 403
6. **Kết quả thực tế:** Kết quả trên Excel: **N/A** — môi trường demo thiếu tiền điều kiện nên chưa kết luận đủ, em ghi trung thực chứ không tính là Passed.

---

## Module 20. Infrastructure — sheet `IT - Infra`

**Câu dẫn cho cả module:**

> Module Infrastructure nằm ở sheet `IT - Infra`, chạy bằng class `InfrastructureIT`. Vai trò sử dụng: TRAINING_MANAGER. Thành phần tích hợp chính: TrainingManagerInfrastructureController, ClassroomInfrastructureService.

**Nhóm: Infra CRUD**

### IT_INFRA_01

**Nói ngắn (15 giây):**

> IT_INFRA_01: Kiểm tra TM list campuses. Em gọi `API tương ứng`, luồng TrainingManagerInfrastructureController → ClassroomInfrastructureService. Mong đợi 403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra TM list campuses.
2. **Tiền điều kiện:** TM/STAFF JWT.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** TrainingManagerInfrastructureController → ClassroomInfrastructureService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Campus/room rows are inserted
   - LEARNER receives 403
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_INFRA_02

**Nói ngắn (15 giây):**

> IT_INFRA_02: Kiểm tra TM list rooms. Em gọi `API tương ứng`, luồng TrainingManagerInfrastructureController → ClassroomInfrastructureService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra TM list rooms.
2. **Tiền điều kiện:** Offering and template prerequisites exist.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** TrainingManagerInfrastructureController → ClassroomInfrastructureService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Template is saved
   - Sessions are generated according to template rules
   - Duplicate generation is handled per service policy
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_INFRA_03

**Nói ngắn (15 giây):**

> IT_INFRA_03: Kiểm tra TM list session templates. Em gọi `API tương ứng`, luồng TrainingManagerInfrastructureController → ClassroomInfrastructureService. Mong đợi 400. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra TM list session templates.
2. **Tiền điều kiện:** Existing room.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** TrainingManagerInfrastructureController → ClassroomInfrastructureService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 400, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 400
   - Room row is unchanged
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 21. Reports & Revenue — sheet `IT - Report`

**Câu dẫn cho cả module:**

> Module Reports & Revenue nằm ở sheet `IT - Report`, chạy bằng class `ReportIT`. Vai trò sử dụng: TRAINING_MANAGER và CONTENT_MANAGER. Thành phần tích hợp chính: TrainingManagerDashboardController, ContentManagerRevenueController, TrainingManagerOpsService, PaymentService.

**Nhóm: Dashboards**

### IT_REPORT_01

**Nói ngắn (15 giây):**

> IT_REPORT_01: Kiểm tra TM dashboard. Em gọi `GET /api/staff/dashboard`, luồng TrainingManagerDashboardController / ContentManagerRevenueController → TrainingManagerOpsService / PaymentService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra TM dashboard.
2. **Tiền điều kiện:** Seeded ops data; STAFF/TM JWT.
3. **Bước thực hiện:** gọi `GET /api/staff/dashboard` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** TrainingManagerDashboardController / ContentManagerRevenueController → TrainingManagerOpsService / PaymentService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 200, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 200 with coherent metrics
   - LEARNER receives 403
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_REPORT_02

**Nói ngắn (15 giây):**

> IT_REPORT_02: Kiểm tra CM revenue analytics. Em gọi `GET /api/content-manager/revenue/analytics`, luồng ContentManagerRevenueController / TrainingManagerDashboardController → PaymentService / TrainingManagerOpsService. Mong đợi 200. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra CM revenue analytics.
2. **Tiền điều kiện:** Paid orders seeded; CM JWT.
3. **Bước thực hiện:** gọi `GET /api/content-manager/revenue/analytics` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ContentManagerRevenueController / TrainingManagerDashboardController → PaymentService / TrainingManagerOpsService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 200, không tạo ra dữ liệu sai — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - CM receives 200 analytics payload
   - LEARNER receives 403
   - Totals are consistent with seeded paid orders
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 22. Classroom Proposals — sheet `IT - Proposal`

**Câu dẫn cho cả module:**

> Module Classroom Proposals nằm ở sheet `IT - Proposal`, chạy bằng class `ClassroomProposalIT`. Vai trò sử dụng: STAFF. Thành phần tích hợp chính: StaffClassroomProposalController, ManagerClassroomProposalController, ClassroomProposalService.

**Nhóm: Proposal flow**

### IT_PROPOSAL_01

**Nói ngắn (15 giây):**

> IT_PROPOSAL_01: Kiểm tra Staff list classroom proposals. Em gọi `POST /api/staff/classroom-proposals`, luồng StaffClassroomProposalController / ManagerClassroomProposalController → ClassroomProposalService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra Staff list classroom proposals.
2. **Tiền điều kiện:** STAFF JWT.
3. **Bước thực hiện:** gọi `POST /api/staff/classroom-proposals` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StaffClassroomProposalController / ManagerClassroomProposalController → ClassroomProposalService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Proposal is persisted
   - Status becomes SUBMITTED/PENDING per service rules
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_PROPOSAL_02

**Nói ngắn (15 giây):**

> IT_PROPOSAL_02: Kiểm tra tạo/xem chi tiết proposal. Em gọi `API tương ứng`, luồng StaffClassroomProposalController / ManagerClassroomProposalController → ClassroomProposalService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra tạo/xem chi tiết proposal.
2. **Tiền điều kiện:** Pending proposal exists.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StaffClassroomProposalController / ManagerClassroomProposalController → ClassroomProposalService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Status is updated for manager
   - Approve side effects occur only when implemented
   - STAFF cannot approve
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_PROPOSAL_03

**Nói ngắn (15 giây):**

> IT_PROPOSAL_03: Kiểm tra cập nhật trạng thái proposal. Em gọi `API tương ứng`, luồng StaffClassroomProposalController / ManagerClassroomProposalController → ClassroomProposalService. Mong đợi 403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra cập nhật trạng thái proposal.
2. **Tiền điều kiện:** LEARNER JWT.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StaffClassroomProposalController / ManagerClassroomProposalController → ClassroomProposalService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 403
   - No proposal row is created
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 23. Attendance Disputes — sheet `IT - Dispute`

**Câu dẫn cho cả module:**

> Module Attendance Disputes nằm ở sheet `IT - Dispute`, chạy bằng class `AttendanceDisputeIT`. Vai trò sử dụng: LEARNER và TEACHER. Thành phần tích hợp chính: ClassroomAttendanceDisputeController, ClassroomAttendanceDisputeService.

**Nhóm: Disputes**

### IT_DISPUTE_01

**Nói ngắn (15 giây):**

> IT_DISPUTE_01: Kiểm tra HV list attendance disputes. Em gọi `POST /api/student/attendance/{attendanceId}/disputes`, luồng ClassroomAttendanceDisputeController → ClassroomAttendanceDisputeService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV list attendance disputes.
2. **Tiền điều kiện:** Attendance row exists.
3. **Bước thực hiện:** gọi `POST /api/student/attendance/{attendanceId}/disputes` bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ClassroomAttendanceDisputeController → ClassroomAttendanceDisputeService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và dữ liệu tương ứng được ghi/cập nhật đúng trong database.
   Nguyên văn trên Excel:
   - Dispute row is created with PENDING status
   - Owner is the authenticated learner
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_DISPUTE_02

**Nói ngắn (15 giây):**

> IT_DISPUTE_02: Kiểm tra GV list disputes pending. Em gọi `API tương ứng`, luồng ClassroomAttendanceDisputeController → ClassroomAttendanceDisputeService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra GV list disputes pending.
2. **Tiền điều kiện:** Pending dispute; TEACHER JWT.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ClassroomAttendanceDisputeController → ClassroomAttendanceDisputeService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - Dispute is resolved
   - Attendance reflects the review decision
   - Invalid review payloads are rejected
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_DISPUTE_03

**Nói ngắn (15 giây):**

> IT_DISPUTE_03: Kiểm tra GV xử lý dispute. Em gọi `API tương ứng`, luồng ClassroomAttendanceDisputeController → ClassroomAttendanceDisputeService. Mong đợi 403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra GV xử lý dispute.
2. **Tiền điều kiện:** LEARNER JWT; pending dispute id.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** ClassroomAttendanceDisputeController → ClassroomAttendanceDisputeService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 403, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Response is 403
   - Dispute remains PENDING
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Module 24. Learning Notes — sheet `IT - Notes`

**Câu dẫn cho cả module:**

> Module Learning Notes nằm ở sheet `IT - Notes`, chạy bằng class `LearningNotesIT`. Vai trò sử dụng: LEARNER. Thành phần tích hợp chính: StudentLearningExperienceController, LearnerLearningExperienceService, learner_lesson_notes.

**Nhóm: Notes**

### IT_NOTES_01

**Nói ngắn (15 giây):**

> IT_NOTES_01: Kiểm tra HV list learning notes. Em gọi `API tương ứng`, luồng StudentLearningExperienceController → LearnerLearningExperienceService. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra HV list learning notes.
2. **Tiền điều kiện:** Enrolled LEARNER JWT.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentLearningExperienceController → LearnerLearningExperienceService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API trả về thành công và nội dung phản hồi khớp với nghiệp vụ mong đợi.
   Nguyên văn trên Excel:
   - CRUD reflects database state
   - Other learner cannot access the note
   - Deleted note is removed or hidden per service rules
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

### IT_NOTES_02

**Nói ngắn (15 giây):**

> IT_NOTES_02: Kiểm tra tạo/cập nhật note (nếu có). Em gọi `API tương ứng`, luồng StudentLearningExperienceController → LearnerLearningExperienceService. Mong đợi 401/403. Kết quả Passed.

**Nói đầy đủ (45–60 giây):**

1. **Mục tiêu:** Kiểm tra tạo/cập nhật note (nếu có).
2. **Tiền điều kiện:** Published course without enrollment for the caller.
3. **Bước thực hiện:** gọi API của chức năng này bằng MockMvc trong `@SpringBootTest`.
4. **Luồng tích hợp:** StudentLearningExperienceController → LearnerLearningExperienceService.
5. **Kết quả mong đợi (nói bằng tiếng Việt):** API phải chặn và trả lỗi 401/403, dữ liệu trong database không bị thay đổi — đây là case negative nên bị chặn đúng chính là Passed.
   Nguyên văn trên Excel:
   - Unauthenticated call returns 401/403
   - Non-enrolled call is denied
   - No note row is inserted
6. **Kết quả thực tế:** Kết quả trên Excel: **Passed** — actual khớp expected.

---

## Phần E — Kết thúc (thuộc nguyên văn, 20 giây)

> Tóm lại, bộ Integration Test của em gồm 111 test case trên 24 module, thiết kế trên Excel và thực thi bằng Spring Boot Test với MockMvc. Các case Passed là actual khớp expected, các case N/A là môi trường demo thiếu tiền điều kiện và em ghi nhận trung thực. Em sẵn sàng chạy trực tiếp case nào cô muốn xem ạ.

## Phần F — Mẹo học thuộc

1. Thuộc **Phần A** và **Phần E** nguyên văn (chỉ ~50 giây).
2. Thuộc **6 câu ở Phần B** — đây là phần cô hỏi nhiều nhất.
3. Với 111 case, chỉ cần thuộc dạng **Nói ngắn**; theo công thức: *mã case → mục tiêu → API → luồng Controller-Service-Repository → kết quả*.
4. Chọn 3 case demo sâu: một happy-path (`IT_AUTH_05`), một negative (`IT_AUTH_06`), một N/A (ví dụ case cần enroll khóa học).
5. Khi bí: quay lại công thức *gọi API gì → đi qua tầng nào → mong đợi status nào*.

_Tổng số test case trong tài liệu: 111._
