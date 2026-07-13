# EnglishLab Implementation Changelog

This document records implementation evidence for completed features. New entries must be appended; existing entries must not be overwritten.

---

## Task 1 — UI yêu cầu buổi học bù (Makeup Session Request)

- **Date:** 2026-07-12
- **Branch:** `phongdx`
- **Commit hash:** Not committed yet (base HEAD: `be2136c`)

### Summary

Completed the teacher-facing makeup-session request flow by extending the existing classroom change-request architecture. Teachers can select the source session, choose a valid makeup date, time slot, and room, then submit the request for Training Manager approval. The existing generic approval flow creates a new session with `MAKEUP` status.

### Changed files

- `frontend/src/components/teacher/TeacherChangeRequestForm.jsx`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomChangeRequestServiceImpl.java`
- `backend/src/test/java/fu/sap490/g23/backend/service/classroom/ClassroomChangeRequestServiceImplTest.java`
- `docs/CHANGELOG_IMPLEMENTATION.md`

### Backend changes

- Added explicit validation for `CREATE_MAKEUP_SESSION`.
- Required the source session, makeup date, start time, and end time.
- Allowed completed or cancelled source sessions to provide context without producing a source-session lock conflict.
- Preserved conflict checks for the proposed teacher, room, and enrolled learners.
- Reused the existing Training Manager approval path and `ClassroomOfferingService.createSession`.

### Frontend changes

- Added `CREATE_MAKEUP_SESSION` to `TeacherChangeRequestForm`.
- Reused the existing reschedule date, time-slot, room-availability, and conflict-check UI.
- Allowed all source sessions to be selected for makeup requests, including completed or cancelled sessions.
- Added makeup-specific labels and reset behavior when switching request types.
- Submitted the existing `CreateChangeRequestRequest` payload without introducing duplicate API logic.

### Database changes

- None.
- Existing `classroom_change_requests` and `classroom_sessions` structures are reused.

### Testing checklist

- [x] Frontend ESLint passes.
- [x] Frontend production build passes.
- [x] Backend compilation passes.
- [x] Makeup request accepts a completed source session while checking only the proposed schedule.
- [x] Makeup request rejects missing schedule data.
- [x] Backend tests pass: 2 tests, 0 failures, 0 errors.
- [x] IDE diagnostics report no errors in changed files.
- [ ] Manual browser verification with Teacher and Training Manager accounts.

### Important notes

- No new endpoint was introduced.
- The feature reuses the current create/check-conflict/approve/reject change-request APIs.
- The source session is contextual only; approval creates a separate session with `ClassroomSessionStatus.MAKEUP`.

---

## Task 2 — Giáo viên chỉnh sửa gradebook thủ công (Manual Gradebook Editing)

- **Date:** 2026-07-12
- **Branch:** `phongdx`
- **Commit hash:** Not committed yet (base HEAD: `be2136c`)

### Summary

Completed manual gradebook editing on `TeacherClassroomPage` by reusing the existing `updateGradebookEntry` API. Teachers can edit homework, quiz, attendance, participation, and final-result scores, add a comment, and save one learner entry at a time without reloading the full page.

### Changed files

- `frontend/src/components/teacher/TeacherGradebookSection.jsx`
- `frontend/src/pages/teacher/TeacherClassroomPage.jsx`
- `backend/src/main/java/fu/sap490/g23/backend/dto/request/classroom/UpdateGradebookRequest.java`
- `backend/src/test/java/fu/sap490/g23/backend/service/classroom/ClassroomGradebookServiceImplTest.java`
- `docs/CHANGELOG_IMPLEMENTATION.md`

### Backend changes

- Reused `ClassroomGradebookServiceImpl.updateEntry` and the existing teacher gradebook endpoint.
- Added Jakarta Validation constraints for all editable numeric fields.
- Restricted homework, quiz, participation, and final-result scores to `0–10`.
- Restricted attendance percentage to `0–100`.
- Restricted numeric precision to two decimal places.
- Limited teacher comments to 2000 characters.
- Added a service test covering manual score updates and the `PENDING` to `GRADED` transition.

### Frontend changes

- Extracted gradebook UI into `TeacherGradebookSection` to keep `TeacherClassroomPage` maintainable.
- Added an inline edit panel for each learner.
- Added inputs for homework, quiz, attendance, participation, final result, and teacher comment.
- Added client-side range and comment-length validation.
- Reused `classroomApi.updateGradebookEntry`.
- Replaced only the updated learner row in local state after a successful save.
- Preserved the existing gradebook publication workflow and visual language.

### Database changes

- None.
- Existing `classroom_gradebook_entries` columns are reused.

### Testing checklist

- [x] Frontend ESLint passes.
- [x] Frontend production build passes.
- [x] Backend test compilation passes.
- [x] Manual scores are persisted through `ClassroomGradebookServiceImpl.updateEntry`.
- [x] A pending gradebook entry becomes `GRADED` after manual editing.
- [x] Task 1 and Task 2 classroom tests pass together: 3 tests, 0 failures, 0 errors.
- [x] IDE diagnostics report no errors in changed files.
- [ ] Manual browser verification with a Teacher account.
- [ ] Verify learner visibility after publishing the edited gradebook.

### Important notes

- No new endpoint was introduced.
- Existing endpoint: `PUT /api/teacher/classrooms/{id}/gradebook`.
- Editing an already published entry preserves its current `PUBLISHED` status, so the corrected value remains visible to the learner.
- The quiz score remains editable as a separate gradebook component.

---

## Phụ lục tiếng Việt cho Task 1 và Task 2

Phụ lục này được thêm mới theo yêu cầu ghi changelog bằng tiếng Việt. Nội dung cũ phía trên được giữ nguyên để bảo toàn lịch sử.

### Task 1 — UI yêu cầu buổi học bù

- Hoàn thiện luồng để giáo viên chọn buổi học gốc, ngày học bù, khung giờ và phòng học rồi gửi Training Manager duyệt.
- Tái sử dụng kiến trúc change request và API kiểm tra xung đột hiện có.
- Cho phép dùng buổi đã hoàn thành hoặc đã hủy làm ngữ cảnh cho yêu cầu học bù.
- Khi được duyệt, hệ thống tạo một buổi học mới có trạng thái `MAKEUP`.
- Không thay đổi cấu trúc cơ sở dữ liệu.
- Đã kiểm tra ESLint, frontend build, backend compile và 2 backend tests.

### Task 2 — Giáo viên chỉnh sửa gradebook thủ công

- Tách giao diện bảng điểm thành `TeacherGradebookSection`.
- Cho phép sửa điểm homework, quiz, chuyên cần, tham gia, kết quả cuối cùng và nhận xét.
- Bổ sung validation ở cả frontend và DTO backend.
- Tái sử dụng endpoint `PUT /api/teacher/classrooms/{id}/gradebook`.
- Không thay đổi cấu trúc cơ sở dữ liệu.
- Đã kiểm tra ESLint, frontend build và 3 backend tests của Task 1–2.

---

## Bổ sung Task 2 — Thu hồi công bố bảng điểm

- **Ngày:** 2026-07-12
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit

### Tóm tắt

Đã bổ sung khả năng thu hồi bảng điểm đã công bố để giáo viên tạm ẩn kết quả khỏi học viên trong lúc kiểm tra hoặc chỉnh sửa. Cả thao tác công bố và thu hồi đều yêu cầu xác nhận trước khi gửi request.

### Tệp đã thay đổi

- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/ClassroomGradebookService.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomGradebookServiceImpl.java`
- `backend/src/main/java/fu/sap490/g23/backend/controller/classroom/TeacherClassroomController.java`
- `backend/src/test/java/fu/sap490/g23/backend/service/classroom/ClassroomGradebookServiceImplTest.java`
- `frontend/src/api/classroomApi.js`
- `frontend/src/pages/teacher/TeacherClassroomPage.jsx`
- `frontend/src/components/teacher/TeacherGradebookSection.jsx`
- `docs/CHANGELOG_IMPLEMENTATION.md`

