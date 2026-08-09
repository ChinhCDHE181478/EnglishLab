# Integration Test Plan — EnglishLab

Date: 2026-07-28

## Modules

| Module code | Module name | Sheet | #TCs | SRS |
|---|---|---|---:|---|
| AUTH | Authentication | IT - Auth | 10 | UC-01, UC-03, UC-04 |
| USER | Account Profile | IT - User | 5 | UC-05 |
| NOTIF | Notifications | IT - Notif | 5 | UC-06 |
| COMMERCE | Cart & Wishlist | IT - Commerce | 4 | UC-45, UC-46 |
| PAYMENT | PayOS & Orders | IT - Payment | 5 | UC-47, UC-08 |
| COURSE | Online Learning | IT - Course | 6 | UC-02, UC-08, UC-48 |
| DISCUSS | Course Discussion | IT - Discuss | 5 | UC-49, UC-50 |
| CONTENT | CM Online Courses | IT - Content | 4 | UC-33 |
| PACKAGE | Packages & Bundles | IT - Package | 3 | Package management |
| CURRICULUM | Curriculum & Banks | IT - Curriculum | 5 | UC-32 |
| ENROLLREQ | Enrollment Requests | IT - EnrollReq | 5 | Enrollment request pipeline |
| CLASS | TM Classroom Ops | IT - Classroom | 8 | UC-36, UC-37, UC-38 |
| LEARNERCLS | Learner Classroom | IT - LearnerCls | 6 | UC-09–UC-14 |
| TEACH | Teacher Operations | IT - Teacher | 6 | UC-22, UC-23, UC-26 |
| QUIZ | Classroom Quiz | IT - Quiz | 4 | UC-15, UC-27 |
| ASSESS | Assessment & Placement | IT - Assess | 6 | UC-16–UC-20 |
| SUPPORT | Support Tickets | IT - Support | 4 | UC-07, UC-44 |
| ADMIN | Administration | IT - Admin | 4 | UC-42 |
| LARK | Lark Meetings | IT - Lark | 3 | UC-10 |
| INFRA | Infrastructure | IT - Infra | 3 | Scheduling infrastructure |
| REPORT | Reports & Revenue | IT - Report | 2 | UC-40, UC-41 |
| PROPOSAL | Classroom Proposals | IT - Proposal | 3 | Classroom proposals |
| DISPUTE | Attendance Disputes | IT - Dispute | 3 | Attendance disputes |
| NOTES | Learning Notes | IT - Notes | 2 | UC-51 |

**Total test cases: 111**


## AUTH — Authentication

Requirement scope: Integrate AuthController with AuthService and persistence for register/verify/login/OTP reset.


### Register & verify

#### IT_AUTH_01: Verify registering an account through AuthController persists user and verification token via AuthService.

- **Procedure:**
1. Call POST /api/auth/register via MockMvc with a unique email, password and fullName.
2. AuthController.register() delegates to AuthService.register().
3. AuthService creates the account through UserRepository.save() and stores a verification OTP through AuthTokenRepository.
4. Query the users and auth_tokens tables for the new email.

- **Expected:**
Response is 200/201 with a success message.
One users row is inserted with hashed password (not plaintext).
One verification auth_tokens row is linked to that user.
No access token is required for this call.

- **Pre:** Database is available; email is unused; mail sender is stubbed.

- **Round 1:** Pending

#### IT_AUTH_02: Verify duplicate registration is rejected by AuthService without inserting a second users row.

- **Procedure:**
1. Seed an existing LEARNER in users.
2. Call POST /api/auth/register via MockMvc with the same email.
3. AuthController delegates to AuthService.register() which checks UserRepository before insert.
4. Count users rows for that email.

- **Expected:**
Response is 4xx with a duplicate-email business error.
Exactly one users row remains for the email.
No extra verification token is created for a second account.

- **Pre:** An active user with the target email already exists.

- **Round 1:** Pending

#### IT_AUTH_03: Verify email verification activates the account through AuthService and AuthTokenRepository.

- **Procedure:**
1. Register a user and read the OTP from auth_tokens.
2. Call POST /api/auth/verify-email via MockMvc with email and OTP.
3. AuthController.verifyEmail() delegates to AuthService.verifyEmail().
4. AuthService validates the token via AuthTokenRepository and updates the user via UserRepository.
5. Reload users and auth_tokens.

- **Expected:**
Response is 200 OK.
users.email_verified becomes true.
Verification token is consumed/expired per AuthService rules.
Subsequent login with the password succeeds.

- **Pre:** A pending verification user and valid OTP exist.

- **Round 1:** Pending

#### IT_AUTH_04: Verify invalid OTP is rejected and leaves the user unverified.

- **Procedure:**
1. Seed an unverified user with a real OTP in auth_tokens.
2. Call POST /api/auth/verify-email via MockMvc with a wrong OTP.
3. AuthService.verifyEmail() validates against AuthTokenRepository and must not update UserRepository verification flag.
4. Query users.email_verified.

- **Expected:**
Response is 4xx.
email_verified remains false.
Account is not activated.

- **Pre:** An unverified user exists.

- **Round 1:** Pending


### Login & security

#### IT_AUTH_05: Verify login through AuthController returns a JWT accepted by the security filter and UserController.

- **Procedure:**
1. Seed a verified LEARNER with a known password hash.
2. Call POST /api/auth/login via MockMvc.
3. AuthController.login() delegates to AuthService.login() which authenticates via UserRepository/UserDetails.
4. Call GET /api/user/me via MockMvc with Authorization Bearer accessToken.
5. JwtAuthenticationFilter authenticates the request before UserController.getCurrentUser().

- **Expected:**
Login response is 200 OK and contains accessToken plus user payload.
GET /api/user/me returns 200 with the same email/id.
JWT subject matches the authenticated user.

- **Pre:** A verified LEARNER account exists.

- **Round 1:** Pending

#### IT_AUTH_06: Verify wrong password login fails in AuthService and issues no usable token.

- **Procedure:**
1. Seed a verified user.
2. Call POST /api/auth/login via MockMvc with an incorrect password.
3. AuthService.login() rejects authentication before issuing JWT.

- **Expected:**
Response is 401/403.
No valid accessToken is returned.
Protected endpoints remain inaccessible without a token.

- **Pre:** A verified user exists.

- **Round 1:** Pending

#### IT_AUTH_07: Verify missing JWT is blocked by SecurityFilterChain before UserController executes.

- **Procedure:**
1. Call GET /api/user/me via MockMvc without Authorization header.
2. Observe that JwtAuthenticationFilter / authorizeHttpRequests rejects the call before UserService is invoked.

- **Expected:**
Response is 401 or 403.
No UserResponse body is returned.
Confirms controller-security integration.

- **Pre:** Spring Security configuration is active.

- **Round 1:** Pending


### Password recovery

#### IT_AUTH_08: Verify forgot-password and reset-password flow updates credentials through AuthService.

- **Procedure:**
1. Seed a verified user and capture the current password hash.
2. Call POST /api/auth/forgot-password via MockMvc.
3. AuthService creates a reset token via AuthTokenRepository.
4. Call POST /api/auth/reset-password via MockMvc with OTP and new password.
5. AuthService updates the hash via UserRepository.save().
6. Login with the new password and attempt the old password.

- **Expected:**
Forgot-password returns a generic success response.
Reset succeeds and users.password hash changes.
Login works only with the new password.

- **Pre:** Verified user exists; mail is stubbed.

- **Round 1:** Pending

#### IT_AUTH_09: Verify invalid reset OTP does not change the password hash.

- **Procedure:**
1. Seed a verified user and capture password hash.
2. Call POST /api/auth/reset-password via MockMvc with an invalid OTP.
3. AuthService.resetPassword() rejects via AuthTokenRepository validation.
4. Compare password hash in users.

- **Expected:**
Response is 4xx.
Password hash is unchanged.
Old credentials still authenticate.

- **Pre:** A verified user exists.

- **Round 1:** Pending

#### IT_AUTH_10: Verify resend-verification rotates or recreates a verification token through AuthService.

- **Procedure:**
1. Seed an unverified user.
2. Call POST /api/auth/resend-verification via MockMvc.
3. AuthController delegates to AuthService which writes AuthTokenRepository.
4. Query auth_tokens for an active verification token.

