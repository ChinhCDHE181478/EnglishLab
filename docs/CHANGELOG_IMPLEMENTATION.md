# EnglishLab — Nhật ký triển khai (Changelog)

**Mục đích:** Bằng chứng triển khai cho đồ án tốt nghiệp.  
**Nhánh:** `phongdx`  
**Ngôn ngữ:** Tiếng Việt (định dạng chuẩn hóa)  
**Quy tắc:** Sau mỗi task hoàn thành, luôn **nối thêm** một mục mới với **đúng** cấu trúc mục 1–11. Không bịa tính năng; chỉ ghi những gì có trong code.

### Tài khoản demo (dùng chung)

| Vai trò | Email | Mật khẩu |
|---------|-------|----------|
| Giáo viên | `classroom.teacher1@englishlab.vn` | `Password123!` |
| Học viên | `0386852628z@gmail.com` | `Password123!` |
| Training Manager | `training.manager@englishlab.vn` | `Password123!` |
| Manager | `classroom.manager@englishlab.vn` | `Password123!` |
| Content Manager | `content.manager@englishlab.vn` | `Password123!` (khi chạy seed classroom demo) |
| Học viên waitlist | `waitlist.learner.a@test.vn`, `waitlist.learner.b@test.vn` | `Password123!` |

### Môi trường

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173` (hoặc cổng Vite hiện tại)

---

## Task 1: UI yêu cầu buổi học bù (Makeup Session)

- **Ngày:** 2026-07-12
- **Commit:** `1142848` — `feat(classroom): Task 1 — yêu cầu buổi học bù (makeup session)`

### 1. Tóm tắt

Hoàn thiện luồng giáo viên gửi yêu cầu tạo buổi học bù trên hệ thống change-request hiện có. Giáo viên chọn buổi gốc (kể cả đã hoàn thành/hủy để lấy ngữ cảnh), đề xuất ngày/giờ/phòng, kiểm tra xung đột, rồi gửi Training Manager duyệt. Khi duyệt, hệ thống tạo **buổi mới** trạng thái `MAKEUP` (buổi gốc chỉ mang tính ngữ cảnh).

### 2. Phạm vi thay đổi

- Backend
- Frontend
- API (tái sử dụng API change-request; không thêm path mới)
- UI/UX
- Validation

### 3. Tệp đã thay đổi

- `frontend/src/components/teacher/TeacherChangeRequestForm.jsx`
  - Thêm loại `CREATE_MAKEUP_SESSION` (“Tạo buổi học bù”), nhãn makeup, chọn buổi gốc kể cả completed/cancelled.
- `frontend/src/pages/teacher/TeacherClassroomPage.jsx`
  - Gắn form yêu cầu trong tab gửi yêu cầu của lớp.
- `frontend/src/pages/training-manager/TrainingManagerRequestsPage.jsx`
  - TM xem / duyệt / từ chối yêu cầu chờ.
- `frontend/src/api/classroomApi.js`
  - Dùng helper create / check-conflict / approve / reject có sẵn.
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomChangeRequestServiceImpl.java`
  - Validate riêng makeup; duyệt tạo buổi `MAKEUP`; tránh `SESSION_LOCKED` giả trên buổi gốc đã hoàn thành.
- `backend/src/main/java/fu/sap490/g23/backend/entity/classroom/enums/ClassroomChangeRequestType.java`
  - Enum `CREATE_MAKEUP_SESSION`.
- `backend/src/test/java/fu/sap490/g23/backend/service/classroom/ClassroomChangeRequestServiceImplTest.java`
  - Unit test create/approve makeup.
- `docs/CHANGELOG_IMPLEMENTATION.md`
  - Ghi nhận bằng chứng triển khai.

### 4. Thay đổi Backend

- Controller: tái sử dụng endpoint change-request của `TeacherClassroomController` và `TrainingManagerController`.
- Service: `ClassroomChangeRequestServiceImpl` kiểm tra buổi gốc, ngày, giờ bắt đầu/kết thúc; kiểm tra xung đột GV/phòng/HV cho **slot đề xuất**; khi duyệt gọi `ClassroomOfferingService.createSession` với `ClassroomSessionStatus.MAKEUP`.
- Validation: thiếu lịch bị từ chối; buổi gốc completed/cancelled được phép làm ngữ cảnh.
- Nghiệp vụ: TM có thể ghi đè xung đột khi duyệt (`overrideConflict`).

### 5. Thay đổi Frontend

- Form `TeacherChangeRequestForm` trên chi tiết lớp GV; hàng đợi TM tại `TrainingManagerRequestsPage`.
- Tái sử dụng UI chọn ngày/khung giờ/phòng và panel xung đột như đổi lịch; đổi nhãn theo loại yêu cầu.
- Điều hướng: lớp GV → tab yêu cầu; TM → `/training-manager/requests`.

### 6. Thay đổi Database

- Không đổi schema.
- Tái sử dụng `classroom_change_requests` và `classroom_sessions`.

### 7. Thay đổi API

- `POST /api/teacher/classrooms/requests/check-conflict`
  - Mục đích: Xem trước xung đột lịch cho slot học bù.
  - Request: payload change-request (`requestType`, offering, session, `newValuesJson`, lý do).
  - Response: danh sách xung đột / tóm tắt khả dụng.
  - Phân quyền: Giáo viên (và staff trên `/api/teacher/**`).

- `POST /api/teacher/classrooms/requests`
  - Mục đích: Gửi yêu cầu học bù (hoặc loại khác).
  - Request: `CreateChangeRequestRequest`.
  - Response: yêu cầu đã tạo.
  - Phân quyền: Giáo viên.

- `GET /api/teacher/classrooms/requests/mine`
  - Mục đích: Lịch sử yêu cầu của GV.
  - Phân quyền: Giáo viên.

- `GET /api/training-manager/requests/pending`
  - Mục đích: Hàng đợi chờ duyệt.
  - Phân quyền: Training Manager / Manager / Admin.

- `POST /api/training-manager/requests/{requestId}/conflict-check`
  - Mục đích: Kiểm tra lại xung đột trước khi duyệt.
  - Phân quyền: Training Manager / Manager / Admin.

- `POST /api/training-manager/requests/{requestId}/approve`
  - Mục đích: Duyệt và áp dụng (makeup → tạo buổi `MAKEUP`).
  - Request: `ReviewChangeRequestRequest` (có thể có `overrideConflict`, ghi chú).
  - Phân quyền: Training Manager / Manager / Admin.

- `POST /api/training-manager/requests/{requestId}/reject`
  - Mục đích: Từ chối kèm ghi chú.
  - Phân quyền: Training Manager / Manager / Admin.

### 8. Thay đổi UI/UX

- **Trang:** `/teacher/classrooms/{id}` — vai trò Giáo viên.
- **Thao tác:** Chọn “Tạo buổi học bù”, buổi gốc, ngày/giờ/phòng, lý do, check xung đột, gửi.
- **Trang:** `/training-manager/requests` — TM duyệt / từ chối / ghi đè xung đột.
- **Thành công:** Yêu cầu chờ → đã duyệt; buổi mới `MAKEUP` xuất hiện trong lịch buổi học.
- **Lỗi:** Thiếu lịch hoặc xung đột chưa xử lý sẽ chặn gửi/duyệt.

### 9. Các bước test trên web

1. Chạy backend (`:8080`) và frontend (`:5173`).
2. Đăng nhập GV `classroom.teacher1@englishlab.vn` / `Password123!`.
3. Vào `/teacher/classrooms/{id}` → tab gửi yêu cầu.
4. Chọn **Tạo buổi học bù**, chọn buổi gốc (có thể đã hoàn thành), điền ngày/giờ/phòng, lý do.
5. Kiểm tra xung đột rồi gửi.
6. Xác nhận yêu cầu hiện ở trạng thái chờ duyệt.
7. Đăng nhập TM `training.manager@englishlab.vn` / `Password123!`.
8. Vào `/training-manager/requests` → conflict-check → **Duyệt**.
9. Quay lại lớp GV → tab buổi học → thấy buổi `MAKEUP` mới.
10. Case lỗi: gửi makeup thiếu ngày/giờ → báo validation.

### 10. Kết quả mong đợi

GV gửi được yêu cầu học bù; TM duyệt được; hệ thống tạo buổi makeup riêng mà không cần API mới. Buổi gốc đã hoàn thành không bị chặn sai bởi `SESSION_LOCKED`.

### 11. Ghi chú / Rủi ro

- Không thêm REST path mới; phụ thuộc pipeline change-request + tạo buổi hiện có.
- Đã sửa: `SESSION_LOCKED` giả; hỗ trợ ghi đè xung đột khi tạo makeup.
- Nên kiểm tra lại trên trình duyệt sau mỗi lần reset môi trường.

---

## Task 2: Chỉnh sửa bảng điểm thủ công và công bố / thu hồi

- **Ngày:** 2026-07-12
- **Commit:** `98ddd65` — `feat(classroom): Task 2 — chỉnh sửa bảng điểm thủ công và công bố/thu hồi`

### 1. Tóm tắt

Cho phép giáo viên sửa điểm thủ công (bài tập, quiz, chuyên cần, tham gia, kết quả cuối, nhận xét), rồi công bố hoặc thu hồi để học viên xem. Điểm Homework và Quiz **tách riêng** (không trung bình). Học viên chỉ xem khi entry ở trạng thái `PUBLISHED`.

### 2. Phạm vi thay đổi

- Backend
- Frontend
- API
- UI/UX
- Validation

### 3. Tệp đã thay đổi

- `frontend/src/components/teacher/TeacherGradebookSection.jsx`
  - Form sửa từng HV, lưu, công bố/thu hồi với `ConfirmModal`, cột Homework/Quiz riêng.
- `frontend/src/pages/teacher/TeacherClassroomPage.jsx`
  - Gắn tab bảng điểm và handler.
- `frontend/src/pages/classroom/MyClassroomDetailPage.jsx`
  - HV xem Homework/Quiz thành hai thẻ điểm riêng.
- `frontend/src/api/classroomApi.js`
  - `updateGradebookEntry`, `publishGradebook`, `unpublishGradebook`, `getMyGradebook`.