### Thay đổi chức năng

- Thêm nút `Thu hồi công bố` nằm cạnh nút `Công bố bảng điểm`.
- Thêm hộp xác nhận trước cả hai thao tác.
- Nút công bố bị vô hiệu hóa khi toàn bộ bảng điểm đã ở trạng thái `PUBLISHED`.
- Nút thu hồi bị vô hiệu hóa khi chưa có bảng điểm nào được công bố.
- Khi thu hồi, chỉ các entry đang `PUBLISHED` được chuyển về `GRADED`; entry `PENDING` hoặc trạng thái khác được giữ nguyên.
- Sau khi thu hồi, API bảng điểm học viên không trả kết quả cho đến khi giáo viên công bố lại.

### API

- Thêm `POST /api/teacher/classrooms/{id}/gradebook/unpublish`.
- Giữ nguyên `POST /api/teacher/classrooms/{id}/gradebook/publish`.

### Cơ sở dữ liệu

- Không thay đổi schema.
- Tái sử dụng trạng thái `GRADED` và `PUBLISHED` hiện có.

### Kiểm thử

- [x] Frontend ESLint chạy thành công.
- [x] Frontend production build chạy thành công.
- [x] Backend test: 2 tests, 0 failures, 0 errors.
- [x] IDE diagnostics không có lỗi trong các tệp đã thay đổi.
- [ ] Kiểm tra thủ công hộp xác nhận công bố trên trình duyệt.
- [ ] Kiểm tra thủ công hộp xác nhận thu hồi và quyền xem của học viên.

### Bổ sung UI xác nhận đồng bộ

- Thay hộp thoại mặc định `window.confirm` bằng `ConfirmModal` dùng chung của hệ thống.
- Hộp xác nhận công bố sử dụng màu thương hiệu và nội dung giải thích phạm vi hiển thị cho học viên.
- Hộp xác nhận thu hồi sử dụng trạng thái cảnh báo nguy hiểm và giải thích rằng học viên sẽ tạm thời không xem được kết quả.
- Hỗ trợ đóng bằng nút Hủy, click backdrop hoặc phím Escape.
- Frontend ESLint và production build chạy thành công sau thay đổi.

### Sửa cách hiển thị riêng điểm Homework và Quiz

- Loại bỏ công thức trung bình `homeworkScore` và `quizScore` khiến điểm `2` và `1` bị hiển thị thành `1.5`.
- Cột `Bài tập` hiện chỉ hiển thị đúng giá trị `homeworkScore`.
- Bổ sung cột `Quiz` riêng trên bảng điểm giáo viên.
- Form chỉnh sửa giữ hai trường `Bài tập` và `Quiz` độc lập.
- Trang lớp của học viên hiển thị Homework và Quiz thành hai thẻ điểm riêng.
- Tổng quan lớp của giáo viên chỉ dùng `homeworkScore` cho chỉ số Bài tập.
- Frontend ESLint, production build và IDE diagnostics đều đạt.

### Sửa lỗi duyệt yêu cầu học bù (Task 1)

- **Ngày:** 2026-07-12
- Không còn báo xung đột `SESSION_LOCKED` giả khi buổi gốc đã hoàn thành/khóa.
- Khi TM chọn ghi đè xung đột, tạo buổi `MAKEUP` không bị chặn lần hai bởi `createSession`.
- Kiểm tra trùng lịch học bù dùng phòng/giáo viên mặc định giống lúc tạo buổi thật.
- 4 unit tests change-request đều đạt.

---

## Task 4 — Ưu tiên và vị trí danh sách chờ lớp học

- **Ngày:** 2026-07-12
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit

### Tóm tắt

Đã bổ sung hàng đợi có thứ tự ổn định cho đăng ký lớp khi lớp đã đủ chỗ. Học viên mới được thêm vào cuối danh sách chờ, Training Manager có thể thay đổi thứ tự, và học viên xem được vị trí hiện tại theo dạng `#N / tổng số`.

### Tệp đã thay đổi