- **Expected:**
Response is 200 OK.
An active verification token exists for the user.
No account is marked verified by this call alone.

- **Pre:** An unverified user exists; mail is stubbed.

- **Round 1:** Pending


## USER — Account Profile

Requirement scope: Integrate UserController profile/avatar/password with UserService and UserRepository.


### Profile

#### IT_USER_01: Verify fetching the current profile via UserController loads data through UserService and UserRepository.

- **Procedure:**
1. Authenticate as LEARNER and obtain a JWT.
2. Call GET /api/user/me via MockMvc with the Bearer token.
3. UserController.getCurrentUser() delegates to UserService.getCurrentUser().
4. UserService loads the account through UserRepository.
5. Compare response fields with the users table row.

- **Expected:**
Response is 200 OK.
Returned JSON contains correct id, email, fullName and role.
Values match the users row for the token subject.
No other user's data is returned.

- **Pre:** A LEARNER account exists and a valid JWT is available.

- **Round 1:** Pending

#### IT_USER_02: Verify updating profile through UserController persists fields via UserService/UserRepository.

- **Procedure:**
1. Call PUT /api/user/me via MockMvc with updated fullName/phone/target fields.
2. UserController.updateCurrentUser() delegates to UserService.updateProfile().
3. UserService saves changes through UserRepository.save().
4. Query users and call GET /api/user/me again.

- **Expected:**
Response is 200 OK.
Persisted columns match the request payload.
GET /api/user/me reflects the updates.

- **Pre:** Valid JWT; request passes validation.

- **Round 1:** Pending

#### IT_USER_03: Verify change-password requires the current password and updates the hash via UserService.

- **Procedure:**
1. Call PUT /api/user/me/password via MockMvc with a wrong currentPassword.
2. UserService.changePassword() must reject before UserRepository update.
3. Call PUT again with the correct currentPassword and a newPassword.
4. UserService updates the hash through UserRepository.
5. Login with the new password.

- **Expected:**
Wrong current password returns 4xx and hash is unchanged.
Correct change returns success/204 and hash changes.
Login succeeds only with the new password.

- **Pre:** Account with a known password exists.

- **Round 1:** Pending

#### IT_USER_04: Verify avatar upload through UserController stores a file and updates users.avatar_url.

- **Procedure:**
1. Call POST /api/user/me/avatar via MockMvc as multipart.
2. UserController.updateAvatar() delegates to UserService/AvatarStorageService.
3. AvatarStorageService stores the file and UserRepository updates avatar_url.
4. Call GET /api/user/avatars/{fileName}.

- **Expected:**
Upload response is 200 OK with updated user payload.
users.avatar_url is set.
Public avatar GET returns the stored file content type.

- **Pre:** Valid JWT; avatar storage is writable.

- **Round 1:** Pending

#### IT_USER_05: Verify unauthenticated profile update is blocked before UserService runs.

- **Procedure:**
1. Call PUT /api/user/me via MockMvc without Authorization.
2. Confirm SecurityFilterChain rejects the request before UserController/UserService execution.
3. Verify users table is unchanged.

- **Expected:**
Response is 401/403.
No profile fields are updated in the database.

- **Pre:** None.

- **Round 1:** Pending


## NOTIF — Notifications

Requirement scope: Integrate notification preferences and AppNotification list/read flows.


### Preferences

#### IT_NOTIF_01: Verify GET notification-preferences defaults both channels when no preference row exists.

- **Procedure:**
1. Authenticate a user that has no notification_preferences row.
2. Call GET /api/user/me/notification-preferences via MockMvc.
3. UserController delegates to NotificationPreferenceService.getForUser().
4. Service returns defaults when NotificationPreferenceRepository.findByUserId() is empty.

- **Expected:**
Response is 200 OK.
emailEnabled=true and inAppEnabled=true.
GET does not require a pre-inserted preference row.

- **Pre:** Authenticated user without a preference row.

- **Round 1:** Pending

#### IT_NOTIF_02: Verify PUT preferences upserts notification_preferences and gates AppNotificationService.createForUser().

- **Procedure:**
1. Call PUT /api/user/me/notification-preferences via MockMvc with inAppEnabled=false.
2. NotificationPreferenceService.updateForUser() saves through NotificationPreferenceRepository.
3. Trigger a business path that calls AppNotificationService.createForUser() for the same user.
4. AppNotificationService checks NotificationPreferenceService.isInAppEnabled() before AppNotificationRepository.save().
5. Re-enable in-app and trigger again; compare app_notifications counts.

- **Expected:**
Preference row is persisted with inAppEnabled=false.
No new app_notifications row is created while in-app is disabled.
After re-enable, a new notification row is inserted.

- **Pre:** Valid JWT; a notify path is available in the test harness.

- **Round 1:** Pending

#### IT_NOTIF_03: Verify preference update validation rejects null channel flags before persistence.

- **Procedure:**
1. Call PUT /api/user/me/notification-preferences via MockMvc with a null/missing channel field.
2. Controller validation fails before NotificationPreferenceService.updateForUser().
3. Confirm notification_preferences is unchanged.

- **Expected:**
Response is 400 Bad Request.
No partial preference overwrite occurs.

- **Pre:** Valid JWT.

- **Round 1:** Pending


### In-app list

#### IT_NOTIF_04: Verify listing and marking notifications read through AppNotificationService.

- **Procedure:**
1. Seed app_notifications owned by the learner.
2. Call GET /api/student/notifications via MockMvc.
3. StudentNotificationController delegates to AppNotificationService.listForUser().
4. Call PATCH /api/student/notifications/{id}/read.
5. AppNotificationService.markRead() updates AppNotificationRepository.
6. Call GET /api/student/notifications/unread-count.

- **Expected:**
List returns the seeded notifications.
Mark-read sets read=true and read_at.
Unread count decreases by one.

- **Pre:** LEARNER JWT; notification rows owned by that user.

- **Round 1:** Pending

#### IT_NOTIF_05: Verify mark-all-read updates only the authenticated user's unread notifications.

- **Procedure:**
1. Seed unread notifications for learner A and learner B.
2. Authenticate as A and call PATCH /api/student/notifications/read-all via MockMvc.
3. AppNotificationService.markAllRead() updates only A's rows through AppNotificationRepository.
4. Query app_notifications for A and B.

- **Expected:**
All of A's unread notifications become read.
B's notifications remain unread.
No cross-user update occurs.

- **Pre:** Two LEARNER accounts with unread notifications.

- **Round 1:** Pending


## COMMERCE — Cart & Wishlist

Requirement scope: Integrate StudentCommerceController cart/wishlist with StudentCommerceService persistence.


### Cart & wishlist

#### IT_COMMERCE_01: Verify adding a course to cart through StudentCommerceController persists cart state.

- **Procedure:**
1. Seed a published online course.
2. Call POST /api/student/commerce/cart/{courseId} via MockMvc as LEARNER.
3. StudentCommerceController delegates to StudentCommerceService.addToCart().
4. Call GET /api/student/commerce/cart and inspect persisted cart contents.

- **Expected:**
Add response is 200 OK.
GET cart contains the courseId.
Unauthenticated POST is rejected by security.

- **Pre:** Published course and LEARNER JWT are available.

- **Round 1:** Pending

#### IT_COMMERCE_02: Verify wishlist add and move-to-cart keep wishlist and cart collections consistent.

- **Procedure:**
1. Call POST /api/student/commerce/wishlist/{courseId} via MockMvc.
2. StudentCommerceService.addToWishlist() persists wishlist membership.
3. Call POST /api/student/commerce/wishlist/{courseId}/move-to-cart.
4. Service updates both wishlist and cart stores.
5. GET wishlist and GET cart.

- **Expected:**
Course appears in wishlist then moves to cart per service rules.
No course ownership/enrollment is created yet.

- **Pre:** LEARNER JWT; course is not already owned.

- **Round 1:** Pending

#### IT_COMMERCE_03: Verify clearing cart removes all items through StudentCommerceService.

- **Procedure:**
1. Add one or more courses to cart.
2. Call DELETE /api/student/commerce/cart via MockMvc.
3. StudentCommerceService clears persisted cart state.
4. GET cart and ensure no payment_orders were created.