- `backend/src/main/java/fu/sap490/g23/backend/dto/request/classroom/UpdateGradebookRequest.java`
  - Validation khoảng điểm và độ dài nhận xét.
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomGradebookServiceImpl.java`
  - `updateEntry`, `publishGradebook`, `unpublishGradebook`, quy tắc hiển thị cho HV.
- `backend/src/main/java/fu/sap490/g23/backend/controller/classroom/TeacherClassroomController.java`
  - Endpoint GET/PUT/publish/unpublish gradebook.
- `backend/src/test/java/fu/sap490/g23/backend/service/classroom/ClassroomGradebookServiceImplTest.java`
  - Test cập nhật điểm và thu hồi công bố.

### 4. Thay đổi Backend

- Controller: gradebook GV; HV `GET .../gradebook/me`.
- Service: sửa điểm đưa `PENDING` → `GRADED`; publish → `PUBLISHED`; unpublish chỉ đổi entry `PUBLISHED` → `GRADED`.
- Validation: homework/quiz/tham gia/cuối `0–10`; chuyên cần `0–100`; tối đa 2 chữ số thập phân; nhận xét ≤ 2000 ký tự.
- Nghiệp vụ: sửa entry đã publish vẫn giữ `PUBLISHED` để HV thấy điểm đã chỉnh đến khi thu hồi.

### 5. Thay đổi Frontend

- Tách `TeacherGradebookSection` để dễ bảo trì.
- Panel sửa từng HV; nút công bố/thu hồi kèm modal xác nhận (Esc/backdrop để hủy).
- Validate phía client trước khi gọi API.
- Sau lưu chỉ cập nhật hàng HV tương ứng (không reload cả trang).

### 6. Thay đổi Database

- Không đổi schema.
- Tái sử dụng `classroom_gradebook_entries` và trạng thái `PENDING` / `GRADED` / `PUBLISHED`.

### 7. Thay đổi API

- `GET /api/teacher/classrooms/{id}/gradebook`
  - Mục đích: Tải bảng điểm lớp cho GV.
  - Phân quyền: Giáo viên / staff.

- `PUT /api/teacher/classrooms/{id}/gradebook`
  - Mục đích: Cập nhật một entry HV.
  - Request: `UpdateGradebookRequest`.
  - Response: entry/bảng điểm đã cập nhật.
  - Phân quyền: Giáo viên / staff.

- `POST /api/teacher/classrooms/{id}/gradebook/publish`
  - Mục đích: Công bố bảng điểm cho HV.
  - Phân quyền: Giáo viên / staff.

- `POST /api/teacher/classrooms/{id}/gradebook/unpublish`
  - Mục đích: Thu hồi công bố (ẩn khỏi HV).
  - Phân quyền: Giáo viên / staff.

- `GET /api/student/classrooms/{id}/gradebook/me`
  - Mục đích: HV xem điểm của mình (chỉ khi đã publish).
  - Phân quyền: Học viên.

### 8. Thay đổi UI/UX

- **Trang:** `/teacher/classrooms/{id}` → **Bảng điểm** (GV).
- **Thao tác:** Chỉnh sửa / Lưu; Công bố; Thu hồi công bố.
- **Trạng thái nút:** Công bố disabled nếu đã publish hết; Thu hồi disabled nếu chưa có entry publish.
- **HV:** `/my-classrooms/{id}` — thẻ Homework và Quiz riêng khi đã publish.
- **Lỗi:** Điểm ngoài khoảng bị chặn; HV không xem được khi chưa publish.

### 9. Các bước test trên web

1. Chạy backend và frontend.
2. Đăng nhập GV → `/teacher/classrooms/{id}` → **Bảng điểm**.
3. Sửa một HV: Homework `2`, Quiz `1` riêng → Lưu.
4. Kiểm tra bảng hiện `2` và `1` (không thành `1.5`).
5. **Công bố bảng điểm** → xác nhận modal.
6. Đăng nhập HV → `/my-classrooms/{id}` → thấy điểm đã publish.
7. GV **Thu hồi công bố** → xác nhận.
8. HV reload → không còn xem được bảng điểm đã publish.
9. Case lỗi: nhập điểm `11` → validation từ chối.

### 10. Kết quả mong đợi

GV sửa/công bố/thu hồi được bảng điểm. Homework và Quiz luôn là hai trường độc lập end-to-end.

### 11. Ghi chú / Rủi ro

- Quiz vẫn là thành phần riêng (Task 3 gộp quiz đã bỏ qua).
- Modal xác nhận thay `window.confirm` cho đồng bộ thương hiệu.

---

## Task 3: Gộp Quiz vào Bài tập (Đã bỏ qua)

- **Ngày:** N/A (nhóm quyết định bỏ qua)
- **Commit:** Không có

### 1. Tóm tắt

Task 3 (gộp Quiz vào Homework, `MULTIPLE_CHOICE` chấm tự động, deprecate quiz riêng) **không được triển khai** theo quyết định nhóm. Homework và Quiz vẫn tách trong entity, API và UI.

### 2. Phạm vi thay đổi

- Không có

### 3. Tệp đã thay đổi

- Không có tệp triển khai cho Task 3.

### 4. Thay đổi Backend

- Không có thay đổi backend.

### 5. Thay đổi Frontend

- Không có thay đổi frontend.

### 6. Thay đổi Database

- Không có thay đổi database.

### 7. Thay đổi API

- Không có thay đổi API.

### 8. Thay đổi UI/UX

- Không có. UI Homework/Quiz riêng vẫn giữ như hiện tại.

### 9. Các bước test trên web

1. Không áp dụng — tính năng không giao.
2. Chỉ regression: bảng điểm GV vẫn có cột Homework và Quiz riêng (hành vi Task 2).

### 10. Kết quả mong đợi

Không có gộp Quiz→Homework. Module quiz và cột điểm quiz vẫn tồn tại.

### 11. Ghi chú / Rủi ro

- Ghi rõ “đã bỏ qua” để reviewer không kỳ vọng deliverable Task 3.
- Nếu làm sau cần chốt enum/migration `MULTIPLE_CHOICE` vs `SKILL_PRACTICE`.

---

## Task 4: Ưu tiên và vị trí danh sách chờ lớp học

- **Ngày:** 2026-07-12
- **Commit:** `7d05f86` — `feat(classroom): Task 4 — ưu tiên và sắp xếp danh sách chờ`

### 1. Tóm tắt

Bổ sung hàng đợi có thứ tự khi lớp đầy chỗ. Đăng ký waitlist mới nhận `waitlist_priority` nối đuôi. TM sắp xếp lại hàng đợi; HV xem vị trí dạng `#N / tổng số`.

### 2. Phạm vi thay đổi

- Backend
- Frontend
- Database
- API
- UI/UX

### 3. Tệp đã thay đổi

- `backend/src/main/java/fu/sap490/g23/backend/entity/classroom/ClassroomEnrollment.java`
  - Trường `waitlistPriority`.
- `backend/src/main/java/fu/sap490/g23/backend/migration/ClassroomWaitlistSchemaMigration.java`
  - Thêm cột, backfill, index.
- `backend/src/main/java/fu/sap490/g23/backend/dto/request/classroom/ReorderWaitlistRequest.java`
  - Danh sách `enrollmentIds` theo thứ tự.
- `backend/src/main/java/fu/sap490/g23/backend/dto/response/classroom/ClassroomEnrollmentResponse.java`
  - `waitlistPriority`, `waitlistPosition`, `waitlistSize`.
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomOfferingServiceImpl.java`
  - Gán priority, `reorderWaitlist`, compact khi rời waitlist.
- `backend/src/main/java/fu/sap490/g23/backend/controller/classroom/TrainingManagerClassroomController.java`
  - `PUT /{id}/waitlist/order`.
- `backend/src/test/java/fu/sap490/g23/backend/service/classroom/ClassroomOfferingServiceImplWaitlistTest.java`
  - Unit test waitlist.
- `frontend/src/components/training-manager/TrainingManagerRegistrationPanel.jsx`
  - Tab **Danh sách chờ**, nút lên/xuống.
- `frontend/src/pages/classroom/ClassroomPublicDetailPage.jsx` / UI lớp của tôi
  - Badge vị trí cho HV.
- `frontend/src/api/classroomApi.js`
  - `reorderClassWaitlist`.

### 4. Thay đổi Backend

- Entity/repository: lưu và truy vấn thứ tự waitlist.
- Service: FIFO append; reorder phải khớp đúng tập ID hiện tại; đánh số lại `1..N`; xóa priority khi rời `WAITLIST`.
- Bảo mật: reorder kiểm tra quyền Training Manager ở service.

### 5. Thay đổi Frontend

- Panel đăng ký TM: tab waitlist, badge vị trí, nút reorder.
- UI HV: hiện `#N / tổng` khi đang chờ.
- API client gửi danh sách ID đã sắp xếp.

### 6. Thay đổi Database

- `classroom_enrollments.waitlist_priority` (INTEGER, nullable).
- Index `idx_classroom_enrollment_waitlist_order` trên `(classroom_offering_id, registration_status, waitlist_priority)`.
- Migration khởi động backfill dữ liệu waitlist cũ.

### 7. Thay đổi API

- `PUT /api/training-manager/classrooms/{id}/waitlist/order`
  - Mục đích: Đổi thứ tự danh sách chờ.
  - Request: `{ "enrollmentIds": [id1, id2, ...] }` (đủ bộ, đúng tập).
  - Response: danh sách enrollment / view đăng ký đã cập nhật.
  - Phân quyền: Training Manager / Manager / Admin.

- Liên quan (có sẵn): list đăng ký `WAITLIST`; API đăng ký HV đưa vào waitlist khi lớp đầy.

### 8. Thay đổi UI/UX

- **TM:** `/training-manager/classrooms/{id}` → **Danh sách chờ** — lên/xuống.
- **HV:** lịch khai giảng / chi tiết lớp / lớp của tôi — hiện vị trí chờ.
- **Thành công:** vị trí cập nhật sau khi TM reorder.
- **Lỗi:** reorder thiếu/thừa ID → backend từ chối.

### 9. Các bước test trên web

1. Chạy backend/frontend.
2. Dùng lớp giảm `maxCapacity` (hoặc lớp đầy) để đăng ký mới vào `WAITLIST`.
3. HV A đăng ký → thấy `#1`.
4. HV B đăng ký → thấy `#2`.
5. TM vào đăng ký lớp → **Danh sách chờ** → đưa B lên trên A.
6. HV reload → vị trí đổi chỗ.
7. Case lỗi: gọi reorder với tập ID sai → 400.

### 10. Kết quả mong đợi

Thứ tự waitlist ổn định, TM chỉnh được, HV thấy `#N / tổng`. Rời waitlist sẽ compact lại priority còn lại.

### 11. Ghi chú / Rủi ro

- Phụ thuộc sức chứa và trạng thái đăng ký.
- Demo có thể cần giảm tạm `maxCapacity` để tái hiện waitlist nhanh.

---

## Task 5: Thanh toán học phí lớp qua PayOS và minh chứng (Plan B)

- **Ngày:** 2026-07-13
- **Commit:** `04416b1` — `feat(classroom): Task 5 — thanh toán học phí lớp qua PayOS`

### 1. Tóm tắt

Nối học phí lớp vào pipeline quote/checkout PayOS sẵn có, giữ upload minh chứng chuyển khoản làm phương án B. HV thanh toán số còn lại của **một** lớp; khi PAID, webhook/đồng bộ trạng thái ghi nhận học phí qua `applyPayosTuitionPayment` (idempotent theo note `PayOS #orderCode`).

### 2. Phạm vi thay đổi

- Backend
- Frontend
- Database
- API
- UI/UX
- Validation / quy tắc nghiệp vụ

### 3. Tệp đã thay đổi