- `backend/src/main/java/fu/sap490/g23/backend/entity/classroom/ClassroomEnrollment.java`
- `backend/src/main/java/fu/sap490/g23/backend/dto/request/classroom/ReorderWaitlistRequest.java`
- `backend/src/main/java/fu/sap490/g23/backend/dto/response/classroom/ClassroomEnrollmentResponse.java`
- `backend/src/main/java/fu/sap490/g23/backend/dto/response/classroom/ClassroomOfferingResponse.java`
- `backend/src/main/java/fu/sap490/g23/backend/repository/classroom/ClassroomEnrollmentRepository.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/ClassroomOfferingService.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/ClassroomMapper.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomOfferingServiceImpl.java`
- `backend/src/main/java/fu/sap490/g23/backend/controller/classroom/TrainingManagerClassroomController.java`
- `backend/src/main/java/fu/sap490/g23/backend/migration/ClassroomWaitlistSchemaMigration.java`
- `backend/src/test/java/fu/sap490/g23/backend/service/classroom/ClassroomOfferingServiceImplWaitlistTest.java`
- `frontend/src/api/classroomApi.js`
- `frontend/src/components/training-manager/TrainingManagerRegistrationPanel.jsx`
- `frontend/src/pages/classroom/ClassroomPublicDetailPage.jsx`
- `frontend/src/pages/classroom/MyClassroomsPage.jsx`
- `docs/CHANGELOG_IMPLEMENTATION.md`

### Thay đổi backend

- Thêm trường `waitlistPriority` vào đăng ký lớp.
- Khi lớp đầy, đăng ký mới được nối vào cuối hàng đợi theo thứ tự `1..N`.
- Khi học viên hủy, bị loại khỏi hàng chờ hoặc được xếp lớp, các vị trí còn lại tự động được đánh lại liên tục.
- Dùng khóa pessimistic trên lớp khi cấp thứ tự hoặc thay đổi toàn bộ hàng đợi để tránh hai thao tác đồng thời tạo cùng vị trí.
- API reorder kiểm tra payload phải chứa đúng và đủ toàn bộ enrollment đang ở trạng thái `WAITLIST`; từ chối ID trùng, thiếu hoặc thuộc lớp khác.
- Response đăng ký bổ sung `waitlistPriority`, `waitlistPosition`, `waitlistSize`.
- Response lớp của học viên bổ sung `waitlistPosition`; giữ `waitlistCount` làm tổng số người chờ.
- Giữ quy trình Training Manager xếp lớp thủ công, không tự động chuyển học viên đầu hàng đợi vào lớp.

### Thay đổi frontend

- Thêm tab `Danh sách chờ` trong màn hình điều phối của từng lớp.
- Hiển thị vị trí của từng học viên và nút lên/xuống để thay đổi thứ tự.
- Trang chi tiết lớp công khai hiển thị `Vị trí #N / M` cho học viên đang chờ.
- Trang `Lớp học của tôi` hiển thị vị trí hàng chờ ngay trên thẻ lớp.

### API

- Thêm `PUT /api/training-manager/classrooms/{id}/waitlist/order`.
- Request:
  - `enrollmentIds`: danh sách ID enrollment theo thứ tự mới, từ vị trí đầu đến cuối.
- Các API đăng ký và xem lớp hiện có tự trả thêm thông tin vị trí hàng chờ, không đổi đường dẫn.

### Cơ sở dữ liệu

- Thêm cột nullable `classroom_enrollments.waitlist_priority INTEGER`.
- Backfill dữ liệu `WAITLIST` hiện có theo ưu tiên cũ nếu có, sau đó theo `enrolled_at` và `id`.
- Xóa priority khỏi enrollment không còn ở trạng thái `WAITLIST`.
- Thêm index `idx_classroom_enrollment_waitlist_order` trên lớp, trạng thái và priority.
- Migration có tính idempotent và chạy an toàn khi bảng đã tồn tại.

### Kiểm thử

- [x] Backend compile thành công.
- [x] 5 unit tests waitlist: nối cuối hàng đợi, reorder, payload không đầy đủ, phân quyền và đánh lại vị trí khi hủy.
- [x] Tổng cộng 9 backend tests của Task 1, Task 2 và Task 4 đều đạt.
- [x] Frontend ESLint chạy thành công.
- [x] Frontend production build chạy thành công.
- [x] IDE diagnostics không có lỗi mới trong các tệp đã thay đổi.
- [x] Backend DevTools restart ổn định với PostgreSQL và migration mới đã được nạp.
- [ ] Kiểm tra thủ công hai tài khoản học viên đăng ký khi lớp đầy và Training Manager đổi thứ tự.

---

## Feature: Sửa tài khoản demo Training Manager

- **Ngày:** 2026-07-12
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit (base HEAD: `be2136c`)

### Tóm tắt

Sửa lỗi đăng nhập tài khoản Training Manager demo. Trước đó mật khẩu mặc định không dùng được vì tài khoản chỉ được tạo khi bật seed classroom demo, và seeder không reset mật khẩu nếu tài khoản đã tồn tại.

### Tệp đã thay đổi

- `backend/src/main/java/fu/sap490/g23/backend/seed/DemoTrainingManagerAccountRepairSeeder.java`
- `docs/CHANGELOG_IMPLEMENTATION.md`

### Thay đổi backend

- Thêm seeder sửa tài khoản demo mỗi lần backend khởi động.
- Đảm bảo tồn tại và đặt lại mật khẩu cho:
  - `training.manager@englishlab.vn` (`TRAINING_MANAGER`)
  - `classroom.manager@englishlab.vn` (`MANAGER`)
- Mật khẩu demo: `Password123!`
- Gán đúng role và đánh dấu email đã xác thực.

### Thay đổi frontend

- Không đổi.

### Cơ sở dữ liệu

- Không đổi schema.
- Cập nhật/tạo bản ghi user demo trong bảng `users` và `user_roles`.

### Kiểm thử

- [x] Đăng nhập `training.manager@englishlab.vn` / `Password123!` thành công.
- [x] Đăng nhập `classroom.manager@englishlab.vn` / `Password123!` thành công.
- [x] Response login trả đúng role `TRAINING_MANAGER`.

### Ghi chú

- Chỉ phục vụ môi trường demo/local.
- Không ảnh hưởng tài khoản học viên/giáo viên đang dùng được.

---

## Feature: Sửa lỗi duyệt yêu cầu buổi học bù