- **Expected:**
Cart is empty after delete.
No payment_orders row is inserted by clear-cart.

- **Pre:** LEARNER JWT with a non-empty cart.

- **Round 1:** Pending

#### IT_COMMERCE_04: Verify TEACHER cannot mutate learner cart endpoints under SecurityConfig.

- **Procedure:**
1. Authenticate as TEACHER.
2. Call POST /api/student/commerce/cart/{courseId} via MockMvc.
3. Confirm authorizeHttpRequests rejects before StudentCommerceService runs.

- **Expected:**
Response is 403 Forbidden.
No cart mutation is persisted.

- **Pre:** TEACHER JWT.

- **Round 1:** Pending


## PAYMENT — PayOS & Orders

Requirement scope: Integrate payment link/quote, payment_orders persistence and PayOS webhook side effects.


### Checkout

#### IT_PAYMENT_01: Verify creating a PayOS payment link inserts a PENDING payment_orders row via PaymentService.

- **Procedure:**
1. Put a published course into the learner cart.
2. Call POST /api/student/payments/payos/link via MockMvc.
3. StudentPaymentController delegates to PaymentService.createPaymentLink().
4. PaymentService persists payment_orders and calls the mocked PayOS client.
5. Query payment_orders by returned orderCode.

- **Expected:**
Response is 200 OK with checkout URL/orderCode.
One payment_orders row exists with PENDING (or equivalent) status.
Amount matches the quoted total.

- **Pre:** LEARNER JWT; PayOS client stubbed.

- **Round 1:** Pending

#### IT_PAYMENT_02: Verify quote/link on empty cart fails without inserting payment_orders.

- **Procedure:**
1. Clear the cart.
2. Call payment quote/link via MockMvc.
3. PaymentService validates cart contents before repository insert.
4. Count payment_orders for the user.

- **Expected:**
Response is 4xx business error.
No new payment_orders row is created.

- **Pre:** LEARNER with empty cart.

- **Round 1:** Pending

#### IT_PAYMENT_03: Verify PayOS webhook marks order paid and grants course access idempotently.

- **Procedure:**
1. Seed a PENDING payment_orders row for learner+course.
2. Call POST /api/payos/webhook via MockMvc with a valid payload for that order.
3. PayosWebhookController delegates to PaymentService.handlePayosWebhook().
4. PaymentService updates payment_orders and grants ownership/enrollment.
5. Replay the same webhook once.

- **Expected:**
Order status becomes PAID/SUCCESS.
Learner gains course access exactly once.
Replay does not create a duplicate enrollment/ownership row.

- **Pre:** Pending order exists; webhook verification configured for test.

- **Round 1:** Pending

#### IT_PAYMENT_04: Verify GET payment orders returns only the authenticated learner's orders.

- **Procedure:**
1. Seed payment_orders for learner A and learner B.
2. Authenticate as A and call GET /api/student/payments/orders via MockMvc.
3. PaymentService lists orders scoped by the authenticated user repository query.

- **Expected:**
Response is 200 OK.
Only learner A's orders are returned.
No cross-user order leakage.

- **Pre:** Two LEARNER accounts with distinct orders.

- **Round 1:** Pending

#### IT_PAYMENT_05: Verify manager/CM payment order listing is role-protected.

- **Procedure:**
1. Authenticate as MANAGER or CONTENT_MANAGER.
2. Call GET /api/manager/payments/orders (or CM alias) via MockMvc.
3. PaymentService returns aggregated/order list data.
4. Repeat with LEARNER token.

- **Expected:**
Staff/CM call returns 200 with order data.
LEARNER receives 403 on the manager payments path.

- **Pre:** Seeded orders; MANAGER/CM and LEARNER tokens.

- **Round 1:** Pending


## COURSE — Online Learning

Requirement scope: Integrate public catalog and learner content/progress through OnlineCourseService.


### Catalog

#### IT_COURSE_01: Verify guest course catalog listing goes through OnlineCourseService and returns published courses only.

- **Procedure:**
1. Seed published and draft online_courses.
2. Call GET /api/online-courses via MockMvc without JWT.
3. PublicOnlineCourseController delegates to OnlineCourseService.getPublicCourses().
4. Inspect returned list against online_courses.status.

- **Expected:**
Response is 200 OK.
Published courses appear.
Draft courses are hidden according to service rules.

- **Pre:** Published and draft courses are seeded.

- **Round 1:** Pending

#### IT_COURSE_02: Verify public course detail by slug/id through OnlineCourseService.getPublicCourse().

- **Procedure:**
1. Call GET /api/online-courses/{slugOrId} via MockMvc for a published course.
2. Controller delegates to OnlineCourseService.getPublicCourse().
3. Call again with a non-existent slug.

- **Expected:**
Published detail returns 200 with course payload.
Missing course returns 404.
No authentication is required for public GET.

- **Pre:** A published course exists.

- **Round 1:** Pending


### Learner progress

#### IT_COURSE_03: Verify enrolled learner content and lesson progress update through OnlineCourseService.

- **Procedure:**
1. Seed enrollment/ownership for a LEARNER.
2. Call GET /api/student/online-courses/{courseId}/content via MockMvc.
3. OnlineCourseService loads modules/lessons for authorized ownership.
4. Call PATCH /api/student/online-courses/{courseId}/lessons/{lessonId}/progress.
5. Service persists progress and reload progress storage.

- **Expected:**
Content response is 200 with modules/lessons.
Progress patch persists for the lesson.
Unauthenticated content call is rejected.

- **Pre:** Enrolled LEARNER; course contains lessons.

- **Round 1:** Pending

#### IT_COURSE_04: Verify non-enrolled learner is denied protected course content by OnlineCourseService.

- **Procedure:**
1. Authenticate a LEARNER without ownership of the course.
2. Call GET content via MockMvc.
3. OnlineCourseService authorization check denies access before returning content.
4. Confirm no progress rows were created.

- **Expected:**
Response is 403/404.
No protected content payload is returned.
No progress side effect occurs.

- **Pre:** Published course without enrollment for the caller.

- **Round 1:** Pending

#### IT_COURSE_05: Verify my-enrollments lists only courses owned by the authenticated learner.

- **Procedure:**
1. Seed distinct enrollments for learner A and learner B.
2. Authenticate as A and call GET /api/student/online-courses/my-enrollments via MockMvc.
3. OnlineCourseService.getMyEnrollments() queries enrollments scoped to the current user.

- **Expected:**
Response is 200 OK.
Only learner A courses are returned.
No cross-user enrollment leakage.

- **Pre:** Two LEARNER accounts with different enrollments.

- **Round 1:** Pending

#### IT_COURSE_06: Verify course rating create/get persists through CourseReviewService into course_reviews.

- **Procedure:**
1. Authenticate an eligible LEARNER.
2. Call POST /api/student/online-courses/{courseId}/rating via MockMvc.
3. StudentOnlineCourseController delegates to CourseReviewService.saveRating().
4. Query course_reviews and call GET rating.

- **Expected:**
Review is upserted per unique student-course constraint.
GET returns the saved rating.
Unauthorized/non-eligible caller is denied.

- **Pre:** Eligible LEARNER for the target course.

- **Round 1:** Pending


## DISCUSS — Course Discussion

Requirement scope: Integrate discussion create/reply/report and CM moderation through discussion services.


### Learner discussion

#### IT_DISCUSS_01: Verify enrolled learner creates a discussion thread via CourseDiscussionService.

- **Procedure:**
1. Authenticate an enrolled LEARNER.
2. Call the student create-thread API via MockMvc.
3. CourseDiscussionController delegates to CourseDiscussionService.createThread().
4. Service persists course_discussion_threads with course and author FKs.

- **Expected:**
Response is 200 OK.
One thread row is inserted for the course.
Thread owner matches the authenticated learner.

- **Pre:** Enrolled LEARNER JWT.

- **Round 1:** Pending

#### IT_DISCUSS_02: Verify reply and reaction/helpful updates are persisted by CourseDiscussionService.

- **Procedure:**
1. Seed an existing thread.
2. Call reply and reaction/helpful endpoints via MockMvc.
3. CourseDiscussionService writes reply/reaction repositories.
4. Query course_discussion_replies and related reaction data.

