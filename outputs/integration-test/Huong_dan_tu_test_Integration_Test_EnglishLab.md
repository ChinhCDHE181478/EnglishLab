# Hướng dẫn tự test Integration Test – EnglishLab

File Word đầy đủ nằm tại Downloads. Dưới đây là bản rút gọn.

## Quy trình 1 case
1. Đọc Pre-condition
2. Gọi API Postman
3. So Expected
4. Ghi Round/Tester/Note

## Chấm điểm
- Passed: đúng expected
- Failed: sai/500
- N/A: thiếu precondition

## 1. Authentication (IT - Auth)

- Role: Không cần token (trừ case dùng /me)
- Account: Tự đăng ký email mới; hoặc Learner 0386852628z@gmail.com / Password123!
- Goal: Kiểm tra đăng ký, xác thực email, đăng nhập JWT, quên/đặt lại mật khẩu.

### IT_AUTH_01 – Đăng ký tài khoản mới

**Bước:**
```
POST /api/auth/register
Body JSON: email (chưa dùng), password Password123!, fullName
```

**Mong đợi:** HTTP 200/201; tài khoản được tạo (có thể kiểm tra bằng login sau khi verify).

**Chấm:** Passed nếu 200/201. Failed nếu 500.

### IT_AUTH_02 – Đăng ký trùng email (negative)

**Bước:**
```
POST /api/auth/register với email Learner đã tồn tại
```

**Mong đợi:** HTTP 400/409

**Chấm:** Passed nếu bị từ chối. Failed nếu vẫn tạo được.

### IT_AUTH_03 – Xác thực email bằng OTP

**Bước:**
```
1) Register email mới
2) Lấy OTP trong bảng auth_tokens (type EMAIL_VERIFICATION) hoặc mail
3) POST /api/auth/verify-email Body: email, code=<OTP>
```

**Mong đợi:** HTTP 200; sau đó login được

**Chấm:** Passed nếu verify thành công. N/A nếu không đọc được OTP.

### IT_AUTH_04 – OTP sai (negative)

**Bước:**
```
POST /api/auth/verify-email với otp/code = 000000
```

**Mong đợi:** HTTP 400

**Chấm:** Passed nếu bị từ chối.

### IT_AUTH_05 – Login lấy JWT + gọi /me

**Bước:**
```
1) POST /api/auth/login {email, password}
2) Copy accessToken
3) GET /api/user/me Header Authorization: Bearer <token>
```

**Mong đợi:** Login 200 có accessToken; /me 200 có thông tin user

**Chấm:** Passed nếu cả 2 bước OK.

### IT_AUTH_06 – Sai mật khẩu (negative)

**Bước:**
```
POST /api/auth/login với password sai
```

**Mong đợi:** HTTP 401/400

**Chấm:** Passed nếu không cấp token.

### IT_AUTH_07 – Gọi /me không token (negative)

**Bước:**
```
GET /api/user/me (không gắn Authorization)
```

**Mong đợi:** HTTP 401/403

**Chấm:** Passed nếu bị chặn.

### IT_AUTH_08 – Quên mật khẩu

**Bước:**
```
POST /api/auth/forgot-password {email}. Nếu báo chờ 15–60 giây thì đợi rồi gọi lại.
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200. Failed nếu 500.

### IT_AUTH_09 – Đặt lại mật khẩu bằng OTP

**Bước:**
```
1) forgot-password
2) Lấy OTP type PASSWORD_RESET trong auth_tokens
3) POST /api/auth/reset-password {email, code, newPassword}
Nên đặt lại đúng Password123! để không phá demo.
```

**Mong đợi:** HTTP 200; login bằng mật khẩu mới được

**Chấm:** Passed nếu reset + login OK.

### IT_AUTH_10 – Reset OTP sai (negative)

**Bước:**
```
POST /api/auth/reset-password với code=000000
```

**Mong đợi:** HTTP 400

**Chấm:** Passed nếu bị từ chối.

## 2. Account Profile (IT - User)

- Role: LEARNER (Bearer token)
- Account: 0386852628z@gmail.com / Password123!
- Goal: Xem/sửa hồ sơ, đổi mật khẩu (negative), avatar, bảo mật.

### IT_USER_01 – GET hồ sơ

**Bước:**
```
GET /api/user/me + Bearer
```

**Mong đợi:** HTTP 200 + email/fullName

**Chấm:** Passed nếu 200.

### IT_USER_02 – Cập nhật hồ sơ

**Bước:**
```
PUT /api/user/me với fullName, phoneNumber, targetExam, targetScore, studyGoal
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_USER_03 – Đổi mật khẩu sai current (negative)

