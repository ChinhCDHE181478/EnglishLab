# SRS Use Case Diagram Map

Tài liệu này đối chiếu SRS với các Mermaid source hiện có trong `C:\Users\Chinh\Downloads\sds-diagrams` và hành vi backend được ghi trong 7 SDS guide đã cập nhật. Trạng thái “đã vẽ” chỉ xác nhận file `.mmd` tồn tại; nó không có nghĩa mọi hành vi trong SRS đã được code hỗ trợ.

## Quy ước và tổng quan

- Có **51 parent UC** (`UC-01` đến `UC-51`). Các mục `UC-05a/b`, `UC-32a-d`, `UC-33a-d`, `UC-34a-d` là các flow con của parent UC tương ứng.
- Có **64 sequence source** (`SQ01`-`SQ64`) và **9 class source** (`CD01`, `CD02A`, `CD02B`, `CD03`-`CD08`), tổng cộng **73 Mermaid source**.
- Không áp dụng giả định “mỗi parent UC có đúng một sequence”: các flow authentication được tách thành nhiều file, PayOS webhook có file riêng, còn các CRUD management được tách theo operation.
- Tất cả **73 source** đã được vẽ và có **73 SVG render** cùng basename.

## Phase 1 / CD01 - Authentication and Common

**File:** `CD01-authentication-common.mmd`  
**Class inventory thực tế:** controllers `AuthController`, `UserController`, `StudentNotificationController`, `AdminUserController`; services `AuthService`, `AuthTokenService`, `GoogleAuthService`, `FacebookAuthService`, `UserService`, `AppNotificationService`, `AdminUserService`, `AuditLogService`, `UserRoleService`; entities `User`, `Role`, `AuthToken`, `AppNotification`, `AuditLog`; các repository tương ứng.

| UC | Use Case | Sequence source | Code alignment |
|---|---|---|---|
| UC-01 | Register Account | `SQ01-register-account.mmd`, `SQ02-verify-email.mmd`, `SQ03-resend-verification-otp.mmd` | Implemented |
| UC-03 | Login | `SQ04-login-email-password.mmd`, `SQ05-login-google.mmd`, `SQ06-login-facebook.mmd` | Implemented |
| UC-04 | Forgot Password | `SQ07-forgot-password.mmd`, `SQ08-reset-password.mmd` | Implemented |
| UC-05a | View Profile | `SQ09-view-profile.mmd` | Implemented |
| UC-05b | Update Profile | `SQ10-update-profile.mmd` | Implemented |
| UC-06 | View Notifications | `SQ11-view-notifications.mmd` | Implemented |
| UC-07 | Submit Support Ticket | Không có SQ | **Unsupported:** không có ticket model/controller/service |
| UC-42 | Manage User Accounts | `SQ64-manage-user-accounts.mmd` | **Implemented:** search/create/update/roles/status và audit |
| UC-43 | Manage System Notifications | Không có SQ | **Unsupported:** learner list/read có code, admin create/broadcast/delete không có |
| UC-44 | Resolve Support Tickets | Không có SQ | **Unsupported:** không có support-ticket resolution workflow |

## Phase 2 / CD02A-CD02B - Online Course, Commerce, and Learning

**Files:** `CD02A-course-commerce-enrollment.mmd`, `CD02B-online-learning-experience.mmd`  
**CD02A:** public catalog/category, wishlist/cart, checkout/PayOS/discount, ownership, payment và enrollment; primary mapping SQ12-SQ17.  
**CD02B:** enrolled modules/lessons/progress, progression assessment dependencies, discussion/reply/helpful/reaction/report/moderation, notes, access policy, notifications và audit; primary mapping SQ18 và SQ61-SQ63. SQ17 enrollment được cross-reference qua shared `User`, `OnlineCourse`, `LearningPackage`, `PackageEnrollment`.

| UC | Use Case | Sequence source | Code alignment |
|---|---|---|---|
| UC-02 | View Public Courses | `SQ12-view-public-courses.mmd` | Implemented; CD02A |
| UC-45 | Wishlist Courses | `SQ13-wishlist-courses.mmd` | Implemented; CD02A; không có cart-to-wishlist endpoint |
| UC-46 | Add Courses to Cart | `SQ14-add-courses-to-cart.mmd` | Implemented; CD02A |
| UC-47 | Checkout | `SQ15-checkout.mmd`, `SQ16-payos-webhook.mmd` | Implemented; CD02A; webhook được tách riêng |
| UC-08 | Enroll in Course | `SQ17-enroll-in-course.mmd` | Implemented; CD02A primary, CD02B cross-reference |
| UC-48 | Access Online Learning Materials | `SQ18-access-online-learning-materials.mmd` | Implemented; CD02B; authorized content/material URL, không có download endpoint riêng |
| UC-49 | Discuss in Course | `SQ61-discuss-in-course.mmd` | CD02B; implemented read/create/reply/reaction/helpful/resolve; **thiếu edit/delete thread và edit/delete reply** |
| UC-50 | Report Discussion | `SQ62-report-discussion.mmd` | CD02B; learner report là **create-only**; moderation queue/hide/dismiss được code hỗ trợ |
| UC-51 | Take Note in Course | `SQ63-take-note-in-course.mmd` | CD02B; implemented **list/create/update/delete**; không có **GET-one** |