- **Expected:**
Reply row is created.
Reaction/helpful state is persisted consistently.
Counts in response match database state.

- **Pre:** Existing discussion thread.

- **Round 1:** Pending

#### IT_DISCUSS_03: Verify reporting a discussion creates a moderation queue item.

- **Procedure:**
1. Call learner report API via MockMvc.
2. CourseDiscussionService creates a report record.
3. Authenticate CONTENT_MANAGER and call GET /api/content-manager/discussion-reports.
4. DiscussionModerationService.getReports() returns the pending report.

- **Expected:**
Report is visible in CM queue.
Report references the reported thread/reply.
Learner cannot call moderation hide/dismiss.

- **Pre:** LEARNER and CONTENT_MANAGER accounts.

- **Round 1:** Pending


### Moderation

#### IT_DISCUSS_04: Verify CM hide/dismiss updates report status through DiscussionModerationService.

- **Procedure:**
1. Seed a pending discussion report.
2. Call POST hide or dismiss via MockMvc as CONTENT_MANAGER.
3. DiscussionModerationService.hide()/dismiss() updates report and visibility rules.
4. Query report status and thread visibility.

- **Expected:**
Report status is updated.
Thread/reply visibility follows moderation outcome.
Audit/report row remains queryable.

- **Pre:** Pending report exists.

- **Round 1:** Pending

#### IT_DISCUSS_05: Verify LEARNER cannot call discussion moderation endpoints.

- **Procedure:**
1. Authenticate as LEARNER.
2. Call moderation hide/dismiss via MockMvc.
3. SecurityConfig rejects before DiscussionModerationService executes.

- **Expected:**
Response is 403 Forbidden.
Report status is unchanged.

- **Pre:** LEARNER JWT; pending report id.

- **Round 1:** Pending


## CONTENT — CM Online Courses

Requirement scope: Integrate Content Manager online course CRUD/publish/version/category flows.


### Course CM

#### IT_CONTENT_01: Verify CM creates an online course through OnlineCourseService into online_courses.

- **Procedure:**
1. Authenticate as CONTENT_MANAGER.
2. Call POST /api/content-manager/online-courses via MockMvc.
3. ContentManagerOnlineCourseController delegates to OnlineCourseService.createCourse().
4. Query online_courses.
5. Repeat create with LEARNER token.

- **Expected:**
CM create returns 200 and inserts online_courses.
LEARNER receives 403 on CM API.
Persisted title/status match request.

- **Pre:** CONTENT_MANAGER JWT.

- **Round 1:** Pending

#### IT_CONTENT_02: Verify publish/archive transitions update status and public catalog visibility.

- **Procedure:**
1. Seed a draft course.
2. Call CM publish then archive endpoints via MockMvc.
3. OnlineCourseService updates online_courses.status.
4. Call public GET /api/online-courses and compare visibility.

- **Expected:**
Status transitions are persisted.
Public catalog reflects publish rules.
Archived/draft course is not publicly listed.

- **Pre:** Draft course owned/managed by CM.

- **Round 1:** Pending

#### IT_CONTENT_03: Verify course version create/publish snapshot path through version APIs.

- **Procedure:**
1. Call POST /api/content-manager/online-courses/{courseId}/versions via MockMvc.
2. Call PATCH publish on the version.
3. Version service persists version tables and publish flags.
4. Query version records.

- **Expected:**
Version row is created.
Publish flag/status is consistent.
Unauthorized role cannot publish versions.

- **Pre:** CM course exists.

- **Round 1:** Pending

#### IT_CONTENT_04: Verify category CRUD persists course_categories through category management service.

- **Procedure:**
1. Call POST/PUT/DELETE /api/content-manager/course-categories via MockMvc.
2. CourseCategoryManagementService writes course_categories.
3. Query table after each operation.
4. Send an invalid payload.

- **Expected:**
CRUD operations persist correctly.
Invalid payload returns 400 without insert.
LEARNER is forbidden.

- **Pre:** CONTENT_MANAGER JWT.

- **Round 1:** Pending


## PACKAGE — Packages & Bundles

Requirement scope: Integrate CM package/bundle management through LearningPackageManagementService.


### Packages

#### IT_PACKAGE_01: Verify creating a package persists packages via LearningPackageManagementService.

- **Procedure:**
1. Authenticate as CONTENT_MANAGER.
2. Call POST /api/content-manager/packages via MockMvc.
3. Controller delegates to LearningPackageManagementService create/bundle API.
4. Query packages table.

- **Expected:**
Response is 200 OK.
One packages row is inserted with request fields.

- **Pre:** CONTENT_MANAGER JWT.

- **Round 1:** Pending

#### IT_PACKAGE_02: Verify bundle-items update replaces relations without orphan rows.

- **Procedure:**
1. Seed a package.
2. Call PUT /api/content-manager/packages/{id}/bundle-items via MockMvc.
3. LearningPackageManagementService replaces bundle relations.
4. Query bundle-item relation table.

- **Expected:**
Relations match the requested course/item set.
No orphan relation rows remain.

- **Pre:** Package and candidate courses exist.

- **Round 1:** Pending

#### IT_PACKAGE_03: Verify publish/archive package updates status through package service.

- **Procedure:**
1. Call PATCH publish/archive on a package via MockMvc.
2. Service updates packages.status.
3. Attempt the same call as LEARNER.

- **Expected:**
Status is updated for CM caller.
LEARNER receives 403.
Database status matches the last successful transition.

- **Pre:** Package exists; CONTENT_MANAGER JWT.

- **Round 1:** Pending


## CURRICULUM — Curriculum & Banks

Requirement scope: Integrate curriculum programs/units, exercise bank, rubrics and learning paths.


### Curriculum

#### IT_CURRICULUM_01: Verify creating a curriculum program persists curriculum_programs via CurriculumProgramService.

- **Procedure:**
1. Call POST curriculum-programs via MockMvc as CONTENT_MANAGER.
2. ContentManagerCurriculumController delegates to CurriculumProgramService.createProgram().
3. Query curriculum_programs and list programs.

- **Expected:**
Program row is inserted.
List includes the created program.
Unauthorized role is rejected.

- **Pre:** CONTENT_MANAGER JWT.

- **Round 1:** Pending

#### IT_CURRICULUM_02: Verify unit creation under a program writes curriculum_units with program FK.

- **Procedure:**
1. Seed a curriculum program.
2. Call create-unit API via MockMvc.
3. CurriculumProgramService persists curriculum_units.
4. Query unit.program_id.

- **Expected:**
Unit is saved.
Foreign key points to the parent program.

- **Pre:** Program exists.

- **Round 1:** Pending

#### IT_CURRICULUM_03: Verify exercise-bank item CRUD through ExerciseBank service/controller.

- **Procedure:**
1. Call POST/PUT/GET/DELETE /api/content-manager/exercise-bank via MockMvc.
2. Service persists exercise_bank_items (or equivalent bank table).
3. Send invalid payload and confirm no insert.

- **Expected:**
CRUD succeeds for valid payloads.
Invalid payload returns 400.
Deleted/archived item is no longer returned by default list rules.

- **Pre:** CONTENT_MANAGER JWT.

- **Round 1:** Pending

#### IT_CURRICULUM_04: Verify learning-path create and course ordering through LearningPath APIs.

- **Procedure:**
1. Call POST /api/content-manager/learning-paths via MockMvc.
2. Add courses and PUT order endpoint.
3. Service persists learning_paths and membership/order tables.
4. Query ordered membership.

- **Expected:**
Path is created.
Course membership and order match the request.

- **Pre:** CONTENT_MANAGER JWT; courses exist.

- **Round 1:** Pending

#### IT_CURRICULUM_05: Verify rubric create persists assessment_rubrics and criteria.

- **Procedure:**
1. Call POST /api/content-manager/rubrics via MockMvc.
2. Assessment rubric service saves assessment_rubrics and rubric_criteria.
3. Query both tables.

- **Expected:**
Rubric header and criteria rows are inserted.
Weights/names match the request payload.

- **Pre:** CONTENT_MANAGER JWT.

- **Round 1:** Pending


## ENROLLREQ — Enrollment Requests

Requirement scope: Integrate learner enrollment requests and staff processing via EnrollmentRequestService.