**Bước:**
```
PUT /api/user/me/password {currentPassword sai, newPassword}
```

**Mong đợi:** HTTP 400

**Chấm:** Passed nếu bị từ chối.

### IT_USER_04 – Upload avatar

**Bước:**
```
POST /api/user/me/avatar form-data field file = ảnh PNG/JPG thật
```

**Mong đợi:** HTTP 200/201

**Chấm:** Passed nếu upload OK. N/A nếu API báo không đọc được ảnh.

### IT_USER_05 – Sửa hồ sơ không token (negative)

**Bước:**
```
PUT /api/user/me không Authorization
```

**Mong đợi:** HTTP 401/403

**Chấm:** Passed nếu bị chặn.

## 3. Notifications (IT - Notif)

- Role: LEARNER
- Account: 0386852628z@gmail.com / Password123!
- Goal: Preference thông báo + danh sách/đọc thông báo.

### IT_NOTIF_01 – Lấy preference

**Bước:**
```
GET /api/user/me/notification-preferences
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_NOTIF_02 – Tắt/bật in-app

**Bước:**
```
PUT /api/user/me/notification-preferences {inAppEnabled, emailEnabled, larkEnabled} rồi bật lại
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_NOTIF_03 – Body thiếu (negative)

**Bước:**
```
PUT preference với {}
```

**Mong đợi:** HTTP 400

**Chấm:** Passed nếu validation lỗi.

### IT_NOTIF_04/05 – List + unread

**Bước:**
```
GET /api/student/notifications và GET /api/student/notifications/unread-count; có thể PATCH .../{id}/read
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu list/count 200.

## 4. Cart & Wishlist (IT - Commerce)

- Role: LEARNER
- Account: 0386852628z@gmail.com / Password123!
- Goal: Thêm/xóa giỏ, wishlist.

### IT_COMMERCE_01 – Thêm vào giỏ

**Bước:**
```
1) GET /api/online-courses lấy id
2) DELETE /api/student/commerce/cart (xóa sạch)
3) POST /api/student/commerce/cart/{courseId}
4) GET /api/student/commerce/cart
```

**Mong đợi:** Add 200 + GET thấy khóa

**Chấm:** Passed nếu thêm được.

### IT_COMMERCE_02 – Wishlist → cart

**Bước:**
```
POST /api/student/commerce/wishlist/{courseId} rồi POST .../move-to-cart
```

**Mong đợi:** HTTP 200 hoặc báo đã có trong giỏ

**Chấm:** Passed nếu chuyển được. N/A nếu khóa đã trong giỏ.

### IT_COMMERCE_03 – Xóa giỏ

**Bước:**
```
DELETE /api/student/commerce/cart
```

**Mong đợi:** HTTP 200/204

**Chấm:** Passed nếu xóa được.

### IT_COMMERCE_04 – Thêm lại

**Bước:**
```
Lặp COMMERCE_01
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu OK.

## 5. PayOS & Orders (IT - Payment)

- Role: LEARNER + MANAGER
- Account: Learner + classroom.manager@englishlab.vn / Password123!
- Goal: Quote, tạo link PayOS, webhook (negative), xem orders.

### IT_PAYMENT_01 – Tạo PayOS link

**Bước:**
```
POST /api/student/payments/payos/link Body: {"courseIds":[<id khóa public>]}
```

**Mong đợi:** HTTP 200 có checkoutUrl/orderCode

**Chấm:** Passed nếu tạo link. Failed nếu 500.

### IT_PAYMENT_02 – Quote

**Bước:**
```
POST /api/student/payments/quote {"courseIds":[<id>]}
```

**Mong đợi:** HTTP 200 có totalAmount

**Chấm:** Passed nếu 200.

### IT_PAYMENT_03 – Webhook thiếu chữ ký (negative)

**Bước:**
```
POST /api/payos/webhook body giả {}
```

**Mong đợi:** HTTP 400 (từ chối)

**Chấm:** Passed nếu bị từ chối. Failed nếu 404.

### IT_PAYMENT_04/05 – Manager xem orders

**Bước:**
```
GET /api/manager/payments/orders (token MANAGER)
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 6. Online Learning (IT - Course)

- Role: Public + LEARNER
- Account: Không token (public) / Learner
- Goal: Catalog công khai; content/progress cần đã mua/enroll.

### IT_COURSE_01 – Danh sách khóa public

**Bước:**
```
GET /api/online-courses (không token)
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_COURSE_02 – Chi tiết khóa