- **Ngày:** 2026-07-12
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit (base HEAD: `be2136c`)

### Tóm tắt

Sửa lỗi Training Manager không duyệt được yêu cầu `CREATE_MAKEUP_SESSION` khi buổi học gốc đã hoàn thành hoặc đã khóa. Hệ thống trước đó báo xung đột giả `SESSION_LOCKED`, và khi ghi đè xung đột vẫn bị `createSession` chặn lần hai.

### Tệp đã thay đổi

- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/ClassroomOfferingService.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomOfferingServiceImpl.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomChangeRequestServiceImpl.java`
- `backend/src/test/java/fu/sap490/g23/backend/service/classroom/ClassroomChangeRequestServiceImplTest.java`
- `docs/CHANGELOG_IMPLEMENTATION.md`

### Thay đổi backend

- Không ép `checkSessionLocked=true` với yêu cầu học bù khi duyệt hoặc kiểm tra trùng lịch.
- Buổi gốc chỉ dùng làm ngữ cảnh; chỉ lịch đề xuất được kiểm tra xung đột.
- Thêm overload `createSession(..., enforceConflictCheck)` để TM ghi đè xung đột thì tạo buổi `MAKEUP` không bị chặn lần hai.
- Đồng bộ kiểm tra xung đột học bù với phòng/giáo viên mặc định giống lúc tạo buổi thật.

### Thay đổi frontend

- Không đổi API payload.
- Vẫn dùng nút `Duyệt và áp dụng` / `Duyệt và ghi đè xung đột` hiện có.

### Cơ sở dữ liệu

- Không đổi.

### Kiểm thử

- [x] Conflict-check yêu cầu học bù không còn báo `SESSION_LOCKED` giả.
- [x] Duyệt ghi đè xung đột tạo buổi học bù thành công.
- [x] 4 unit tests change-request đều đạt.
- [ ] Kiểm tra thủ công thêm một yêu cầu học bù mới trên trình duyệt.

### Ghi chú

- Khi ghi đè xung đột vẫn bắt buộc nhập ghi chú phản hồi.
- Không tạo endpoint mới.

---

## Feature: Tách nhãn điều hướng yêu cầu của giáo viên

- **Ngày:** 2026-07-12
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit (base HEAD: `be2136c`)

### Tóm tắt

Loại bỏ trùng lặp và nhập nhằng nhãn `Yêu cầu thay đổi`. Phân rõ hai hành động: theo dõi tiến độ duyệt và gửi yêu cầu mới. Đồng thời bỏ các mục điều hướng trùng trong dropdown avatar của staff.

### Tệp đã thay đổi

- `frontend/src/components/ai-learning/Header.jsx`
- `frontend/src/pages/teacher/TeacherClassroomPage.jsx`
- `frontend/src/pages/teacher/TeacherRequestsPage.jsx`
- `frontend/src/pages/teacher/TeacherDashboardPage.jsx`
- `docs/CHANGELOG_IMPLEMENTATION.md`

### Thay đổi backend

- Không đổi.

### Thay đổi frontend

- Header giáo viên: `Yêu cầu thay đổi` → `Theo dõi yêu cầu`.
- Trang danh sách yêu cầu và card dashboard dùng cùng nhãn `Theo dõi yêu cầu`.
- Tab trong chi tiết lớp: `Yêu cầu thay đổi` → `Gửi yêu cầu`.
- Dropdown avatar của Teacher / Training Manager / Manager / Admin / Content Manager chỉ còn thông tin tài khoản và `Đăng xuất`.

### Cơ sở dữ liệu

- Không đổi.

### Kiểm thử

- [x] Không còn hai mục trùng nhau trong dropdown avatar giáo viên.
- [x] Nhãn header và tab lớp đã phân biệt rõ mục đích.
- [ ] Kiểm tra thủ công trên desktop sau khi refresh frontend.

### Ghi chú

- Không đổi route: vẫn là `/teacher/requests` và tab `change-requests` trong lớp.
- Học viên giữ nguyên các mục hồ sơ trong dropdown vì khác với thanh điều hướng marketing.

---

## Nhật ký triển khai đầy đủ các task đã hoàn thành (tiếng Việt)

Phần này tổng hợp lại Task 1, Task 2, Task 4 theo đúng mẫu log triển khai. Nội dung phía trên được giữ nguyên để bảo toàn lịch sử. Task 3 đã bỏ qua theo yêu cầu nhóm.

---

## Feature: Task 1 — UI yêu cầu buổi học bù

- **Ngày:** 2026-07-12
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit (base HEAD: `be2136c`)

### Tóm tắt

Hoàn thiện luồng giáo viên gửi yêu cầu tạo buổi học bù. Giáo viên chọn buổi gốc, ngày học bù, khung giờ và phòng học; Training Manager duyệt. Khi duyệt thành công hệ thống tạo buổi mới trạng thái `MAKEUP`.

### Tệp đã thay đổi

- `frontend/src/components/teacher/TeacherChangeRequestForm.jsx`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomChangeRequestServiceImpl.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/ClassroomOfferingService.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomOfferingServiceImpl.java`
- `backend/src/test/java/fu/sap490/g23/backend/service/classroom/ClassroomChangeRequestServiceImplTest.java`
- `docs/CHANGELOG_IMPLEMENTATION.md`

### Thay đổi backend

- Bổ sung validation cho `CREATE_MAKEUP_SESSION`.
- Bắt buộc buổi gốc, ngày học bù, giờ bắt đầu và giờ kết thúc.
- Cho phép buổi gốc đã hoàn thành hoặc đã hủy làm ngữ cảnh, không báo `SESSION_LOCKED` giả.
- Chỉ kiểm tra xung đột trên lịch đề xuất.
- Khi TM ghi đè xung đột, tạo buổi `MAKEUP` không bị `createSession` chặn lần hai.
- Tái sử dụng luồng duyệt change request hiện có.

### Thay đổi frontend

- Thêm loại yêu cầu `CREATE_MAKEUP_SESSION` vào form giáo viên.
- Tái dụng UI chọn ngày, khung giờ, phòng và kiểm tra xung đột.
- Cho phép chọn mọi buổi gốc, kể cả đã hoàn thành hoặc đã hủy.
- Tách nhãn: header dùng `Theo dõi yêu cầu`, tab lớp dùng `Gửi yêu cầu`.

