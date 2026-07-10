# Diagram Drawing Tracking Plan

File này dùng để theo dõi kế hoạch vẽ Class Diagram và Sequence Diagram theo `srs-usecase-diagram-map.md`.

Quy ước trạng thái:


| Trạng thái             | Ý nghĩa                                                |
| ---------------------- | ------------------------------------------------------ |
| Chưa vẽ                | Có đủ căn cứ để vẽ theo SRS/backend hiện tại           |
| Chưa vẽ được theo code | SRS có nhưng backend chưa làm tới, để lại vẽ sau       |
| Cần tách UC riêng      | Practice hoặc flow cần sequence riêng, không gộp chung |


## Thứ tự ưu tiên tổng thể


| Phase | Nhóm cần vẽ                                 | Mục tiêu                                                    |
| ----- | ------------------------------------------- | ----------------------------------------------------------- |
| 1     | Authentication / Account                    | Vẽ nền tảng user, role, token, profile, notification        |
| 2     | Online Course / Commerce / Learning         | Vẽ luồng học online, cart, wishlist, checkout, enrollment   |
| 3     | Manage Online Course / Syllabus / Placement | Vẽ các CRUD quản lý course, syllabus, placement exam        |
| 4     | Classroom Participation                     | Vẽ luồng học viên tham gia lớp học                          |
| 5     | Practice trong Classroom                    | Vẽ riêng từng practice theo từng UC                         |
| 6     | Teacher Manage Classroom Activities         | Vẽ giáo viên quản lý attendance, homework, practice content |
| 7     | Training Management / Reports               | Vẽ quản lý lớp, phân công, báo cáo                          |




## Phase 1 - Authentication / Account

Class diagram chung: `User`, `Role`, `AuthToken`, `UserProfile`, `AppNotification`.


| UC     | Use Case                    | Sequence diagram cần vẽ     | Trạng thái             | Ghi chú                                                           |
| ------ | --------------------------- | --------------------------- | ---------------------- | ----------------------------------------------------------------- |
| UC-01  | Register Account            | Register Account            | Chưa vẽ                | Register, verify OTP, resend OTP                                  |
| UC-03  | Login                       | Login                       | Chưa vẽ                | Email/password, Google login, invalid credentials, account locked |
| UC-04  | Forgot Password             | Forgot Password             | Chưa vẽ                | Request reset OTP, reset password, resend OTP                     |
| UC-05a | View Profile                | View Profile                | Chưa vẽ                | Retrieve and display current profile                              |
| UC-05b | Update Profile              | Update Profile              | Chưa vẽ                | Update profile, change password                                   |
| UC-06  | View Notifications          | View Notifications          | Chưa vẽ                | List notifications, mark read                                     |
| UC-07  | Submit Support Ticket       | Submit Support Ticket       | Chưa vẽ được theo code | Backend chưa có support ticket                                    |
| UC-42  | Manage User Accounts        | Manage User Accounts        | Chưa vẽ được theo code | Backend chưa có admin user CRUD rõ                                |
| UC-43  | Manage System Notifications | Manage System Notifications | Chưa vẽ được theo code | Code hiện có student read notification, chưa có admin broadcast   |
| UC-44  | Resolve Support Tickets     | Resolve Support Tickets     | Chưa vẽ được theo code | Backend chưa có support ticket resolve flow                       |




## Phase 2 - Online Course / Commerce / Learning

Class diagram chung: `OnlineCourse`, `CourseCategory`, `CartItem`, `WishlistItem`, `PaymentOrder`, `PaymentTransaction`, `PackageEnrollment`, `Lesson`, `LessonProgress`, `CourseMaterial`.


| UC    | Use Case                         | Sequence diagram cần vẽ          | Trạng thái | Ghi chú                                                                           |
| ----- | -------------------------------- | -------------------------------- | ---------- | --------------------------------------------------------------------------------- |
| UC-02 | View Public Courses              | View Public Courses              | Chưa vẽ    | Browse, search, filter, view detail                                               |
| UC-45 | Wishlist Courses                 | Wishlist Courses                 | Chưa vẽ    | Add wishlist, view wishlist, remove wishlist, add to cart from wishlist           |
| UC-46 | Add Courses to Cart              | Add Courses to Cart              | Chưa vẽ    | Add cart, view cart, remove item, move item to wishlist, continue payment         |
| UC-47 | Checkout                         | Checkout                         | Chưa vẽ    | Review order, apply coupon, PayOS, free order, cancel/failure/timeout             |
| UC-08 | Enroll in Course                 | Enroll in Course                 | Chưa vẽ    | Validate course/enrollment, checkout/payment, create enrollment, grant access     |
| UC-48 | Access Online Learning Materials | Access Online Learning Materials | Chưa vẽ    | Open enrolled course, select lesson, render content, mark complete, open material |




## Phase 3 - Manage Online Course / Syllabus / Placement

Class diagram chung: `Syllabus`, `CurriculumProgram`, `CoursebookFile`, `OnlineCourse`, `CourseModule`, `Lesson`, `PlacementExam`, `PlacementQuestion`, `QuestionOption`, `AnswerKey`, `AssessmentBankItem`, `FlashcardSet`.