**Bước:**
```
GET /api/online-courses/{slugOrId}
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_COURSE_03/06 – Xem content learner

**Bước:**
```
GET /api/student/online-courses/{id}/content + Bearer
```

**Mong đợi:** 200 nếu đã enroll; 400 nếu chưa

**Chấm:** Passed nếu đã enroll và 200. N/A nếu chưa enroll (400 đúng business).

### IT_COURSE_04 – Cập nhật progress

**Bước:**
```
PATCH /api/student/online-courses/{id}/lessons/{lessonId}/progress {"completed":true}
```

**Mong đợi:** HTTP 200 nếu đủ quyền

**Chấm:** Passed/N/A tương tự.

### IT_COURSE_05 – Rating

**Bước:**
```
POST /api/student/online-courses/{id}/rating {"score":5,"comment":"ok"}
```

**Mong đợi:** HTTP 200/201

**Chấm:** Passed nếu gửi được. N/A nếu thiếu điều kiện.

## 7. Course Discussion (IT - Discuss)

- Role: LEARNER + CONTENT_MANAGER
- Account: Learner + content.manager@englishlab.vn
- Goal: List thảo luận; tạo/report cần enroll; CM xem reports.

### IT_DISCUSS_01 – Tạo thảo luận

**Bước:**
```
POST /api/student/online-courses/{courseId}/discussions {title, content}
```

**Mong đợi:** 200 nếu đã enroll

**Chấm:** Passed nếu tạo được. N/A nếu 400 chưa enroll.

### IT_DISCUSS_02 – List thảo luận

**Bước:**
```
GET /api/online-courses/{courseId}/discussions
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_DISCUSS_03/04 – Report thread

**Bước:**
```
POST /api/student/online-courses/discussions/{threadId}/reports
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu report được. N/A nếu không có thread.

### IT_DISCUSS_05 – CM moderation

**Bước:**
```
GET /api/content-manager/discussion-reports (token CM)
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 8. CM Online Courses (IT - Content)

- Role: CONTENT_MANAGER
- Account: content.manager@englishlab.vn / Password123!
- Goal: CM quản lý khóa học online.

### IT_CONTENT_01..04 – List khóa CM

**Bước:**
```
GET /api/content-manager/online-courses
```

**Mong đợi:** HTTP 200 danh sách

**Chấm:** Passed nếu 200. (Create/publish chi tiết: làm thêm POST/PUT theo UI nếu cô yêu cầu sâu).

## 9. Packages & Bundles (IT - Package)

- Role: CONTENT_MANAGER
- Account: content.manager@englishlab.vn
- Goal: Quản lý package/bundle.

### IT_PACKAGE_01..03 – List packages

**Bước:**
```
GET /api/content-manager/packages
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 10. Curriculum & Banks (IT - Curriculum)

- Role: CONTENT_MANAGER
- Account: content.manager@englishlab.vn
- Goal: Chương trình, ngân hàng bài, rubric.

### IT_CURRICULUM_01/05 – Curriculum programs

**Bước:**
```
GET /api/content-manager/curriculum-programs
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_CURRICULUM_02 – Exercise/Assessment bank

**Bước:**
```
GET /api/content-manager/exercise-bank hoặc /assessment-bank
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_CURRICULUM_03 – Learning paths

**Bước:**
```
GET /api/content-manager/learning-paths
```

**Mong đợi:** HTTP 200 hoặc N/A nếu chưa có API

**Chấm:** Ghi đúng thực tế.

### IT_CURRICULUM_04 – Rubrics

**Bước:**
```
GET /api/content-manager/rubrics
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 11. Enrollment Requests (IT - EnrollReq)

- Role: LEARNER + STAFF
- Account: Learner + staff@englishlab.vn
- Goal: HV gửi form tư vấn; Staff xem danh sách.

### IT_ENROLLREQ_01/04 – HV tạo request

**Bước:**
```
1) GET /api/course-offerings lấy id
2) POST /api/student/course-enrollment-requests
Body: courseOfferingId, contactName, contactEmail, contactPhone, consultationTrack
3) GET /api/student/course-enrollment-requests/my
```

**Mong đợi:** 200 nếu tạo mới được

**Chấm:** Passed nếu tạo + list /my OK. N/A nếu báo đã có form đang xử lý.

### IT_ENROLLREQ_02/03/05 – Staff list