### Learner request

#### IT_ENROLLREQ_01: Verify learner submit enrollment request persists through EnrollmentRequestService.

- **Procedure:**
1. Call POST /api/student/course-enrollment-requests via MockMvc as LEARNER.
2. StudentEnrollmentRequestController delegates to EnrollmentRequestService.submit().
3. Service inserts course_enrollment_requests.
4. Call GET /my and query the table.

- **Expected:**
Request row is created for the learner.
It appears in listMine.
Initial status matches service defaults.

- **Pre:** LEARNER JWT.

- **Round 1:** Pending

#### IT_ENROLLREQ_02: Verify learner cancel updates request status and history.

- **Procedure:**
1. Seed an open enrollment request.
2. Call PATCH /{requestId}/cancel via MockMvc.
3. EnrollmentRequestService.cancel() updates request and writes course_enrollment_request_history.
4. Query request + history tables.

- **Expected:**
Request status becomes cancelled.
A history entry is written.
Cancelled request cannot be processed as open.

- **Pre:** Open request owned by the learner.

- **Round 1:** Pending


### Staff processing

#### IT_ENROLLREQ_03: Verify staff schedule-test and complete-test transitions via EnrollmentRequestService.

- **Procedure:**
1. Authenticate as STAFF.
2. Call PATCH schedule-test then complete-test via MockMvc.
3. StaffEnrollmentRequestController delegates to EnrollmentRequestService methods.
4. Query request status after each step.

- **Expected:**
Status transitions follow the staff workflow.
Invalid transition is rejected.
Mail side effects may be stubbed.

- **Pre:** Pending request exists.

- **Round 1:** Pending

#### IT_ENROLLREQ_04: Verify staff assign-class creates/updates classroom enrollment side effects.

- **Procedure:**
1. Call PATCH assign-class with a target offering via MockMvc as STAFF.
2. EnrollmentRequestService.assignClass() updates the request and classroom enrollment state.
3. Query course_enrollment_requests and classroom_enrollments.

- **Expected:**
Request reaches assigned/completed state per rules.
Related classroom enrollment is created or updated.
Capacity/business rules are enforced.

- **Pre:** Eligible request and offering with capacity.

- **Round 1:** Pending

#### IT_ENROLLREQ_05: Verify LEARNER cannot call staff enrollment-request endpoints.

- **Procedure:**
1. Authenticate as LEARNER.
2. Call a staff PATCH endpoint via MockMvc.
3. Security rejects before EnrollmentRequestService staff methods run.

- **Expected:**
Response is 403.
Request status is unchanged.

- **Pre:** LEARNER JWT; existing request id.

- **Round 1:** Pending


## CLASS — TM Classroom Ops

Requirement scope: Integrate public offerings and TM enrollment/teacher operations via ClassroomOfferingService.


### Public & TM offering

#### IT_CLASS_01: Verify public classroom offerings listing through ClassroomOfferingService without authentication.

- **Procedure:**
1. Seed ACTIVE classroom_offerings.
2. Call GET /api/classroom-offerings via MockMvc without JWT.
3. PublicClassroomController delegates to ClassroomOfferingService.getPublicOfferings().
4. Call GET /{slugOrId} for detail.

- **Expected:**
List/detail return 200.
Payload maps from classroom_offerings and related package title fields.

- **Pre:** At least one public offering exists.

- **Round 1:** Pending

#### IT_CLASS_02: Verify TM/staff can list and get classroom detail via ClassroomOfferingService.

- **Procedure:**
1. Authenticate as TRAINING_MANAGER/STAFF.
2. Call GET /api/training-manager/classrooms and GET /{id} via MockMvc.
3. TrainingManagerClassroomController delegates to ClassroomOfferingService manager queries.
4. Repeat with LEARNER token.

- **Expected:**
TM/staff calls return 200.
LEARNER receives 403 on TM classroom APIs.

- **Pre:** TM/STAFF JWT; offerings seeded.

- **Round 1:** Pending


### Enrollment pipeline

#### IT_CLASS_03: Verify confirm → tuition → assign updates enrollment consistently through ClassroomOfferingService.

- **Procedure:**
1. Seed enrollment with PENDING_CONFIRMATION.
2. Call POST .../confirm via MockMvc as TM.
3. Call POST .../tuition with FULL payment.
4. Call POST .../assign.
5. ClassroomOfferingService methods update classroom_enrollments and classroom_tuition_payments.
6. Query both tables and final registration_status.

- **Expected:**
Final registration_status is ASSIGNED.
Tuition payment row exists.
Learner class-access semantics become true.

- **Pre:** TM JWT; offering has free capacity.

- **Round 1:** Pending

#### IT_CLASS_04: Verify reject registration sets REJECTED without occupying a class slot.

- **Procedure:**
1. Seed a pending enrollment.
2. Call POST .../reject via MockMvc.
3. ClassroomOfferingService.rejectRegistration() updates classroom_enrollments.
4. Compare offering occupied-slot count.

- **Expected:**
registration_status=REJECTED.
No ASSIGNED capacity is consumed.
Reject reason is stored per service rules.

- **Pre:** Pending enrollment; TM JWT.

- **Round 1:** Pending

#### IT_CLASS_05: Verify reorderWaitlist updates waitlist_priority through ClassroomOfferingService.

- **Procedure:**
1. Seed two WAITLIST enrollments with priorities 1 and 2.
2. Call ClassroomOfferingService.reorderWaitlist() (or HTTP mapping if exposed) with swapped IDs.
3. Service rewrites waitlist_priority via ClassroomEnrollmentRepository.
4. Query priorities; also send an incomplete ID list.

- **Expected:**
Priorities are swapped to the requested order.
Incomplete ID list is rejected.
If HTTP mapping is missing, document service-level IT coverage of the gap.

- **Pre:** Two WAITLIST rows on the same offering; TM actor.

- **Round 1:** Pending

#### IT_CLASS_06: Verify transfer enrollment moves the learner through ClassroomOfferingService.transferEnrollment().

- **Procedure:**
1. Seed an ASSIGNED enrollment.
2. Call POST .../transfer via MockMvc with target offering id.
3. Service updates source/target enrollment records.
4. Query classroom_enrollments for both offerings.

- **Expected:**
Target enrollment reflects the learner.
Source enrollment is closed/transferred per rules.
No duplicate active assignments remain.

- **Pre:** ASSIGNED enrollment and eligible target offering.

- **Round 1:** Pending

#### IT_CLASS_07: Verify assign/replace teacher writes classroom_teacher_assignments.

- **Procedure:**
1. Call POST teachers/{teacherId}/assign or replace via MockMvc as TM.
2. ClassroomOfferingService updates classroom_teacher_assignments.
3. Query assignment rows for the offering.

- **Expected:**
Teacher is linked as expected.
Replace removes/supersedes the old assignment cleanly.

- **Pre:** TM JWT; teacher user exists.

- **Round 1:** Pending

#### IT_CLASS_08: Verify LEARNER cannot call TM enrollment management endpoints.

- **Procedure:**
1. Authenticate as LEARNER.
2. Call POST .../enrollments/{id}/assign via MockMvc.
3. Security rejects before ClassroomOfferingService.assignToClass().

- **Expected:**
Response is 403.
Enrollment row is unchanged.

- **Pre:** LEARNER JWT; target enrollment id.

- **Round 1:** Pending


## LEARNERCLS — Learner Classroom

Requirement scope: Integrate assigned learner classroom access, homework submit, materials and tuition proofs.


### Access

#### IT_LEARNERCLS_01: Verify my-classrooms lists only classes the learner can access via ClassroomOfferingService.

- **Procedure:**
1. Seed ASSIGNED and non-assigned enrollments.
2. Call GET /api/student/classrooms/my-classrooms via MockMvc.
3. StudentClassroomController delegates to ClassroomOfferingService.getMyClasses().
4. Compare response ids with classroom_enrollments.

- **Expected:**
Only accessible/ASSIGNED classes are returned.
Other learners' classes are not included.

- **Pre:** ASSIGNED LEARNER JWT.

- **Round 1:** Pending

#### IT_LEARNERCLS_02: Verify sessions listing and join endpoints enforce class ownership.

