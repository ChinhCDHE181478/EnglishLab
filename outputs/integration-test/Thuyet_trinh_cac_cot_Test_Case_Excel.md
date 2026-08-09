# Thuyết trình: Các cột Test Case trên Excel Integration Test

Dự án: **EnglishLab (SEP490_G23)**  
File Excel: `SEP490_G23_Report5.2_Integration Test_COMPLETED_HONEST_FORMATTED.xlsx`  
Áp dụng cho các sheet: `IT - Auth`, `IT - User`, … `IT - Notes`

---

## 1. Bảng test case là gì? (mở đầu 20–30 giây)

Trên mỗi sheet **IT - …**, mỗi dòng là **một Integration Test case**.  
Các cột trong ảnh chính là khung “viết test case bình thường” mà cô yêu cầu:

| Cột trên Excel | Vai trò ngắn |
|----------------|--------------|
| **Test Case ID** | Mã định danh case |
| **Test Case Description** | Đang kiểm tra / tích hợp gì |
| **Test Case Procedure** | Các bước thực hiện (luồng hàm) |
| **Expected Results** | Kết quả đúng cần thấy |
| **Pre-conditions** | Điều kiện phải có trước khi chạy |

**Câu nói với cô:**

> Em thiết kế Integration Test trên Excel theo đủ các cột ID – Description – Procedure – Expected – Pre-condition. Procedure mô tả tích hợp Controller–Service–Repository; em thực thi bằng Spring Boot Test/MockMvc rồi ghi Round.

---

## 2. Test Case ID

### 2.1. Là gì?

Mã **duy nhất** của từng test case, để:

- Tra cứu trên Excel  
- Map sang code (`@DisplayName("IT_AUTH_01 …")`)  
- Ghi kết quả Round / nói chuyện với cô không nhầm case  

### 2.2. Quy ước đặt tên trong project

Ví dụ: `IT_AUTH_01`

| Phần | Ý nghĩa |
|------|---------|
| `IT_` | Integration Test |
| `AUTH` | Module / sheet (Auth, User, Classroom…) |
| `01` | Số thứ tự trong module |

Ví dụ khác: `IT_USER_02`, `IT_CLASS_05`, `IT_PAYMENT_01`…

### 2.3. Liên hệ code

Trong Java:

```java
@DisplayName("IT_AUTH_01 register")
void itAuth01_register() { ... }
```

→ Cùng một ID với cột **Test Case ID** trên Excel.

### 2.4. Câu thuyết trình

> Cột Test Case ID là mã định danh. Em dùng cùng mã trên `@DisplayName` trong class MockMvc để Excel và code khớp 1–1.

---

## 3. Test Case Description

### 3.1. Là gì?

Mô tả **mục tiêu kiểm thử**: case này muốn chứng minh điều gì?

Thường trả lời câu hỏi:

- Tích hợp những thành phần nào? (Controller / Service / Repository…)  
- Hành vi happy-path hay negative?  

### 3.2. Ví dụ (IT_AUTH_01)

> *Verify registering an account through AuthController persists user and verification token via AuthService.*

**Dịch / diễn giải:**

Kiểm tra đăng ký tài khoản: request vào `AuthController`, xử lý bởi `AuthService`, lưu user + OTP qua Repository.

### 3.3. Cách đọc khi thuyết trình

1. Đọc Description trước → nắm “đang test gì”.  
2. Không cần đọc hết Procedure ngay.  
3. Nhấn mạnh **tên class/hàm** xuất hiện trong Description (đúng tinh thần Integration = hàm với nhau).

### 3.4. Câu thuyết trình

> Description nói rõ mục tiêu tích hợp. Ví dụ Auth: Controller nhận đăng ký, Service tạo user và token — không chỉ “bấm API cho vui”.

---

## 4. Test Case Procedure

### 4.1. Là gì?

**Các bước thực hiện** của test case — cột quan trọng nhất với Integration Test.

Trong Excel EnglishLab, Procedure viết theo kiểu:

1. Gọi API bằng MockMvc (`POST /api/...`)  
2. Controller gọi Service  
3. Service gọi Repository / DB  
4. (Có thể) kiểm tra bảng dữ liệu  

### 4.2. Ví dụ (IT_AUTH_01) — từng bước

| Bước Excel | Ý nghĩa |
|------------|---------|
| 1. Call `POST /api/auth/register` via MockMvc… | Gửi request đăng ký (trong code: `mockMvc.perform(post(...))`) |
| 2. `AuthController.register()` delegates to `AuthService.register()` | Tầng Controller → Service |
| 3. Service → `UserRepository.save()` + `AuthTokenRepository` OTP | Service → Repository → DB |
| 4. Query `users` và `auth_tokens` | Xác minh dữ liệu đã lưu |

### 4.3. Procedure ≠ hướng dẫn bấm Postman

| Đúng (Integration) | Sai (chỉ tool) |
|--------------------|----------------|
| Nêu Controller / Service / Repository | Chỉ “mở Postman → Send” |
| Có path API + luồng hàm | Không nói tầng nào chạy |
| Map được sang code MockMvc | Không gắn với Excel ID |

**Postman** chỉ là tool phụ (nếu dùng). **Procedure trên Excel** mới là thiết kế IT; **MockMvc** mới là cách thực thi đúng chuẩn môn.

### 4.4. Câu thuyết trình

> Cột Procedure mô tả tuần tự tích hợp hàm. Em thực thi đúng các bước đó bằng MockMvc trong `@SpringBootTest`, không thay Procedure bằng thao tác Postman.

---

## 5. Expected Results

### 5.1. Là gì?

**Kết quả mong đợi** nếu hệ thống đúng — tiêu chí để chấm Passed / Failed.

Thường gồm:

- HTTP Status (200/201, 400, 401…)  
- Nội dung JSON (có `accessToken`, đúng `email`…)  
- Trạng thái DB (có row `users`, password đã hash, có `auth_tokens`…)  
- Hành vi negative (không cấp token, không tạo user thứ 2…)  

### 5.2. Ví dụ (IT_AUTH_01)

- Response **200/201** + thông báo thành công  
- Có dòng trong `users`, password **đã hash** (không plaintext)  
- Có dòng `auth_tokens` verification gắn user  
- Không cần accessToken để gọi register  

### 5.3. Liên hệ code

```java
.andExpect(status().is2xxSuccessful());
.andExpect(jsonPath("$.email").value(LEARNER));
```

→ Đây là cách “khẳng định Expected” trong MockMvc.

### 5.4. Passed với negative test

Nếu Expected là **4xx** (từ chối sai mật khẩu / OTP sai) mà hệ thống trả 4xx → **Passed**.  
Không phải Fail chỉ vì “không 200”.

### 5.5. Câu thuyết trình

> Expected Results là chuẩn đối chiếu. Sau khi chạy MockMvc, Actual khớp Expected thì ghi Passed — kể cả case negative bị từ chối đúng.

---

## 6. Pre-conditions

### 6.1. Là gì?

**Điều kiện tiên quyết** phải có **trước** khi làm Procedure.  
Thiếu precondition → có thể **chưa kết luận đủ** → ghi **N/A** (không phải giấu Fail).

### 6.2. Ví dụ thường gặp

| Pre-condition | Ý nghĩa |
|---------------|---------|
| Database available | PostgreSQL đang chạy |
| Email unused | Email đăng ký chưa tồn tại |
| Mail sender stubbed | Không cần gửi mail thật |
| Verified LEARNER exists | Đã có user demo để login |
| Student enrolled in course | Đã mua/enroll khóa mới xem content |
| Waitlist ≥ 2 students | Mới reorder waitlist được |

### 6.3. Ví dụ (IT_AUTH_01)

- DB sẵn sàng  
- Email chưa dùng  
- Mail stubbed  

### 6.4. Pre-condition vs Procedure