**Bước:**
```
GET /api/staff/enrollment-requests (token STAFF)
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 12. TM Classroom Ops (IT - Classroom)

- Role: TRAINING_MANAGER (+ public)
- Account: training.manager@englishlab.vn / Password123!
- Goal: Offerings, registrations, waitlist reorder.

### IT_CLASS_01 – Public offerings

**Bước:**
```
GET /api/classroom-offerings
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_CLASS_02/08 – TM list lớp

**Bước:**
```
GET /api/training-manager/classrooms
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_CLASS_03 – TM chi tiết

**Bước:**
```
GET /api/training-manager/classrooms/{id}
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_CLASS_04/06 – Registrations

**Bước:**
```
GET /api/training-manager/classrooms/registrations?classroomOfferingId={id}
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_CLASS_05 – Reorder waitlist

**Bước:**
```
1) GET registrations?status=WAITLIST&classroomOfferingId=
2) Nếu ≥2 HV: PUT /api/training-manager/classrooms/{id}/waitlist/order {"enrollmentIds":[...]}
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu reorder OK. N/A nếu <2 HV waitlist.

### IT_CLASS_07 – Xem lớp trước khi gán GV

**Bước:**
```
GET classroom detail
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 13. Learner Classroom (IT - LearnerCls)

- Role: LEARNER
- Account: 0386852628z@gmail.com (cần đã được gán lớp)
- Goal: Lớp của tôi, session, homework, materials, gradebook.

### IT_LEARNERCLS_01 – My classrooms

**Bước:**
```
GET /api/student/classrooms/my-classrooms
```

**Mong đợi:** HTTP 200 (có thể [] nếu chưa gán)

**Chấm:** Passed nếu 200.

### IT_LEARNERCLS_02 – Sessions

**Bước:**
```
GET /api/student/classrooms/{id}/sessions
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200. N/A nếu không có lớp.

### IT_LEARNERCLS_03/05 – Homework

**Bước:**
```
GET /api/student/classrooms/{id}/homework
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_LEARNERCLS_04 – Materials

**Bước:**
```
GET /api/student/classrooms/{id}/materials
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_LEARNERCLS_06 – Gradebook của tôi

**Bước:**
```
GET /api/student/classrooms/{id}/gradebook/me
```

**Mong đợi:** HTTP 200/204

**Chấm:** Passed nếu 200/204.

## 14. Teacher Operations (IT - Teacher)

- Role: TEACHER
- Account: classroom.teacher1@englishlab.vn / Password123!
- Goal: Lớp được assign, homework, điểm danh, gradebook, change request.

### IT_TEACH_01/06 – Lớp assigned

**Bước:**
```
GET /api/teacher/classrooms/assigned
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_TEACH_02 – Homework

**Bước:**
```
GET /api/teacher/classrooms/{id}/homework
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_TEACH_03 – Điểm danh theo session

**Bước:**
```
1) GET .../sessions
2) GET /api/teacher/classrooms/sessions/{sessionId}/attendance
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200. N/A nếu chưa có session.

### IT_TEACH_04 – Gradebook lớp

**Bước:**
```
GET /api/teacher/classrooms/{id}/gradebook
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_TEACH_05 – Change requests của tôi

**Bước:**
```
GET /api/teacher/classrooms/requests/mine
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 15. Classroom Quiz (IT - Quiz)

- Role: TEACHER + LEARNER
- Account: Teacher + Learner
- Goal: List quiz; delete destructive → thường N/A trên demo.

### IT_QUIZ_01/02 – Teacher list quiz

**Bước:**
```
GET /api/teacher/classrooms/{offeringId}/quizzes
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_QUIZ_03 – Learner list quiz

**Bước:**
```
GET /api/student/classrooms/quizzes
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_QUIZ_04 – Xóa quiz

**Bước:**
```
DELETE /api/teacher/quizzes/{id} (chỉ quiz test tự tạo)
```

**Mong đợi:** HTTP 204

**Chấm:** N/A nếu không muốn xóa data demo.

## 16. Assessment & Placement (IT - Assess)

- Role: LEARNER
- Account: 0386852628z@gmail.com
- Goal: Placement, mock test; assessment khóa cần enroll.

### IT_ASSESS_01 – Placement hiện tại

**Bước:**
```
GET /api/student/placement-tests/current
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_ASSESS_02 – Submit placement thiếu đáp án (negative)

**Bước:**
```
POST /api/student/placement-tests/current/submit {"answers":[]}
```

**Mong đợi:** HTTP 400 hoặc 200 tùy trạng thái

**Chấm:** Passed nếu hành vi hợp lý (reject/accept). Failed nếu 500.

### IT_ASSESS_03/05 – Assessments theo khóa