- `backend/src/main/java/fu/sap490/g23/backend/service/payment/impl/PaymentServiceImpl.java`
  - Nhận `classroomOfferingIds`; quy tắc quote/create học phí; áp dụng sau PAID.
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomOfferingServiceImpl.java`
  - `applyPayosTuitionPayment` ghi nhận idempotent.
- `backend/src/main/java/fu/sap490/g23/backend/entity/payment/PaymentOrder.java`
  - Liên kết `enrollmentId`.
- `backend/src/main/java/fu/sap490/g23/backend/migration/PaymentOrderEnrollmentSchemaMigration.java`
  - Thêm `payment_orders.enrollment_id` (+ index pending).
- `backend/src/test/java/fu/sap490/g23/backend/service/payment/PaymentServiceImplClassroomTuitionTest.java`
  - Unit test học phí lớp.
- `frontend/src/components/classroom/TuitionPaymentSection.jsx`
  - Nút PayOS + form minh chứng.
- `frontend/src/api/paymentApi.js`
  - Gửi `classroomOfferingIds` khi quote/link.
- `frontend/src/pages/CheckoutPage.jsx`
  - Return PayOS học phí → nút quay lại lớp.
- `frontend/src/pages/classroom/MyClassroomDetailPage.jsx` / `ClassroomPublicDetailPage.jsx`
  - Host khu vực thanh toán học phí.

### 4. Thay đổi Backend

- Payment: số tiền = học phí còn lại; mỗi đơn 1 lớp; không áp mã giảm giá; chặn khi còn `PENDING_CONFIRMATION` hoặc đã có đơn pending trùng enrollment.
- Classroom: ghi nhận thanh toán; có thể xếp lớp khi đóng đủ (theo quy tắc hiện có).
- Webhook PayOS đánh PAID rồi áp dụng học phí.

### 5. Thay đổi Frontend

- Khu vực học phí: PayOS chính, upload minh chứng phụ.
- Checkout nhận diện đơn học phí lớp và dẫn về lớp.
- API client truyền `classroomOfferingIds`.

### 6. Thay đổi Database

- `payment_orders.enrollment_id` (BIGINT, nullable).
- Index đơn pending theo enrollment.
- Tái sử dụng `classroom_offering_ids_csv`.

### 7. Thay đổi API

- `POST /api/student/payments/quote`
  - Mục đích: Báo giá (có thể gồm học phí lớp).
  - Request: `courseIds`, `classroomOfferingIds`, `couponCode`.
  - Phân quyền: Học viên.

- `POST /api/student/payments/payos/link`
  - Mục đích: Tạo link PayOS.
  - Phân quyền: Học viên.

- `GET /api/student/payments/orders/{orderCode}`
  - Mục đích: Đồng bộ trạng thái sau return.
  - Phân quyền: Học viên (chủ đơn).

- `POST /api/payos/webhook`
  - Mục đích: Callback nhà cung cấp → PAID.
  - Phân quyền: Public webhook.

- Plan B (giữ nguyên):
  - `POST /api/student/classrooms/{id}/tuition-proofs`
  - `POST /api/training-manager/classrooms/tuition-proofs/{proofId}/confirm`
  - `POST /api/training-manager/classrooms/tuition-proofs/{proofId}/reject`

### 8. Thay đổi UI/UX

- **HV:** `/my-classrooms/{id}` hoặc chi tiết lớp công khai — **Thanh toán học phí** (PayOS + minh chứng).
- **Return:** `/checkout` → **Quay lại lớp học**.
- **TM:** duyệt/từ chối minh chứng.
- **Lỗi:** còn chờ xác nhận đăng ký; không gộp khóa + lớp; không áp coupon học phí lớp.

### 9. Các bước test trên web

1. Chạy backend/frontend; cấu hình PayOS sandbox hợp lệ.
2. TM xác nhận đăng ký để trạng thái có thể thanh toán (còn học phí > 0).
3. HV vào khu vực học phí → **PayOS**.
4. Thanh toán sandbox → về `/checkout` → quay lại lớp.
5. Kiểm tra lịch sử có `PayOS #...` và số còn lại cập nhật.
6. Plan B: upload minh chứng → TM xác nhận → học phí được ghi.
7. Case lỗi: gắn coupon học phí lớp / gộp khóa+lớp → bị từ chối.

### 10. Kết quả mong đợi

HV đóng học phí lớp online được; webhook/status tự ghi nhận. Upload minh chứng vẫn dùng được làm phương án B.

### 11. Ghi chú / Rủi ro

- Cần enrollment đang ở trạng thái có thể đóng học phí.
- Lớp demo `#12` có thể `ACTIVE`/đóng đăng ký — dùng lớp `UPCOMING` hoặc chỉnh tạm dữ liệu.
- Không có PayOS payout/refund học phí lớp trong task này.

---

## Task 6: Hoàn tiền đơn khóa học và biên lai PDF

- **Ngày:** 2026-07-13
- **Commit:** `75303d6` — `feat(payment): Task 6 — hoàn tiền và biên lai đơn khóa học`

### 1. Tóm tắt

Thêm thao tác staff đánh dấu đơn **khóa học** `REFUNDED` và HV tải biên lai PDF (OpenPDF). Hoàn tiền trong hệ thống: hủy quyền học, hoàn lượt coupon. **Không** gọi PayOS payout. Đơn học phí lớp bị loại trừ.

### 2. Phạm vi thay đổi

- Backend
- Frontend
- Database
- API
- UI/UX
- Bảo mật / validation

### 3. Tệp đã thay đổi

- `backend/src/main/java/fu/sap490/g23/backend/service/payment/impl/PaymentServiceImpl.java`
  - `refundCourseOrder`, `downloadCourseReceipt`, `assertCourseOrder`.
- `backend/src/main/java/fu/sap490/g23/backend/service/payment/PaymentReceiptPdfService.java`
  - Sinh PDF biên lai.
- `backend/src/main/java/fu/sap490/g23/backend/controller/payment/ManagerPaymentController.java`
  - List + refund cho staff.
- `backend/src/main/java/fu/sap490/g23/backend/controller/payment/StudentPaymentController.java`
  - Endpoint tải biên lai.
- `backend/src/main/java/fu/sap490/g23/backend/dto/request/payment/RefundCourseOrderRequest.java`
  - `reason` bắt buộc (max 500).
- `backend/src/main/java/fu/sap490/g23/backend/migration/PaymentOrderRefundSchemaMigration.java`
  - Cột audit hoàn tiền; CHECK status có `REFUNDED`.
- `backend/src/main/java/fu/sap490/g23/backend/service/course/impl/OnlineCourseServiceImpl.java`
  - `revokePaidCourseAccess` khi hoàn.
- `backend/src/test/java/fu/sap490/g23/backend/service/payment/PaymentServiceImplRefundReceiptTest.java`
  - Unit test hoàn tiền/biên lai.
- `frontend/src/pages/TransactionHistoryPage.jsx`
  - HV tải PDF.
- `frontend/src/pages/content-manager/ContentManagerAnalyticsPage.jsx`
  - UI staff hoàn đơn khóa học.
- `frontend/src/api/paymentApi.js`
  - `downloadReceipt`, `refundCourseOrder`, list đơn staff.

### 4. Thay đổi Backend

- Chỉ hoàn toàn phần (`refundedAmount = order.amount`).
- Chỉ đơn khóa học (`enrollmentId == null` và có course IDs).
- Đặt `REFUNDED`, lưu lý do/người/thời điểm/số tiền.
- Thu hồi quyền khóa; hoàn coupon nếu có.
- PDF cho đơn PAID/REFUNDED thuộc về HV.

### 5. Thay đổi Frontend

- Lịch sử giao dịch: nút tải biên lai.
- Analytics CM: staff hoàn đơn PAID kèm lý do.
- Helper tải blob PDF và gọi refund.

### 6. Thay đổi Database

- `payment_orders`: `refunded_amount_vnd`, `refunded_at`, `refund_reason`, `refunded_by_id`.
- CHECK status cho phép `REFUNDED`.

### 7. Thay đổi API

- `GET /api/student/payments/orders/{orderCode}/receipt`
  - Mục đích: Tải PDF biên lai.
  - Response: `application/pdf`.
  - Phân quyền: Học viên (chủ đơn).

- `POST /api/manager/payments/orders/{orderCode}/refund`
  - Mục đích: Đánh dấu hoàn đơn khóa học.
  - Request: `{ "reason": "..." }`.
  - Phân quyền: Manager / Admin.

- `POST /api/content-manager/payments/orders/{orderCode}/refund`
  - Mục đích: Hoàn tiền từ cổng CM.
  - Phân quyền: Content Manager / Manager / Admin.

- `GET /api/manager/payments/orders` / `GET /api/content-manager/payments/orders`
  - Mục đích: List đơn cho UI hoàn tiền.
  - Phân quyền: theo role tương ứng.

### 8. Thay đổi UI/UX

- **HV:** `/transaction-history` — tải biên lai.
- **Staff:** `/content-manager/analytics` — hoàn đơn + lý do.
- **Lỗi:** đơn học phí lớp không hoàn/biên lai tại đây; thiếu lý do bị từ chối; HV không gọi API refund staff.

### 9. Các bước test trên web

1. Chạy backend/frontend.
2. HV có đơn khóa **PAID** → `/transaction-history` → tải PDF → kiểm tra mã đơn/số tiền/trạng thái.
3. Manager `classroom.manager@englishlab.vn` → `/content-manager/analytics` → hoàn đơn đó kèm lý do.
4. Đơn thành `REFUNDED`; HV mất quyền khóa; coupon được hoàn nếu dùng.
5. Case lỗi: hoàn đơn học phí lớp → 400.
6. Case lỗi: HV gọi API refund → 403.
7. Sau hoàn, HV vẫn có thể tải biên lai (nếu code cho phép với `REFUNDED`).

### 10. Kết quả mong đợi

Đơn khóa học hoàn được trong hệ thống (quyền + coupon); HV tải được PDF. Không có hoàn tiền tự động qua PayOS.

### 11. Ghi chú / Rủi ro

- Training Manager không có quyền API refund payment staff (theo SecurityConfig).
- DTO request: `RefundCourseOrderRequest` (phạm vi đơn khóa học).

---

## Task 7: Workflow NEED_REFUND / settlement học phí lớp

- **Ngày:** 2026-07-13
- **Commit:** `b13533b` — `feat(classroom): Task 7 — duyệt/từ chối NEED_REFUND học phí lớp`

### 1. Tóm tắt

Hoàn thiện workflow TM xử lý settlement `NEED_REFUND`. TM duyệt hoặc từ chối; duyệt ghi audit `ClassroomTuitionPayment` loại `REFUND` và điều chỉnh `tuition_amount_paid`. Nguồn phát sinh: chuyển lớp overpay, hủy/xóa/từ chối khi đã thu tiền. Chi trả ngân hàng/PayOS ngoài hệ thống.

### 2. Phạm vi thay đổi

- Backend
- Frontend
- Database
- API
- UI/UX
- Validation

### 3. Tệp đã thay đổi

- `backend/src/main/java/fu/sap490/g23/backend/entity/classroom/enums/TuitionSettlementStatus.java`
  - `NONE | PENDING | RESOLVED | REJECTED`.
- `backend/src/main/java/fu/sap490/g23/backend/entity/classroom/enums/TuitionPaymentKind.java`
  - Thêm `REFUND`.
