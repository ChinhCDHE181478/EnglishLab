# Integration Test Analysis — EnglishLab (SEP490_G23)

- Generated: 2026-07-28 (**v4.0 full-project coverage**)
- Scope: entire backend business surface (~56 RestControllers), grouped into integration modules.
- Sources: backend source, SRS Report3, srs-usecase-diagram-map.md, Excel template, controller inventory.

## A. Project overview

| Item | Fact from repo |
|---|---|
| Backend | Java 21, Spring Boot 4.0.6 (`backend/pom.xml`) |
| Frontend | React + Vite (`frontend/`) |
| Database | PostgreSQL via Spring Data JPA |
| AuthN/Z | Spring Security + JWT (`JwtAuthenticationFilter`, `SecurityConfig`) |
| API docs | springdoc OpenAPI (`/swagger-ui`, `/v3/api-docs`) |
| Mail | `spring-boot-starter-mail` (OTP/verification/business mail) |
| Payments | PayOS (`vn.payos:payos-java`, `PayosWebhookController`) |
| Meetings | Lark webhook/events (`LarkWebhookController`) |
| AI | Gemini evaluation client (assessment speaking/writing) |
| Run backend | Spring Boot main `BackendApplication`; typical `mvnw spring-boot:run` with `.env` |
| Run tests | `mvnw test` / targeted `-Dtest=...`; mostly Mockito unit tests + few `@SpringBootTest` |
| Testcontainers | Not present in `pom.xml` (as surveyed) |

### Main directories

- `backend/src/main/java/.../controller|service|repository|entity|security|migration|seed`
- `backend/src/test/java/...`
- `frontend/src/pages|api|components`
- `docs/` / root SRS extracts

## B. Component inventory (representative)

| Layer | Component | Path | Responsibility | Dependencies |
|---|---|---|---|---|
| Controller | AuthController | controller/AuthController.java | Register/login/verify/OTP/social | AuthService |
| Controller | UserController | controller/UserController.java | Profile/avatar/password/prefs | UserService, NotificationPreferenceService |
| Controller | StudentCommerceController | controller/commerce/... | Cart/wishlist | Commerce services |
| Controller | StudentPaymentController | controller/payment/... | PayOS link/orders | PaymentService |
| Controller | PayosWebhookController | controller/payment/... | Payment webhook | PaymentService |
| Controller | PublicOnlineCourseController | controller/course/... | Public catalog | OnlineCourseService |
| Controller | StudentOnlineCourseController | controller/course/... | Enrollment content/progress | OnlineCourse services |
| Controller | PublicClassroomController | controller/classroom/... | Public offerings | ClassroomOfferingService |
| Controller | TrainingManagerClassroomController | controller/classroom/... | TM classroom + enrollments | ClassroomOfferingService, TuitionProofService |
| Controller | TeacherClassroomController | controller/classroom/... | Teacher ops | Homework/attendance/gradebook services |
| Controller | StudentClassroomController | controller/classroom/... | Learner classroom | Offering/homework/tuition proof |
| Controller | PlacementTestController | controller/assessment/... | Placement current/submit | Placement services |
| Controller | StudentSupportTicketController | controller/support/... | Learner tickets | SupportTicketService |
| Controller | AdminUserController | controller/admin/... | Admin users | AdminUserService |
| Security | SecurityConfig | config/SecurityConfig.java | Route authorization | Jwt filter, UserDetails |
| Security | JwtAuthenticationFilter | security/... | Bearer JWT parse | Token service |
| Service | ClassroomOfferingServiceImpl | service/classroom/impl/... | Enrollment/waitlist/tuition | Repos + notifications |
| Service | NotificationPreferenceServiceImpl | service/notification/impl/... | Channel prefs | notification_preferences |
| Service | AppNotificationServiceImpl | service/notification/impl/... | Persist in-app notifications | prefs gate + app_notifications |
| Service | PaymentService | service/payment/... | Orders + webhook side effects | payment_orders, PayOS |
| Entity/DB | User | entity/User.java | users | roles |
| Entity/DB | ClassroomEnrollment | entity/classroom/... | classroom_enrollments | offering, student |
| Entity/DB | PaymentOrder | entity/payment/... | payment_orders | user/courses |
| Entity/DB | SupportTicket | entity/support/... | support_tickets | messages |
| External | PayOS | payos-java + webhook | Checkout | PaymentService |
| External | Mail | JavaMailSender services | OTP/business mail | Auth/classroom mail services |
| External | Lark | LarkWebhookController | Meeting events | classroom sessions |
| External | Gemini AI | GeminiAiEvaluationClientImpl | Speaking/writing eval | Assessment services |

## C. API inventory (verified mappings)