- **Procedure:**
1. Call GET /{id}/sessions via MockMvc for an owned class.
2. Call POST /sessions/{sessionId}/join.
3. ClassroomOfferingService/session services authorize by enrollment.
4. Repeat with a foreign classroom id.

- **Expected:**
Owned class returns 200 and join succeeds under rules.
Foreign class is denied.
Session data matches classroom_sessions.

- **Pre:** Owned session exists.

- **Round 1:** Pending

#### IT_LEARNERCLS_03: Verify homework attachment upload and submit persist through ClassroomHomeworkService.

- **Procedure:**
1. Call POST /api/student/classrooms/homework/attachments via MockMvc.
2. HomeworkAttachmentStorageService stores the file.
3. Call POST /homework/{homeworkId}/submit.
4. ClassroomHomeworkService.submit() persists submission records.
5. Query submission tables.

- **Expected:**
Attachment metadata/URL is stored.
Submission row is created for the learner/homework.
Deadline/eligibility rules are enforced when coded.

- **Pre:** Open homework; ASSIGNED learner.

- **Round 1:** Pending

#### IT_LEARNERCLS_04: Verify materials/announcements/syllabus are readable only for class members.

- **Procedure:**
1. Call GET materials/announcements/syllabus via MockMvc as class member.
2. ClassroomContentService loads classroom_materials and related content.
3. Repeat as a non-member.

- **Expected:**
Member receives 200 with content.
Non-member receives 403/404.
Returned items match DB for that offering.

- **Pre:** Seeded classroom content.

- **Round 1:** Pending

#### IT_LEARNERCLS_05: Verify tuition proof upload creates a pending proof via TuitionProofService.

- **Procedure:**
1. Call POST multipart /{id}/tuition-proofs via MockMvc.
2. StudentClassroomController delegates to TuitionProofService.submitProof().
3. Service inserts classroom_tuition_payment_proofs.
4. TM lists pending proofs through TuitionProofService.listPendingProofs().

- **Expected:**
Proof row is PENDING.
File URL is stored.
Proof appears in TM pending list.

- **Pre:** Eligible enrollment; storage writable.

- **Round 1:** Pending

#### IT_LEARNERCLS_06: Verify gradebook/me visibility follows publish state from ClassroomGradebookService.

- **Procedure:**
1. Seed gradebook rows unpublished.
2. Call GET /{id}/gradebook/me as learner.
3. Publish via teacher gradebook publish API.
4. ClassroomGradebookService toggles publish flags and learner GET again.
5. Unpublish and re-check.

- **Expected:**
Unpublished state hides or limits learner scores per rules.
Published state returns scores.
DB publish flags match API visibility.

- **Pre:** Gradebook rows and teacher publish capability.

- **Round 1:** Pending


## TEACH — Teacher Operations

Requirement scope: Integrate teacher homework, attendance, gradebook and change-request flows.


### Teaching ops

#### IT_TEACH_01: Verify teacher creates homework through ClassroomHomeworkService for an assigned class.

- **Procedure:**
1. Authenticate a TEACHER assigned to the offering.
2. Call POST /api/teacher/classrooms/{id}/homework via MockMvc.
3. TeacherClassroomController delegates to ClassroomHomeworkService.create().
4. Query classroom_homework and optionally learner homework list.

- **Expected:**
Homework row is inserted with offering FK.
Assigned learner can see it when allowed.
Non-assigned teacher is denied.

- **Pre:** TEACHER assignment exists.

- **Round 1:** Pending

#### IT_TEACH_02: Verify grading a submission updates scores through ClassroomHomeworkService.grade().

- **Procedure:**
1. Seed homework and a learner submission.
2. Call POST .../homework/{homeworkId}/students/{studentId}/grade via MockMvc.
3. ClassroomHomeworkService persists grade fields.
4. Query submission/grade repositories.

- **Expected:**
Score/feedback are saved.
Teacher not owning the class is rejected.
Learner academic views reflect the grade when publish rules allow.

- **Pre:** Submitted homework exists.

- **Round 1:** Pending

#### IT_TEACH_03: Verify attendance save upserts records through ClassroomAttendanceService.

- **Procedure:**
1. Call GET session attendance then POST /api/teacher/classrooms/attendance via MockMvc.
2. ClassroomAttendanceService.saveBulk() writes attendance tables.
3. Learner calls GET attendance/me.
4. Confirm learner sees only own rows.

- **Expected:**
Attendance records are upserted.
Learner endpoint returns only own attendance.
Non-teacher is forbidden.

- **Pre:** Session and enrolled students exist.

- **Round 1:** Pending

#### IT_TEACH_04: Verify gradebook publish/unpublish toggles learner visibility via ClassroomGradebookService.

- **Procedure:**
1. Call PUT gradebook then POST publish via MockMvc.
2. ClassroomGradebookService updates publish flags.
3. Learner GET gradebook/me.
4. POST unpublish and re-check learner GET.

- **Expected:**
Publish makes scores visible to learner.
Unpublish hides/restricts per service rules.
Flags in DB match API outcomes.

- **Pre:** TEACHER JWT; gradebook rows exist.

- **Round 1:** Pending

#### IT_TEACH_05: Verify teacher change-request create persists and can notify TM through notification services.

- **Procedure:**
1. Call POST /api/teacher/classrooms/requests via MockMvc.
2. ClassroomChangeRequestService saves classroom_change_requests.
3. Notification path may call AppNotificationService for TM users.
4. Query change request and optional app_notifications.

- **Expected:**
PENDING change request is stored.
Notification creation respects notification preferences.
Invalid request payloads return 400.

- **Pre:** TEACHER assigned to offering.

- **Round 1:** Pending

#### IT_TEACH_06: Verify LEARNER cannot create teacher homework.

- **Procedure:**
1. Authenticate as LEARNER.
2. Call POST /api/teacher/classrooms/{id}/homework via MockMvc.
3. Security rejects before ClassroomHomeworkService.create().

- **Expected:**
Response is 403.
No classroom_homework insert occurs.

- **Pre:** LEARNER JWT.

- **Round 1:** Pending


## QUIZ — Classroom Quiz

Requirement scope: Integrate teacher quiz lifecycle and learner submit through ClassroomQuizService.


### Quiz lifecycle

#### IT_QUIZ_01: Verify teacher creates quiz with questions through ClassroomQuizService.

- **Procedure:**
1. Call POST /api/teacher/classrooms/{offeringId}/quizzes via MockMvc.
2. ClassroomQuizController delegates to ClassroomQuizService.create().
3. Service persists classroom_quizzes and classroom_quiz_questions.
4. Query both tables.

- **Expected:**
Quiz header and questions are saved.
Offering FK is correct.

- **Pre:** TEACHER assigned to class.

- **Round 1:** Pending

#### IT_QUIZ_02: Verify open/close toggles attempt eligibility in ClassroomQuizService.

- **Procedure:**
1. PATCH open quiz via MockMvc.
2. Learner submit should succeed.
3. PATCH close quiz.
4. Learner submit should be rejected by service status checks.

- **Expected:**
OPEN allows attempts.
CLOSED rejects new attempts.
Status in DB matches API.

- **Pre:** Quiz exists; ASSIGNED learner.

- **Round 1:** Pending

#### IT_QUIZ_03: Verify learner quiz submit stores attempt score through ClassroomQuizService.submit().

- **Procedure:**
1. Call POST /api/student/quizzes/{quizId}/submit via MockMvc.
2. ClassroomQuizService scores and saves attempt repository rows.
3. Query attempts table.
4. Repeat as non-member learner.

- **Expected:**
Attempt row is created with score per rules.
Non-member is denied.
No duplicate illegal attempts beyond service rules.

- **Pre:** OPEN quiz; ASSIGNED learner.

- **Round 1:** Pending

#### IT_QUIZ_04: Verify delete quiz removes or archives quiz data consistently.

- **Procedure:**
1. Call DELETE /api/teacher/quizzes/{quizId} via MockMvc.
2. ClassroomQuizService deletes/archives quiz and related questions per implementation.
3. Query quiz/questions/attempts policy outcome.

- **Expected:**
Quiz is no longer active in teacher list.
Child questions are cascaded or archived without orphans if cascade is configured.

- **Pre:** TEACHER owner of quiz.

- **Round 1:** Pending