### Cơ sở dữ liệu

- Không đổi schema.
- Tái sử dụng `classroom_change_requests` và `classroom_sessions`.

### Kiểm thử

- [x] ESLint frontend đạt.
- [x] Build frontend đạt.
- [x] Compile backend đạt.
- [x] Unit test chấp nhận buổi gốc đã hoàn thành.
- [x] Unit test từ chối thiếu lịch học bù.
- [x] Unit test duyệt học bù không ép `SESSION_LOCKED`.
- [x] Duyệt ghi đè xung đột tạo buổi học bù thành công.
- [ ] Kiểm tra thủ công thêm trên trình duyệt với tài khoản Teacher và Training Manager.

### Ghi chú

- Không tạo endpoint mới.
- Buổi gốc chỉ là ngữ cảnh; buổi học bù được tạo riêng với status `MAKEUP`.
- Task 3 đã bỏ qua theo yêu cầu nhóm.

---

## Feature: Task 2 — Giáo viên chỉnh sửa gradebook thủ công

- **Ngày:** 2026-07-12
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit (base HEAD: `be2136c`)

### Tóm tắt

Cho phép giáo viên chỉnh sửa bảng điểm thủ công trên trang lớp: điểm bài tập, quiz, chuyên cần, tham gia, kết quả cuối và nhận xét. Bổ sung công bố/thu hồi công bố kèm hộp xác nhận. Hiển thị riêng điểm Homework và Quiz, không còn lấy trung bình.

### Tệp đã thay đổi

- `frontend/src/components/teacher/TeacherGradebookSection.jsx`
- `frontend/src/pages/teacher/TeacherClassroomPage.jsx`
- `frontend/src/pages/classroom/MyClassroomDetailPage.jsx`
- `frontend/src/api/classroomApi.js`
- `backend/src/main/java/fu/sap490/g23/backend/dto/request/classroom/UpdateGradebookRequest.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/ClassroomGradebookService.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomGradebookServiceImpl.java`
- `backend/src/main/java/fu/sap490/g23/backend/controller/classroom/TeacherClassroomController.java`
- `backend/src/test/java/fu/sap490/g23/backend/service/classroom/ClassroomGradebookServiceImplTest.java`
- `docs/CHANGELOG_IMPLEMENTATION.md`

### Thay đổi backend

- Tái sử dụng `updateEntry` và endpoint gradebook hiện có.
- Thêm validation DTO: điểm `0–10`, chuyên cần `0–100`, tối đa 2 chữ số thập phân, nhận xét tối đa 2000 ký tự.
- Thêm `POST /api/teacher/classrooms/{id}/gradebook/unpublish`.
- Thu hồi chỉ chuyển entry `PUBLISHED` về `GRADED`.

### Thay đổi frontend

- Tách UI thành `TeacherGradebookSection`.
- Chỉnh sửa inline theo từng học viên, validate phía client.
- Thêm nút `Công bố bảng điểm` và `Thu hồi công bố` với `ConfirmModal`.
- Hiển thị riêng cột/thẻ `Bài tập` và `Quiz`, không trung bình hai điểm.

### Cơ sở dữ liệu

- Không đổi schema.
- Tái sử dụng `classroom_gradebook_entries` và trạng thái `PENDING` / `GRADED` / `PUBLISHED`.

### Kiểm thử

- [x] ESLint frontend đạt.
- [x] Build frontend đạt.
- [x] Unit test cập nhật điểm thủ công và chuyển `PENDING` → `GRADED`.
- [x] Unit test thu hồi công bố `PUBLISHED` → `GRADED`.
- [x] Regression Task 1 + Task 2 đạt.
- [ ] Kiểm tra thủ công công bố/thu hồi trên trình duyệt.
- [ ] Kiểm tra học viên không còn thấy điểm sau khi thu hồi.

### Ghi chú

- Endpoint cập nhật điểm: `PUT /api/teacher/classrooms/{id}/gradebook`.
- Endpoint công bố: `POST /api/teacher/classrooms/{id}/gradebook/publish`.
- Endpoint thu hồi: `POST /api/teacher/classrooms/{id}/gradebook/unpublish`.
- Sửa entry đã `PUBLISHED` vẫn giữ trạng thái `PUBLISHED` để học viên thấy điểm đã chỉnh.

---

## Feature: Task 4 — Ưu tiên và vị trí danh sách chờ lớp học

- **Ngày:** 2026-07-12
- **Nhánh:** `phongdx`
- **Commit:** Chưa commit (base HEAD: `be2136c`)

### Tóm tắt

Bổ sung hàng đợi có thứ tự ổn định khi lớp đủ chỗ. Học viên mới vào cuối hàng chờ, Training Manager có thể sắp xếp lại, học viên xem được vị trí `#N / tổng số`.

### Tệp đã thay đổi

- `backend/src/main/java/fu/sap490/g23/backend/entity/classroom/ClassroomEnrollment.java`
- `backend/src/main/java/fu/sap490/g23/backend/dto/request/classroom/ReorderWaitlistRequest.java`
- `backend/src/main/java/fu/sap490/g23/backend/dto/response/classroom/ClassroomEnrollmentResponse.java`
- `backend/src/main/java/fu/sap490/g23/backend/dto/response/classroom/ClassroomOfferingResponse.java`
- `backend/src/main/java/fu/sap490/g23/backend/repository/classroom/ClassroomEnrollmentRepository.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/ClassroomOfferingService.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/ClassroomMapper.java`
- `backend/src/main/java/fu/sap490/g23/backend/service/classroom/impl/ClassroomOfferingServiceImpl.java`
- `backend/src/main/java/fu/sap490/g23/backend/controller/classroom/TrainingManagerClassroomController.java`
- `backend/src/main/java/fu/sap490/g23/backend/migration/ClassroomWaitlistSchemaMigration.java`
- `backend/src/test/java/fu/sap490/g23/backend/service/classroom/ClassroomOfferingServiceImplWaitlistTest.java`
- `frontend/src/api/classroomApi.js`
- `frontend/src/components/training-manager/TrainingManagerRegistrationPanel.jsx`
- `frontend/src/pages/classroom/ClassroomPublicDetailPage.jsx`
- `frontend/src/pages/classroom/MyClassroomsPage.jsx`
- `docs/CHANGELOG_IMPLEMENTATION.md`