| UC     | Use Case              | Sequence diagram cần vẽ | Trạng thái | Ghi chú                                                       |
| ------ | --------------------- | ----------------------- | ---------- | ------------------------------------------------------------- |
| UC-32a | Create Syllabus       | Create Syllabus         | Chưa vẽ    | Create syllabus/curriculum program                            |
| UC-32b | View Syllabus         | View Syllabus           | Chưa vẽ    | View/search list, view detail                                 |
| UC-32c | Update Syllabus       | Update Syllabus         | Chưa vẽ    | Update syllabus                                               |
| UC-32d | Delete Syllabus       | Delete Syllabus         | Chưa vẽ    | Delete/archive, block if linked to active course/classroom    |
| UC-33a | Create Online Course  | Create Online Course    | Chưa vẽ    | Create online course                                          |
| UC-33b | View Online Courses   | View Online Courses     | Chưa vẽ    | View/search course list, view detail/module structure         |
| UC-33c | Update Online Course  | Update Online Course    | Chưa vẽ    | Update course, module, lesson structure                       |
| UC-33d | Delete Online Course  | Delete Online Course    | Chưa vẽ    | Delete/archive, block if active enrolled learners             |
| UC-34a | Create Placement Exam | Create Placement Exam   | Chưa vẽ    | Create question/exam, import questions from file              |
| UC-34b | View Placement Exams  | View Placement Exams    | Chưa vẽ    | View/search/filter placement bank, view detail                |
| UC-34c | Update Placement Exam | Update Placement Exam   | Chưa vẽ    | Update exam/question, cancel edit, restrict affected attempts |
| UC-34d | Delete Placement Exam | Delete Placement Exam   | Chưa vẽ    | Delete/deactivate, cancel deletion, preserve attempts         |




## Phase 4 - Classroom Participation

Class diagram chung: `Classroom`, `ClassroomSession`, `ClassroomEnrollment`, `ClassroomMaterial`, `Homework`, `HomeworkSubmission`, `AttendanceRecord`, `GradebookEntry`, `AcademicReport`.


| UC    | Use Case                    | Sequence diagram cần vẽ               | Trạng thái | Ghi chú                                                                    |
| ----- | --------------------------- | ------------------------------------- | ---------- | -------------------------------------------------------------------------- |
| UC-09 | View Timetable              | View Timetable                        | Chưa vẽ    | Select week/month, load enrolled classroom sessions, view session detail   |
| UC-10 | Join Online Meeting         | Join Online Meeting                   | Chưa vẽ    | Open virtual session, join Lark link, link not ready                       |
| UC-11 | Access Learning Materials   | Access Classroom Learning Materials   | Chưa vẽ    | Open classroom material, render embedded viewer, material unavailable      |
| UC-12 | Download Learning Materials | Download Classroom Learning Materials | Chưa vẽ    | Verify downloadable classroom material, trigger download                   |
| UC-13 | Submit Homework             | Submit Homework                       | Chưa vẽ    | Upload file, validate file, submit, resubmit before deadline               |
| UC-14 | View Academic Report        | View Academic Report                  | Chưa vẽ    | Load grades, quiz scores, attendance, progress, calculate average/progress |




## Phase 5 - Practice Trong Classroom

Phần practice phải vẽ sequence riêng theo từng UC, không gộp chung. Class diagram có thể dùng chung với classroom practice: `ClassroomQuiz`, `Question`, `AnswerOption`, `AnswerKey`, `Attempt`, `AnswerSubmission`, `WritingSubmission`, `SpeakingSubmission`, `AudioSubmission`, `Flashcard`, `FlashcardProgress`.


| UC    | Use Case                 | Sequence diagram cần vẽ  | Trạng thái | Ghi chú                                                            |
| ----- | ------------------------ | ------------------------ | ---------- | ------------------------------------------------------------------ |
| UC-15 | Take Quiz                | Take Quiz                | Chưa vẽ    | Cần tách UC riêng; thuộc classroom/practice context                |
| UC-16 | Take Placement Exam      | Take Placement Exam      | Chưa vẽ    | Nên vẽ riêng với placement exam, không nhập vào classroom practice |
| UC-17 | Practice Writing Skill   | Practice Writing Skill   | Chưa vẽ    | Cần tách UC riêng                                                  |
| UC-18 | Practice Listening Skill | Practice Listening Skill | Chưa vẽ    | Cần tách UC riêng                                                  |
| UC-19 | Practice Speaking Skill  | Practice Speaking Skill  | Chưa vẽ    | Cần tách UC riêng                                                  |
| UC-20 | Practice Reading Skill   | Practice Reading Skill   | Chưa vẽ    | Cần tách UC riêng                                                  |
| UC-21 | Practice Flashcard       | Practice Flashcard       | Chưa vẽ    | Cần tách UC riêng                                                  |




## Phase 6 - Teacher Manage Classroom Activities