- `backend/src/main/java/fu/sap490/g23/backend/migration/ClassroomTuitionSettlementSchemaMigration.java`
  - Cột audit settlement; CHECK `payment_kind` có `REFUND`.
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/ClassroomRegistrationSupport.java`
  - Helper settlement (`markNeedRefundForExit`, compute/apply).
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomOfferingServiceImpl.java`
  - `resolveTuitionSettlement`; producer; hoàn full khi exit vs chỉ phần thừa khi còn trong lớp.
- `backend/src/main/java/fu/sap490/g23/backend/controller/classroom/TrainingManagerClassroomController.java`
  - Endpoint resolve; list `settlementPending`.
- `backend/src/main/java/fu/sap490/g23/backend/dto/request/classroom/ResolveTuitionSettlementRequest.java`
  - `action`, `note`.
- Tests: `ClassroomOfferingServiceImplSettlementTest.java`, `ClassroomRegistrationSupportSettlementTest.java`.
- `frontend/src/components/training-manager/TrainingManagerRegistrationPanel.jsx`
  - Tab **Cần hoàn tiền** + duyệt/từ chối.
- `frontend/src/components/classroom/ClassroomUi.jsx` / `classroomHelpers.js`
  - Nhãn settlement; không báo “đã hoàn thành” khi còn pending.
- `frontend/src/api/classroomApi.js`
  - `resolveTuitionSettlement`.

### 4. Thay đổi Backend

- Duyệt (`APPROVE_REFUND`):
  - Đã thoát lớp (`CANCELLED`/`REJECTED`): hoàn hết `paid` → `paid = 0`.
  - Còn trong lớp (vd. chuyển lớp overpay): chỉ hoàn `paid - due`.
- Từ chối (`REJECT_REFUND`): bắt buộc có note; giữ paid; `REJECTED`; không ghi REFUND.
- Double-resolve / action sai → 400.
- List `settlementPending=true` trả các case `PENDING`.

### 5. Thay đổi Frontend

- Tab hàng đợi cần hoàn tiền trên panel đăng ký TM.
- Duyệt / từ chối (từ chối cần lý do).
- Lịch sử học phí hiện dòng REFUND dạng giảm.
- Thẻ trạng thái không coi là xong khi còn settlement pending.

### 6. Thay đổi Database

- `classroom_enrollments`: `tuition_settlement_status`, `tuition_settlement_resolved_at`, `tuition_settlement_resolved_by_id`, `tuition_settlement_resolution_note` (+ type/note sẵn có).
- `classroom_tuition_payments.payment_kind` cho phép `REFUND`.
- Index truy vấn settlement pending.

### 7. Thay đổi API

- `POST /api/training-manager/classrooms/enrollments/{id}/settlement/resolve`
  - Mục đích: Duyệt/từ chối NEED_REFUND.
  - Request: `{ "action": "APPROVE_REFUND" | "REJECT_REFUND", "note": "..." }`.
  - Response: `ClassroomEnrollmentResponse` đã cập nhật.
  - Phân quyền: Training Manager / Manager / Admin.

- `GET /api/training-manager/classrooms/registrations?settlementPending=true`
  - Mục đích: Hàng đợi settlement.
  - Phân quyền: Training Manager / Manager / Admin.

- `GET /api/training-manager/classrooms/enrollments/{id}/tuition-history`
  - Mục đích: Kiểm tra dòng REFUND audit.
  - Phân quyền: Training Manager / Manager / Admin.

### 8. Thay đổi UI/UX

- **Trang:** Panel đăng ký TM — tab **Cần hoàn tiền**.
- **Thao tác:** Duyệt hoàn tiền; Từ chối (có lý do).
- **Thành công:** `RESOLVED`/`REJECTED`; lịch sử có REFUND khi duyệt.
- **Lỗi:** thiếu lý do từ chối; resolve hai lần; HV bị cấm.

### 9. Các bước test trên web

1. Chạy backend/frontend.
2. Tạo case NEED_REFUND (vd. chuyển lớp overpay; hoặc hủy/xóa HV đã đóng tiền).
3. Đăng nhập TM → đăng ký → **Cần hoàn tiền**.
4. Duyệt hoàn → kiểm tra `paid` và lịch sử REFUND.
5. Case khác → từ chối có lý do → `paid` giữ nguyên, không có REFUND.
6. Case lỗi: resolve lần 2 → lỗi; HV không gọi được API TM.

### 10. Kết quả mong đợi

TM xử lý hết hàng đợi NEED_REFUND có audit trong app. Tiền mặt/PayOS vẫn xử lý ngoài hệ thống.

### 11. Ghi chú / Rủi ro

- Khác Task 6 (hoàn đơn khóa học PayOS).
- Đã E2E sau restart backend (overpay, hoàn full khi exit, transfer, auth).

---

## Task 8: Cài đặt CM và quản lý gói / Bundle

- **Ngày:** 2026-07-13
- **Commit:** `99710d8` — `feat(course): Task 8 — CM settings và quản lý gói Bundle`

### 1. Tóm tắt

Thêm khu vực Content Manager quản lý **gói/bundle** và trang **Cài đặt** nhẹ. CM tạo/sửa/xóa mềm gói `BUNDLE`, ghép sản phẩm con (`ONLINE_COURSE` / `CLASSROOM`), chạy vòng đời xuất bản. Settings chỉ xem loại gói (read-only). Chưa có checkout/enrollment bundle.

### 2. Phạm vi thay đổi

- Backend
- Frontend
- Database
- API
- UI/UX
- Validation

### 3. Tệp đã thay đổi

- `backend/src/main/java/fu/sap490/g23/backend/entity/course/PackageBundleItem.java`
  - Entity join composition bundle.
- `backend/src/main/java/fu/sap490/g23/backend/repository/course/PackageBundleItemRepository.java`
  - Lưu/truy vấn item bundle.
- `backend/src/main/java/fu/sap490/g23/backend/migration/PackageBundleSchemaMigration.java`
  - Tạo `package_bundle_items`; đảm bảo type `BUNDLE`; mở CHECK status `PENDING_REVIEW`/`REJECTED`.
- `backend/src/main/java/fu/sap490/g23/backend/service/course/LearningPackageManagementService.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/course/impl/LearningPackageManagementServiceImpl.java`
  - Chỉ mutate `BUNDLE`; validate composition; lifecycle.
- `backend/src/main/java/fu/sap490/g23/backend/controller/course/ContentManagerPackageController.java`
  - `/api/content-manager/packages/**`.
- DTO: `LearningPackageRequest`, `UpdatePackageBundleItemsRequest`, `LearningPackageResponse`, `LearningPackageSummaryResponse`, `PackageTypeResponse`.
- `backend/src/test/java/fu/sap490/g23/backend/service/course/LearningPackageManagementServiceImplTest.java`
- `frontend/src/api/packageApi.js`
- `frontend/src/pages/content-manager/ContentManagerPackagesPage.jsx`
- `frontend/src/pages/content-manager/ContentManagerSettingsPage.jsx`
- `frontend/src/pages/content-manager/ContentManagerRoutes.jsx`
- `frontend/src/components/content-manager/contentManagerConfig.js`
  - Nav **Gói học / Bundle** và **Cài đặt**.

### 4. Thay đổi Backend

- Chỉ tạo/sửa/xóa/publish/submit/archive loại `BUNDLE`.
- Con chỉ `ONLINE_COURSE` hoặc `CLASSROOM`; không self-ref; không bundle lồng bundle.
- Publish/gửi duyệt cần ≥ 1 con.
- Soft-delete: `deleted=true` + archive.
- Endpoint loại gói chỉ đọc.

### 5. Thay đổi Frontend

- `/content-manager/packages`: list/lọc, tạo/sửa bundle, ghép con, vòng đời.
- `/content-manager/settings`: bảng loại gói + nút sang packages.
- Gói không phải BUNDLE khi hiện trong list → chỉ xem.

### 6. Thay đổi Database

- Bảng mới `package_bundle_items` (`bundle_package_id`, `child_package_id`, `display_order`, unique cặp).
- Đảm bảo có `package_types.code = BUNDLE`.
- `packages_status_check` cho phép `DRAFT`, `PENDING_REVIEW`, `REJECTED`, `PUBLISHED`, `ARCHIVED`.

### 7. Thay đổi API

Tất cả dưới `/api/content-manager/packages` — Phân quyền: Content Manager / Manager / Admin.

- `GET /api/content-manager/packages/types` — danh mục loại gói (read-only).
- `GET /api/content-manager/packages/candidates` — ứng viên con.
- `GET /api/content-manager/packages` — list/lọc (`keyword`, `packageTypeCode`, `status`).
- `GET /api/content-manager/packages/{id}` — chi tiết + children.
- `POST /api/content-manager/packages` — tạo BUNDLE.
- `PUT /api/content-manager/packages/{id}` — cập nhật metadata BUNDLE.
- `PUT /api/content-manager/packages/{id}/bundle-items` — thay composition.
- `PATCH /api/content-manager/packages/{id}/submit-review` — `PENDING_REVIEW`.
- `PATCH /api/content-manager/packages/{id}/publish` — xuất bản.
- `PATCH /api/content-manager/packages/{id}/archive` — lưu trữ.
- `DELETE /api/content-manager/packages/{id}` — xóa mềm.

### 8. Thay đổi UI/UX

- **Cài đặt:** `/content-manager/settings` — CM/Manager — bảng type + link packages.
- **Gói:** `/content-manager/packages` — tạo bundle, chọn con, sắp xếp, publish.
- **Trống:** thông báo khi chưa có candidate.
- **Lỗi:** publish không có con; self-ref; sửa CLASSROOM qua API này bị chặn.

### 9. Các bước test trên web

1. Chạy backend/frontend (restart backend để migration chạy).
2. Đăng nhập Manager `classroom.manager@englishlab.vn` / `Password123!` (hoặc CM nếu đã seed).
3. `/content-manager/settings` → thấy type có `BUNDLE`.
4. `/content-manager/packages` → **Tạo bundle**.
5. Thêm ≥ 1 con CLASSROOM/ONLINE_COURSE → Lưu.
6. **Gửi duyệt** → `PENDING_REVIEW`.
7. **Xuất bản** → `PUBLISHED`.
8. Sửa composition/metadata → Lưu.
9. Lưu trữ / xóa mềm bundle test → khỏi list active.
10. Case lỗi: publish bundle trống; cố sửa CLASSROOM tại màn này → bị chặn/chỉ xem.

### 10. Kết quả mong đợi

CM quản lý được gói BUNDLE và composition; xem loại gói ở settings. Chưa bán/đăng ký bundle trên storefront.

### 11. Ghi chú / Rủi ro

- Ngoài phạm vi: mua PayOS bundle, fan-out enrollment, CRUD `PackageType`, `package_availability`.
- DB demo có thể chỉ có candidate CLASSROOM nếu chưa seed khóa online.
- Task 8 đã được commit riêng tại `99710d8`.

---

## Task 10: Upload avatar và đổi mật khẩu thật

- **Ngày:** 2026-07-13
- **Nhánh:** `phongdx`
- **Commit:** `17c2c2f`