| Pre-conditions | Procedure |
|----------------|-----------|
| Chuẩn bị môi trường / data | Bước chạy test |
| Đọc **trước** khi Send/MockMvc | Làm **sau** khi đủ điều kiện |

### 6.5. Câu thuyết trình

> Pre-conditions là điều kiện đầu vào. Thiếu enroll/OTP/data thì em ghi N/A và Note rõ — thể hiện chấm điểm trung thực.

---

## 7. Quan hệ 5 cột (sơ đồ thuyết trình)

```
Pre-conditions  →  đủ điều kiện chưa?
       ↓
Description     →  đang muốn chứng minh gì?
       ↓
Procedure       →  làm các bước tích hợp hàm (MockMvc)
       ↓
Expected        →  đối chiếu Status / JSON / DB
       ↓
(Test Case ID)  →  ghi Round trên đúng dòng Excel
```

---

## 8. Map sang code & Excel (1 ví dụ full)

**Excel:** sheet `IT - Auth` · ID `IT_AUTH_01`

| Cột | Nội dung (tóm tắt) | Trong code |
|-----|--------------------|------------|
| ID | `IT_AUTH_01` | `@DisplayName("IT_AUTH_01 register")` |
| Description | Register qua AuthController/AuthService | Class `AuthIT` |
| Pre-conditions | DB; email mới; mail stub | `UUID` tạo email mới |
| Procedure | POST register → Controller → Service → Repo | `mockMvc.perform(post("/api/auth/register")...)` |
| Expected | 2xx; user + OTP trong DB | `.andExpect(status().is2xxSuccessful())` |

---

## 9. Cách chấm kết quả (cột Round — ngoài ảnh nhưng cần nói)

Ảnh chưa hiện cột Round, nhưng khi chạy test em ghi:

| Ghi Round | Khi nào |
|-----------|---------|
| **Passed** | Actual = Expected (kể cả negative đúng) |
| **Failed** | Sai Expected / lỗi 500 |
| **N/A** | Thiếu Pre-condition |

Kèm: **Test date**, **Tester**.

---

## 10. Kịch bản thuyết trình 2–3 phút (học thuộc ý)

1. **Mở đầu:** Em trình bày cấu trúc test case trên Excel Integration Test gồm 5 cột chính.  
2. **ID:** Mã định danh, map `@DisplayName`.  
3. **Description:** Mục tiêu tích hợp hàm.  
4. **Procedure:** Trọng tâm — Controller→Service→Repository; thực thi bằng MockMvc.  
5. **Expected:** Chuẩn đối chiếu Status/DB.  
6. **Pre-conditions:** Thiếu thì N/A trung thực.  
7. **Demo 1 dòng:** Mở `IT_AUTH_01` trên Excel + chỉ method tương ứng trong `AuthIT.java`.  
8. **Kết:** Postman không thay các cột này; IT là Excel + Spring Boot Test/MockMvc.

---

## 11. Hỏi – đáp nhanh

| Cô hỏi | Em trả lời |
|--------|------------|
| Procedure viết gì? | Các bước tích hợp hàm + gọi API via MockMvc, không chỉ thao tác UI/tool. |
| Khác Description thế nào? | Description = mục tiêu; Procedure = cách làm từng bước. |
| Expected lấy từ đâu? | Thiết kế trên Excel; code `andExpect` phải khớp. |
| Thiếu Pre-condition? | Ghi N/A, không Forced Passed. |
| Postman nằm cột nào? | Không thay cột nào — chỉ tool phụ; thực thi chính là MockMvc. |

---

## 12. Checklist trước khi thuyết trình

- [ ] Mở được Excel đúng sheet (vd `IT - Auth`)  
- [ ] Chỉ đúng 5 cột trong ảnh và giải thích từng cột  
- [ ] Ví dụ 1 case (`IT_AUTH_01`) đủ 5 cột  
- [ ] Chỉ được file `AuthIT.java` có cùng `@DisplayName`  
- [ ] Nói được Passed / Failed / N/A  
- [ ] Không nói “IT của em là Postman”  
