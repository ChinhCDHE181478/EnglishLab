# -*- coding: utf-8 -*-
"""Full-project IT modules — content written in Controller→Service→Repository style."""


def C(id_, desc, proc, exp, pre):
    return {"id": id_, "desc": desc, "proc": proc, "exp": exp, "pre": pre}


def G(name, cases):
    return {"name": name, "cases": cases}


MODULES = [
    {
        "code": "AUTH",
        "sheet": "IT - Auth",
        "name": "Authentication",
        "function": "AuthController",
        "requirement": "Integrate AuthController with AuthService and persistence for register/verify/login/OTP reset.",
        "components": "AuthController, AuthService, UserRepository, AuthTokenRepository, JwtAuthenticationFilter",
        "integrations": "REST → AuthService → users/auth_tokens; mail stubbed",
        "srs": "UC-01, UC-03, UC-04",
        "groups": [
            G("Register & verify", [
                C(
                    "IT_AUTH_01",
                    "Verify registering an account through AuthController persists user and verification token via AuthService.",
                    "1. Call POST /api/auth/register via MockMvc with a unique email, password and fullName.\n"
                    "2. AuthController.register() delegates to AuthService.register().\n"
                    "3. AuthService creates the account through UserRepository.save() and stores a verification OTP through AuthTokenRepository.\n"
                    "4. Query the users and auth_tokens tables for the new email.",
                    "Response is 200/201 with a success message.\n"
                    "One users row is inserted with hashed password (not plaintext).\n"
                    "One verification auth_tokens row is linked to that user.\n"
                    "No access token is required for this call.",
                    "Database is available; email is unused; mail sender is stubbed.",
                ),
                C(
                    "IT_AUTH_02",
                    "Verify duplicate registration is rejected by AuthService without inserting a second users row.",
                    "1. Seed an existing LEARNER in users.\n"
                    "2. Call POST /api/auth/register via MockMvc with the same email.\n"
                    "3. AuthController delegates to AuthService.register() which checks UserRepository before insert.\n"
                    "4. Count users rows for that email.",
                    "Response is 4xx with a duplicate-email business error.\n"
                    "Exactly one users row remains for the email.\n"
                    "No extra verification token is created for a second account.",
                    "An active user with the target email already exists.",
                ),
                C(
                    "IT_AUTH_03",
                    "Verify email verification activates the account through AuthService and AuthTokenRepository.",
                    "1. Register a user and read the OTP from auth_tokens.\n"
                    "2. Call POST /api/auth/verify-email via MockMvc with email and OTP.\n"
                    "3. AuthController.verifyEmail() delegates to AuthService.verifyEmail().\n"
                    "4. AuthService validates the token via AuthTokenRepository and updates the user via UserRepository.\n"
                    "5. Reload users and auth_tokens.",
                    "Response is 200 OK.\n"
                    "users.email_verified becomes true.\n"
                    "Verification token is consumed/expired per AuthService rules.\n"
                    "Subsequent login with the password succeeds.",
                    "A pending verification user and valid OTP exist.",
                ),
                C(
                    "IT_AUTH_04",
                    "Verify invalid OTP is rejected and leaves the user unverified.",
                    "1. Seed an unverified user with a real OTP in auth_tokens.\n"
                    "2. Call POST /api/auth/verify-email via MockMvc with a wrong OTP.\n"
                    "3. AuthService.verifyEmail() validates against AuthTokenRepository and must not update UserRepository verification flag.\n"
                    "4. Query users.email_verified.",
                    "Response is 4xx.\n"
                    "email_verified remains false.\n"
                    "Account is not activated.",
                    "An unverified user exists.",
                ),
            ]),
            G("Login & security", [
                C(
                    "IT_AUTH_05",
                    "Verify login through AuthController returns a JWT accepted by the security filter and UserController.",
                    "1. Seed a verified LEARNER with a known password hash.\n"
                    "2. Call POST /api/auth/login via MockMvc.\n"
                    "3. AuthController.login() delegates to AuthService.login() which authenticates via UserRepository/UserDetails.\n"
                    "4. Call GET /api/user/me via MockMvc with Authorization Bearer accessToken.\n"
                    "5. JwtAuthenticationFilter authenticates the request before UserController.getCurrentUser().",
                    "Login response is 200 OK and contains accessToken plus user payload.\n"
                    "GET /api/user/me returns 200 with the same email/id.\n"
                    "JWT subject matches the authenticated user.",
                    "A verified LEARNER account exists.",
                ),
                C(
                    "IT_AUTH_06",
                    "Verify wrong password login fails in AuthService and issues no usable token.",
                    "1. Seed a verified user.\n"
                    "2. Call POST /api/auth/login via MockMvc with an incorrect password.\n"
                    "3. AuthService.login() rejects authentication before issuing JWT.",
                    "Response is 401/403.\n"
                    "No valid accessToken is returned.\n"
                    "Protected endpoints remain inaccessible without a token.",
                    "A verified user exists.",
                ),
                C(
                    "IT_AUTH_07",
                    "Verify missing JWT is blocked by SecurityFilterChain before UserController executes.",
                    "1. Call GET /api/user/me via MockMvc without Authorization header.\n"
                    "2. Observe that JwtAuthenticationFilter / authorizeHttpRequests rejects the call before UserService is invoked.",
                    "Response is 401 or 403.\n"
                    "No UserResponse body is returned.\n"
                    "Confirms controller-security integration.",
                    "Spring Security configuration is active.",
                ),
            ]),
            G("Password recovery", [
                C(
                    "IT_AUTH_08",
                    "Verify forgot-password and reset-password flow updates credentials through AuthService.",
                    "1. Seed a verified user and capture the current password hash.\n"
                    "2. Call POST /api/auth/forgot-password via MockMvc.\n"
                    "3. AuthService creates a reset token via AuthTokenRepository.\n"
                    "4. Call POST /api/auth/reset-password via MockMvc with OTP and new password.\n"
                    "5. AuthService updates the hash via UserRepository.save().\n"
                    "6. Login with the new password and attempt the old password.",
                    "Forgot-password returns a generic success response.\n"
                    "Reset succeeds and users.password hash changes.\n"
                    "Login works only with the new password.",
                    "Verified user exists; mail is stubbed.",
                ),
                C(
                    "IT_AUTH_09",
                    "Verify invalid reset OTP does not change the password hash.",
                    "1. Seed a verified user and capture password hash.\n"
                    "2. Call POST /api/auth/reset-password via MockMvc with an invalid OTP.\n"
                    "3. AuthService.resetPassword() rejects via AuthTokenRepository validation.\n"
                    "4. Compare password hash in users.",
                    "Response is 4xx.\n"
                    "Password hash is unchanged.\n"
                    "Old credentials still authenticate.",
                    "A verified user exists.",
                ),
                C(
                    "IT_AUTH_10",
                    "Verify resend-verification rotates or recreates a verification token through AuthService.",
                    "1. Seed an unverified user.\n"
                    "2. Call POST /api/auth/resend-verification via MockMvc.\n"
                    "3. AuthController delegates to AuthService which writes AuthTokenRepository.\n"
                    "4. Query auth_tokens for an active verification token.",
                    "Response is 200 OK.\n"
                    "An active verification token exists for the user.\n"
                    "No account is marked verified by this call alone.",
                    "An unverified user exists; mail is stubbed.",
                ),
            ]),
        ],
    },
    {
        "code": "USER",
        "sheet": "IT - User",
        "name": "Account Profile",
        "function": "UserController",
        "requirement": "Integrate UserController profile/avatar/password with UserService and UserRepository.",
        "components": "UserController, UserService, AvatarStorageService, UserRepository",
        "integrations": "authenticated REST → UserService → users/storage",
        "srs": "UC-05",
        "groups": [
            G("Profile", [
                C(
                    "IT_USER_01",
                    "Verify fetching the current profile via UserController loads data through UserService and UserRepository.",
                    "1. Authenticate as LEARNER and obtain a JWT.\n"
                    "2. Call GET /api/user/me via MockMvc with the Bearer token.\n"
                    "3. UserController.getCurrentUser() delegates to UserService.getCurrentUser().\n"
                    "4. UserService loads the account through UserRepository.\n"
                    "5. Compare response fields with the users table row.",
                    "Response is 200 OK.\n"
                    "Returned JSON contains correct id, email, fullName and role.\n"
                    "Values match the users row for the token subject.\n"
                    "No other user's data is returned.",
                    "A LEARNER account exists and a valid JWT is available.",
                ),
                C(
                    "IT_USER_02",
                    "Verify updating profile through UserController persists fields via UserService/UserRepository.",
                    "1. Call PUT /api/user/me via MockMvc with updated fullName/phone/target fields.\n"
                    "2. UserController.updateCurrentUser() delegates to UserService.updateProfile().\n"
                    "3. UserService saves changes through UserRepository.save().\n"
                    "4. Query users and call GET /api/user/me again.",
                    "Response is 200 OK.\n"
                    "Persisted columns match the request payload.\n"
                    "GET /api/user/me reflects the updates.",
                    "Valid JWT; request passes validation.",
                ),
                C(
                    "IT_USER_03",
                    "Verify change-password requires the current password and updates the hash via UserService.",
                    "1. Call PUT /api/user/me/password via MockMvc with a wrong currentPassword.\n"
                    "2. UserService.changePassword() must reject before UserRepository update.\n"
                    "3. Call PUT again with the correct currentPassword and a newPassword.\n"
                    "4. UserService updates the hash through UserRepository.\n"
                    "5. Login with the new password.",
                    "Wrong current password returns 4xx and hash is unchanged.\n"
                    "Correct change returns success/204 and hash changes.\n"
                    "Login succeeds only with the new password.",
                    "Account with a known password exists.",
                ),
                C(
                    "IT_USER_04",
                    "Verify avatar upload through UserController stores a file and updates users.avatar_url.",
                    "1. Call POST /api/user/me/avatar via MockMvc as multipart.\n"
                    "2. UserController.updateAvatar() delegates to UserService/AvatarStorageService.\n"
                    "3. AvatarStorageService stores the file and UserRepository updates avatar_url.\n"
                    "4. Call GET /api/user/avatars/{fileName}.",
                    "Upload response is 200 OK with updated user payload.\n"
                    "users.avatar_url is set.\n"
                    "Public avatar GET returns the stored file content type.",
                    "Valid JWT; avatar storage is writable.",
                ),
                C(
                    "IT_USER_05",
                    "Verify unauthenticated profile update is blocked before UserService runs.",
                    "1. Call PUT /api/user/me via MockMvc without Authorization.\n"
                    "2. Confirm SecurityFilterChain rejects the request before UserController/UserService execution.\n"
                    "3. Verify users table is unchanged.",
                    "Response is 401/403.\n"
                    "No profile fields are updated in the database.",
                    "None.",
                ),
            ]),
        ],
    },
    {
        "code": "NOTIF",
        "sheet": "IT - Notif",
        "name": "Notifications",
        "function": "StudentNotificationController",
        "requirement": "Integrate notification preferences and AppNotification list/read flows.",
        "components": "UserController, NotificationPreferenceService, StudentNotificationController, AppNotificationService",
        "integrations": "REST → preference/notification services → notification_preferences/app_notifications",
        "srs": "UC-06",
        "groups": [
            G("Preferences", [
                C(
                    "IT_NOTIF_01",
                    "Verify GET notification-preferences defaults both channels when no preference row exists.",
                    "1. Authenticate a user that has no notification_preferences row.\n"
                    "2. Call GET /api/user/me/notification-preferences via MockMvc.\n"
                    "3. UserController delegates to NotificationPreferenceService.getForUser().\n"
                    "4. Service returns defaults when NotificationPreferenceRepository.findByUserId() is empty.",
                    "Response is 200 OK.\n"
                    "emailEnabled=true and inAppEnabled=true.\n"
                    "GET does not require a pre-inserted preference row.",
                    "Authenticated user without a preference row.",
                ),
                C(
                    "IT_NOTIF_02",
                    "Verify PUT preferences upserts notification_preferences and gates AppNotificationService.createForUser().",
                    "1. Call PUT /api/user/me/notification-preferences via MockMvc with inAppEnabled=false.\n"
                    "2. NotificationPreferenceService.updateForUser() saves through NotificationPreferenceRepository.\n"
                    "3. Trigger a business path that calls AppNotificationService.createForUser() for the same user.\n"
                    "4. AppNotificationService checks NotificationPreferenceService.isInAppEnabled() before AppNotificationRepository.save().\n"
                    "5. Re-enable in-app and trigger again; compare app_notifications counts.",
                    "Preference row is persisted with inAppEnabled=false.\n"
                    "No new app_notifications row is created while in-app is disabled.\n"
                    "After re-enable, a new notification row is inserted.",
                    "Valid JWT; a notify path is available in the test harness.",
                ),
                C(
                    "IT_NOTIF_03",
                    "Verify preference update validation rejects null channel flags before persistence.",
                    "1. Call PUT /api/user/me/notification-preferences via MockMvc with a null/missing channel field.\n"
                    "2. Controller validation fails before NotificationPreferenceService.updateForUser().\n"
                    "3. Confirm notification_preferences is unchanged.",
                    "Response is 400 Bad Request.\n"
                    "No partial preference overwrite occurs.",
                    "Valid JWT.",
                ),
            ]),
            G("In-app list", [
                C(
                    "IT_NOTIF_04",
                    "Verify listing and marking notifications read through AppNotificationService.",
                    "1. Seed app_notifications owned by the learner.\n"
                    "2. Call GET /api/student/notifications via MockMvc.\n"
                    "3. StudentNotificationController delegates to AppNotificationService.listForUser().\n"
                    "4. Call PATCH /api/student/notifications/{id}/read.\n"
                    "5. AppNotificationService.markRead() updates AppNotificationRepository.\n"
                    "6. Call GET /api/student/notifications/unread-count.",
                    "List returns the seeded notifications.\n"
                    "Mark-read sets read=true and read_at.\n"
                    "Unread count decreases by one.",
                    "LEARNER JWT; notification rows owned by that user.",
                ),
                C(
                    "IT_NOTIF_05",
                    "Verify mark-all-read updates only the authenticated user's unread notifications.",
                    "1. Seed unread notifications for learner A and learner B.\n"
                    "2. Authenticate as A and call PATCH /api/student/notifications/read-all via MockMvc.\n"
                    "3. AppNotificationService.markAllRead() updates only A's rows through AppNotificationRepository.\n"
                    "4. Query app_notifications for A and B.",
                    "All of A's unread notifications become read.\n"
                    "B's notifications remain unread.\n"
                    "No cross-user update occurs.",
                    "Two LEARNER accounts with unread notifications.",
                ),
            ]),
        ],
    },
    {
        "code": "COMMERCE",
        "sheet": "IT - Commerce",
        "name": "Cart & Wishlist",
        "function": "StudentCommerceController",
        "requirement": "Integrate StudentCommerceController cart/wishlist with StudentCommerceService persistence.",
        "components": "StudentCommerceController, StudentCommerceService",
        "integrations": "LEARNER REST → commerce service → cart/wishlist storage",
        "srs": "UC-45, UC-46",
        "groups": [
            G("Cart & wishlist", [
                C(
                    "IT_COMMERCE_01",
                    "Verify adding a course to cart through StudentCommerceController persists cart state.",
                    "1. Seed a published online course.\n"
                    "2. Call POST /api/student/commerce/cart/{courseId} via MockMvc as LEARNER.\n"
                    "3. StudentCommerceController delegates to StudentCommerceService.addToCart().\n"
                    "4. Call GET /api/student/commerce/cart and inspect persisted cart contents.",
                    "Add response is 200 OK.\n"
                    "GET cart contains the courseId.\n"
                    "Unauthenticated POST is rejected by security.",
                    "Published course and LEARNER JWT are available.",
                ),
                C(
                    "IT_COMMERCE_02",
                    "Verify wishlist add and move-to-cart keep wishlist and cart collections consistent.",
                    "1. Call POST /api/student/commerce/wishlist/{courseId} via MockMvc.\n"
                    "2. StudentCommerceService.addToWishlist() persists wishlist membership.\n"
                    "3. Call POST /api/student/commerce/wishlist/{courseId}/move-to-cart.\n"
                    "4. Service updates both wishlist and cart stores.\n"
                    "5. GET wishlist and GET cart.",
                    "Course appears in wishlist then moves to cart per service rules.\n"
                    "No course ownership/enrollment is created yet.",
                    "LEARNER JWT; course is not already owned.",
                ),
                C(
                    "IT_COMMERCE_03",
                    "Verify clearing cart removes all items through StudentCommerceService.",
                    "1. Add one or more courses to cart.\n"
                    "2. Call DELETE /api/student/commerce/cart via MockMvc.\n"
                    "3. StudentCommerceService clears persisted cart state.\n"
                    "4. GET cart and ensure no payment_orders were created.",
                    "Cart is empty after delete.\n"
                    "No payment_orders row is inserted by clear-cart.",
                    "LEARNER JWT with a non-empty cart.",
                ),
                C(
                    "IT_COMMERCE_04",
                    "Verify TEACHER cannot mutate learner cart endpoints under SecurityConfig.",
                    "1. Authenticate as TEACHER.\n"
                    "2. Call POST /api/student/commerce/cart/{courseId} via MockMvc.\n"
                    "3. Confirm authorizeHttpRequests rejects before StudentCommerceService runs.",
                    "Response is 403 Forbidden.\n"
                    "No cart mutation is persisted.",
                    "TEACHER JWT.",
                ),
            ]),
        ],
    },
    {
        "code": "PAYMENT",
        "sheet": "IT - Payment",
        "name": "PayOS & Orders",
        "function": "StudentPaymentController",
        "requirement": "Integrate payment link/quote, payment_orders persistence and PayOS webhook side effects.",
        "components": "StudentPaymentController, PayosWebhookController, PaymentService, payment_orders",
        "integrations": "REST → PaymentService → DB; PayOS client mocked; webhook → ownership",
        "srs": "UC-47, UC-08",
        "groups": [
            G("Checkout", [
                C(
                    "IT_PAYMENT_01",
                    "Verify creating a PayOS payment link inserts a PENDING payment_orders row via PaymentService.",
                    "1. Put a published course into the learner cart.\n"
                    "2. Call POST /api/student/payments/payos/link via MockMvc.\n"
                    "3. StudentPaymentController delegates to PaymentService.createPaymentLink().\n"
                    "4. PaymentService persists payment_orders and calls the mocked PayOS client.\n"
                    "5. Query payment_orders by returned orderCode.",
                    "Response is 200 OK with checkout URL/orderCode.\n"
                    "One payment_orders row exists with PENDING (or equivalent) status.\n"
                    "Amount matches the quoted total.",
                    "LEARNER JWT; PayOS client stubbed.",
                ),
                C(
                    "IT_PAYMENT_02",
                    "Verify quote/link on empty cart fails without inserting payment_orders.",
                    "1. Clear the cart.\n"
                    "2. Call payment quote/link via MockMvc.\n"
                    "3. PaymentService validates cart contents before repository insert.\n"
                    "4. Count payment_orders for the user.",
                    "Response is 4xx business error.\n"
                    "No new payment_orders row is created.",
                    "LEARNER with empty cart.",
                ),
                C(
                    "IT_PAYMENT_03",
                    "Verify PayOS webhook marks order paid and grants course access idempotently.",
                    "1. Seed a PENDING payment_orders row for learner+course.\n"
                    "2. Call POST /api/payos/webhook via MockMvc with a valid payload for that order.\n"
                    "3. PayosWebhookController delegates to PaymentService.handlePayosWebhook().\n"
                    "4. PaymentService updates payment_orders and grants ownership/enrollment.\n"
                    "5. Replay the same webhook once.",
                    "Order status becomes PAID/SUCCESS.\n"
                    "Learner gains course access exactly once.\n"
                    "Replay does not create a duplicate enrollment/ownership row.",
                    "Pending order exists; webhook verification configured for test.",
                ),
                C(
                    "IT_PAYMENT_04",
                    "Verify GET payment orders returns only the authenticated learner's orders.",
                    "1. Seed payment_orders for learner A and learner B.\n"
                    "2. Authenticate as A and call GET /api/student/payments/orders via MockMvc.\n"
                    "3. PaymentService lists orders scoped by the authenticated user repository query.",
                    "Response is 200 OK.\n"
                    "Only learner A's orders are returned.\n"
                    "No cross-user order leakage.",
                    "Two LEARNER accounts with distinct orders.",
                ),
                C(
                    "IT_PAYMENT_05",
                    "Verify manager/CM payment order listing is role-protected.",
                    "1. Authenticate as MANAGER or CONTENT_MANAGER.\n"
                    "2. Call GET /api/manager/payments/orders (or CM alias) via MockMvc.\n"
                    "3. PaymentService returns aggregated/order list data.\n"
                    "4. Repeat with LEARNER token.",
                    "Staff/CM call returns 200 with order data.\n"
                    "LEARNER receives 403 on the manager payments path.",
                    "Seeded orders; MANAGER/CM and LEARNER tokens.",
                ),
            ]),
        ],
    },
    {
        "code": "COURSE",
        "sheet": "IT - Course",
        "name": "Online Learning",
        "function": "PublicOnlineCourseController",
        "requirement": "Integrate public catalog and learner content/progress through OnlineCourseService.",
        "components": "PublicOnlineCourseController, StudentOnlineCourseController, OnlineCourseService",
        "integrations": "permitAll catalog; LEARNER content/progress → DB",
        "srs": "UC-02, UC-08, UC-48",
        "groups": [
            G("Catalog", [
                C(
                    "IT_COURSE_01",
                    "Verify guest course catalog listing goes through OnlineCourseService and returns published courses only.",
                    "1. Seed published and draft online_courses.\n"
                    "2. Call GET /api/online-courses via MockMvc without JWT.\n"
                    "3. PublicOnlineCourseController delegates to OnlineCourseService.getPublicCourses().\n"
                    "4. Inspect returned list against online_courses.status.",
                    "Response is 200 OK.\n"
                    "Published courses appear.\n"
                    "Draft courses are hidden according to service rules.",
                    "Published and draft courses are seeded.",
                ),
                C(
                    "IT_COURSE_02",
                    "Verify public course detail by slug/id through OnlineCourseService.getPublicCourse().",
                    "1. Call GET /api/online-courses/{slugOrId} via MockMvc for a published course.\n"
                    "2. Controller delegates to OnlineCourseService.getPublicCourse().\n"
                    "3. Call again with a non-existent slug.",
                    "Published detail returns 200 with course payload.\n"
                    "Missing course returns 404.\n"
                    "No authentication is required for public GET.",
                    "A published course exists.",
                ),
            ]),
            G("Learner progress", [
                C(
                    "IT_COURSE_03",
                    "Verify enrolled learner content and lesson progress update through OnlineCourseService.",
                    "1. Seed enrollment/ownership for a LEARNER.\n"
                    "2. Call GET /api/student/online-courses/{courseId}/content via MockMvc.\n"
                    "3. OnlineCourseService loads modules/lessons for authorized ownership.\n"
                    "4. Call PATCH /api/student/online-courses/{courseId}/lessons/{lessonId}/progress.\n"
                    "5. Service persists progress and reload progress storage.",
                    "Content response is 200 with modules/lessons.\n"
                    "Progress patch persists for the lesson.\n"
                    "Unauthenticated content call is rejected.",
                    "Enrolled LEARNER; course contains lessons.",
                ),
                C(
                    "IT_COURSE_04",
                    "Verify non-enrolled learner is denied protected course content by OnlineCourseService.",
                    "1. Authenticate a LEARNER without ownership of the course.\n"
                    "2. Call GET content via MockMvc.\n"
                    "3. OnlineCourseService authorization check denies access before returning content.\n"
                    "4. Confirm no progress rows were created.",
                    "Response is 403/404.\n"
                    "No protected content payload is returned.\n"
                    "No progress side effect occurs.",
                    "Published course without enrollment for the caller.",
                ),
                C(
                    "IT_COURSE_05",
                    "Verify my-enrollments lists only courses owned by the authenticated learner.",
                    "1. Seed distinct enrollments for learner A and learner B.\n"
                    "2. Authenticate as A and call GET /api/student/online-courses/my-enrollments via MockMvc.\n"
                    "3. OnlineCourseService.getMyEnrollments() queries enrollments scoped to the current user.",
                    "Response is 200 OK.\n"
                    "Only learner A courses are returned.\n"
                    "No cross-user enrollment leakage.",
                    "Two LEARNER accounts with different enrollments.",
                ),
                C(
                    "IT_COURSE_06",
                    "Verify course rating create/get persists through CourseReviewService into course_reviews.",
                    "1. Authenticate an eligible LEARNER.\n"
                    "2. Call POST /api/student/online-courses/{courseId}/rating via MockMvc.\n"
                    "3. StudentOnlineCourseController delegates to CourseReviewService.saveRating().\n"
                    "4. Query course_reviews and call GET rating.",
                    "Review is upserted per unique student-course constraint.\n"
                    "GET returns the saved rating.\n"
                    "Unauthorized/non-eligible caller is denied.",
                    "Eligible LEARNER for the target course.",
                ),
            ]),
        ],
    },
]