### Thay đổi backend

- Thêm `waitlistPriority` cho enrollment.
- Đăng ký mới khi lớp đầy được nối cuối hàng chờ `1..N`.
- Tự đánh lại vị trí khi hủy, từ chối hoặc rời hàng chờ.
- Khóa pessimistic khi cấp/sắp xếp priority.
- Thêm `PUT /api/training-manager/classrooms/{id}/waitlist/order`.
- Response bổ sung `waitlistPriority`, `waitlistPosition`, `waitlistSize`.
- Giữ xếp lớp thủ công, không auto-promote đầu hàng chờ.

### Thay đổi frontend

- Tab `Danh sách chờ` cho Training Manager, có nút lên/xuống.
- Học viên xem `Vị trí #N / M` ở chi tiết lớp và `Lớp học của tôi`.

### Cơ sở dữ liệu

- Thêm cột `classroom_enrollments.waitlist_priority INTEGER`.
- Backfill FIFO cho dữ liệu `WAITLIST` hiện có.
- Thêm index `idx_classroom_enrollment_waitlist_order`.
- Migration idempotent.

### Kiểm thử

- [x] Compile backend đạt.
- [x] 5 unit tests waitlist đạt.
- [x] Regression Task 1 + Task 2 + Task 4: 9 tests đạt.
- [x] ESLint và build frontend đạt.
- [x] Backend restart ổn định với migration mới.
- [ ] Kiểm tra thủ công 2 học viên đăng ký lớp đầy và TM đổi thứ tự.

### Ghi chú

- Task 3 đã bỏ qua theo yêu cầu nhóm.
- Tài khoản demo Training Manager: `training.manager@englishlab.vn` / `Password123!`.

---

## Quy trình test trên web (Task 1, 2, 4)

- **Ngày ghi:** 2026-07-12
- **Nhánh:** `phongdx`
- **Môi trường:** Frontend `http://localhost:5173` (hoặc cổng Vite hiện tại), Backend `http://localhost:8080`

### Tài khoản demo dùng để test

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Giáo viên | `classroom.teacher1@englishlab.vn` | `Password123!` |
| Học viên | `0386852628z@gmail.com` | `Password123!` |
| Training Manager | `training.manager@englishlab.vn` | `Password123!` |

Ghi chú: nếu cần học viên thứ 2 cho waitlist, dùng `classroom.learner2@englishlab.vn` / `Password123!` (khi seed classroom demo đã tạo).

---

### Task 1 — Yêu cầu buổi học bù

#### Mục tiêu
Giáo viên gửi yêu cầu học bù; Training Manager duyệt; hệ thống tạo buổi mới `MAKEUP`.

#### Vị trí trên web
- Giáo viên gửi: `/teacher/classrooms/{id}` → tab **Gửi yêu cầu**
- Giáo viên theo dõi: `/teacher/requests` (menu **Theo dõi yêu cầu**)
- Training Manager duyệt: `/training-manager/requests`

#### Các bước test

1. Đăng nhập Giáo viên.
2. Vào **Giảng dạy** → chọn lớp đang học (ví dụ TOEIC 650).
3. Chọn tab **Gửi yêu cầu**.
4. Chọn loại **Tạo buổi học bù**.
5. Chọn buổi học gốc (có thể chọn buổi đã hoàn thành).
6. Chọn ngày học bù, khung giờ, phòng (nếu có), nhập lý do → gửi.
7. Vào **Theo dõi yêu cầu** (`/teacher/requests`) → thấy yêu cầu trạng thái **Chờ duyệt**.
8. Đăng xuất → đăng nhập Training Manager.
9. Vào `/training-manager/requests`.
10. Chọn yêu cầu học bù vừa gửi.
11. Bấm **Kiểm tra lại trùng lịch**.
12. Nếu không có xung đột thật → **Duyệt và áp dụng**.
13. Nếu có xung đột thật → nhập ghi chú → **Duyệt và ghi đè xung đột**.
14. Đăng nhập lại Giáo viên → mở lớp → tab **Buổi học** → kiểm tra có buổi mới trạng thái học bù / `MAKEUP`.

#### Kết quả mong đợi
- [ ] Gửi yêu cầu thành công, hiện ở danh sách theo dõi.
- [ ] Buổi gốc đã hoàn thành/khóa **không** bị báo xung đột giả `SESSION_LOCKED`.
- [ ] Sau duyệt, buổi học bù xuất hiện trong lịch lớp.
- [ ] Giáo viên thấy trạng thái yêu cầu đã áp dụng/duyệt.

#### Lỗi cần tránh nhầm
- Hai nhãn khác nhau: **Theo dõi yêu cầu** (xem tiến độ) ≠ **Gửi yêu cầu** (tạo mới).

---

### Task 2 — Chỉnh sửa / công bố / thu hồi bảng điểm

#### Mục tiêu
Giáo viên sửa điểm thủ công, công bố cho học viên xem, thu hồi khi cần.

#### Vị trí trên web
- Giáo viên: `/teacher/classrooms/{id}` → tab **Bảng điểm**
- Học viên xem điểm: `/my-classrooms/{id}` → phần bảng điểm / kết quả

#### Các bước test — chỉnh sửa điểm

1. Đăng nhập Giáo viên → mở lớp → tab **Bảng điểm**.
2. Chọn một học viên → **Chỉnh sửa**.
3. Nhập điểm **Bài tập** = `2`, **Quiz** = `1` (độc lập, không trung bình).
4. Có thể sửa chuyên cần, tham gia, kết quả cuối, nhận xét.
5. **Lưu thay đổi**.
6. Kiểm tra bảng: cột Bài tập = `2`, cột Quiz = `1` (không thành `1.5`).

#### Các bước test — công bố