### 1. Tóm tắt

Thay hai luồng giả trên trang `/profile` bằng chức năng production: học viên có thể tải lên/thay/xóa ảnh hồ sơ được lưu bền vững và đổi mật khẩu sau khi backend xác minh mật khẩu hiện tại. Avatar mới được đồng bộ ngay vào `AuthContext`, local storage và header.

### 2. Phạm vi thay đổi

- Trong phạm vi: avatar JPG/PNG/GIF tối đa 1 MB; lưu file cục bộ có cấu hình; thay/xóa file cũ; đổi mật khẩu có xác minh và validation mạnh; cập nhật UI/header.
- Ngoài phạm vi: crop ảnh, lưu cloud/object storage, thu hồi toàn bộ JWT sau đổi mật khẩu, 2FA và liên kết/hủy liên kết mạng xã hội.
- Tái sử dụng: `/api/user/me`, `UserService`, `PasswordEncoder`, `AuthContext`, trang `CompleteProfile` và mẫu storage file hiện có.

### 3. Tệp đã thay đổi

- `backend/src/main/java/fu/sap490/g23/backend/controller/UserController.java`
  - Why: Trang hồ sơ cần API thật cho avatar và mật khẩu.
  - What: Thêm upload/delete/read avatar và change-password endpoint.
- `backend/src/main/java/fu/sap490/g23/backend/service/user/impl/UserServiceImpl.java`
  - Why: Business rule phải nằm ở service.
  - What: Điều phối lưu/xóa avatar, cập nhật user, xác minh/hash mật khẩu.
- `backend/src/main/java/fu/sap490/g23/backend/service/user/AvatarStorageService.java` và `impl/AvatarStorageServiceImpl.java`
  - Why: Tách trách nhiệm filesystem và validation ảnh.
  - What: UUID filename, giới hạn 1 MB, whitelist JPG/PNG/GIF, kiểm tra nội dung ảnh/kích thước/path traversal, load/delete file.
- `backend/src/main/java/fu/sap490/g23/backend/dto/request/ChangePasswordRequest.java`
  - Why: Validation authoritative tại boundary.
  - What: Current/new/confirm password; mật khẩu mới 8–72 ký tự và đủ nhóm ký tự.
- `backend/src/main/java/fu/sap490/g23/backend/entity/User.java`, `dto/response/UserResponse.java`
  - Why: Persist và trả URL avatar cho mọi phiên đăng nhập/profile.
  - What: Thêm `avatarUrl`.
- `backend/src/main/java/fu/sap490/g23/backend/migration/UserAccountSchemaMigration.java`
  - Why: Schema additive nhất quán với dự án.
  - What: Thêm `users.avatar_url` nếu chưa có.
- `backend/src/main/java/fu/sap490/g23/backend/service/auth/impl/AuthServiceImpl.java`, `GoogleAuthServiceImpl.java`, `FacebookAuthServiceImpl.java`
  - Why: Auth response phải đồng nhất với `/api/user/me`.
  - What: Map `avatarUrl` vào `UserResponse`.
- `backend/src/main/java/fu/sap490/g23/backend/config/SecurityConfig.java`
  - Why: Thẻ `<img>` không gửi Bearer token.
  - What: Chỉ public GET URL avatar UUID; mutate vẫn bắt buộc đăng nhập.
- `frontend/src/api/authApi.js`
  - Why: API call phải nằm trong module API.
  - What: Thêm upload/delete avatar và change password.
- `frontend/src/pages/CompleteProfile.jsx`
  - Why: UI cũ chỉ preview ảnh và giả lập đổi mật khẩu bằng `setTimeout`.
  - What: Gọi API thật, validation/error/loading/success, confirm xóa ảnh, cập nhật user hiện tại.
- `frontend/src/components/ai-learning/Header.jsx`
  - Why: Avatar mới cần hiển thị ngay trên header.
  - What: Hiển thị `user.avatarUrl`, fallback icon cũ.
- `backend/src/test/java/fu/sap490/g23/backend/service/user/UserServiceImplTest.java`, `AvatarStorageServiceImplTest.java`
  - Why: Bảo vệ logic account/security và filesystem.
  - What: 8 case cho password, avatar lifecycle, file giả và traversal.

### 4. Thay đổi Backend

- Controller giữ mapping HTTP/DTO/resource; service xử lý business rule và transaction.
- Upload avatar lưu file UUID rồi persist URL; khi thay ảnh sẽ xóa file cũ; khi persistence lỗi sẽ dọn file mới.
- Delete avatar xóa URL database và file do ứng dụng quản lý.
- Password change kiểm tra BCrypt mật khẩu hiện tại, confirm trùng, mật khẩu mới khác mật khẩu cũ rồi hash trước khi lưu.
- `UserResponse` từ login thường, Google, Facebook và `/me` đều trả `avatarUrl`.

### 5. Thay đổi Frontend

- Upload thực hiện ngay khi chọn file; lỗi type/size được báo trước khi gọi backend.
- Thành công cập nhật preview, `AuthContext`, local storage và avatar header mà không reload trang.
- Xóa ảnh yêu cầu xác nhận và trả về avatar chữ/icon mặc định.
- Form đổi mật khẩu gọi backend thật; hiển thị lỗi mật khẩu hiện tại, policy hoặc confirm; xóa input khi thành công.

### 6. Thay đổi Database

- Bảng: `users`.
- Cột: `avatar_url VARCHAR(500)`, nullable.
- Quan hệ: không thay đổi.
- Migration: `UserAccountSchemaMigration`; additive `ADD COLUMN IF NOT EXISTS`, không cần backfill.

### 7. Thay đổi API

- `POST /api/user/me/avatar`
  - Request: `multipart/form-data`, part `file`.
  - Response: `UserResponse` có `avatarUrl` mới.
  - Authorization: mọi user đã đăng nhập; chỉ sửa tài khoản hiện tại.
  - Validation: JPG/JPEG/PNG/GIF, tối đa 1 MB, ảnh đọc được, tối đa 4096 x 4096 pixel.
- `DELETE /api/user/me/avatar`
  - Response: `UserResponse` với `avatarUrl = null`.
  - Authorization: mọi user đã đăng nhập; chỉ sửa tài khoản hiện tại.
- `GET /api/user/avatars/{fileName}`
  - Response: binary image và content type tương ứng.
  - Authorization: public read để trình duyệt render `<img>`; filename UUID/path được kiểm tra.
- `PUT /api/user/me/password`
  - Request: `{ "currentPassword", "newPassword", "confirmPassword" }`.
  - Response: `204 No Content`.
  - Authorization: mọi user đã đăng nhập; chỉ đổi tài khoản hiện tại.
  - Validation: current đúng; new/confirm trùng; new khác current; 8–72 ký tự, có hoa/thường/số/ký tự đặc biệt.

### 8. Thay đổi UI/UX

- Trang/role: `/profile`, Learner.
- Avatar: trạng thái đang xử lý, thành công, lỗi, fallback initials; nút xóa có confirm.
- Password: giữ show/hide input; thêm policy rõ ràng; submit thật; success/error actionable.
- Header: avatar cập nhật tức thì sau upload/xóa.
- Empty state: khi chưa có avatar dùng initials/icon hiện có.

### 9. Các bước test trên web

1. Restart backend để `UserAccountSchemaMigration` thêm cột, chạy frontend.
2. Đăng nhập learner `0386852628z@gmail.com` / `Password123!`, mở `/profile`.
3. Chọn JPG/PNG/GIF nhỏ hơn 1 MB; kiểm tra preview và header đổi ngay.
4. Refresh trang; kiểm tra avatar vẫn còn và URL ảnh tải được.
5. Upload ảnh khác; kiểm tra ảnh mới thay ảnh cũ.
6. Nhấn **Xóa ảnh**, xác nhận; kiểm tra fallback initials/icon và refresh vẫn không còn ảnh.
7. Đổi từ `Password123!` sang một mật khẩu mạnh mới; đăng xuất rồi đăng nhập bằng mật khẩu mới.
8. Negative: nhập sai mật khẩu hiện tại; backend trả lỗi và mật khẩu không đổi.
9. Validation: thử password thiếu hoa/số/ký tự đặc biệt, confirm lệch, password mới trùng cũ.
10. Avatar validation: thử file lớn hơn 1 MB, file không phải ảnh đổi đuôi `.png`, hoặc định dạng không hỗ trợ.
11. Permission: gọi POST/DELETE/password không có JWT phải nhận 401/403; GET avatar hợp lệ vẫn render công khai.

### 10. Kết quả mong đợi

Học viên quản lý avatar bền vững và đổi mật khẩu thật end-to-end. Dữ liệu avatar tồn tại sau refresh/login lại, header đồng bộ ngay, mật khẩu cũ hết hiệu lực sau đổi, và backend là nguồn validation cuối cùng.

### 11. Ghi chú / Rủi ro

- File avatar hiện lưu local theo `ENGLISHLAB_AVATARS_DIR`; production nhiều instance nên chuyển sang object storage/CDN.
- JWT hiện tại vẫn còn hiệu lực sau đổi mật khẩu theo kiến trúc stateless hiện có; chưa có token revocation/versioning.
- 2FA và tab tài khoản liên kết vẫn ngoài Task 10.
- Verification: backend compile pass; 8 focused tests pass; frontend production build pass.
- Full backend regression với `-Duser.timezone=Asia/Ho_Chi_Minh`: 96/97 pass; test seeder có sẵn `ToeicShowcaseClassroomSeederIntegrationTest` fail tại dòng 69 do danh sách enrollment rỗng, không thuộc luồng account Task 10.
- JVM mặc định `Asia/Saigon` trên máy hiện tại bị PostgreSQL từ chối; backend/test context cần timezone chuẩn `Asia/Ho_Chi_Minh`.
- Live API smoke với `waitlist.learner.a@test.vn`: upload avatar `200`, public read `200 image/png`, refresh vẫn có URL, file >1 MB `400`, định dạng không hỗ trợ `400`, upload thiếu JWT `403`, delete trả avatar null và file cũ không còn đọc được.
- Live password smoke: sai current/weak/confirm lệch/new trùng current đều `400`, thiếu JWT `403`, đổi mật khẩu `204`, mật khẩu cũ đăng nhập `401`, mật khẩu mới đăng nhập `200`. Sau test đã đổi lại `Password123!` (`204`) và xác nhận đăng nhập lại `200`.
- Manual UI smoke trên Browser tích hợp: đăng nhập learner thành công, mở `/profile`, dữ liệu tài khoản hiển thị đúng; validation đổi mật khẩu hiển thị đúng cho mật khẩu yếu, confirm lệch, mật khẩu mới trùng mật khẩu hiện tại và mật khẩu hiện tại sai. Browser tích hợp không hỗ trợ file chooser nên thao tác chọn avatar trực tiếp chưa thể click-test; lifecycle avatar đã được xác nhận qua live API smoke ở trên.

---

## Task 11: Tùy chọn thông báo email và trong ứng dụng

- **Ngày:** 2026-07-13
- **Nhánh:** `phongdx`
- **Commit:** `895f9e2`