def _extend_remaining():
    """Append remaining modules with the same writing style."""
    more = [
        {
            "code": "DISCUSS",
            "sheet": "IT - Discuss",
            "name": "Course Discussion",
        "function": "CourseDiscussionController",
            "requirement": "Integrate discussion create/reply/report and CM moderation through discussion services.",
            "components": "CourseDiscussionController, DiscussionModerationController, CourseDiscussionService, DiscussionModerationService",
            "integrations": "LEARNER write + CM moderate → discussion tables",
            "srs": "UC-49, UC-50",
            "groups": [
                G("Learner discussion", [
                    C(
                        "IT_DISCUSS_01",
                        "Verify enrolled learner creates a discussion thread via CourseDiscussionService.",
                        "1. Authenticate an enrolled LEARNER.\n"
                        "2. Call the student create-thread API via MockMvc.\n"
                        "3. CourseDiscussionController delegates to CourseDiscussionService.createThread().\n"
                        "4. Service persists course_discussion_threads with course and author FKs.",
                        "Response is 200 OK.\n"
                        "One thread row is inserted for the course.\n"
                        "Thread owner matches the authenticated learner.",
                        "Enrolled LEARNER JWT.",
                    ),
                    C(
                        "IT_DISCUSS_02",
                        "Verify reply and reaction/helpful updates are persisted by CourseDiscussionService.",
                        "1. Seed an existing thread.\n"
                        "2. Call reply and reaction/helpful endpoints via MockMvc.\n"
                        "3. CourseDiscussionService writes reply/reaction repositories.\n"
                        "4. Query course_discussion_replies and related reaction data.",
                        "Reply row is created.\n"
                        "Reaction/helpful state is persisted consistently.\n"
                        "Counts in response match database state.",
                        "Existing discussion thread.",
                    ),
                    C(
                        "IT_DISCUSS_03",
                        "Verify reporting a discussion creates a moderation queue item.",
                        "1. Call learner report API via MockMvc.\n"
                        "2. CourseDiscussionService creates a report record.\n"
                        "3. Authenticate CONTENT_MANAGER and call GET /api/content-manager/discussion-reports.\n"
                        "4. DiscussionModerationService.getReports() returns the pending report.",
                        "Report is visible in CM queue.\n"
                        "Report references the reported thread/reply.\n"
                        "Learner cannot call moderation hide/dismiss.",
                        "LEARNER and CONTENT_MANAGER accounts.",
                    ),
                ]),
                G("Moderation", [
                    C(
                        "IT_DISCUSS_04",
                        "Verify CM hide/dismiss updates report status through DiscussionModerationService.",
                        "1. Seed a pending discussion report.\n"
                        "2. Call POST hide or dismiss via MockMvc as CONTENT_MANAGER.\n"
                        "3. DiscussionModerationService.hide()/dismiss() updates report and visibility rules.\n"
                        "4. Query report status and thread visibility.",
                        "Report status is updated.\n"
                        "Thread/reply visibility follows moderation outcome.\n"
                        "Audit/report row remains queryable.",
                        "Pending report exists.",
                    ),
                    C(
                        "IT_DISCUSS_05",
                        "Verify LEARNER cannot call discussion moderation endpoints.",
                        "1. Authenticate as LEARNER.\n"
                        "2. Call moderation hide/dismiss via MockMvc.\n"
                        "3. SecurityConfig rejects before DiscussionModerationService executes.",
                        "Response is 403 Forbidden.\n"
                        "Report status is unchanged.",
                        "LEARNER JWT; pending report id.",
                    ),
                ]),
            ],
        },
        {
            "code": "CONTENT",
            "sheet": "IT - Content",
            "name": "CM Online Courses",
        "function": "ContentManagerOnlineCourseController",
            "requirement": "Integrate Content Manager online course CRUD/publish/version/category flows.",
            "components": "ContentManagerOnlineCourseController, OnlineCourseService, version/category services",
            "integrations": "CONTENT_MANAGER → online_courses and related tables",
            "srs": "UC-33",
            "groups": [
                G("Course CM", [
                    C(
                        "IT_CONTENT_01",
                        "Verify CM creates an online course through OnlineCourseService into online_courses.",
                        "1. Authenticate as CONTENT_MANAGER.\n"
                        "2. Call POST /api/content-manager/online-courses via MockMvc.\n"
                        "3. ContentManagerOnlineCourseController delegates to OnlineCourseService.createCourse().\n"
                        "4. Query online_courses.\n"
                        "5. Repeat create with LEARNER token.",
                        "CM create returns 200 and inserts online_courses.\n"
                        "LEARNER receives 403 on CM API.\n"
                        "Persisted title/status match request.",
                        "CONTENT_MANAGER JWT.",
                    ),
                    C(
                        "IT_CONTENT_02",
                        "Verify publish/archive transitions update status and public catalog visibility.",
                        "1. Seed a draft course.\n"
                        "2. Call CM publish then archive endpoints via MockMvc.\n"
                        "3. OnlineCourseService updates online_courses.status.\n"
                        "4. Call public GET /api/online-courses and compare visibility.",
                        "Status transitions are persisted.\n"
                        "Public catalog reflects publish rules.\n"
                        "Archived/draft course is not publicly listed.",
                        "Draft course owned/managed by CM.",
                    ),
                    C(
                        "IT_CONTENT_03",
                        "Verify course version create/publish snapshot path through version APIs.",
                        "1. Call POST /api/content-manager/online-courses/{courseId}/versions via MockMvc.\n"
                        "2. Call PATCH publish on the version.\n"
                        "3. Version service persists version tables and publish flags.\n"
                        "4. Query version records.",
                        "Version row is created.\n"
                        "Publish flag/status is consistent.\n"
                        "Unauthorized role cannot publish versions.",
                        "CM course exists.",
                    ),
                    C(
                        "IT_CONTENT_04",
                        "Verify category CRUD persists course_categories through category management service.",
                        "1. Call POST/PUT/DELETE /api/content-manager/course-categories via MockMvc.\n"
                        "2. CourseCategoryManagementService writes course_categories.\n"
                        "3. Query table after each operation.\n"
                        "4. Send an invalid payload.",
                        "CRUD operations persist correctly.\n"
                        "Invalid payload returns 400 without insert.\n"
                        "LEARNER is forbidden.",
                        "CONTENT_MANAGER JWT.",
                    ),
                ]),
            ],
        },
        {
            "code": "PACKAGE",
            "sheet": "IT - Package",
            "name": "Packages & Bundles",
        "function": "LearningPackageManagementController",
            "requirement": "Integrate CM package/bundle management through LearningPackageManagementService.",
            "components": "ContentManagerPackageController, LearningPackageManagementService, packages",
            "integrations": "CONTENT_MANAGER → packages/bundle relations",
            "srs": "Package management",
            "groups": [
                G("Packages", [
                    C(
                        "IT_PACKAGE_01",
                        "Verify creating a package persists packages via LearningPackageManagementService.",
                        "1. Authenticate as CONTENT_MANAGER.\n"
                        "2. Call POST /api/content-manager/packages via MockMvc.\n"
                        "3. Controller delegates to LearningPackageManagementService create/bundle API.\n"
                        "4. Query packages table.",
                        "Response is 200 OK.\n"
                        "One packages row is inserted with request fields.",
                        "CONTENT_MANAGER JWT.",
                    ),
                    C(
                        "IT_PACKAGE_02",
                        "Verify bundle-items update replaces relations without orphan rows.",
                        "1. Seed a package.\n"
                        "2. Call PUT /api/content-manager/packages/{id}/bundle-items via MockMvc.\n"
                        "3. LearningPackageManagementService replaces bundle relations.\n"
                        "4. Query bundle-item relation table.",
                        "Relations match the requested course/item set.\n"
                        "No orphan relation rows remain.",
                        "Package and candidate courses exist.",
                    ),
                    C(
                        "IT_PACKAGE_03",
                        "Verify publish/archive package updates status through package service.",
                        "1. Call PATCH publish/archive on a package via MockMvc.\n"
                        "2. Service updates packages.status.\n"
                        "3. Attempt the same call as LEARNER.",
                        "Status is updated for CM caller.\n"
                        "LEARNER receives 403.\n"
                        "Database status matches the last successful transition.",
                        "Package exists; CONTENT_MANAGER JWT.",
                    ),
                ]),
            ],
        },
        {
            "code": "CURRICULUM",
            "sheet": "IT - Curriculum",
            "name": "Curriculum & Banks",
        "function": "ContentManagerCurriculumController",
            "requirement": "Integrate curriculum programs/units, exercise bank, rubrics and learning paths.",
            "components": "ContentManagerCurriculumController, CurriculumProgramService, ExerciseBankController, AssessmentRubricController, LearningPathController",
            "integrations": "CONTENT_MANAGER → curriculum_*/banks/paths",
            "srs": "UC-32",
            "groups": [
                G("Curriculum", [
                    C(
                        "IT_CURRICULUM_01",
                        "Verify creating a curriculum program persists curriculum_programs via CurriculumProgramService.",
                        "1. Call POST curriculum-programs via MockMvc as CONTENT_MANAGER.\n"
                        "2. ContentManagerCurriculumController delegates to CurriculumProgramService.createProgram().\n"
                        "3. Query curriculum_programs and list programs.",
                        "Program row is inserted.\n"
                        "List includes the created program.\n"
                        "Unauthorized role is rejected.",
                        "CONTENT_MANAGER JWT.",
                    ),
                    C(
                        "IT_CURRICULUM_02",
                        "Verify unit creation under a program writes curriculum_units with program FK.",
                        "1. Seed a curriculum program.\n"
                        "2. Call create-unit API via MockMvc.\n"
                        "3. CurriculumProgramService persists curriculum_units.\n"
                        "4. Query unit.program_id.",
                        "Unit is saved.\n"
                        "Foreign key points to the parent program.",
                        "Program exists.",
                    ),
                    C(
                        "IT_CURRICULUM_03",
                        "Verify exercise-bank item CRUD through ExerciseBank service/controller.",
                        "1. Call POST/PUT/GET/DELETE /api/content-manager/exercise-bank via MockMvc.\n"
                        "2. Service persists exercise_bank_items (or equivalent bank table).\n"
                        "3. Send invalid payload and confirm no insert.",
                        "CRUD succeeds for valid payloads.\n"
                        "Invalid payload returns 400.\n"
                        "Deleted/archived item is no longer returned by default list rules.",
                        "CONTENT_MANAGER JWT.",
                    ),
                    C(
                        "IT_CURRICULUM_04",
                        "Verify learning-path create and course ordering through LearningPath APIs.",
                        "1. Call POST /api/content-manager/learning-paths via MockMvc.\n"
                        "2. Add courses and PUT order endpoint.\n"
                        "3. Service persists learning_paths and membership/order tables.\n"
                        "4. Query ordered membership.",
                        "Path is created.\n"
                        "Course membership and order match the request.",
                        "CONTENT_MANAGER JWT; courses exist.",
                    ),
                    C(
                        "IT_CURRICULUM_05",
                        "Verify rubric create persists assessment_rubrics and criteria.",
                        "1. Call POST /api/content-manager/rubrics via MockMvc.\n"
                        "2. Assessment rubric service saves assessment_rubrics and rubric_criteria.\n"
                        "3. Query both tables.",
                        "Rubric header and criteria rows are inserted.\n"
                        "Weights/names match the request payload.",
                        "CONTENT_MANAGER JWT.",
                    ),
                ]),
            ],
        },
        {
            "code": "ENROLLREQ",
            "sheet": "IT - EnrollReq",
            "name": "Enrollment Requests",
        "function": "ManagerEnrollmentController",
            "requirement": "Integrate learner enrollment requests and staff processing via EnrollmentRequestService.",
            "components": "StudentEnrollmentRequestController, StaffEnrollmentRequestController, EnrollmentRequestService",
            "integrations": "LEARNER↔STAFF → course_enrollment_requests (+ history)",
            "srs": "Enrollment request pipeline",
            "groups": [
                G("Learner request", [
                    C(
                        "IT_ENROLLREQ_01",
                        "Verify learner submit enrollment request persists through EnrollmentRequestService.",
                        "1. Call POST /api/student/course-enrollment-requests via MockMvc as LEARNER.\n"
                        "2. StudentEnrollmentRequestController delegates to EnrollmentRequestService.submit().\n"
                        "3. Service inserts course_enrollment_requests.\n"
                        "4. Call GET /my and query the table.",
                        "Request row is created for the learner.\n"
                        "It appears in listMine.\n"
                        "Initial status matches service defaults.",
                        "LEARNER JWT.",
                    ),
                    C(
                        "IT_ENROLLREQ_02",
                        "Verify learner cancel updates request status and history.",
                        "1. Seed an open enrollment request.\n"
                        "2. Call PATCH /{requestId}/cancel via MockMvc.\n"
                        "3. EnrollmentRequestService.cancel() updates request and writes course_enrollment_request_history.\n"
                        "4. Query request + history tables.",
                        "Request status becomes cancelled.\n"
                        "A history entry is written.\n"
                        "Cancelled request cannot be processed as open.",
                        "Open request owned by the learner.",
                    ),
                ]),
                G("Staff processing", [
                    C(
                        "IT_ENROLLREQ_03",
                        "Verify staff schedule-test and complete-test transitions via EnrollmentRequestService.",
                        "1. Authenticate as STAFF.\n"
                        "2. Call PATCH schedule-test then complete-test via MockMvc.\n"
                        "3. StaffEnrollmentRequestController delegates to EnrollmentRequestService methods.\n"
                        "4. Query request status after each step.",
                    "Status transitions follow the staff workflow.\n"
                        "Invalid transition is rejected.\n"
                        "Mail side effects may be stubbed.",
                        "Pending request exists.",
                    ),
                    C(
                        "IT_ENROLLREQ_04",
                        "Verify staff assign-class creates/updates classroom enrollment side effects.",
                        "1. Call PATCH assign-class with a target offering via MockMvc as STAFF.\n"
                        "2. EnrollmentRequestService.assignClass() updates the request and classroom enrollment state.\n"
                        "3. Query course_enrollment_requests and classroom_enrollments.",
                        "Request reaches assigned/completed state per rules.\n"
                        "Related classroom enrollment is created or updated.\n"
                        "Capacity/business rules are enforced.",
                        "Eligible request and offering with capacity.",
                    ),
                    C(
                        "IT_ENROLLREQ_05",
                        "Verify LEARNER cannot call staff enrollment-request endpoints.",
                        "1. Authenticate as LEARNER.\n"
                        "2. Call a staff PATCH endpoint via MockMvc.\n"
                        "3. Security rejects before EnrollmentRequestService staff methods run.",
                        "Response is 403.\n"
                        "Request status is unchanged.",
                        "LEARNER JWT; existing request id.",
                    ),
                ]),
            ],
        },
        {
            "code": "CLASS",
            "sheet": "IT - Classroom",
            "name": "TM Classroom Ops",
        "function": "TrainingManagerClassroomController",
            "requirement": "Integrate public offerings and TM enrollment/teacher operations via ClassroomOfferingService.",
            "components": "PublicClassroomController, TrainingManagerClassroomController, ClassroomOfferingService",
            "integrations": "STAFF/TM → classroom_offerings/enrollments/tuition",
            "srs": "UC-36, UC-37, UC-38",
            "groups": [
                G("Public & TM offering", [
                    C(
                        "IT_CLASS_01",
                        "Verify public classroom offerings listing through ClassroomOfferingService without authentication.",
                        "1. Seed ACTIVE classroom_offerings.\n"
                        "2. Call GET /api/classroom-offerings via MockMvc without JWT.\n"
                        "3. PublicClassroomController delegates to ClassroomOfferingService.getPublicOfferings().\n"
                        "4. Call GET /{slugOrId} for detail.",
                        "List/detail return 200.\n"
                        "Payload maps from classroom_offerings and related package title fields.",
                        "At least one public offering exists.",
                    ),
                    C(
                        "IT_CLASS_02",
                        "Verify TM/staff can list and get classroom detail via ClassroomOfferingService.",
                        "1. Authenticate as TRAINING_MANAGER/STAFF.\n"
                        "2. Call GET /api/training-manager/classrooms and GET /{id} via MockMvc.\n"
                        "3. TrainingManagerClassroomController delegates to ClassroomOfferingService manager queries.\n"
                        "4. Repeat with LEARNER token.",
                        "TM/staff calls return 200.\n"
                        "LEARNER receives 403 on TM classroom APIs.",
                        "TM/STAFF JWT; offerings seeded.",
                    ),
                ]),
                G("Enrollment pipeline", [
                    C(
                        "IT_CLASS_03",
                        "Verify confirm → tuition → assign updates enrollment consistently through ClassroomOfferingService.",
                        "1. Seed enrollment with PENDING_CONFIRMATION.\n"
                        "2. Call POST .../confirm via MockMvc as TM.\n"
                        "3. Call POST .../tuition with FULL payment.\n"
                        "4. Call POST .../assign.\n"
                        "5. ClassroomOfferingService methods update classroom_enrollments and classroom_tuition_payments.\n"
                        "6. Query both tables and final registration_status.",
                        "Final registration_status is ASSIGNED.\n"
                        "Tuition payment row exists.\n"
                        "Learner class-access semantics become true.",
                        "TM JWT; offering has free capacity.",
                    ),
                    C(
                        "IT_CLASS_04",
                        "Verify reject registration sets REJECTED without occupying a class slot.",
                        "1. Seed a pending enrollment.\n"
                        "2. Call POST .../reject via MockMvc.\n"
                        "3. ClassroomOfferingService.rejectRegistration() updates classroom_enrollments.\n"
                        "4. Compare offering occupied-slot count.",
                        "registration_status=REJECTED.\n"
                        "No ASSIGNED capacity is consumed.\n"
                        "Reject reason is stored per service rules.",
                        "Pending enrollment; TM JWT.",
                    ),
                    C(
                        "IT_CLASS_05",
                        "Verify reorderWaitlist updates waitlist_priority through ClassroomOfferingService.",
                        "1. Seed two WAITLIST enrollments with priorities 1 and 2.\n"
                        "2. Call ClassroomOfferingService.reorderWaitlist() (or HTTP mapping if exposed) with swapped IDs.\n"
                        "3. Service rewrites waitlist_priority via ClassroomEnrollmentRepository.\n"
                        "4. Query priorities; also send an incomplete ID list.",
                        "Priorities are swapped to the requested order.\n"
                        "Incomplete ID list is rejected.\n"
                        "If HTTP mapping is missing, document service-level IT coverage of the gap.",
                        "Two WAITLIST rows on the same offering; TM actor.",
                    ),
                    C(
                        "IT_CLASS_06",
                        "Verify transfer enrollment moves the learner through ClassroomOfferingService.transferEnrollment().",
                        "1. Seed an ASSIGNED enrollment.\n"
                        "2. Call POST .../transfer via MockMvc with target offering id.\n"
                        "3. Service updates source/target enrollment records.\n"
                        "4. Query classroom_enrollments for both offerings.",
                        "Target enrollment reflects the learner.\n"
                        "Source enrollment is closed/transferred per rules.\n"
                        "No duplicate active assignments remain.",
                        "ASSIGNED enrollment and eligible target offering.",
                    ),
                    C(
                        "IT_CLASS_07",
                        "Verify assign/replace teacher writes classroom_teacher_assignments.",
                        "1. Call POST teachers/{teacherId}/assign or replace via MockMvc as TM.\n"
                        "2. ClassroomOfferingService updates classroom_teacher_assignments.\n"
                        "3. Query assignment rows for the offering.",
                        "Teacher is linked as expected.\n"
                        "Replace removes/supersedes the old assignment cleanly.",
                        "TM JWT; teacher user exists.",
                    ),
                    C(
                        "IT_CLASS_08",
                        "Verify LEARNER cannot call TM enrollment management endpoints.",
                        "1. Authenticate as LEARNER.\n"
                        "2. Call POST .../enrollments/{id}/assign via MockMvc.\n"
                        "3. Security rejects before ClassroomOfferingService.assignToClass().",
                        "Response is 403.\n"
                        "Enrollment row is unchanged.",
                        "LEARNER JWT; target enrollment id.",
                    ),
                ]),
            ],
        },
        {
            "code": "LEARNERCLS",
            "sheet": "IT - LearnerCls",
            "name": "Learner Classroom",
        "function": "StudentClassroomController",
            "requirement": "Integrate assigned learner classroom access, homework submit, materials and tuition proofs.",
            "components": "StudentClassroomController, ClassroomOfferingService, ClassroomHomeworkService, TuitionProofService, related services",
            "integrations": "LEARNER → classroom_* tables with ownership checks",
            "srs": "UC-09–UC-14",
            "groups": [
                G("Access", [
                    C(
                        "IT_LEARNERCLS_01",
                        "Verify my-classrooms lists only classes the learner can access via ClassroomOfferingService.",
                        "1. Seed ASSIGNED and non-assigned enrollments.\n"
                        "2. Call GET /api/student/classrooms/my-classrooms via MockMvc.\n"
                        "3. StudentClassroomController delegates to ClassroomOfferingService.getMyClasses().\n"
                        "4. Compare response ids with classroom_enrollments.",
                        "Only accessible/ASSIGNED classes are returned.\n"
                        "Other learners' classes are not included.",
                        "ASSIGNED LEARNER JWT.",
                    ),
                    C(
                        "IT_LEARNERCLS_02",
                        "Verify sessions listing and join endpoints enforce class ownership.",
                        "1. Call GET /{id}/sessions via MockMvc for an owned class.\n"
                        "2. Call POST /sessions/{sessionId}/join.\n"
                        "3. ClassroomOfferingService/session services authorize by enrollment.\n"
                        "4. Repeat with a foreign classroom id.",
                        "Owned class returns 200 and join succeeds under rules.\n"
                        "Foreign class is denied.\n"
                        "Session data matches classroom_sessions.",
                        "Owned session exists.",
                    ),
                    C(
                        "IT_LEARNERCLS_03",
                        "Verify homework attachment upload and submit persist through ClassroomHomeworkService.",
                        "1. Call POST /api/student/classrooms/homework/attachments via MockMvc.\n"
                        "2. HomeworkAttachmentStorageService stores the file.\n"
                        "3. Call POST /homework/{homeworkId}/submit.\n"
                        "4. ClassroomHomeworkService.submit() persists submission records.\n"
                        "5. Query submission tables.",
                        "Attachment metadata/URL is stored.\n"
                        "Submission row is created for the learner/homework.\n"
                        "Deadline/eligibility rules are enforced when coded.",
                        "Open homework; ASSIGNED learner.",
                    ),
                    C(
                        "IT_LEARNERCLS_04",
                        "Verify materials/announcements/syllabus are readable only for class members.",
                        "1. Call GET materials/announcements/syllabus via MockMvc as class member.\n"
                        "2. ClassroomContentService loads classroom_materials and related content.\n"
                        "3. Repeat as a non-member.",
                        "Member receives 200 with content.\n"
                        "Non-member receives 403/404.\n"
                        "Returned items match DB for that offering.",
                        "Seeded classroom content.",
                    ),
                    C(
                        "IT_LEARNERCLS_05",
                        "Verify tuition proof upload creates a pending proof via TuitionProofService.",
                        "1. Call POST multipart /{id}/tuition-proofs via MockMvc.\n"
                        "2. StudentClassroomController delegates to TuitionProofService.submitProof().\n"
                        "3. Service inserts classroom_tuition_payment_proofs.\n"
                        "4. TM lists pending proofs through TuitionProofService.listPendingProofs().",
                        "Proof row is PENDING.\n"
                        "File URL is stored.\n"
                        "Proof appears in TM pending list.",
                        "Eligible enrollment; storage writable.",
                    ),
                    C(
                        "IT_LEARNERCLS_06",
                        "Verify gradebook/me visibility follows publish state from ClassroomGradebookService.",
                        "1. Seed gradebook rows unpublished.\n"
                        "2. Call GET /{id}/gradebook/me as learner.\n"
                        "3. Publish via teacher gradebook publish API.\n"
                        "4. ClassroomGradebookService toggles publish flags and learner GET again.\n"
                        "5. Unpublish and re-check.",
                        "Unpublished state hides or limits learner scores per rules.\n"
                        "Published state returns scores.\n"
                        "DB publish flags match API visibility.",
                        "Gradebook rows and teacher publish capability.",
                    ),
                ]),
            ],
        },
        {
            "code": "TEACH",
            "sheet": "IT - Teacher",
            "name": "Teacher Operations",
        "function": "TeacherClassroomController",
            "requirement": "Integrate teacher homework, attendance, gradebook and change-request flows.",
            "components": "TeacherClassroomController, ClassroomHomeworkService, ClassroomAttendanceService, ClassroomGradebookService, ClassroomChangeRequestService",
            "integrations": "TEACHER → classroom_homework/attendance/gradebook/change_requests",
            "srs": "UC-22, UC-23, UC-26",
            "groups": [
                G("Teaching ops", [
                    C(
                        "IT_TEACH_01",
                        "Verify teacher creates homework through ClassroomHomeworkService for an assigned class.",
                        "1. Authenticate a TEACHER assigned to the offering.\n"
                        "2. Call POST /api/teacher/classrooms/{id}/homework via MockMvc.\n"
                        "3. TeacherClassroomController delegates to ClassroomHomeworkService.create().\n"
                        "4. Query classroom_homework and optionally learner homework list.",
                        "Homework row is inserted with offering FK.\n"
                        "Assigned learner can see it when allowed.\n"
                        "Non-assigned teacher is denied.",
                        "TEACHER assignment exists.",
                    ),
                    C(
                        "IT_TEACH_02",
                        "Verify grading a submission updates scores through ClassroomHomeworkService.grade().",
                        "1. Seed homework and a learner submission.\n"
                        "2. Call POST .../homework/{homeworkId}/students/{studentId}/grade via MockMvc.\n"
                        "3. ClassroomHomeworkService persists grade fields.\n"
                        "4. Query submission/grade repositories.",
                        "Score/feedback are saved.\n"
                        "Teacher not owning the class is rejected.\n"
                        "Learner academic views reflect the grade when publish rules allow.",
                        "Submitted homework exists.",
                    ),
                    C(
                        "IT_TEACH_03",
                        "Verify attendance save upserts records through ClassroomAttendanceService.",
                        "1. Call GET session attendance then POST /api/teacher/classrooms/attendance via MockMvc.\n"
                        "2. ClassroomAttendanceService.saveBulk() writes attendance tables.\n"
                        "3. Learner calls GET attendance/me.\n"
                        "4. Confirm learner sees only own rows.",
                        "Attendance records are upserted.\n"
                        "Learner endpoint returns only own attendance.\n"
                        "Non-teacher is forbidden.",
                        "Session and enrolled students exist.",
                    ),
                    C(
                        "IT_TEACH_04",
                        "Verify gradebook publish/unpublish toggles learner visibility via ClassroomGradebookService.",
                        "1. Call PUT gradebook then POST publish via MockMvc.\n"
                        "2. ClassroomGradebookService updates publish flags.\n"
                        "3. Learner GET gradebook/me.\n"
                        "4. POST unpublish and re-check learner GET.",
                        "Publish makes scores visible to learner.\n"
                        "Unpublish hides/restricts per service rules.\n"
                        "Flags in DB match API outcomes.",
                        "TEACHER JWT; gradebook rows exist.",
                    ),
                    C(
                        "IT_TEACH_05",
                        "Verify teacher change-request create persists and can notify TM through notification services.",
                        "1. Call POST /api/teacher/classrooms/requests via MockMvc.\n"
                        "2. ClassroomChangeRequestService saves classroom_change_requests.\n"
                        "3. Notification path may call AppNotificationService for TM users.\n"
                        "4. Query change request and optional app_notifications.",
                        "PENDING change request is stored.\n"
                        "Notification creation respects notification preferences.\n"
                        "Invalid request payloads return 400.",
                        "TEACHER assigned to offering.",
                    ),
                    C(
                        "IT_TEACH_06",
                        "Verify LEARNER cannot create teacher homework.",
                        "1. Authenticate as LEARNER.\n"
                        "2. Call POST /api/teacher/classrooms/{id}/homework via MockMvc.\n"
                        "3. Security rejects before ClassroomHomeworkService.create().",
                        "Response is 403.\n"
                        "No classroom_homework insert occurs.",
                        "LEARNER JWT.",
                    ),
                ]),
            ],
        },
        {
            "code": "QUIZ",
            "sheet": "IT - Quiz",
            "name": "Classroom Quiz",
        "function": "ClassroomQuizController",
            "requirement": "Integrate teacher quiz lifecycle and learner submit through ClassroomQuizService.",
            "components": "ClassroomQuizController, ClassroomQuizService, classroom_quizzes/questions/attempts",
            "integrations": "TEACHER manage + LEARNER submit → quiz tables",
            "srs": "UC-15, UC-27",
            "groups": [
                G("Quiz lifecycle", [
                    C(
                        "IT_QUIZ_01",
                        "Verify teacher creates quiz with questions through ClassroomQuizService.",
                        "1. Call POST /api/teacher/classrooms/{offeringId}/quizzes via MockMvc.\n"
                        "2. ClassroomQuizController delegates to ClassroomQuizService.create().\n"
                        "3. Service persists classroom_quizzes and classroom_quiz_questions.\n"
                        "4. Query both tables.",
                        "Quiz header and questions are saved.\n"
                        "Offering FK is correct.",
                        "TEACHER assigned to class.",
                    ),
                    C(
                        "IT_QUIZ_02",
                        "Verify open/close toggles attempt eligibility in ClassroomQuizService.",
                        "1. PATCH open quiz via MockMvc.\n"
                        "2. Learner submit should succeed.\n"
                        "3. PATCH close quiz.\n"
                        "4. Learner submit should be rejected by service status checks.",
                        "OPEN allows attempts.\n"
                        "CLOSED rejects new attempts.\n"
                        "Status in DB matches API.",
                        "Quiz exists; ASSIGNED learner.",
                    ),
                    C(
                        "IT_QUIZ_03",
                        "Verify learner quiz submit stores attempt score through ClassroomQuizService.submit().",
                        "1. Call POST /api/student/quizzes/{quizId}/submit via MockMvc.\n"
                        "2. ClassroomQuizService scores and saves attempt repository rows.\n"
                        "3. Query attempts table.\n"
                        "4. Repeat as non-member learner.",
                        "Attempt row is created with score per rules.\n"
                        "Non-member is denied.\n"
                        "No duplicate illegal attempts beyond service rules.",
                        "OPEN quiz; ASSIGNED learner.",
                    ),
                    C(
                        "IT_QUIZ_04",
                        "Verify delete quiz removes or archives quiz data consistently.",
                        "1. Call DELETE /api/teacher/quizzes/{quizId} via MockMvc.\n"
                        "2. ClassroomQuizService deletes/archives quiz and related questions per implementation.\n"
                        "3. Query quiz/questions/attempts policy outcome.",
                        "Quiz is no longer active in teacher list.\n"
                        "Child questions are cascaded or archived without orphans if cascade is configured.",
                        "TEACHER owner of quiz.",
                    ),
                ]),
            ],
        },
        {
            "code": "ASSESS",
            "sheet": "IT - Assess",
            "name": "Assessment & Placement",
        "function": "PlacementTestController",
            "requirement": "Integrate placement, course assessment and mock-test submit flows; AI clients mocked.",
            "components": "PlacementTestController, StudentAssessmentController, StudentMockTestController, PlacementTestService, AiAssessmentService, MockTestService",
            "integrations": "LEARNER → assessment tables; Gemini/OpenAI mocked",
            "srs": "UC-16–UC-20",
            "groups": [
                G("Placement", [
                    C(
                        "IT_ASSESS_01",
                        "Verify current placement GET/submit persists attempts through PlacementTestService.",
                        "1. Call GET /api/student/placement-tests/current via MockMvc.\n"
                        "2. PlacementTestController delegates to PlacementTestService.getTest().\n"
                        "3. Call POST /current/submit with answers.\n"
                        "4. PlacementTestService.submit() writes placement_test_attempts.\n"
                        "5. Query attempts table.",
                        "GET returns definition (lazy-seed allowed).\n"
                        "Submit creates an attempt with result fields.\n"
                        "Unauthenticated calls are rejected.",
                        "LEARNER JWT.",
                    ),
                    C(
                        "IT_ASSESS_02",
                        "Verify invalid placement submit fails validation without inserting an attempt.",
                        "1. Call POST /current/submit via MockMvc with empty/invalid body.\n"
                        "2. Validation/service rejects before PlacementTestAttempt persistence.\n"
                        "3. Count placement_test_attempts for the user.",
                        "Response is 400.\n"
                        "Attempt count is unchanged.",
                        "LEARNER JWT.",
                    ),
                    C(
                        "IT_ASSESS_03",
                        "Verify CM placement GET/PUT updates placement_test_definitions.",
                        "1. Authenticate as CONTENT_MANAGER.\n"
                        "2. Call GET/PUT /api/content-manager/placement-test via MockMvc.\n"
                        "3. Placement definition service updates placement_test_definitions.\n"
                        "4. Repeat PUT as LEARNER.",
                        "CM update persists definition fields.\n"
                        "LEARNER receives 403 on CM path.",
                        "CONTENT_MANAGER JWT.",
                    ),
                ]),
                G("Course & mock", [
                    C(
                        "IT_ASSESS_04",
                        "Verify course assessment submit stores assessment_submissions through AiAssessmentService.",
                        "1. Seed course assessment and enrolled learner.\n"
                        "2. Call POST /api/student/assessments/{assessmentId}/submit via MockMvc.\n"
                        "3. StudentAssessmentController delegates to AiAssessmentService.submitAssessment().\n"
                        "4. Mock AI client if speaking/writing path is used.\n"
                        "5. Query assessment_submissions.",
                        "Submission row is created with status/score per mode.\n"
                        "Non-enrolled learner is denied.\n"
                        "AI mock failures are asserted according to actual transactional behavior.",
                        "Enrolled LEARNER; assessment configured; AI mocked.",
                    ),
                    C(
                        "IT_ASSESS_05",
                        "Verify mock-test list/submit persists mock_test_attempts through MockTestService.",
                        "1. Call GET /api/student/mock-tests via MockMvc.\n"
                        "2. Call POST /{id}/submit.\n"
                        "3. StudentMockTestController delegates to MockTestService.submitMockTest().\n"
                        "4. Query mock_test_attempts.",
                        "Published mock tests are listed.\n"
                        "Submit inserts an attempt row.",
                        "LEARNER JWT; published mock test exists.",
                    ),
                    C(
                        "IT_ASSESS_06",
                        "Verify TEACHER cannot call student placement submit endpoint.",
                        "1. Authenticate as TEACHER.\n"
                        "2. Call POST /api/student/placement-tests/current/submit via MockMvc.\n"
                        "3. Security rejects before PlacementTestService.submit().",
                        "Response is 403.\n"
                        "No placement attempt is created for the teacher user.",
                        "TEACHER JWT.",
                    ),
                ]),
            ],
        },
        {
            "code": "SUPPORT",
            "sheet": "IT - Support",
            "name": "Support Tickets",
        "function": "StudentSupportTicketController",
            "requirement": "Integrate learner ticket create/reply and staff claim/update via SupportTicketService.",
            "components": "StudentSupportTicketController, ManagerSupportTicketController, SupportTicketService",
            "integrations": "role-separated REST → support_tickets/messages",
            "srs": "UC-07, UC-44",
            "groups": [
                G("Tickets", [
                    C(
                        "IT_SUPPORT_01",
                        "Verify learner creates a support ticket and initial message through SupportTicketService.",
                        "1. Call POST /api/student/support-tickets via MockMvc as LEARNER.\n"
                        "2. StudentSupportTicketController delegates to SupportTicketService.create().\n"
                        "3. Service inserts support_tickets and support_ticket_messages.\n"
                        "4. Call GET listMine and query both tables.",
                        "Response is 200 OK.\n"
                        "Ticket is owned by the learner.\n"
                        "Initial message row exists.",
                        "LEARNER JWT.",
                    ),
                    C(
                        "IT_SUPPORT_02",
                        "Verify learner cannot read another learner's ticket by id.",
                        "1. Seed a ticket for learner A.\n"
                        "2. Authenticate as learner B and call GET /api/student/support-tickets/{ticketId}.\n"
                        "3. SupportTicketService ownership check denies access.",
                        "Response is 403/404.\n"
                        "No message content is leaked.",
                        "Two LEARNER accounts.",
                    ),
                    C(
                        "IT_SUPPORT_03",
                        "Verify staff/manager claim and reply append staff messages through SupportTicketService.",
                        "1. Seed an OPEN ticket.\n"
                        "2. Call POST claim then POST reply via MockMvc as MANAGER/STAFF.\n"
                        "3. ManagerSupportTicketController delegates to SupportTicketService.claim()/reply.\n"
                        "4. Query assignee and support_ticket_messages.\n"
                        "5. Learner GET detail.",
                        "Assignee is set.\n"
                        "Staff message is appended.\n"
                        "Learner can see the staff reply.",
                        "MANAGER or STAFF JWT.",
                    ),
                    C(
                        "IT_SUPPORT_04",
                        "Verify empty ticket create fails validation without DB insert.",
                        "1. Call POST /api/student/support-tickets via MockMvc with missing subject/body.\n"
                        "2. Validation fails before SupportTicketService.create().\n"
                        "3. Count support_tickets.",
                        "Response is 400.\n"
                        "Ticket count is unchanged.",
                        "LEARNER JWT.",
                    ),
                ]),
            ],
        },
        {
            "code": "ADMIN",
            "sheet": "IT - Admin",
            "name": "Administration",
        "function": "AdminUserController",
            "requirement": "Integrate admin user/role/status/audit flows through AdminUserService.",
            "components": "AdminUserController, AdminSystemController, AdminAuditLogController, AdminUserService, AuditLogService",
            "integrations": "ADMIN-only → users/user_roles/system_audit_logs",
            "srs": "UC-42",
            "groups": [
                G("Admin users", [
                    C(
                        "IT_ADMIN_01",
                        "Verify admin creates a user with roles and writes an audit log.",
                        "1. Authenticate as ADMIN.\n"
                        "2. Call POST /api/admin/users via MockMvc.\n"
                        "3. AdminUserController delegates to AdminUserService.createUser().\n"
                        "4. Service writes users, user_roles and AuditLogService → system_audit_logs.\n"
                        "5. Repeat with LEARNER token.",
                        "User and roles are created.\n"
                        "Audit log entry exists.\n"
                        "Non-admin receives 403.",
                        "ADMIN JWT.",
                    ),
                    C(
                        "IT_ADMIN_02",
                        "Verify patch roles/status updates associations without orphan role rows.",
                        "1. Call PATCH /api/admin/users/{id}/roles via MockMvc.\n"
                        "2. Call PATCH /api/admin/users/{id}/status.\n"
                        "3. AdminUserService.updateRoles()/updateStatus() update repositories.\n"
                        "4. Query users and user_roles.",
                        "Role set matches the request with no orphans.\n"
                        "Status change is persisted.\n"
                        "Audit entries are recorded.",
                        "Target user exists; ADMIN JWT.",
                    ),
                    C(
                        "IT_ADMIN_03",
                        "Verify audit-logs listing is restricted to ADMIN.",
                        "1. Call GET /api/admin/audit-logs via MockMvc as ADMIN.\n"
                        "2. AdminAuditLogController reads through AuditLogService/repository.\n"
                        "3. Repeat as LEARNER.",
                        "ADMIN receives 200 with log rows.\n"
                        "LEARNER receives 403.",
                        "Audit rows exist.",
                    ),
                    C(
                        "IT_ADMIN_04",
                        "Verify system config GET is ADMIN only.",
                        "1. Call GET /api/admin/system/config via MockMvc as ADMIN.\n"
                        "2. AdminSystemController returns config payload.\n"
                        "3. Repeat as non-admin roles.",
                        "ADMIN receives 200.\n"
                        "Other roles receive 403.",
                        "ADMIN JWT.",
                    ),
                ]),
            ],
        },
        {
            "code": "LARK",
            "sheet": "IT - Lark",
            "name": "Lark Meetings",
        "function": "LarkWebhookController",
            "requirement": "Integrate Lark webhook handling and staff sync-lark through Lark services.",
            "components": "LarkWebhookController, LarkWebhookService, LarkMeetingService, classroom_sessions",
            "integrations": "Webhook permitAll; outbound Lark HTTP mocked",
            "srs": "UC-10",
            "groups": [
                G("Webhook & sync", [
                    C(
                        "IT_LARK_01",
                        "Verify Lark URL challenge is echoed by LarkWebhookService without mutating sessions.",
                        "1. Call POST /api/lark/events via MockMvc with a challenge payload.\n"
                        "2. LarkWebhookController calls LarkWebhookService.verifyChallenge().\n"
                        "3. Confirm no classroom_sessions row changed.",
                        "Challenge value is echoed in the response.\n"
                        "No session mutation occurs.",
                        "None.",
                    ),
                    C(
                        "IT_LARK_02",
                        "Verify meeting started/ended events update classroom_sessions through LarkWebhookService.handle().",
                        "1. Seed a classroom_sessions row mapped to Lark ids.\n"
                        "2. Call POST /api/lark/events with meeting started/ended payload via MockMvc.\n"
                        "3. LarkWebhookService.handle() updates session status/timestamps via session repository.\n"
                        "4. Query classroom_sessions.",
                        "Session fields are updated according to the event type.\n"
                        "Invalid events are rejected safely without corrupt state.",
                        "Mapped session exists; Lark test mode enabled.",
                    ),
                    C(
                        "IT_LARK_03",
                        "Verify staff sync-lark persists sync status through LarkMeetingService.",
                        "1. Authenticate as STAFF.\n"
                        "2. Call POST .../sessions/{id}/sync-lark via MockMvc.\n"
                        "3. Controller delegates to LarkMeetingService with mocked Lark HTTP.\n"
                        "4. Query lark_sync_status/error fields.\n"
                        "5. Repeat as LEARNER.",
                        "Sync status/error fields are updated from the mocked response.\n"
                        "LEARNER receives 403.",
                        "STAFF JWT; Lark client mocked.",
                    ),
                ]),
            ],
        },
        {
            "code": "INFRA",
            "sheet": "IT - Infra",
            "name": "Infrastructure",
        "function": "TrainingManagerInfrastructureController",
            "requirement": "Integrate campuses/rooms/templates and generate-sessions via ClassroomInfrastructureService.",
            "components": "TrainingManagerInfrastructureController, ClassroomInfrastructureService",
            "integrations": "STAFF/TM → campuses/rooms/session_templates/sessions",
            "srs": "Scheduling infrastructure",
            "groups": [
                G("Infra CRUD", [
                    C(
                        "IT_INFRA_01",
                        "Verify create campus/room persists through ClassroomInfrastructureService.",
                        "1. Call POST campuses and rooms via MockMvc as TM/STAFF.\n"
                        "2. TrainingManagerInfrastructureController delegates to ClassroomInfrastructureService.\n"
                        "3. Query classroom_campuses and classroom_rooms.\n"
                        "4. Repeat as LEARNER.",
                        "Campus/room rows are inserted.\n"
                        "LEARNER receives 403.",
                        "TM/STAFF JWT.",
                    ),
                    C(
                        "IT_INFRA_02",
                        "Verify session-template and generate-sessions create classroom_sessions.",
                        "1. Call POST session-templates via MockMvc.\n"
                        "2. Call POST generate-sessions for an offering.\n"
                        "3. ClassroomInfrastructureService generates classroom_sessions from the template.\n"
                        "4. Count sessions for the offering.",
                        "Template is saved.\n"
                        "Sessions are generated according to template rules.\n"
                        "Duplicate generation is handled per service policy.",
                        "Offering and template prerequisites exist.",
                    ),
                    C(
                        "IT_INFRA_03",
                        "Verify invalid room update is rejected without changing the database row.",
                        "1. Call PUT room with invalid capacity/constraints via MockMvc.\n"
                        "2. Validation/service rejects before repository save.\n"
                        "3. Reload classroom_rooms.",
                        "Response is 400.\n"
                        "Room row is unchanged.",
                        "Existing room.",
                    ),
                ]),
            ],
        },
        {
            "code": "REPORT",
            "sheet": "IT - Report",
            "name": "Reports & Revenue",
        "function": "TrainingManagerDashboardController",
            "requirement": "Integrate TM/staff dashboard and CM revenue analytics endpoints.",
            "components": "TrainingManagerDashboardController, ContentManagerRevenueController, TrainingManagerOpsService, PaymentService",
            "integrations": "Role-gated aggregate queries over enrollments/payments",
            "srs": "UC-40, UC-41",
            "groups": [
                G("Dashboards", [
                    C(
                        "IT_REPORT_01",
                        "Verify STAFF/TM dashboard aggregates through TrainingManagerOpsService.getDashboard().",
                        "1. Seed operational classroom/enrollment data.\n"
                        "2. Call GET /api/staff/dashboard (or TM alias) via MockMvc.\n"
                        "3. TrainingManagerDashboardController delegates to TrainingManagerOpsService.getDashboard().\n"
                        "4. Compare key counts with seeded DB totals.\n"
                        "5. Repeat as LEARNER.",
                        "Response is 200 with coherent metrics.\n"
                        "LEARNER receives 403.",
                        "Seeded ops data; STAFF/TM JWT.",
                    ),
                    C(
                        "IT_REPORT_02",
                        "Verify CM revenue analytics through PaymentService.getRevenueAnalytics().",
                        "1. Seed paid payment_orders.\n"
                        "2. Call GET /api/content-manager/revenue/analytics via MockMvc as CONTENT_MANAGER.\n"
                        "3. ContentManagerRevenueController delegates to PaymentService.getRevenueAnalytics().\n"
                        "4. Repeat as LEARNER.",
                        "CM receives 200 analytics payload.\n"
                        "LEARNER receives 403.\n"
                        "Totals are consistent with seeded paid orders.",
                        "Paid orders seeded; CM JWT.",
                    ),
                ]),
            ],
        },
        {
            "code": "PROPOSAL",
            "sheet": "IT - Proposal",
            "name": "Classroom Proposals",
        "function": "ManagerClassroomController",
            "requirement": "Integrate staff proposal submit and manager approve/reject via ClassroomProposalService.",
            "components": "StaffClassroomProposalController, ManagerClassroomProposalController, ClassroomProposalService",
            "integrations": "STAFF↔MANAGER → classroom_proposals",
            "srs": "Classroom proposals",
            "groups": [
                G("Proposal flow", [
                    C(
                        "IT_PROPOSAL_01",
                        "Verify staff creates and submits a classroom proposal through ClassroomProposalService.",
                        "1. Call POST /api/staff/classroom-proposals via MockMvc as STAFF.\n"
                        "2. Call PATCH submit.\n"
                        "3. ClassroomProposalService.create()/submit() persist classroom_proposals.\n"
                        "4. Query proposal status.",
                        "Proposal is persisted.\n"
                        "Status becomes SUBMITTED/PENDING per service rules.",
                        "STAFF JWT.",
                    ),
                    C(
                        "IT_PROPOSAL_02",
                        "Verify manager approve/reject updates proposal status and side effects.",
                        "1. Seed a pending proposal.\n"
                        "2. Call PATCH approve or reject via MockMvc as MANAGER.\n"
                        "3. ClassroomProposalService.approve()/reject() updates classroom_proposals.\n"
                        "4. If approve creates an offering, query classroom_offerings.\n"
                        "5. Attempt approve as STAFF.",
                        "Status is updated for manager.\n"
                        "Approve side effects occur only when implemented.\n"
                        "STAFF cannot approve.",
                        "Pending proposal exists.",
                    ),
                    C(
                        "IT_PROPOSAL_03",
                        "Verify LEARNER cannot access proposal APIs.",
                        "1. Authenticate as LEARNER.\n"
                        "2. Call GET/POST proposal endpoints via MockMvc.\n"
                        "3. Security rejects before ClassroomProposalService.",
                        "Response is 403.\n"
                        "No proposal row is created.",
                        "LEARNER JWT.",
                    ),
                ]),
            ],
        },
        {
            "code": "DISPUTE",
            "sheet": "IT - Dispute",
            "name": "Attendance Disputes",
        "function": "ClassroomAttendanceDisputeController",
            "requirement": "Integrate learner dispute create and teacher review via ClassroomAttendanceDisputeService.",
            "components": "ClassroomAttendanceDisputeController, ClassroomAttendanceDisputeService",
            "integrations": "LEARNER↔TEACHER → classroom_attendance_disputes",
            "srs": "Attendance disputes",
            "groups": [
                G("Disputes", [
                    C(
                        "IT_DISPUTE_01",
                        "Verify learner creates an attendance dispute through ClassroomAttendanceDisputeService.",
                        "1. Seed an attendance row for the learner.\n"
                        "2. Call POST /api/student/attendance/{attendanceId}/disputes via MockMvc.\n"
                        "3. Controller delegates to ClassroomAttendanceDisputeService.create().\n"
                        "4. Query classroom_attendance_disputes and learner dispute list.",
                        "Dispute row is created with PENDING status.\n"
                        "Owner is the authenticated learner.",
                        "Attendance row exists.",
                    ),
                    C(
                        "IT_DISPUTE_02",
                        "Verify teacher review resolves dispute and updates attendance consistently.",
                        "1. Seed a pending dispute.\n"
                        "2. Call teacher review endpoint via MockMvc.\n"
                        "3. ClassroomAttendanceDisputeService.review() updates dispute and attendance repositories.\n"
                        "4. Query both tables.",
                        "Dispute is resolved.\n"
                        "Attendance reflects the review decision.\n"
                        "Invalid review payloads are rejected.",
                        "Pending dispute; TEACHER JWT.",
                    ),
                    C(
                        "IT_DISPUTE_03",
                        "Verify learner cannot call teacher dispute review endpoints.",
                        "1. Authenticate as LEARNER.\n"
                        "2. Call teacher review API via MockMvc.\n"
                        "3. Security rejects before ClassroomAttendanceDisputeService.review().",
                        "Response is 403.\n"
                        "Dispute remains PENDING.",
                        "LEARNER JWT; pending dispute id.",
                    ),
                ]),
            ],
        },
        {
            "code": "NOTES",
            "sheet": "IT - Notes",
            "name": "Learning Notes",
        "function": "StudentClassroomController",
            "requirement": "Integrate learner lesson notes CRUD through LearnerLearningExperienceService.",
            "components": "StudentLearningExperienceController, LearnerLearningExperienceService, learner_lesson_notes",
            "integrations": "LEARNER → learner_lesson_notes with enrollment checks",
            "srs": "UC-51",
            "groups": [
                G("Notes", [
                    C(
                        "IT_NOTES_01",
                        "Verify create/update/delete lesson note persists through LearnerLearningExperienceService.",
                        "1. Authenticate an enrolled LEARNER.\n"
                        "2. Call create/update/delete note APIs via MockMvc.\n"
                        "3. StudentLearningExperienceController delegates to LearnerLearningExperienceService note methods.\n"
                        "4. Query learner_lesson_notes after each step.\n"
                        "5. Attempt access as another learner.",
                        "CRUD reflects database state.\n"
                        "Other learner cannot access the note.\n"
                        "Deleted note is removed or hidden per service rules.",
                        "Enrolled LEARNER JWT.",
                    ),
                    C(
                        "IT_NOTES_02",
                        "Verify note APIs reject unauthenticated or non-enrolled callers before persistence.",
                        "1. Call create note via MockMvc without JWT.\n"
                        "2. Call create note as LEARNER without enrollment.\n"
                        "3. Confirm LearnerLearningExperienceService authorization denies insert.\n"
                        "4. Count learner_lesson_notes.",
                        "Unauthenticated call returns 401/403.\n"
                        "Non-enrolled call is denied.\n"
                        "No note row is inserted.",
                        "Published course without enrollment for the caller.",
                    ),
                ]),
            ],
        },
    ]
    MODULES.extend(more)


_extend_remaining()


def count_cases():
    n = 0
    for m in MODULES:
        for g in m["groups"]:
            n += len(g["cases"])
    return n


if __name__ == "__main__":
    print(len(MODULES), "modules", count_cases(), "cases")