1. Bấm **Công bố bảng điểm**.
2. Hộp xác nhận hiện ra → xác nhận.
3. Đăng nhập Học viên → `/my-classrooms` → mở đúng lớp.
4. Kiểm tra thấy điểm Bài tập và Quiz riêng (hai thẻ/chỉ số).

#### Các bước test — thu hồi

1. Đăng nhập lại Giáo viên → tab **Bảng điểm**.
2. Bấm **Thu hồi công bố** → xác nhận trong hộp thoại.
3. Đăng nhập Học viên → mở lại lớp → **không còn thấy** bảng điểm đã công bố.
4. Giáo viên công bố lại → học viên thấy điểm trở lại.

#### Kết quả mong đợi
- [ ] Sửa điểm lưu thành công, không reload cả trang lỗi.
- [ ] Homework và Quiz hiển thị riêng.
- [ ] Công bố cần xác nhận; học viên xem được điểm.
- [ ] Thu hồi cần xác nhận; học viên tạm không xem được điểm.
- [ ] Nút công bố/thu hồi disable đúng trạng thái (đã công bố hết / chưa có gì công bố).

---

### Task 4 — Danh sách chờ: ưu tiên và vị trí

#### Mục tiêu
Khi lớp đầy, học viên vào hàng chờ có thứ tự; TM đổi thứ tự; học viên xem `#N / tổng số`.

#### Vị trí trên web
- Học viên đăng ký: trang chi tiết lớp công khai (ví dụ `/classrooms/{slug-or-id}` hoặc link từ lịch khai giảng).
- Học viên xem vị trí: chi tiết lớp đã đăng ký / `/my-classrooms`.
- Training Manager sắp xếp: `/training-manager/classrooms/{id}` → khu vực đăng ký → tab **Danh sách chờ**  
  hoặc `/training-manager/registrations` lọc `WAITLIST`.

#### Chuẩn bị
- Chọn lớp `UPCOMING` còn mở đăng ký và **đã đủ sĩ số** (hoặc tạm giảm `maxCapacity` trong DB/admin nếu cần).
- Có ít nhất 2 tài khoản học viên.

#### Các bước test

1. Đăng nhập Học viên A → mở lớp đầy → bấm **Đăng ký vào danh sách chờ**.
2. Kiểm tra UI hiện **Vị trí #1 / …** (hoặc vị trí cuối hàng chờ hiện tại).
3. Đăng xuất → đăng nhập Học viên B → đăng ký cùng lớp.
4. Học viên B thấy vị trí sau A (ví dụ `#2`).
5. Vào `/my-classrooms` tab **Chờ xếp lớp** → thẻ lớp hiện `Danh sách chờ #N`.
6. Đăng nhập Training Manager → mở lớp đó → tab đăng ký **Danh sách chờ**.
7. Dùng nút **lên/xuống** để đổi thứ tự B lên trước A.
8. Refresh trang học viên A và B → vị trí `#N / M` đã đổi theo.
9. (Tuỳ chọn) Học viên hủy đăng ký chờ → các vị trí còn lại tự đánh lại `1..N`.

#### Kết quả mong đợi
- [ ] Đăng ký lớp đầy vào `WAITLIST`, không báo lỗi “hết chỗ” cứng.
- [ ] Học viên thấy vị trí và tổng số người chờ.
- [ ] TM đổi thứ tự thành công, UI cập nhật ngay.
- [ ] Sau hủy/rời hàng chờ, thứ tự còn lại liên tục không bị lỗ trống.

---

### Task 3 — Gộp Quiz vào Bài tập

- **Trạng thái:** Đã bỏ qua theo yêu cầu nhóm.
- **Không test** trên web trong phạm vi log này.

---

### Task 5 — Thanh toán học phí lớp online (PayOS)

- **Trạng thái:** Đã triển khai (2026-07-12).
- **Backend:** Mở `classroomOfferingIds` trong quote/create PayOS; lưu `enrollment_id` trên `payment_orders`; webhook/PAID gọi `applyPayosTuitionPayment` (idempotent theo note `PayOS #orderCode`); giữ upload minh chứng + TM duyệt làm phương án B.
- **Frontend:** Nút PayOS trên `TuitionPaymentSection`; chứng từ thủ công vẫn còn; trang `/checkout` nhận return PayOS học phí và dẫn về lớp.
- **Ràng buộc:** Không gộp khóa online + học phí lớp trong cùng đơn; mỗi đơn chỉ 1 lớp; không áp mã giảm giá cho học phí lớp; chặn thanh toán khi đăng ký còn `PENDING_CONFIRMATION`.

#### Vị trí trên web
1. Học viên → lớp của tôi / chi tiết lớp công khai (đã đăng ký) → khu vực **Thanh toán học phí**
2. Return PayOS → `/checkout?...` → nút **Quay lại lớp học**

#### Các bước test
1. [ ] TM xác nhận đăng ký học viên → status `PENDING_TUITION_PAYMENT`, còn học phí > 0.
2. [ ] HV bấm **Thanh toán ... qua PayOS** → chuyển sang PayOS.
3. [ ] Thanh toán thành công → `/checkout` báo ghi nhận học phí → quay lại lớp → lịch sử có dòng `PayOS #...`, số còn lại giảm/về 0.
4. [ ] (Fallback) Gửi minh chứng chuyển khoản → TM xác nhận/từ chối vẫn hoạt động như cũ.
5. [ ] Thử tạo đơn PayOS thứ 2 khi đơn trước còn PENDING → bị chặn.

#### Kết quả mong đợi
- [ ] PayOS ghi nhận học phí không cần TM bấm tay.
- [ ] Upload proof vẫn dùng được khi không PayOS.
- [ ] Checkout khóa học online không bị ảnh hưởng.

---

### Checklist nhanh trước khi demo

1. Backend chạy tại `:8080`, frontend chạy tại Vite.
2. Đăng nhập được 3 role demo ở bảng trên.
3. Task 1: gửi học bù → TM duyệt → có buổi mới.
4. Task 2: sửa điểm → công bố → học viên xem → thu hồi → học viên mất điểm.
5. Task 4: 2 học viên vào waitlist → TM đổi thứ tự → vị trí cập nhật.
6. Task 5: HV PayOS học phí → về checkout thành công → lịch sử có PayOS; fallback minh chứng vẫn gửi được.

