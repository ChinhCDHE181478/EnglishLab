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
- **Commit:** Chưa commit (code trên working tree sau `b13533b`)

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
- Commit Task 8 riêng khi sẵn sàng; cập nhật hash commit vào mục này.

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