## Phase 3 / CD03 - Content Management

**File:** `CD03-content-management.mmd`  
**Class inventory thực tế:** content-manager course/curriculum/placement controllers; `OnlineCourseService`, `CourseProgressService`, `CurriculumProgramService`, `PlacementTestDefinitionService`; entities `LearningPackage`, `OnlineCourse`, `CourseCategory`, `CourseModule`, `Lesson`, `CurriculumProgram`, `CurriculumUnit`, `AssessmentBankItem`, `PlacementTestDefinition`, `PlacementTestAttempt`; các repository tương ứng.

| UC flow | Use Case | Sequence source | Code alignment |
|---|---|---|---|
| UC-32a | Create Syllabus | `SQ19-create-syllabus.mmd` | Implemented như curriculum/program |
| UC-32b | View Syllabus | `SQ20-view-syllabus.mmd` | Implemented |
| UC-32c | Update Syllabus | `SQ21-update-syllabus.mmd` | Implemented |
| UC-32d | Delete Syllabus | `SQ22-delete-syllabus.mmd` | Archive/delete implemented; SRS active-link blocking phải được ghi là SRS-only nếu code không enforce |
| UC-33a | Create Online Course | `SQ23-create-online-course.mmd` | Implemented |
| UC-33b | View Online Courses | `SQ24-view-online-courses.mmd` | Implemented |
| UC-33c | Update Online Course | `SQ25-update-online-course.mmd` | Implemented |
| UC-33d | Delete Online Course | `SQ26-delete-online-course.mmd` | Soft delete/archive implemented; **SRS active-enrollment guard không có trong code** |
| UC-34a | Create Placement Exam | `SQ27-create-placement-exam.mmd` | **SRS-only unsupported:** không có create/import endpoint; GET lazy-seed singleton |
| UC-34b | View Placement Exams | `SQ28-view-placement-exams.mmd` | Implemented |
| UC-34c | Update Placement Exam | `SQ29-update-placement-exam.mmd` | Implemented |
| UC-34d | Delete Placement Exam | `SQ30-delete-placement-exam.mmd` | **Không có DELETE**; code chỉ PUT singleton với `active=false`, giữ attempts |

## Phase 4 / CD04 - Classroom Participation

**File:** `CD04-learner-classroom-participation.mmd`  
**Class inventory thực tế:** `StudentClassroomController`; offering, homework, gradebook, content, material-sync, attachment-storage và attendance services; entities `ClassroomOffering`, `ClassroomEnrollment`, `ClassroomSession`, material/syllabus/announcement/homework/submission/attendance/gradebook; các repository tương ứng.

| UC | Use Case | Sequence source | Code alignment |
|---|---|---|---|
| UC-09 | View Timetable | `SQ31-view-timetable.mmd` | Implemented |
| UC-10 | Join Online Meeting | `SQ32-join-online-meeting.mmd` | Implemented |
| UC-11 | Access Learning Materials | `SQ33-access-classroom-learning-materials.mmd` | Implemented |
| UC-12 | Download Learning Materials | `SQ34-download-classroom-learning-materials.mmd` | Chỉ trả stored `fileUrl`; không có backend download/downloadability check |
| UC-13 | Submit Homework | `SQ35-submit-homework.mmd` | Implemented; upload attachment trước khi submit URL |
| UC-14 | View Academic Report | `SQ36-view-academic-report.mmd` | Published gradebook entry và attendance là endpoint riêng |

## Phase 5 / CD05 - Classroom Practice

**File:** `CD05-classroom-practice-assessment.mmd`  
**Class inventory thực tế:** quiz, student assessment, placement, flashcard-practice và student-course controllers; quiz/AI assessment/audio/placement/flashcard/course services và policies; entities `ClassroomQuiz`, `ClassroomQuizQuestion`, `ClassroomQuizAttempt`, `CourseAssessment`, `AssessmentRubric`, `AssessmentSubmission`, `PlacementTestAttempt`, `OnlineCourse`, `VocabularyProgress`; các repository tương ứng.

| UC | Use Case | Sequence source | Code alignment / SRS-only gap |
|---|---|---|---|
| UC-15 | Take Quiz | `SQ37-take-quiz.mmd` | Implemented submit; **SRS countdown/forced auto-submit unsupported** |
| UC-16 | Take Placement Exam | `SQ38-take-placement-exam.mmd` | Implemented current-test read/submit; draft/timer là client-side, không có start/autosave API |
| UC-17 | Practice Writing Skill | `SQ39-practice-writing-skill.mmd` | Implemented; **SRS resumable periodic autosave backend unsupported** |
| UC-18 | Practice Listening Skill | `SQ40-practice-listening-skill.mmd` | Implemented; timer/auto-submit là client-side |
| UC-19 | Practice Speaking Skill | `SQ41-practice-speaking-skill.mmd` | Implemented upload/submit; **SRS five-minute cap không được backend enforce** |
| UC-20 | Practice Reading Skill | `SQ42-practice-reading-skill.mmd` | Implemented; timer/auto-submit là client-side |
| UC-21 | Practice Flashcard | `SQ43-practice-flashcard.mmd` | Implemented read và progress qua hai boundaries |