## ASSESS — Assessment & Placement

Requirement scope: Integrate placement, course assessment and mock-test submit flows; AI clients mocked.


### Placement

#### IT_ASSESS_01: Verify current placement GET/submit persists attempts through PlacementTestService.

- **Procedure:**
1. Call GET /api/student/placement-tests/current via MockMvc.
2. PlacementTestController delegates to PlacementTestService.getTest().
3. Call POST /current/submit with answers.
4. PlacementTestService.submit() writes placement_test_attempts.
5. Query attempts table.

- **Expected:**
GET returns definition (lazy-seed allowed).
Submit creates an attempt with result fields.
Unauthenticated calls are rejected.

- **Pre:** LEARNER JWT.

- **Round 1:** Pending

#### IT_ASSESS_02: Verify invalid placement submit fails validation without inserting an attempt.

- **Procedure:**
1. Call POST /current/submit via MockMvc with empty/invalid body.
2. Validation/service rejects before PlacementTestAttempt persistence.
3. Count placement_test_attempts for the user.

- **Expected:**
Response is 400.
Attempt count is unchanged.

- **Pre:** LEARNER JWT.

- **Round 1:** Pending

#### IT_ASSESS_03: Verify CM placement GET/PUT updates placement_test_definitions.

- **Procedure:**
1. Authenticate as CONTENT_MANAGER.
2. Call GET/PUT /api/content-manager/placement-test via MockMvc.
3. Placement definition service updates placement_test_definitions.
4. Repeat PUT as LEARNER.

- **Expected:**
CM update persists definition fields.
LEARNER receives 403 on CM path.

- **Pre:** CONTENT_MANAGER JWT.

- **Round 1:** Pending


### Course & mock

#### IT_ASSESS_04: Verify course assessment submit stores assessment_submissions through AiAssessmentService.

- **Procedure:**
1. Seed course assessment and enrolled learner.
2. Call POST /api/student/assessments/{assessmentId}/submit via MockMvc.
3. StudentAssessmentController delegates to AiAssessmentService.submitAssessment().
4. Mock AI client if speaking/writing path is used.
5. Query assessment_submissions.

- **Expected:**
Submission row is created with status/score per mode.
Non-enrolled learner is denied.
AI mock failures are asserted according to actual transactional behavior.

- **Pre:** Enrolled LEARNER; assessment configured; AI mocked.

- **Round 1:** Pending

#### IT_ASSESS_05: Verify mock-test list/submit persists mock_test_attempts through MockTestService.

- **Procedure:**
1. Call GET /api/student/mock-tests via MockMvc.
2. Call POST /{id}/submit.
3. StudentMockTestController delegates to MockTestService.submitMockTest().
4. Query mock_test_attempts.

- **Expected:**
Published mock tests are listed.
Submit inserts an attempt row.

- **Pre:** LEARNER JWT; published mock test exists.

- **Round 1:** Pending

#### IT_ASSESS_06: Verify TEACHER cannot call student placement submit endpoint.

- **Procedure:**
1. Authenticate as TEACHER.
2. Call POST /api/student/placement-tests/current/submit via MockMvc.
3. Security rejects before PlacementTestService.submit().

- **Expected:**
Response is 403.
No placement attempt is created for the teacher user.

- **Pre:** TEACHER JWT.

- **Round 1:** Pending


## SUPPORT — Support Tickets

Requirement scope: Integrate learner ticket create/reply and staff claim/update via SupportTicketService.


### Tickets

#### IT_SUPPORT_01: Verify learner creates a support ticket and initial message through SupportTicketService.

- **Procedure:**
1. Call POST /api/student/support-tickets via MockMvc as LEARNER.
2. StudentSupportTicketController delegates to SupportTicketService.create().
3. Service inserts support_tickets and support_ticket_messages.
4. Call GET listMine and query both tables.

- **Expected:**
Response is 200 OK.
Ticket is owned by the learner.
Initial message row exists.

- **Pre:** LEARNER JWT.

- **Round 1:** Pending

#### IT_SUPPORT_02: Verify learner cannot read another learner's ticket by id.

- **Procedure:**
1. Seed a ticket for learner A.
2. Authenticate as learner B and call GET /api/student/support-tickets/{ticketId}.
3. SupportTicketService ownership check denies access.

- **Expected:**
Response is 403/404.
No message content is leaked.

- **Pre:** Two LEARNER accounts.

- **Round 1:** Pending

#### IT_SUPPORT_03: Verify staff/manager claim and reply append staff messages through SupportTicketService.

- **Procedure:**
1. Seed an OPEN ticket.
2. Call POST claim then POST reply via MockMvc as MANAGER/STAFF.
3. ManagerSupportTicketController delegates to SupportTicketService.claim()/reply.
4. Query assignee and support_ticket_messages.
5. Learner GET detail.

- **Expected:**
Assignee is set.
Staff message is appended.
Learner can see the staff reply.

- **Pre:** MANAGER or STAFF JWT.

- **Round 1:** Pending

#### IT_SUPPORT_04: Verify empty ticket create fails validation without DB insert.

- **Procedure:**
1. Call POST /api/student/support-tickets via MockMvc with missing subject/body.
2. Validation fails before SupportTicketService.create().
3. Count support_tickets.

- **Expected:**
Response is 400.
Ticket count is unchanged.

- **Pre:** LEARNER JWT.

- **Round 1:** Pending


## ADMIN — Administration

Requirement scope: Integrate admin user/role/status/audit flows through AdminUserService.


### Admin users

#### IT_ADMIN_01: Verify admin creates a user with roles and writes an audit log.

- **Procedure:**
1. Authenticate as ADMIN.
2. Call POST /api/admin/users via MockMvc.
3. AdminUserController delegates to AdminUserService.createUser().
4. Service writes users, user_roles and AuditLogService → system_audit_logs.
5. Repeat with LEARNER token.

- **Expected:**
User and roles are created.
Audit log entry exists.
Non-admin receives 403.

- **Pre:** ADMIN JWT.

- **Round 1:** Pending

#### IT_ADMIN_02: Verify patch roles/status updates associations without orphan role rows.

- **Procedure:**
1. Call PATCH /api/admin/users/{id}/roles via MockMvc.
2. Call PATCH /api/admin/users/{id}/status.
3. AdminUserService.updateRoles()/updateStatus() update repositories.
4. Query users and user_roles.

- **Expected:**
Role set matches the request with no orphans.
Status change is persisted.
Audit entries are recorded.

- **Pre:** Target user exists; ADMIN JWT.

- **Round 1:** Pending

#### IT_ADMIN_03: Verify audit-logs listing is restricted to ADMIN.

- **Procedure:**
1. Call GET /api/admin/audit-logs via MockMvc as ADMIN.
2. AdminAuditLogController reads through AuditLogService/repository.
3. Repeat as LEARNER.

- **Expected:**
ADMIN receives 200 with log rows.
LEARNER receives 403.

- **Pre:** Audit rows exist.

- **Round 1:** Pending

#### IT_ADMIN_04: Verify system config GET is ADMIN only.

- **Procedure:**
1. Call GET /api/admin/system/config via MockMvc as ADMIN.
2. AdminSystemController returns config payload.
3. Repeat as non-admin roles.

- **Expected:**
ADMIN receives 200.
Other roles receive 403.

- **Pre:** ADMIN JWT.

- **Round 1:** Pending


## LARK — Lark Meetings

Requirement scope: Integrate Lark webhook handling and staff sync-lark through Lark services.


### Webhook & sync

#### IT_LARK_01: Verify Lark URL challenge is echoed by LarkWebhookService without mutating sessions.

- **Procedure:**
1. Call POST /api/lark/events via MockMvc with a challenge payload.
2. LarkWebhookController calls LarkWebhookService.verifyChallenge().
3. Confirm no classroom_sessions row changed.

- **Expected:**
Challenge value is echoed in the response.
No session mutation occurs.

- **Pre:** None.

- **Round 1:** Pending

#### IT_LARK_02: Verify meeting started/ended events update classroom_sessions through LarkWebhookService.handle().

- **Procedure:**
1. Seed a classroom_sessions row mapped to Lark ids.
2. Call POST /api/lark/events with meeting started/ended payload via MockMvc.
3. LarkWebhookService.handle() updates session status/timestamps via session repository.
4. Query classroom_sessions.