**Bước:**
```
GET /api/student/online-courses/{courseId}/assessments
```

**Mong đợi:** 200 nếu enroll

**Chấm:** Passed nếu 200. N/A nếu 400 chưa enroll.

### IT_ASSESS_04/06 – Mock tests

**Bước:**
```
GET /api/student/mock-tests
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 17. Support Tickets (IT - Support)

- Role: LEARNER + MANAGER
- Account: Learner + classroom.manager@englishlab.vn
- Goal: Tạo/list ticket; staff xem.

### IT_SUPPORT_01 – Tạo ticket

**Bước:**
```
POST /api/student/support-tickets
Body: subject (≥5 ký tự), category=TECHNICAL|ACCOUNT|..., message (≥10 ký tự)
```

**Mong đợi:** HTTP 200/201

**Chấm:** Passed nếu tạo được.

### IT_SUPPORT_02 – List của tôi

**Bước:**
```
GET /api/student/support-tickets
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_SUPPORT_03 – Manager list

**Bước:**
```
GET /api/manager/support-tickets
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_SUPPORT_04 – Body rỗng (negative)

**Bước:**
```
POST {} 
```

**Mong đợi:** HTTP 400

**Chấm:** Passed nếu validation lỗi.

## 18. Administration (IT - Admin)

- Role: ADMIN
- Account: classroom.admin@englishlab.vn / Password123!
- Goal: Users, audit, config.

### IT_ADMIN_01/02 – List users

**Bước:**
```
GET /api/admin/users
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_ADMIN_03 – Audit logs

**Bước:**
```
GET /api/admin/audit-logs
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_ADMIN_04 – System config

**Bước:**
```
GET /api/admin/system/config
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 19. Lark Meetings (IT - Lark)

- Role: Public webhook + TM
- Account: TM cho sync
- Goal: Webhook challenge; sync recording.

### IT_LARK_01/02 – Webhook

**Bước:**
```
POST /api/lark/events body challenge/url_verification
```

**Mong đợi:** 200 nếu challenge đúng format hệ thống; có thể 400 nếu thiếu chữ ký

**Chấm:** Passed nếu 200. N/A nếu 400 do cấu hình Lark.

### IT_LARK_03 – Sync Lark session

**Bước:**
```
POST /api/training-manager/recordings/sessions/{sessionId}/sync-lark
```

**Mong đợi:** 200 nếu session thật

**Chấm:** N/A nếu session không tồn tại.

## 20. Infrastructure (IT - Infra)

- Role: TRAINING_MANAGER
- Account: training.manager@englishlab.vn
- Goal: Campus, room, session template.

### IT_INFRA_01 – Campuses

**Bước:**
```
GET /api/training-manager/infrastructure/campuses
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_INFRA_02 – Rooms

**Bước:**
```
GET /api/training-manager/infrastructure/rooms
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_INFRA_03 – Session templates

**Bước:**
```
GET /api/training-manager/infrastructure/session-templates
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 21. Reports & Revenue (IT - Report)

- Role: TM + CM
- Account: training.manager@englishlab.vn + content.manager@englishlab.vn
- Goal: Dashboard / doanh thu.

### IT_REPORT_01 – TM dashboard

**Bước:**
```
GET /api/training-manager/dashboard
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

### IT_REPORT_02 – Revenue analytics

**Bước:**
```
GET /api/content-manager/revenue/analytics
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 22. Classroom Proposals (IT - Proposal)

- Role: STAFF
- Account: staff@englishlab.vn
- Goal: Đề xuất mở lớp.

### IT_PROPOSAL_01..03 – List proposals

**Bước:**
```
GET /api/staff/classroom-proposals
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 23. Attendance Disputes (IT - Dispute)

- Role: LEARNER + TEACHER
- Account: Learner + Teacher
- Goal: Khiếu nại điểm danh.

### IT_DISPUTE_01 – HV xem disputes

**Bước:**
```
GET /api/student/attendance/disputes
```

**Mong đợi:** HTTP 200 (có thể [])

**Chấm:** Passed nếu 200.

### IT_DISPUTE_02/03 – GV pending

**Bước:**
```
GET /api/teacher/attendance-disputes/pending
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.

## 24. Learning Notes (IT - Notes)

- Role: LEARNER
- Account: 0386852628z@gmail.com
- Goal: Ghi chú bài học.

### IT_NOTES_01/02 – List notes

**Bước:**
```
GET /api/student/learning/notes
```

**Mong đợi:** HTTP 200

**Chấm:** Passed nếu 200.