## Phase 6 / CD06 - Teacher Classroom Operations

**File:** `CD06-teacher-classroom-operations.mmd`  
**Class inventory thực tế:** `TeacherClassroomController`, `ClassroomQuizController`, `ExerciseBankController`; offering/attendance/homework/content/quiz/exercise-bank services; entities offering/assignment/session/attendance/homework/submission/material/quiz/question và `ExerciseBankItem`; các repository tương ứng.

| UC | Use Case | Sequence source | Code alignment / SRS-only gap |
|---|---|---|---|
| UC-22 | View Teaching Schedule | `SQ44-view-teaching-schedule.mmd` | Assigned classrooms + sessions; không có aggregate schedule/date filter |
| UC-23 | Manage Class Attendance | `SQ45-manage-class-attendance.mmd` | Implemented; current ownership/lock enforcement hạn chế |
| UC-24 | View Syllabus | `SQ46-teacher-view-syllabus.mmd` | **Không có teacher syllabus endpoint riêng**; detail chỉ có `syllabusSummary` |
| UC-25 | Download Syllabus | `SQ47-teacher-download-syllabus.mmd` | **SRS-only unsupported:** không có syllabus generation/download endpoint |
| UC-26 | Manage Homework | `SQ48-manage-homework.mmd` | Implemented list/create/update/delete/submissions/grade |
| UC-27 | Manage Quiz Practice Content | `SQ49-manage-quiz-practice-content.mmd` | List/create/open/close/delete; **content update unsupported** |
| UC-28 | Manage Writing Practice Content | `SQ50-manage-writing-practice-content.mmd` | **SRS teacher operation unsupported:** CRUD chỉ ở content-manager exercise bank |
| UC-29 | Manage Speaking Practice Content | `SQ51-manage-speaking-practice-content.mmd` | Cùng role mismatch; teacher CRUD unsupported |
| UC-30 | Manage Reading Practice Content | `SQ52-manage-reading-practice-content.mmd` | Cùng role mismatch; teacher CRUD unsupported |
| UC-31 | Manage Listening Practice Content | `SQ53-manage-listening-practice-content.mmd` | Cùng role mismatch; không có audio storage/upload |

## Phase 7 / CD07 - Training Management

**File:** `CD07-training-management.mmd`  
**Class inventory thực tế:** `TrainingManagerClassroomController`, `ClassroomOfferingService`, `ClassroomConflictService`; `User`, offering, teacher assignment, enrollment, session, tuition payment, room; các repository tương ứng.

| UC | Use Case | Sequence source | Code alignment / SRS-only gap |
|---|---|---|---|
| UC-35 | View Teacher Profiles | `SQ54-view-teacher-profiles.mmd` | Chỉ teacher picker; **profile detail/search/paging/history unsupported** |
| UC-36 | Manage Classrooms | `SQ55-manage-classrooms.mmd` | List/get/create/update/publish/close; **SRS delete/archive/filter/paging unsupported** |
| UC-37 | Assign Teacher to Classroom | `SQ56-assign-teacher-to-classroom.mmd` | Implemented assign/replace |
| UC-38 | Assign Learner to Classroom | `SQ57-assign-learner-to-classroom.mmd` | Implemented registration/confirm/tuition/conflict/assign và direct enroll |
| UC-39 | Evaluate Teacher Performance | `SQ58-evaluate-teacher-performance.mmd` | **SRS-only unsupported:** không có teacher evaluation subsystem |

## Phase 7 / CD08 - Reports

**File:** `CD08-reports.mmd`  
**Class inventory thực tế:** `TrainingManagerDashboardController`, `ContentManagerRevenueController`, `TrainingManagerOpsService`, `PaymentService`; offering/enrollment/change-request/session/payment repositories và entities; dashboard/revenue response models.

| UC | Use Case | Sequence source | Code alignment / SRS-only gap |
|---|---|---|---|
| UC-40 | View Operational Report | `SQ59-view-operational-report.mmd` | Fixed training-manager dashboard; **SRS date/filter/detail/export unsupported** |
| UC-41 | View Revenue Analytic of Online Course | `SQ60-view-revenue-analytics.mmd` | Fixed content-manager analytics; **SRS date/course filter, comparison, export/detail unsupported** |

## Final counts

| Artifact / scope | Count |
|---|---:|
| Parent use cases | **51** |
| Sequence diagram source files | **64** |
| Class diagram source files | **9** |
| Total Mermaid source files | **73** |
| Existing SVG render files | **73** |