- **Expected:**
Session fields are updated according to the event type.
Invalid events are rejected safely without corrupt state.

- **Pre:** Mapped session exists; Lark test mode enabled.

- **Round 1:** Pending

#### IT_LARK_03: Verify staff sync-lark persists sync status through LarkMeetingService.

- **Procedure:**
1. Authenticate as STAFF.
2. Call POST .../sessions/{id}/sync-lark via MockMvc.
3. Controller delegates to LarkMeetingService with mocked Lark HTTP.
4. Query lark_sync_status/error fields.
5. Repeat as LEARNER.

- **Expected:**
Sync status/error fields are updated from the mocked response.
LEARNER receives 403.

- **Pre:** STAFF JWT; Lark client mocked.

- **Round 1:** Pending


## INFRA — Infrastructure

Requirement scope: Integrate campuses/rooms/templates and generate-sessions via ClassroomInfrastructureService.


### Infra CRUD

#### IT_INFRA_01: Verify create campus/room persists through ClassroomInfrastructureService.

- **Procedure:**
1. Call POST campuses and rooms via MockMvc as TM/STAFF.
2. TrainingManagerInfrastructureController delegates to ClassroomInfrastructureService.
3. Query classroom_campuses and classroom_rooms.
4. Repeat as LEARNER.

- **Expected:**
Campus/room rows are inserted.
LEARNER receives 403.

- **Pre:** TM/STAFF JWT.

- **Round 1:** Pending

#### IT_INFRA_02: Verify session-template and generate-sessions create classroom_sessions.

- **Procedure:**
1. Call POST session-templates via MockMvc.
2. Call POST generate-sessions for an offering.
3. ClassroomInfrastructureService generates classroom_sessions from the template.
4. Count sessions for the offering.

- **Expected:**
Template is saved.
Sessions are generated according to template rules.
Duplicate generation is handled per service policy.

- **Pre:** Offering and template prerequisites exist.

- **Round 1:** Pending

#### IT_INFRA_03: Verify invalid room update is rejected without changing the database row.

- **Procedure:**
1. Call PUT room with invalid capacity/constraints via MockMvc.
2. Validation/service rejects before repository save.
3. Reload classroom_rooms.

- **Expected:**
Response is 400.
Room row is unchanged.

- **Pre:** Existing room.

- **Round 1:** Pending


## REPORT — Reports & Revenue

Requirement scope: Integrate TM/staff dashboard and CM revenue analytics endpoints.


### Dashboards

#### IT_REPORT_01: Verify STAFF/TM dashboard aggregates through TrainingManagerOpsService.getDashboard().

- **Procedure:**
1. Seed operational classroom/enrollment data.
2. Call GET /api/staff/dashboard (or TM alias) via MockMvc.
3. TrainingManagerDashboardController delegates to TrainingManagerOpsService.getDashboard().
4. Compare key counts with seeded DB totals.
5. Repeat as LEARNER.

- **Expected:**
Response is 200 with coherent metrics.
LEARNER receives 403.

- **Pre:** Seeded ops data; STAFF/TM JWT.

- **Round 1:** Pending

#### IT_REPORT_02: Verify CM revenue analytics through PaymentService.getRevenueAnalytics().

- **Procedure:**
1. Seed paid payment_orders.
2. Call GET /api/content-manager/revenue/analytics via MockMvc as CONTENT_MANAGER.
3. ContentManagerRevenueController delegates to PaymentService.getRevenueAnalytics().
4. Repeat as LEARNER.

- **Expected:**
CM receives 200 analytics payload.
LEARNER receives 403.
Totals are consistent with seeded paid orders.

- **Pre:** Paid orders seeded; CM JWT.

- **Round 1:** Pending


## PROPOSAL — Classroom Proposals

Requirement scope: Integrate staff proposal submit and manager approve/reject via ClassroomProposalService.


### Proposal flow

#### IT_PROPOSAL_01: Verify staff creates and submits a classroom proposal through ClassroomProposalService.

- **Procedure:**
1. Call POST /api/staff/classroom-proposals via MockMvc as STAFF.
2. Call PATCH submit.
3. ClassroomProposalService.create()/submit() persist classroom_proposals.
4. Query proposal status.

- **Expected:**
Proposal is persisted.
Status becomes SUBMITTED/PENDING per service rules.

- **Pre:** STAFF JWT.

- **Round 1:** Pending

#### IT_PROPOSAL_02: Verify manager approve/reject updates proposal status and side effects.

- **Procedure:**
1. Seed a pending proposal.
2. Call PATCH approve or reject via MockMvc as MANAGER.
3. ClassroomProposalService.approve()/reject() updates classroom_proposals.
4. If approve creates an offering, query classroom_offerings.
5. Attempt approve as STAFF.

- **Expected:**
Status is updated for manager.
Approve side effects occur only when implemented.
STAFF cannot approve.

- **Pre:** Pending proposal exists.

- **Round 1:** Pending

#### IT_PROPOSAL_03: Verify LEARNER cannot access proposal APIs.

- **Procedure:**
1. Authenticate as LEARNER.
2. Call GET/POST proposal endpoints via MockMvc.
3. Security rejects before ClassroomProposalService.

- **Expected:**
Response is 403.
No proposal row is created.

- **Pre:** LEARNER JWT.

- **Round 1:** Pending


## DISPUTE — Attendance Disputes

Requirement scope: Integrate learner dispute create and teacher review via ClassroomAttendanceDisputeService.


### Disputes

#### IT_DISPUTE_01: Verify learner creates an attendance dispute through ClassroomAttendanceDisputeService.

- **Procedure:**
1. Seed an attendance row for the learner.
2. Call POST /api/student/attendance/{attendanceId}/disputes via MockMvc.
3. Controller delegates to ClassroomAttendanceDisputeService.create().
4. Query classroom_attendance_disputes and learner dispute list.

- **Expected:**
Dispute row is created with PENDING status.
Owner is the authenticated learner.

- **Pre:** Attendance row exists.

- **Round 1:** Pending

#### IT_DISPUTE_02: Verify teacher review resolves dispute and updates attendance consistently.

- **Procedure:**
1. Seed a pending dispute.
2. Call teacher review endpoint via MockMvc.
3. ClassroomAttendanceDisputeService.review() updates dispute and attendance repositories.
4. Query both tables.

- **Expected:**
Dispute is resolved.
Attendance reflects the review decision.
Invalid review payloads are rejected.

- **Pre:** Pending dispute; TEACHER JWT.

- **Round 1:** Pending

#### IT_DISPUTE_03: Verify learner cannot call teacher dispute review endpoints.

- **Procedure:**
1. Authenticate as LEARNER.
2. Call teacher review API via MockMvc.
3. Security rejects before ClassroomAttendanceDisputeService.review().

- **Expected:**
Response is 403.
Dispute remains PENDING.

- **Pre:** LEARNER JWT; pending dispute id.

- **Round 1:** Pending


## NOTES — Learning Notes

Requirement scope: Integrate learner lesson notes CRUD through LearnerLearningExperienceService.


### Notes

#### IT_NOTES_01: Verify create/update/delete lesson note persists through LearnerLearningExperienceService.

- **Procedure:**
1. Authenticate an enrolled LEARNER.
2. Call create/update/delete note APIs via MockMvc.
3. StudentLearningExperienceController delegates to LearnerLearningExperienceService note methods.
4. Query learner_lesson_notes after each step.
5. Attempt access as another learner.

- **Expected:**
CRUD reflects database state.
Other learner cannot access the note.
Deleted note is removed or hidden per service rules.

- **Pre:** Enrolled LEARNER JWT.

- **Round 1:** Pending

#### IT_NOTES_02: Verify note APIs reject unauthenticated or non-enrolled callers before persistence.

- **Procedure:**
1. Call create note via MockMvc without JWT.
2. Call create note as LEARNER without enrollment.
3. Confirm LearnerLearningExperienceService authorization denies insert.
4. Count learner_lesson_notes.

- **Expected:**
Unauthenticated call returns 401/403.
Non-enrolled call is denied.
No note row is inserted.

- **Pre:** Published course without enrollment for the caller.

- **Round 1:** Pending