| Method | Endpoint | Controller | Role | Main flow |
|---|---|---|---|---|
| POST | /api/auth/register | AuthController | permitAll | Create user + verification token |
| POST | /api/auth/login | AuthController | permitAll | JWT issue |
| POST | /api/auth/verify-email | AuthController | permitAll | Verify OTP |
| POST | /api/auth/forgot-password | AuthController | permitAll | Reset OTP |
| POST | /api/auth/reset-password | AuthController | permitAll | Password update |
| GET/PUT | /api/user/me | UserController | authenticated | Profile |
| GET/PUT | /api/user/me/notification-preferences | UserController | authenticated | Prefs upsert |
| GET/POST/DELETE | /api/student/commerce/cart* | StudentCommerceController | LEARNER(+) | Cart |
| GET/POST/DELETE | /api/student/commerce/wishlist* | StudentCommerceController | LEARNER(+) | Wishlist |
| POST | /api/student/payments/payos/link | StudentPaymentController | LEARNER(+) | Create payment_orders |
| POST | /api/payos/webhook | PayosWebhookController | permitAll | Confirm payment → access |
| GET | /api/online-courses/** | PublicOnlineCourseController | permitAll GET | Catalog |
| GET/PATCH | /api/student/online-courses/** | StudentOnlineCourseController | LEARNER(+) | Content/progress |
| GET | /api/classroom-offerings/** | PublicClassroomController | permitAll GET | Public classes |
| * | /api/training-manager/classrooms/** | TrainingManagerClassroomController | STAFF/TM/MANAGER/ADMIN | Ops + enrollments |
| * | /api/teacher/classrooms/** | TeacherClassroomController | TEACHER(+) | Teaching ops |
| * | /api/student/classrooms/** | StudentClassroomController | LEARNER(+) | Learner classroom |
| GET/POST | /api/student/placement-tests/current* | PlacementTestController | LEARNER(+) | Placement |
| * | /api/student/support-tickets/** | StudentSupportTicketController | LEARNER(+) | Tickets |
| * | /api/manager|/staff|/training-manager/support-tickets/** | ManagerSupportTicketController | staff roles | Claim/reply |
| * | /api/admin/** | AdminUserController | ADMIN | User admin |
| GET/PATCH | /api/student/notifications/** | StudentNotificationController | authenticated learner path | In-app notifications |

## D. Business flows (examples from code)

1. `POST /api/auth/register` → AuthController → AuthService → UserRepository + AuthTokenRepository → `users`/`auth_tokens` → (mail) → response
2. `POST /api/auth/login` → AuthService → JWT → response; then `GET /api/user/me` → JwtFilter → UserService → `users`
3. `POST /api/student/payments/payos/link` → PaymentService → `payment_orders` → PayOS API → checkout URL
4. `POST /api/payos/webhook` → PaymentService → update `payment_orders` → grant course ownership/enrollment
5. TM `confirm` → `tuition` → `assign` → ClassroomOfferingService → `classroom_enrollments` (+ payments) → AppNotification optional
6. Teacher `POST .../homework` → ClassroomHomeworkService → `classroom_homework` → optional homework mail gated by NotificationPreferenceService
7. `PUT /api/user/me/notification-preferences` → NotificationPreferenceService → `notification_preferences` → later `AppNotificationService.createForUser` respects in-app flag
8. `POST /api/student/support-tickets` → SupportTicketService → `support_tickets` + `support_ticket_messages`

## E. Integration points

| ID | Source | Target | Type | Related req | Risk |
|---|---|---|---|---|---|
| IP-01 | AuthController | User/AuthToken repos | REST+DB | UC-01/03/04 | OTP/mail dependency |
| IP-02 | JwtFilter | Protected controllers | Security | All authenticated UC | Wrong role mapping |
| IP-03 | PaymentService | PayOS + payment_orders | External+DB | UC-47 | Webhook idempotency |
| IP-04 | PayOS webhook | Course ownership | Webhook+DB | UC-08/47 | Duplicate grants |
| IP-05 | ClassroomOfferingService | enrollments+tuition+notifications | Transaction+DB | UC-38 | Partial status updates |
| IP-06 | AppNotificationService | notification_preferences | Direct service | UC-06 + prefs | Channel bypass |
| IP-07 | HomeworkService | Mail service | External | UC-26 | Mail failure vs DB commit |
| IP-08 | Assessment services | Gemini client | External | UC-17/19 | AI timeout consistency |
| IP-09 | SupportTicketService | tickets+messages | DB transaction | UC-07/44 | Cross-user access |
| IP-10 | AdminUserService | users+roles+audit | DB multi-table | UC-42 | Orphan roles |
| IP-11 | Lark webhook | classroom_sessions | Webhook | UC-10 | Invalid event handling |
| IP-12 | reorderWaitlist service | classroom_enrollments.waitlist_priority | Service/DB | Waitlist priority | Controller mapping may be missing |

## Module partitioning rationale

Modules are grouped by business capability (auth, account, commerce, online learning, classroom ops, teacher ops, assessment, content management, support, admin), not one-sheet-per-controller.

- **AUTH (Authentication)**: AuthController, AuthService, UserRepository, AuthTokenRepository, JwtAuthenticationFilter
- **USER (Account Profile)**: UserController, UserService, AvatarStorageService, UserRepository
- **NOTIF (Notifications)**: UserController, NotificationPreferenceService, StudentNotificationController, AppNotificationService
- **COMMERCE (Cart & Wishlist)**: StudentCommerceController, StudentCommerceService
- **PAYMENT (PayOS & Orders)**: StudentPaymentController, PayosWebhookController, PaymentService, payment_orders
- **COURSE (Online Learning)**: PublicOnlineCourseController, StudentOnlineCourseController, OnlineCourseService
- **DISCUSS (Course Discussion)**: CourseDiscussionController, DiscussionModerationController, CourseDiscussionService, DiscussionModerationService
- **CONTENT (CM Online Courses)**: ContentManagerOnlineCourseController, OnlineCourseService, version/category services
- **PACKAGE (Packages & Bundles)**: ContentManagerPackageController, LearningPackageManagementService, packages
- **CURRICULUM (Curriculum & Banks)**: ContentManagerCurriculumController, CurriculumProgramService, ExerciseBankController, AssessmentRubricController, LearningPathController
- **ENROLLREQ (Enrollment Requests)**: StudentEnrollmentRequestController, StaffEnrollmentRequestController, EnrollmentRequestService
- **CLASS (TM Classroom Ops)**: PublicClassroomController, TrainingManagerClassroomController, ClassroomOfferingService
- **LEARNERCLS (Learner Classroom)**: StudentClassroomController, ClassroomOfferingService, ClassroomHomeworkService, TuitionProofService, related services
- **TEACH (Teacher Operations)**: TeacherClassroomController, ClassroomHomeworkService, ClassroomAttendanceService, ClassroomGradebookService, ClassroomChangeRequestService
- **QUIZ (Classroom Quiz)**: ClassroomQuizController, ClassroomQuizService, classroom_quizzes/questions/attempts
- **ASSESS (Assessment & Placement)**: PlacementTestController, StudentAssessmentController, StudentMockTestController, PlacementTestService, AiAssessmentService, MockTestService
- **SUPPORT (Support Tickets)**: StudentSupportTicketController, ManagerSupportTicketController, SupportTicketService
- **ADMIN (Administration)**: AdminUserController, AdminSystemController, AdminAuditLogController, AdminUserService, AuditLogService
- **LARK (Lark Meetings)**: LarkWebhookController, LarkWebhookService, LarkMeetingService, classroom_sessions
- **INFRA (Infrastructure)**: TrainingManagerInfrastructureController, ClassroomInfrastructureService
- **REPORT (Reports & Revenue)**: TrainingManagerDashboardController, ContentManagerRevenueController, TrainingManagerOpsService, PaymentService
- **PROPOSAL (Classroom Proposals)**: StaffClassroomProposalController, ManagerClassroomProposalController, ClassroomProposalService
- **DISPUTE (Attendance Disputes)**: ClassroomAttendanceDisputeController, ClassroomAttendanceDisputeService
- **NOTES (Learning Notes)**: StudentLearningExperienceController, LearnerLearningExperienceService, learner_lesson_notes

## SRS vs source mismatches (selected)

| Topic | SRS / older map | Current code evidence | IT handling |
|---|---|---|---|
| Support tickets UC-07/44 | Older map: unsupported | `StudentSupportTicketController` + `ManagerSupportTicketController` exist | Covered in SUPPORT module |
| Waitlist reorder API | Implemented historically | `ClassroomOfferingService.reorderWaitlist` exists; **no** `@PutMapping(.../waitlist/order)` found in TM controller at survey time | IT_CLASS_04 notes service-level IT + API gap |
| Placement create/delete | SRS CRUD | Definition often singleton lazy-seed; limited DELETE | ASSESS cases stick to current/submit |
| Teacher skill-bank CRUD | SRS teacher manages 4 skills content | Exercise bank primarily content-manager | Not claimed as TEACH coverage |
| Download materials | SRS downloadability | Often returns stored URL only | CLASS learner access case without fake download API |
| Roles | Training Manager actor | `STAFF` + legacy `TRAINING_MANAGER` both in `RoleEnum`; Security uses both on TM paths | AuthZ cases use real role names |


## Open questions

1. Exact PayOS webhook signature verification mode in local/test profiles.
2. Whether waitlist HTTP endpoint was intentionally removed or regressed.
3. Preferred IT DB strategy (shared PostgreSQL test DB vs future Testcontainers)—pom currently has no Testcontainers.
4. Some FR section pages in SRS extract are sparse (TOC points many FR subsections to same page number)—traceability uses UC IDs as stable anchors.