---

## Kết quả test Task 4 (API + unit) — 2026-07-12

- **Nhánh:** `phongdx`
- **Môi trường:** Backend `http://localhost:8080`, DB PostgreSQL Docker `postgres-englishlab`

### Unit tests

- Lệnh: `mvnw -Dtest=ClassroomOfferingServiceImplWaitlistTest test`
- Kết quả: **5 tests, 0 failures, 0 errors**

### API end-to-end

Chuẩn bị tạm thời: lớp `#12` đặt `UPCOMING`, `maxCapacity=1` (đã có 1 học viên `ASSIGNED`), tạo 2 tài khoản:
- `waitlist.learner.a@test.vn` / `Password123!`
- `waitlist.learner.b@test.vn` / `Password123!`

| Bước | Kết quả |
|---|---|
| TM / Learner A / Learner B đăng nhập | PASS |
| Lớp đầy (`maxCapacity=1`, `enrolledCount=1`) | PASS |
| A đăng ký → `WAITLIST` vị trí `#1 / 1` | PASS |
| B đăng ký → `WAITLIST` vị trí `#2 / 2` | PASS |
| TM list waitlist theo thứ tự A rồi B | PASS |
| TM reorder `PUT .../waitlist/order` thành B rồi A | PASS |
| A thấy `#2 / 2`, B thấy `#1 / 2` | PASS |
| A hủy đăng ký → B được compact còn `#1 / 1` | PASS |

**Tổng API checks đạt:** 21/21 kịch bản nghiệp vụ chính.

Sau test đã khôi phục lớp `#12` về `ACTIVE`, `maxCapacity=16`, `startDate=2026-06-21` và hủy các enrollment waitlist tạm.

### Gợi ý test UI thủ công còn lại

1. Đăng nhập TM → `/training-manager/classrooms/12` → tab đăng ký **Danh sách chờ** (cần tạo lại dữ liệu waitlist nếu muốn xem UI).
2. Hoặc giảm tạm `maxCapacity` rồi đăng ký bằng 2 tài khoản `waitlist.learner.*@test.vn`.
3. Kiểm tra nút lên/xuống và badge `Vị trí #N` trên trang chi tiết lớp.

---

## Task 5 — Thanh toán học phí lớp online (PayOS) + proof fallback

- **Date:** 2026-07-12
- **Branch:** `phongdx`
- **Commit hash:** Not committed yet

### Summary

Connected classroom tuition to the existing PayOS checkout pipeline while keeping manual bank-transfer proof upload as Plan B. Students pay the remaining tuition balance online; webhook/status sync records tuition via `applyPayosTuitionPayment` without requiring Training Manager manual entry.

### Changed files

- `backend/.../PaymentServiceImpl.java`
- `backend/.../PaymentOrder.java` (+ `enrollmentId`)
- `backend/.../PaymentOrderRepository.java`
- `backend/.../PaymentOrderStatusResponse.java`
- `backend/.../ClassroomOfferingService.java` / `ClassroomOfferingServiceImpl.java`
- `backend/.../migration/PaymentOrderEnrollmentSchemaMigration.java`
- `backend/.../PaymentServiceImplClassroomTuitionTest.java`
- `frontend/src/api/paymentApi.js`
- `frontend/src/components/classroom/TuitionPaymentSection.jsx`
- `frontend/src/pages/CheckoutPage.jsx`
- `frontend/src/pages/classroom/MyClassroomDetailPage.jsx`
- `frontend/src/pages/classroom/ClassroomPublicDetailPage.jsx`
- `docs/CHANGELOG_IMPLEMENTATION.md`

### Backend changes

- Allowed quote/create PayOS for a single `classroomOfferingId` (mutually exclusive with course cart).
- Amount = enrollment tuition remaining balance; coupons not applied to classroom tuition.
- Persisted `classroomOfferingIdsCsv` + `enrollmentId` on `PaymentOrder`.
- On PAID: call `applyPayosTuitionPayment` (idempotent by note `PayOS #orderCode`), optionally auto-assign when fully paid.
- Blocked duplicate PENDING/PROCESSING PayOS orders for the same enrollment.
- Blocked PayOS while registration is still `PENDING_CONFIRMATION`.

### Frontend changes

- PayOS primary action on `TuitionPaymentSection`; proof upload kept as fallback.
- `paymentApi` sends `classroomOfferingIds`.
- Checkout return page detects classroom tuition via sessionStorage + order status fields and links back to the class.

### Tests

- `PaymentServiceImplClassroomTuitionTest`: **5 tests, 0 failures**.

---

## Kết quả test Task 5 (API) — 2026-07-12

- **Nhánh:** `phongdx`
- **Môi trường:** Backend `http://localhost:8080` (restart `mvnw spring-boot:run`), DB PostgreSQL Docker `postgres-englishlab`
- **Tài khoản test:** `waitlist.learner.a@test.vn`, `waitlist.learner.b@test.vn` / `Password123!`

| Bước | Kết quả |
|---|---|
| Restart backend (port 8080) | PASS |
| Đăng ký A → TM confirm → `PENDING_TUITION_PAYMENT` | PASS |
| Quote học phí lớp = 3.900.000 | PASS |
| Reject coupon trên học phí lớp | PASS |
| Block PayOS khi HV đã `ASSIGNED` | PASS |
| Create PayOS link (`PENDING` + checkoutUrl) | PASS |
| Block tạo đơn PayOS trùng khi còn PENDING | PASS |
| Order status trả `classroomOfferingId` + `enrollmentId` | PASS |
| Webhook PayOS signed → order `PAID` | PASS |
| Ghi nhận học phí + lịch sử `PayOS #...` + auto `ASSIGNED` | PASS |
| Webhook lần 2 idempotent (không double charge) | PASS |
| Upload minh chứng fallback (learner B) | PASS |

**Tổng:** 12/12 kịch bản API chính đạt.

Sau test đã khôi phục lớp `#12` về `ACTIVE` / `2026-06-21`–`2026-08-16` và hủy enrollment test `#13`, `#14`.