### 1. Tóm tắt

Thêm tùy chọn thông báo theo tài khoản tại `/profile`: người dùng có thể bật/tắt riêng email nghiệp vụ và thông báo trong ứng dụng. Khi chưa có bản ghi preference, hệ thống mặc định bật cả hai kênh để giữ nguyên hành vi cũ.

### 2. Phạm vi thay đổi

- Trong phạm vi: lưu preference toàn cục theo user; API đọc/cập nhật; UI profile; kiểm tra preference trước khi tạo `AppNotification` và trước khi gửi email khóa học/lớp học.
- Ngoài phạm vi: tùy chọn theo từng loại sự kiện, push notification, SMS, xóa thông báo đã tạo và tắt email bảo mật/xác minh tài khoản.
- Email xác minh, quên mật khẩu và các email bảo mật vẫn luôn được gửi để tránh người dùng tự khóa luồng khôi phục tài khoản.

### 3. Tệp đã thay đổi

- `NotificationPreference.java`, repository, DTO request/response, service và implementation.
  - Why: cần một nguồn dữ liệu authoritative theo user cho hai kênh.
  - What: mặc định email/in-app bật; đọc lazily và tạo/cập nhật khi người dùng lưu.
- `NotificationPreferenceSchemaMigration.java`.
  - Why: dự án dùng migration Java additive.
  - What: tạo bảng, unique index và foreign key cascade đến `users`.
- `UserController.java`.
  - Why: profile cần API preference của tài khoản hiện tại.
  - What: thêm GET/PUT `/api/user/me/notification-preferences`.
- `AppNotificationService.java`, `AppNotificationServiceImpl.java`, `ClassroomNotificationServiceImpl.java`.
  - Why: mọi producer thông báo trong ứng dụng phải tôn trọng cùng một rule.
  - What: tập trung thao tác tạo notification vào service và bỏ qua persist khi user tắt kênh in-app.
- `CourseEnrollmentMailServiceImpl.java`, `ClassroomHomeworkMailServiceImpl.java`.
  - Why: email nghiệp vụ phải tôn trọng preference.
  - What: kiểm tra kênh email trước khi gửi.
- `frontend/src/api/authApi.js`, `frontend/src/pages/CompleteProfile.jsx`.
  - Why: người dùng cần xem và chỉnh preference trong profile.
  - What: tab **Thông báo**, hai switch, trạng thái loading/saving/success/error.
- `NotificationPreferenceServiceImplTest.java`, `NotificationPreferenceEnforcementTest.java`.
  - Why: bảo vệ default, persistence và enforcement của cả hai kênh.
  - What: 6 case unit test.

### 4. Thay đổi Backend

- Không có preference được hiểu là `emailEnabled=true`, `inAppEnabled=true`, bảo đảm tương thích dữ liệu hiện hữu.
- PUT tạo bản ghi lần đầu hoặc cập nhật bản ghi hiện có trong transaction.
- `AppNotificationService.createForUser(...)` là điểm tạo notification tập trung; nếu in-app bị tắt thì không ghi bản ghi mới.
- Các email enrollment và homework kiểm tra preference trước khi gọi mail sender.
- `AuthMailServiceImpl` không bị chặn vì phục vụ xác minh và khôi phục tài khoản.

### 5. Thay đổi Frontend

- Thêm tab **Thông báo** trên trang `/profile`.
- Hiển thị hai switch độc lập cho thông báo trong ứng dụng và email.
- Dữ liệu được tải từ backend; lưu có disable/loading và thông báo thành công/lỗi.
- UI ghi rõ email bảo mật, xác minh và khôi phục tài khoản vẫn được gửi.

### 6. Thay đổi Database

- Bảng mới: `notification_preferences`.
- Cột: `id`, `user_id`, `email_enabled`, `in_app_enabled`, `created_at`, `updated_at`.
- `user_id` unique và foreign key đến `users(id)` với `ON DELETE CASCADE`.
- Không cần backfill: thiếu bản ghi được service hiểu là bật cả hai kênh.

### 7. Thay đổi API

- `GET /api/user/me/notification-preferences`
  - Response: `{ "emailEnabled": true, "inAppEnabled": true }`.
  - Authorization: user đã đăng nhập; chỉ đọc preference của chính mình.
- `PUT /api/user/me/notification-preferences`
  - Request: `{ "emailEnabled": boolean, "inAppEnabled": boolean }`.
  - Response: preference sau cập nhật.
  - Validation: cả hai trường bắt buộc, không nhận `null`.
  - Authorization: user đã đăng nhập; chỉ sửa preference của chính mình.

### 8. Thay đổi UI/UX

- Trang/role: `/profile`, mọi user đã đăng nhập dùng trang profile hiện tại.
- Loading state khi đọc cấu hình; nút lưu bị khóa trong lúc request.
- Success/error hiển thị ngay trong tab; switch giữ giá trị hiện tại khi lưu thất bại.
- Hai kênh độc lập, không ép ít nhất một kênh phải bật.

### 9. Các bước test trên web

1. Restart backend để migration tạo `notification_preferences`, chạy frontend và đăng nhập.
2. Mở `/profile` → tab **Thông báo**; tài khoản chưa lưu preference phải thấy cả hai switch đang bật.
3. Tắt **Thông báo trong ứng dụng**, giữ email bật và nhấn **Lưu tùy chọn**; refresh trang, kiểm tra giá trị vẫn được giữ.
4. Phát sinh sự kiện lớp học/homework có AppNotification; xác nhận không có notification server mới cho user này.
5. Bật lại in-app, phát sinh sự kiện khác; xác nhận notification mới xuất hiện tại `/notifications`.
6. Tắt **Email**, lưu và phát sinh email enrollment/homework; xác nhận service không gửi mail nghiệp vụ.
7. Bật lại email và xác nhận email nghiệp vụ được gửi bình thường.
8. Khi email nghiệp vụ đang tắt, thử quên mật khẩu/xác minh tài khoản; email bảo mật vẫn phải được gửi.
9. Gọi PUT thiếu một trường hoặc truyền `null`; backend phải trả lỗi validation.
10. Gọi GET/PUT không có JWT; backend phải từ chối.

### 10. Kết quả mong đợi

Preference tồn tại sau refresh/login lại; mỗi kênh chỉ ảnh hưởng các thông báo mới thuộc kênh tương ứng. Luồng tạo `AppNotification` và email nghiệp vụ tôn trọng cấu hình, trong khi email bảo mật vẫn hoạt động.

### 11. Ghi chú / Rủi ro

- Preference hiện là toàn cục theo kênh, chưa chi tiết theo loại sự kiện.
- Tắt in-app không xóa hoặc ẩn notification đã tồn tại.
- Notification chỉ nằm trong React context cục bộ không phải `AppNotification` server nên không thuộc enforcement backend của Task 11.
- Verification: frontend ESLint pass; frontend production build pass; Spring context pass; 6 focused backend tests pass.
- Full backend regression: 102/103 pass. Lỗi duy nhất là test có sẵn `ToeicShowcaseClassroomSeederIntegrationTest` tại dòng 69 vì enrollment list rỗng, không thuộc luồng notification preference.
- Maven wrapper PowerShell trên máy hiện tại gặp lỗi nội bộ; verification dùng Maven 3.9.15 đã cache, offline repository và timezone `Asia/Ho_Chi_Minh`.

---

## Task 12: Support ticket cho học viên và Manager/Admin

- **Ngày:** 2026-07-13
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit (working tree sau `895f9e2`)

### 1. Tóm tắt

Xây dựng luồng support ticket end-to-end: học viên tạo và theo dõi yêu cầu hỗ trợ tại `/support`; Manager/Admin tiếp nhận hàng đợi tại `/manager/support-tickets`, nhận xử lý, đổi ưu tiên/trạng thái và trao đổi theo hội thoại. Phản hồi từ staff tạo AppNotification cho học viên qua cơ chế preference của Task 11.

### 2. Phạm vi thay đổi

- Trong phạm vi: ticket theo category/status/priority; lịch sử message; ownership; claim; learner đóng/mở lại; Manager/Admin xử lý; filter/search; AppNotification khi cập nhật.
- Ngoài phạm vi: upload tệp đính kèm, SLA/escalation tự động, email support, phân công cho staff khác, xóa ticket và tích hợp hệ thống help desk bên ngoài.
- Tái sử dụng: `User`, `AppNotificationService`, role hiện có, `LearnerPageShell`, `TrainingManagerLayout`, `BrandedSelect` và form styles.

### 3. Tệp đã thay đổi

- `entity/support/SupportTicket.java`, `SupportTicketMessage.java` và các enum.
  - Why: cần lưu trạng thái nghiệp vụ và toàn bộ hội thoại.
  - What: requester, assignee, resolver, subject, category, priority, status, audit timestamps và messages.
- `repository/support/*`, `service/support/*`.
  - Why: tách persistence, business rule và authorization khỏi controller.
  - What: query queue có filter, entity graph tránh N+1, ownership/role/state transition, claim/reply/update và notification.
- `controller/support/StudentSupportTicketController.java`, `ManagerSupportTicketController.java`.
  - Why: learner và staff cần API scope riêng.
  - What: CRUD nghiệp vụ theo `/api/student` và `/api/manager`.
- `dto/request/support/*`, `dto/response/support/*`.
  - Why: validation tại HTTP boundary và response không lộ entity.
  - What: create/reply/status/update requests; ticket/message responses.
- `SupportTicketSchemaMigration.java`.
  - Why: schema additive theo convention dự án.
  - What: hai bảng, foreign keys, indexes và CHECK constraints.
- `SupportTicketServiceImplTest.java`.
  - Why: bảo vệ ownership, lifecycle, assignment và notification.
  - What: 6 test service.
- `frontend/src/api/supportApi.js`, `utils/supportTicketLabels.js`.
  - Why: gom API và nhãn nghiệp vụ dùng chung.
  - What: learner/staff client methods, label/options/status styles/time formatter.
- `SupportTicketsPage.jsx`, `ManagerSupportTicketsPage.jsx`.
  - Why: cung cấp hai workspace đúng role.
  - What: form tạo, danh sách/detail hội thoại, reply/close/reopen; queue filter/search/claim/update/reply.
- `App.jsx`, `Header.jsx`, `TrainingManagerUi.jsx`.
  - Why: route và điều hướng phải truy cập được đúng role.
  - What: thêm `/support`, `/manager/support-tickets`; learner menu và Manager/Admin sidebar.

### 4. Thay đổi Backend

- Learner chỉ xem và thao tác ticket do chính mình tạo.
- Manager/Admin có quyền xem toàn bộ queue, nhận xử lý và cập nhật ticket; Training Manager không có quyền support staff.
- Ticket mới có `OPEN` và `NORMAL`; staff reply chuyển sang `WAITING_FOR_LEARNER`; learner reply chuyển về `IN_PROGRESS` nếu đã có assignee.
- `RESOLVED`/`CLOSED` là terminal; phải mở lại trước khi phản hồi tiếp.
- Staff đổi sang `IN_PROGRESS` khi chưa có assignee sẽ tự nhận ticket.
- Staff reply hoặc đổi status tạo AppNotification cho requester; learner reply tạo notification cho assignee nếu có.
- Queue và detail dùng entity graph cho requester/assignee/author để tránh N+1 query.