Class diagram chung: `TeacherAssignment`, `ClassroomSession`, `ClassroomAttendance`, `Syllabus`, `Homework`, `HomeworkSubmission`, `GradebookEntry`, `QuizPracticeContent`, `WritingPracticeContent`, `SpeakingPracticeContent`, `ReadingPracticeContent`, `ListeningPracticeContent`.


| UC    | Use Case                          | Sequence diagram cần vẽ           | Trạng thái             | Ghi chú                                                                      |
| ----- | --------------------------------- | --------------------------------- | ---------------------- | ---------------------------------------------------------------------------- |
| UC-22 | View Teaching Schedule            | View Teaching Schedule            | Chưa vẽ                | Select week/month/today, load assigned sessions                              |
| UC-23 | Manage Class Attendance           | Manage Class Attendance           | Chưa vẽ                | Load roster, set attendance status, save/update attendance                   |
| UC-24 | View Syllabus                     | Teacher View Syllabus             | Chưa vẽ                | Load linked syllabus, render viewer                                          |
| UC-25 | Download Syllabus                 | Teacher Download Syllabus         | Chưa vẽ                | Prepare syllabus file, trigger download                                      |
| UC-26 | Manage Homework                   | Manage Homework                   | Chưa vẽ                | Create, update, delete, grade/update score                                   |
| UC-27 | Manage Quiz Practice Content      | Manage Quiz Practice Content      | Chưa vẽ                | Create, update, delete quiz                                                  |
| UC-28 | Manage Writing Practice Content   | Manage Writing Practice Content   | Chưa vẽ được theo code | SRS section bị trùng nội dung UC-27, chưa có distinct writing manage flow rõ |
| UC-29 | Manage Speaking Practice Content  | Manage Speaking Practice Content  | Chưa vẽ                | Create, update, delete speaking exercise                                     |
| UC-30 | Manage Reading Practice Content   | Manage Reading Practice Content   | Chưa vẽ                | Create, update, delete reading exercise                                      |
| UC-31 | Manage Listening Practice Content | Manage Listening Practice Content | Chưa vẽ                | Create, update, delete listening exercise                                    |




## Phase 7 - Training Management / Reports

Class diagram chung: `TeacherProfile`, `TeacherPerformanceMetric`, `Classroom`, `TeacherAssignment`, `ClassroomEnrollment`, `OperationalReportData`, `RevenueAnalyticsData`, `RevenueExportFile`.


| UC    | Use Case                               | Sequence diagram cần vẽ                | Trạng thái | Ghi chú                                                                           |
| ----- | -------------------------------------- | -------------------------------------- | ---------- | --------------------------------------------------------------------------------- |
| UC-35 | View Teacher Profiles                  | View Teacher Profiles                  | Chưa vẽ    | Search teacher, view teacher profile with schedule/performance metrics            |
| UC-36 | Manage Classrooms                      | Manage Classrooms                      | Chưa vẽ    | Create, update, delete classroom                                                  |
| UC-37 | Assign Teacher to Classroom            | Assign Teacher to Classroom            | Chưa vẽ    | Search/select teacher, check schedule conflict, save assignment                   |
| UC-38 | Assign Learner to Classroom            | Assign Learner to Classroom            | Chưa vẽ    | Select learners, check capacity, add learners to roster                           |
| UC-39 | Evaluate Teacher Performance           | Evaluate Teacher Performance           | Chưa vẽ    | Aggregate metrics, enter feedback/rating, save evaluation                         |
| UC-40 | View Operational Report                | View Operational Report                | Chưa vẽ    | Select filters/date range, query operational data, render charts/tables           |
| UC-41 | View Revenue Analytic of Online Course | View Revenue Analytic of Online Course | Chưa vẽ    | Select filters/date range, calculate metrics, compare periods, drill down, export |




## Tổng hợp tracking


| Nhóm                                                  | Tổng UC/sequence | Chưa vẽ | Chưa vẽ được theo code | Ghi chú                                                        |
| ----------------------------------------------------- | ---------------- | ------- | ---------------------- | -------------------------------------------------------------- |
| Phase 1 - Authentication / Account                    | 10               | 6       | 4                      | Support ticket, admin user, admin notification chưa có code rõ |
| Phase 2 - Online Course / Commerce / Learning         | 6                | 6       | 0                      | Có thể vẽ ngay theo code hiện tại                              |
| Phase 3 - Manage Online Course / Syllabus / Placement | 12               | 12      | 0                      | Có thể vẽ theo SRS/code ở mức quản lý chính                    |
| Phase 4 - Classroom Participation                     | 6                | 6       | 0                      | Có thể vẽ ngay                                                 |
| Phase 5 - Practice Trong Classroom                    | 7                | 7       | 0                      | Practice phải tách sequence theo từng UC                       |
| Phase 6 - Teacher Manage Classroom Activities         | 10               | 9       | 1                      | UC-28 chưa rõ vì SRS/code chưa có distinct writing manage flow |
| Phase 7 - Training Management / Reports               | 7                | 7       | 0                      | Vẽ sau các phase ưu tiên                                       |
| Tổng                                                  | 58               | 53      | 5                      | Theo tracking hiện tại                                         |