### 5. Thay đổi Frontend

- Learner `/support`: tạo ticket theo nhóm, xem lịch sử, phản hồi, đóng hoặc mở lại.
- Manager/Admin `/manager/support-tickets`: lọc status/priority, tìm mã/chủ đề/học viên, nhận xử lý, đổi status/priority và phản hồi.
- Hai màn hình dùng chung nhãn category/status/priority; có loading, empty, error, success và disabled state.
- Sidebar support chỉ hiển thị cho `MANAGER`/`ADMIN`.

### 6. Thay đổi Database

- Bảng `support_tickets`: requester/assignee/resolver FK, subject, category, status, priority, resolved/audit timestamps.
- Bảng `support_ticket_messages`: ticket FK cascade, author FK, body, created timestamp.
- Index: requester + updated time; queue status/priority/updated time; message ticket/created time.
- CHECK constraints giới hạn category, status và priority theo enum backend.
- Migration additive/idempotent: `SupportTicketSchemaMigration`.

### 7. Thay đổi API

- Learner:
  - `POST /api/student/support-tickets`
  - `GET /api/student/support-tickets`
  - `GET /api/student/support-tickets/{ticketId}`
  - `POST /api/student/support-tickets/{ticketId}/replies`
  - `PATCH /api/student/support-tickets/{ticketId}/status`
- Manager/Admin:
  - `GET /api/manager/support-tickets?status=&priority=`
  - `GET /api/manager/support-tickets/{ticketId}`
  - `POST /api/manager/support-tickets/{ticketId}/claim`
  - `POST /api/manager/support-tickets/{ticketId}/replies`
  - `PATCH /api/manager/support-tickets/{ticketId}`
- Validation: subject 5–160 ký tự; message tạo 10–5000; reply tối đa 5000 và không blank; category bắt buộc; update staff phải có status hoặc priority.
- Authorization: learner ownership tại service; queue chỉ `MANAGER`/`ADMIN` tại SecurityConfig path và service role check.

### 8. Thay đổi UI/UX

- Learner mở **Trung tâm hỗ trợ** từ avatar menu; form ticket có category và hướng dẫn mô tả rõ ràng.
- Ticket list hiển thị mã, trạng thái, subject và thời gian cập nhật; hội thoại phân biệt learner/staff bằng hướng và màu bubble.
- Terminal ticket ẩn reply form và hiển thị hành động mở lại.
- Staff queue có bộ lọc/tìm kiếm, badge status/priority, assignee và action nhận xử lý.
- Responsive: danh sách/detail chuyển từ hai cột sang một cột trên màn hình nhỏ.

### 9. Các bước test trên web

1. Restart backend để migration chạy; chạy frontend.
2. Đăng nhập learner `0386852628z@gmail.com` / `Password123!`, mở `/support`.
3. Tạo ticket với subject/category/message hợp lệ; kiểm tra ticket mới có `Mới gửi`, ưu tiên `Bình thường` và message đầu tiên.
4. Refresh trang; xác nhận ticket và hội thoại còn nguyên.
5. Đăng xuất, đăng nhập Manager `classroom.manager@englishlab.vn` / `Password123!`, mở `/manager/support-tickets`.
6. Lọc/tìm ticket vừa tạo; nhấn **Nhận xử lý**, đổi ưu tiên và gửi phản hồi.
7. Đăng nhập lại learner; kiểm tra notification mới và ticket chuyển `Chờ học viên`; phản hồi ticket.
8. Manager kiểm tra ticket chuyển `Đang xử lý`, đánh dấu `Đã giải quyết`.
9. Learner mở ticket đã giải quyết; reply bị ẩn, nhấn **Mở lại ticket** rồi phản hồi tiếp.
10. Learner nhấn **Đóng ticket**; xác nhận terminal state.
11. Negative: learner A gọi detail ticket learner B phải bị từ chối; Training Manager gọi `/api/manager/support-tickets` phải 403.
12. Validation: subject dưới 5, message dưới 10, reply blank, enum sai và staff PATCH body rỗng phải bị từ chối.

### 10. Kết quả mong đợi

Học viên và đội ngũ Manager/Admin có một kênh hỗ trợ bền vững, phân quyền đúng và giữ đầy đủ lịch sử. Ticket có lifecycle/priority/assignee rõ ràng, tồn tại sau refresh, có notification khi staff cập nhật và không cho truy cập chéo dữ liệu learner.

### 11. Ghi chú / Rủi ro

- Chưa có attachment, email, SLA timer, bulk action hoặc assign cho staff khác.
- Ticket không bị xóa; terminal ticket có thể mở lại để giữ audit trail.
- AppNotification tôn trọng `inAppEnabled`; khi user tắt kênh này, ticket vẫn cập nhật nhưng không tạo notification.
- Verification: frontend ESLint pass; production build pass; Spring context/migration pass; 6/6 focused backend tests pass.
- Full backend regression: 108/109 pass. Lỗi duy nhất là test có sẵn `ToeicShowcaseClassroomSeederIntegrationTest` dòng 69 do enrollment list rỗng, không thuộc module support.
- Live manual API smoke sau khi restart backend: ticket `#2` đi qua create `OPEN/NORMAL`, learner list/detail, Manager queue, claim `IN_PROGRESS`, priority `HIGH`, staff reply `WAITING_FOR_LEARNER`, AppNotification, learner reply, resolve, chặn reply terminal, learner reopen và close. Validation create trả `400`; truy cập chéo learner trả `400`; Training Manager và request thiếu JWT vào staff queue trả `403`.
- Browser click-test chưa chạy được vì Browser runtime trên Windows không khởi tạo được (`CreateProcessWithLogonW failed`); đây là giới hạn công cụ, không phải lỗi frontend/backend. Cần mở lại Browser tích hợp để xác nhận trực quan hai route.
- Maven cache trên Windows đôi lúc báo `AccessDeniedException` khi compiler đóng JAR; chạy lại sau khi class đã compile thành công cho kết quả test nêu trên.

---

## Task 1 (bổ sung): Chống tạo lịch học bù trùng khi duyệt đồng thời

- **Ngày:** 2026-07-14
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit (working tree sau `895f9e2`)

### 1. Tóm tắt

Bổ sung cơ chế khóa transaction theo ngày học để loại bỏ race condition “kiểm tra trước, lưu sau”. Khi hai yêu cầu học bù cùng tác động một ngày được duyệt đồng thời, backend xử lý tuần tự trong transaction; request thứ hai kiểm tra lại trên dữ liệu mới nhất và bị chặn nếu trùng giáo viên, phòng hoặc học viên. Đồng thời khóa riêng bản ghi change request để một yêu cầu không thể bị duyệt hai lần.

### 2. Phạm vi thay đổi

- Trong phạm vi: duyệt change request, tạo buổi học, cập nhật lịch buổi học và chống duyệt lặp cùng request.
- Ngoài phạm vi: thay đổi quy tắc Training Manager chủ động **Duyệt và ghi đè xung đột**; thao tác này vẫn là ngoại lệ có chủ đích và bắt buộc ghi chú.
- Tái sử dụng: transaction hiện có, `ClassroomConflictService`, API và UI change-request hiện có.

### 3. Tệp đã thay đổi

- `service/classroom/ClassroomScheduleLockService.java`, `impl/ClassroomScheduleLockServiceImpl.java`
  - Why: cần một khóa dùng chung cho mọi thao tác ghi lịch trong cùng ngày.
  - What: PostgreSQL transaction advisory lock theo ngày; khóa nhiều ngày theo thứ tự cố định để tránh deadlock.
- `repository/classroom/ClassroomChangeRequestRepository.java`
  - Why: ngăn hai transaction cùng xử lý một request đang `PENDING`.
  - What: thêm truy vấn `PESSIMISTIC_WRITE` theo ID.
- `service/classroom/impl/ClassroomChangeRequestServiceImpl.java`
  - Why: conflict check khi duyệt phải chạy sau khi transaction đã giữ khóa lịch.
  - What: khóa ngày nguồn/ngày đề xuất, đọc request bằng row lock, sau đó mới kiểm tra và áp dụng.
- `service/classroom/impl/ClassroomOfferingServiceImpl.java`
  - Why: đường tạo/cập nhật session trực tiếp cũng có thể gặp cùng race condition.
  - What: khóa ngày trước conflict check và save; cập nhật lịch khóa cả ngày cũ lẫn ngày mới.
- `ClassroomChangeRequestServiceImplTest.java`
  - Why: bảo vệ thứ tự khóa → kiểm tra → tạo session và chặn duyệt lặp.
  - What: cập nhật mock row-lock, thêm assertion thứ tự xử lý và negative test request đã review.

### 4. Thay đổi Backend

- Mỗi ngày lịch được ánh xạ thành một PostgreSQL transaction advisory lock riêng; khóa tự giải phóng khi commit/rollback.
- Hai thao tác trên cùng ngày phải chờ nhau; thao tác ở ngày khác vẫn chạy độc lập.
- Sau khi chờ khóa, conflict check chạy lại trong transaction nên nhìn thấy session mà transaction trước vừa commit.
- `approve` và `reject` khóa row change request trước khi kiểm tra trạng thái `PENDING`.

### 5. Thay đổi Frontend

Không thay đổi frontend. UI hiện có tiếp tục hiển thị lỗi conflict do backend trả về và cho phép tải lại hàng đợi.

### 6. Thay đổi Database

Không thay đổi bảng, cột hoặc migration. Dùng PostgreSQL transaction advisory lock, không lưu thêm dữ liệu khóa.

### 7. Thay đổi API

Không thêm hoặc đổi endpoint/request/response. Các endpoint approve và session hiện có được tăng cường tính nhất quán khi gọi đồng thời.

### 8. Thay đổi UI/UX

Không đổi bố cục. Trong trường hợp hai lượt duyệt cạnh tranh, một lượt thành công; lượt còn lại nhận thông báo xung đột thay vì âm thầm tạo lịch trùng.

### 9. Các bước test trên web

1. Restart backend và chạy frontend; chuẩn bị hai lớp có thể tạo yêu cầu học bù.
2. Giáo viên A gửi yêu cầu học bù vào cùng ngày, giờ và phòng mục tiêu; giáo viên B gửi yêu cầu thứ hai trùng slot/phòng đó trước khi request A được duyệt.
3. Đăng nhập Training Manager, mở hai cửa sổ/tab tại hàng đợi yêu cầu và chọn lần lượt hai request.
4. Ở cả hai tab giữ chế độ duyệt thường, không chọn **Duyệt và ghi đè xung đột**; bấm **Duyệt** gần như đồng thời.
5. Tải lại hàng đợi và lịch hai lớp; xác nhận chỉ một request `APPLIED`, request còn lại vẫn `PENDING` và chỉ có một session `MAKEUP` tại slot/phòng đó.
6. Mở cùng một request `PENDING` ở hai tab và duyệt gần như đồng thời; xác nhận lần đầu thành công, lần sau báo request không còn chờ duyệt và không tạo session thứ hai.
7. Negative hợp lệ: tạo hai request cùng ngày/giờ nhưng khác giáo viên, khác phòng và không có học viên trùng; duyệt cả hai, xác nhận cả hai thành công.
8. Kiểm tra ngoại lệ có chủ đích: khi conflict được hiển thị, chỉ **Duyệt và ghi đè xung đột** kèm ghi chú mới cho phép áp dụng request thứ hai.

### 10. Kết quả mong đợi

Không thể phát sinh lịch học bù trùng do hai thao tác chạy đồng thời ngoài ý muốn. Kết quả luôn tương đương xử lý tuần tự: lượt đến sau nhìn thấy dữ liệu mới và tuân theo conflict rule hiện có.

### 11. Ghi chú / Rủi ro

- Cơ chế khóa phụ thuộc PostgreSQL, phù hợp database production hiện tại của dự án.
- Advisory lock chỉ có hiệu lực khi mọi đường ghi lịch dùng service chung; các câu SQL ghi thẳng ngoài ứng dụng không được bảo vệ.
- Training Manager vẫn có quyền ghi đè conflict có chủ đích khi nhập ghi chú; đây không còn là xung đột phát sinh ngoài ý muốn.
- Verification tự động: `ClassroomChangeRequestServiceImplTest` đạt 5/5 test; Spring context pass; integration test thực thi advisory lock trực tiếp trên PostgreSQL pass.
- Maven cache Windows đôi lúc báo lỗi truy cập JAR; chạy lại cùng lệnh cho kết quả pass.

---

## Task 5 (bổ sung): Thanh toán trước khi hoàn tất đăng ký lớp

- **Ngày:** 2026-07-14
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit

### 1. Tóm tắt

Sửa thứ tự nghiệp vụ Task 5 thành **đăng ký tạm → thanh toán → tự động xếp lớp/cấp quyền học**. Training Manager không còn phải duyệt hồ sơ trước khi học viên thanh toán và không thể xếp lớp khi học phí chưa được thanh toán đủ. Lớp đã đủ chỗ đưa học viên vào danh sách chờ, chưa thu tiền.

### 2. Phạm vi thay đổi

- Backend, frontend, API behavior và UI/UX của đăng ký lớp/học phí.
- Giữ nguyên endpoint, bảng dữ liệu và tích hợp PayOS hiện có.
- Giữ tương thích hồ sơ cũ `PENDING_CONFIRMATION` để học viên vẫn có thể thanh toán.

### 3. Tệp đã thay đổi

- `ClassroomEnrollment.java`: trạng thái mặc định mới là `PENDING_TUITION_PAYMENT`.
- `ClassroomOfferingServiceImpl.java`: tạo hồ sơ chờ thanh toán, không cấp package access trước; lớp miễn phí xếp ngay; chặn ghi học phí/xếp lớp sai trạng thái.
- `PaymentServiceImpl.java`, `TuitionProofServiceImpl.java`: cho hồ sơ cũ thanh toán nhưng chặn `WAITLIST`.
- `TrainingManagerOpsServiceImpl.java`: chỉ `FULLY_PAID` được tính sẵn sàng xếp lớp.
- `ClassroomPublicDetailPage.jsx`, `MyClassroomDetailPage.jsx`: đăng ký xong mở PayOS; danh sách chờ không hiện thanh toán/minh chứng.
- `TrainingManagerRegistrationPanel.jsx`, `TrainingManagerDashboardPage.jsx`: bỏ bước duyệt trước thanh toán; chỉ mời người trong waitlist thanh toán khi có chỗ; chỉ xếp lớp sau khi trả đủ.
- `ClassroomOfferingServiceImplWaitlistTest.java`, `PaymentServiceImplClassroomTuitionTest.java`: regression test cho luồng mới.

### 4. Thay đổi Backend

- Lớp còn chỗ và có học phí: tạo hồ sơ `PENDING_TUITION_PAYMENT`, chưa tạo `PackageEnrollment`, chưa tạo gradebook và chưa có quyền học.
- PayOS ghi nhận đủ học phí: chuyển `FULLY_PAID` rồi tự động thử xếp lớp; quyền học chỉ được tạo tại bước `ASSIGNED`.
- Lớp miễn phí: xếp lớp ngay, không mở PayOS.
- Lớp đầy: tạo `WAITLIST`; PayOS, upload minh chứng và ghi học phí thủ công đều bị chặn cho tới khi Training Manager mời thanh toán.
- API xếp lớp thủ công chỉ chấp nhận `FULLY_PAID`.

### 5. Thay đổi Frontend

- Nút chính đổi thành **Đăng ký và thanh toán**; sau khi tạo hồ sơ, frontend mở link PayOS ngay.
- Bỏ lựa chọn **Giữ chỗ trước** và thông điệp chờ Training Manager duyệt.
- Học viên trong waitlist chỉ thấy vị trí chờ, không thấy khu vực thanh toán/minh chứng.
- Training Manager không còn tab/action duyệt hồ sơ mới; action waitlist là **Mời thanh toán khi có chỗ**.

### 6. Thay đổi Database

Không thêm bảng/cột/migration. Enum và schema hiện có được tái sử dụng; thay đổi chỉ nằm ở trạng thái khởi tạo và validation nghiệp vụ.

### 7. Thay đổi API

Không đổi path/request/response. Thay đổi hành vi:

- `POST /api/student/classrooms/{id}/register`: trả `PENDING_TUITION_PAYMENT`, `WAITLIST` hoặc `ASSIGNED` đối với lớp miễn phí.
- `POST /api/student/payments/payos/link`: nhận hồ sơ chờ thanh toán/legacy; từ chối `WAITLIST`.
- API upload minh chứng và ghi học phí thủ công: từ chối `WAITLIST`.
- API assign enrollment: từ chối mọi trạng thái khác `FULLY_PAID` (trừ hồ sơ đã có quyền học được trả nguyên trạng).

### 8. Thay đổi UI/UX

Luồng học viên ngắn hơn và đúng thứ tự: chọn lớp, thanh toán, sau đó mới vào lớp. Trường hợp PayOS chưa mở được, hồ sơ vẫn ở trạng thái chờ thanh toán và học viên có thể thử lại trong khu vực học phí. Danh sách chờ không bị yêu cầu trả tiền khi chưa có chỗ.

### 9. Các bước test trên web

1. Restart backend/frontend; chọn một lớp `UPCOMING`, còn chỗ và có học phí.
2. Đăng nhập học viên chưa đăng ký lớp, mở chi tiết lớp và nhấn **Đăng ký và thanh toán**.
3. Xác nhận frontend chuyển sang PayOS; nếu quay lại/chưa trả, hồ sơ là **Chờ thanh toán học phí** và chưa truy cập được nội dung lớp.
4. Hoàn tất PayOS hoặc dùng minh chứng/Training Manager ghi nhận đủ tiền; tải lại và xác nhận enrollment được xếp lớp, có quyền học.
5. Training Manager thử xếp một hồ sơ `PENDING_TUITION_PAYMENT`: backend phải từ chối; nút xếp lớp không xuất hiện.
6. Làm đầy một lớp rồi đăng ký bằng học viên khác: trạng thái phải là `WAITLIST`, có vị trí chờ và không có nút PayOS/form minh chứng.
7. Training Manager nhấn **Mời thanh toán khi có chỗ** trong khi lớp vẫn đầy: học viên vẫn ở waitlist; sau khi có chỗ, action chuyển hồ sơ sang chờ thanh toán.
8. Với lớp miễn phí còn chỗ, đăng ký và xác nhận được xếp lớp ngay, không chuyển PayOS.

### 10. Kết quả mong đợi

Không học viên nào được cấp quyền học hoặc được Training Manager xếp lớp trước khi thanh toán đủ, ngoại trừ lớp miễn phí. Không thu tiền người đang ở danh sách chờ. Bước duyệt đăng ký trước thanh toán được loại bỏ khỏi luồng mới.

### 11. Ghi chú / Rủi ro

- Hồ sơ `PENDING_CONFIRMATION` cũ vẫn thanh toán được để không làm kẹt dữ liệu đã tồn tại.
- Nếu nhiều học viên thanh toán gần đồng thời cho suất cuối, bước assign khóa bản ghi lớp và kiểm tra lại sức chứa; giao dịch đến sau có thể cần xử lý vận hành/hoàn tiền nếu lớp vừa đầy. Cơ chế giữ suất PayOS có thời hạn chưa nằm trong Task 5 hiện tại.
- Focused backend tests pass; frontend production build pass. Sau khi clean, full backend suite đạt 114/115; lỗi duy nhất có sẵn là `ToeicShowcaseClassroomSeederIntegrationTest` dòng 69 do enrollment list rỗng.

---

## Task 4 / 6 / 7: Hủy theo quyết định leader

- **Ngày:** 2026-07-14
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit

### 1. Tóm tắt

Leader xác nhận **không cần** Task 4 (sắp xếp/vị trí waitlist), Task 6 (hoàn tiền đơn khóa + biên lai PDF), Task 7 (NEED_REFUND settlement). Đã gỡ giao diện và endpoint công khai tương ứng; giữ WAITLIST cơ bản phục vụ Task 5.

### 2. Phạm vi thay đổi

- Frontend surfaces + HTTP controllers cho Task 4/6/7.
- Docs handoff/changelog.
- Không drop cột DB lịch sử.

### 3–8. Tệp / Backend / Frontend / DB / API / UI

- Gỡ TM reorder waitlist, badge vị trí `#N`.
- Gỡ tải biên lai PDF (HV) và hoàn tiền đơn khóa (CM analytics).
- Gỡ tab/UI NEED_REFUND và `POST .../settlement/resolve`, `PUT .../waitlist/order`, receipt/refund payment endpoints.
- WAITLIST + mời thanh toán Task 5 vẫn giữ.

### 9. Các bước test trên web

1. TM: không còn nút lên/xuống waitlist; không tab Cần hoàn tiền.
2. HV: lịch sử giao dịch không còn nút biên lai PDF.
3. CM analytics: không còn bảng hoàn tiền đơn khóa.
4. Lớp đầy → đăng ký vẫn vào WAITLIST (Task 5).

### 10. Kết quả mong đợi

Sản phẩm không còn expose Task 4/6/7; Task 5 payment-first không bị phá.

### 11. Ghi chú / Rủi ro

Service/unit test backend cũ của Task 4/6/7 có thể còn trong repo nhưng không còn endpoint UI. Migration DB cũ không rollback.

---

## Phụ lục: Mẫu cho task tiếp theo

Sau mỗi task hoàn thành, nối thêm:

```markdown
## Task N: [Tên task]

### 1. Tóm tắt
### 2. Phạm vi thay đổi
### 3. Tệp đã thay đổi
### 4. Thay đổi Backend
### 5. Thay đổi Frontend
### 6. Thay đổi Database
### 7. Thay đổi API
### 8. Thay đổi UI/UX
### 9. Các bước test trên web
### 10. Kết quả mong đợi
### 11. Ghi chú / Rủi ro
```

Không được bỏ các mục 3–7, 9, 10.
