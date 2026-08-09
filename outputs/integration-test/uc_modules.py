# -*- coding: utf-8 -*-
"""
UC-driven IT modules — sample style (Controller, Service, Repository):
- Test Case ID: IT_ENROLL_01 (short)
- Group header: exact SRS Use Case title (no "MODULE N -" prefix)
- Procedure: Call API via MockMvc; Controller delegates to Service; Service uses Repository/DB
- Expected: HTTP status + JSON/DB checks
"""

from __future__ import annotations


def C(id_, desc, proc, exp, pre, status="Pending"):
    return {"id": id_, "desc": desc, "proc": proc, "exp": exp, "pre": pre, "status": status}


def M(num: int, uc_title: str, cases: list):
    """uc_title must be exact SRS Use Case name. Group band = title only (not MODULE N)."""
    return {
        "name": uc_title,
        "scope": "",
        "cases": cases,
    }


MODULES = [
    {
        "code": "COURSE",
        "sheet": "IT_COURSE",
        "function": "View public courses",
        "ucs": ["UC-02 View public courses"],
        "requirement": "IT for public course catalog browse/search/detail (Controller-Service-Repository).",
        "components": "PublicOnlineCourseController, OnlineCourseService, OnlineCourseRepository",
        "groups": [
            M(
                1,
                "View public courses",
                [
                    C(
                        "IT_COURSE_01",
                        "Verify fetching public course list via Controller.",
                        "1. Call GET /api/online-courses via MockMvc without Authorization header.\n"
                        "2. PublicOnlineCourseController receives the request and delegates to OnlineCourseService.getPublicCourses() (or listPublicCourses).\n"
                        "3. OnlineCourseService queries OnlineCourseRepository for PUBLISHED/active courses only.\n"
                        "4. Compare the returned course IDs with a direct DB query of PUBLISHED courses.",
                        "HTTP status is 200 OK.\n"
                        "JSON body is a list/page of courses.\n"
                        "Only PUBLISHED/active courses appear.\n"
                        "DRAFT or unpublished courses do not appear.",
                        "Database is up. At least one PUBLISHED course and one DRAFT course exist.",
                    ),
                    C(
                        "IT_COURSE_02",
                        "Verify fetching public course detail via Controller.",
                        "1. Pick a known PUBLISHED course slug or id from the database.\n"
                        "2. Call GET /api/online-courses/{slugOrId} via MockMvc without Authorization.\n"
                        "3. PublicOnlineCourseController delegates to OnlineCourseService.getPublicCourse().\n"
                        "4. OnlineCourseService loads the course from OnlineCourseRepository.\n"
                        "5. Call GET /api/online-courses/{unknownSlug} via MockMvc for the not-found case.",
                        "For an existing published course: HTTP 200 OK and JSON contains title, price, status.\n"
                        "Payload fields match the OnlineCourseRepository row.\n"
                        "For an unknown slug: HTTP 404 or business not-found (not 500).\n"
                        "No database write occurs (read-only).",
                        "At least one published course exists in DB.",
                    ),
                    C(
                        "IT_COURSE_03",
                        "Verify public course search/filter via Controller.",
                        "1. Seed at least two PUBLISHED courses with different titles/categories.\n"
                        "2. Call GET /api/online-courses?keyword={partOfTitle} via MockMvc without Authorization.\n"
                        "3. PublicOnlineCourseController delegates to OnlineCourseService.getPublicCourses(keyword, ...).\n"
                        "4. OnlineCourseService filters OnlineCourseRepository by keyword (and optionally category).\n"
                        "5. Optionally call GET /api/online-courses?category={categoryCode} and compare results.",
                        "HTTP status is 200 OK.\n"
                        "Returned courses match the keyword/category filter.\n"
                        "Courses that do not match are excluded.\n"
                        "Still only PUBLISHED courses appear.",
                        "At least two published courses with distinguishable titles/categories exist.",
                    ),
                    C(
                        "IT_COURSE_04",
                        "Verify public catalog empty search result via Controller.",
                        "1. Call GET /api/online-courses?keyword=__no_such_course_xyz__ via MockMvc without Authorization.\n"
                        "2. PublicOnlineCourseController delegates to OnlineCourseService.getPublicCourses().\n"
                        "3. OnlineCourseService queries OnlineCourseRepository; no row matches.\n"
                        "4. Confirm response is empty list/page (SRS alt flow: no results found), not HTTP 500.",
                        "HTTP status is 200 OK.\n"
                        "Content/list is empty (or page with totalElements=0).\n"
                        "No HTTP 5xx.",
                        "None.",
                    ),
                    C(
                        "IT_COURSE_05",
                        "Verify listing public course categories via Controller.",
                        "1. Call GET /api/online-courses/categories via MockMvc without Authorization.\n"
                        "2. PublicOnlineCourseController delegates to CourseCategoryManagementService.getActiveCategories().\n"
                        "3. Service reads active categories from the category repository.\n"
                        "4. Compare returned category codes/names with active DB rows.",
                        "HTTP status is 200 OK.\n"
                        "Only active categories are returned.\n"
                        "Read-only: no DB write.",
                        "At least one active course category exists.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "ACCESS",
        "sheet": "IT_ACCESS",
        "function": "Access Online Learning Materials",
        "ucs": ["UC-48 Access Online Learning Materials"],
        "requirement": "IT for enrolled learner course content and progress (Controller-Service-Repository).",
        "components": "StudentOnlineCourseController, OnlineCourseService, OnlineCourseRepository",
        "groups": [
            M(
                1,
                "Access Online Learning Materials",
                [
                    C(
                        "IT_ACCESS_01",
                        "Verify enrolled learner loads course content via Controller.",
                        "1. Login as an enrolled LEARNER and obtain a JWT accessToken.\n"
                        "2. Call GET /api/student/online-courses/{courseId}/content via MockMvc with Authorization Bearer token.\n"
                        "3. StudentOnlineCourseController delegates to OnlineCourseService.getContentForLearner() (or equivalent).\n"
                        "4. OnlineCourseService checks ownership then loads modules/lessons from related repositories.\n"
                        "5. Repeat the same GET without Authorization, then again as a LEARNER who is not enrolled.",
                        "Enrolled authenticated call: HTTP 200 OK with modules/lessons.\n"
                        "Call without token: HTTP 401 or 403.\n"
                        "Non-enrolled learner: HTTP 403 or 404 and no protected lesson body.",
                        "LEARNER is enrolled in a course that has lessons.",
                    ),
                    C(
                        "IT_ACCESS_02",
                        "Verify lesson progress persists via Controller.",
                        "1. Login as an enrolled LEARNER and obtain a JWT.\n"
                        "2. Call PATCH /api/student/online-courses/{courseId}/lessons/{lessonId}/progress via MockMvc with a JSON progress body and Bearer token.\n"
                        "3. StudentOnlineCourseController delegates to OnlineCourseService.saveLessonProgress().\n"
                        "4. OnlineCourseService upserts the progress row through the progress repository.\n"
                        "5. Reload progress (or query DB) and retry the same PATCH as a non-enrolled LEARNER.",
                        "Enrolled call: HTTP 2xx and progress row is persisted for (student, course, lesson).\n"
                        "Reloaded values match the request body.\n"
                        "Non-enrolled call: HTTP 403 or 404 and no progress insert.",
                        "Enrolled LEARNER and a valid lessonId exist.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "ASSIGN",
        "sheet": "IT_ASSIGN",
        "function": "Assign Learner to Classroom",
        "ucs": ["UC-38 Assign Learner to Classroom"],
        "requirement": "IT for staff enrollment request queue and class assignment (Controller-Service-Repository).",
        "components": "StaffEnrollmentRequestController, EnrollmentRequestService, EnrollmentRequestRepository",
        "groups": [
            M(
                1,
                "Assign Learner to Classroom",
                [
                    C(
                        "IT_ASSIGN_01",
                        "Verify staff lists enrollment requests via Controller.",
                        "1. Login as STAFF and obtain a JWT accessToken.\n2. Call GET /api/staff/enrollment-requests via MockMvc with Authorization Bearer token.\n3. StaffEnrollmentRequestController receives the request and delegates to EnrollmentRequestService.listForStaff().\n4. EnrollmentRequestService reads rows from EnrollmentRequestRepository.\n5. Repeat the same GET as LEARNER (must be denied).",
                        "STAFF call: HTTP 200 OK and JSON list with id, status, learner reference.\nLEARNER call: HTTP 403 Forbidden.",
                        "STAFF role is seeded. At least one enrollment_requests row exists.",
                    ),
                    C(
                        "IT_ASSIGN_02",
                        "Verify staff filters enrollment requests by status via Controller.",
                        "1. Seed enrollment requests with different statuses (for example SUBMITTED and WAITING_FOR_CLASS).\n2. Login as STAFF and obtain a JWT.\n3. Call GET /api/staff/enrollment-requests?status=WAITING_FOR_CLASS via MockMvc.\n4. StaffEnrollmentRequestController delegates to EnrollmentRequestService with the status filter.\n5. EnrollmentRequestService queries EnrollmentRequestRepository by status.",
                        "HTTP status is 200 OK.\nEvery returned row has status WAITING_FOR_CLASS.",
                        "Requests with multiple statuses exist. STAFF JWT is available.",
                    ),
                    C(
                        "IT_ASSIGN_03",
                        "Verify staff assigns learner to classroom via Controller.",
                        "1. Seed a WAITING_FOR_CLASS enrollment request and an approved classroom with free seats.\n2. Login as STAFF and obtain a JWT.\n3. Call PATCH /api/staff/enrollment-requests/{requestId}/assign-class via MockMvc with body containing classroomId.\n4. StaffEnrollmentRequestController delegates to EnrollmentRequestService.assignToClassroom().\n5. EnrollmentRequestService updates EnrollmentRequestRepository and creates a classroom enrollment through ClassroomOfferingService / enrollment repository.",
                        "HTTP status is 200 or 201.\nRequest status becomes CLASS_ASSIGNED (or equivalent).\nExactly one classroom enrollment row exists for that learner and class.\nIf the classroom is full: HTTP 4xx and no enrollment insert.",
                        "Eligible WAITING_FOR_CLASS request and classroom with free seats exist.",
                    ),
                    C(
                        "IT_ASSIGN_04",
                        "Verify staff rejects enrollment request via Controller.",
                        "1. Seed an open enrollment request.\n2. Login as STAFF and obtain a JWT.\n3. Call PATCH /api/staff/enrollment-requests/{requestId}/reject via MockMvc with a non-empty reason body.\n4. StaffEnrollmentRequestController delegates to EnrollmentRequestService.reject().\n5. EnrollmentRequestService updates status and reason in EnrollmentRequestRepository, then attempt assign on the same request.",
                        "Reject call: HTTP 200 OK, status REJECTED, reason stored.\nLater assign call: HTTP 4xx and no classroom enrollment created.",
                        "An open enrollment request exists. STAFF JWT is available.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "ENROLL",
        "sheet": "IT_ENROLL",
        "function": "Enroll in Course",
        "ucs": ["UC-08 Enroll in Course"],
        "requirement": "IT for learner enrollment request and my-enrollments (Controller-Service-Repository).",
        "components": "StudentEnrollmentRequestController, StudentOnlineCourseController, EnrollmentRequestService, OnlineCourseService, EnrollmentRequestRepository",
        "groups": [
            M(
                1,
                "Enroll in Course",
                [
                    C(
                        "IT_ENROLL_01",
                        "Verify learner submits enrollment request via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call POST /api/student/course-enrollment-requests with required JSON fields via MockMvc.\n"
                        "3. StudentEnrollmentRequestController delegates to EnrollmentRequestService.create().\n"
                        "4. EnrollmentRequestService saves a new row through EnrollmentRequestRepository owned by the current user.\n"
                        "5. Retry with missing required fields, then retry without Authorization.",
                        "Valid POST: HTTP 200 or 201 and one new enrollment_requests row with default SUBMITTED status.\n"
                        "Invalid payload: HTTP 400 and no insert.\n"
                        "No Authorization: HTTP 401 or 403.",
                        "LEARNER is logged in. A valid courseOfferingId exists.",
                    ),
                    C(
                        "IT_ENROLL_02",
                        "Verify my-enrollments list via Controller.",
                        "1. Seed enrollments for learner A and learner B on different courses.\n"
                        "2. Login as learner A and call GET /api/student/online-courses/my-enrollments via MockMvc with Bearer token.\n"
                        "3. StudentOnlineCourseController delegates to OnlineCourseService.listMyEnrollments().\n"
                        "4. OnlineCourseService queries enrollment data scoped to the current user id.",
                        "HTTP status is 200 OK.\n"
                        "Only learner A courses are returned.\n"
                        "Learner B courses do not appear (no cross-user leak).",
                        "Two LEARNER accounts with different enrollments exist.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "WISHLIST",
        "sheet": "IT_WISHLIST",
        "function": "Wishlist Courses",
        "ucs": ["UC-45 Wishlist Courses"],
        "requirement": "IT for wishlist persistence (Controller-Service-Repository).",
        "components": "StudentCommerceController, StudentCommerceService, WishlistItemRepository",
        "groups": [
            M(
                1,
                "Wishlist Courses",
                [
                    C(
                        "IT_WISHLIST_01",
                        "Verify adding course to wishlist via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call POST /api/student/commerce/wishlist/{courseId} via MockMvc with Bearer token.\n"
                        "3. StudentCommerceController delegates to StudentCommerceService.addToWishlist().\n"
                        "4. StudentCommerceService saves the item through WishlistItemRepository.",
                        "HTTP status is 200 or 201.\n"
                        "A wishlist row exists for the current user and course.",
                        "LEARNER is logged in. A published course exists.",
                    ),
                    C(
                        "IT_WISHLIST_02",
                        "Verify listing wishlist via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call GET /api/student/commerce/wishlist via MockMvc with Bearer token.\n"
                        "3. StudentCommerceController delegates to StudentCommerceService.listWishlist().\n"
                        "4. StudentCommerceService reads WishlistItemRepository scoped to the current user.",
                        "HTTP status is 200 OK.\n"
                        "Only the current learner wishlist items are returned.",
                        "The learner wishlist has at least one item.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "CART",
        "sheet": "IT_CART",
        "function": "Add Courses to Cart",
        "ucs": ["UC-46 Add Courses to Cart"],
        "requirement": "IT for cart persistence (Controller-Service-Repository).",
        "components": "StudentCommerceController, StudentCommerceService, CartItemRepository",
        "groups": [
            M(
                1,
                "Add Courses to Cart",
                [
                    C(
                        "IT_CART_01",
                        "Verify adding course to cart via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call POST /api/student/commerce/cart/{courseId} via MockMvc with Bearer token.\n"
                        "3. StudentCommerceController delegates to StudentCommerceService.addToCart().\n"
                        "4. StudentCommerceService saves through CartItemRepository.",
                        "HTTP status is 200 or 201.\n"
                        "A cart item row exists for the current user.",
                        "LEARNER is logged in. A published course exists.",
                    ),
                    C(
                        "IT_CART_02",
                        "Verify removing cart item via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call DELETE /api/student/commerce/cart/{courseId} via MockMvc with Bearer token.\n"
                        "3. StudentCommerceController delegates to StudentCommerceService.removeFromCart().\n"
                        "4. StudentCommerceService deletes the row through CartItemRepository.",
                        "HTTP status is 200 or 204.\n"
                        "The cart item no longer exists for that user.",
                        "The cart contains the target item.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "CHECKOUT",
        "sheet": "IT_CHECKOUT",
        "function": "Checkout",
        "ucs": ["UC-47 Checkout"],
        "requirement": "IT for checkout quote/order and webhook consistency (Controller-Service-Repository).",
        "components": "StudentPaymentController, PayosWebhookController, PaymentService, PaymentOrderRepository",
        "groups": [
            M(
                1,
                "Checkout",
                [
                    C(
                        "IT_CHECKOUT_01",
                        "Verify creating payment quote/order via Controller.",
                        "1. Login as LEARNER with a non-empty cart and obtain a JWT.\n2. Call POST /api/student/payments/quote via MockMvc with Bearer token (or POST /api/student/payments/payos/link for checkout).\n3. StudentPaymentController delegates to PaymentService.createOrder() (or equivalent).\n4. PaymentService persists a PaymentOrder through PaymentOrderRepository.",
                        "HTTP status is 200 or 201.\nA payment order row exists with PENDING status (or equivalent) and correct amount.",
                        "LEARNER cart has payable items.",
                    ),
                    C(
                        "IT_CHECKOUT_02",
                        "Verify listing my payment orders via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n2. Call GET /api/student/payments/orders via MockMvc with Bearer token.\n3. StudentPaymentController delegates to PaymentService.listMyOrders().\n4. PaymentService queries PaymentOrderRepository by current user.",
                        "HTTP status is 200 OK.\nOnly the current learner orders are returned.",
                        "At least one payment order exists for the learner.",
                    ),
                    C(
                        "IT_CHECKOUT_03",
                        "Verify checkout rejected for empty cart via Controller.",
                        "1. Login as LEARNER with an empty cart and obtain a JWT.\n2. Call POST /api/student/payments/payos/link via MockMvc with Bearer token.\n3. StudentPaymentController delegates to PaymentService; validation fails before save.\n4. Confirm PaymentOrderRepository has no new row for this call.",
                        "HTTP status is 4xx.\nNo new PaymentOrder row is created.",
                        "LEARNER has an empty cart.",
                    ),
                    C(
                        "IT_CHECKOUT_04",
                        "Verify PayOS webhook updates order status via Controller.",
                        "1. Prepare a pending payment order id in the database.\n2. Call POST /api/payos/webhook (or project PayOS webhook path) via MockMvc with sandbox or stub payload.\n3. PayosWebhookController delegates to PaymentService.handleWebhook().\n4. PaymentService updates the order status in PaymentOrderRepository.",
                        "HTTP status is 200.\nOrder status in DB matches the webhook result.\nReplaying the same webhook does not duplicate side effects.",
                        "A pending payment order exists. Webhook stub or sandbox is enabled.",
                    ),
                    C(
                        "IT_CHECKOUT_05",
                        "Verify unauthorized checkout blocked via Controller.",
                        "1. Call POST /api/student/payments/payos/link via MockMvc without Authorization header.\n2. Security filter rejects the request before StudentPaymentController business logic.\n3. Confirm PaymentOrderRepository has no insert from this call.",
                        "HTTP status is 401 or 403.\nNo PaymentOrder insert occurs.",
                        "None.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "AUTH",
        "sheet": "IT_AUTH",
        "function": "Register Account",
        "ucs": ["UC-01 Register Account", "UC-03 Login", "UC-04 Reset password"],
        "requirement": "IT for register/login/reset password (Controller-Service-Repository).",
        "components": "AuthController, AuthService, AuthTokenService, UserRepository, AuthTokenRepository",
        "groups": [
            M(
                1,
                "Register Account",
                [
                    C(
                        "IT_AUTH_01",
                        "Verify registering a new account via Controller.",
                        "1. Prepare a unique email that is not in the database.\n2. Call POST /api/auth/register with valid JSON via MockMvc.\n3. AuthController delegates to AuthService.register().\n4. AuthService saves the user through UserRepository (password hashed) and may create a verify token in AuthTokenRepository.",
                        "HTTP status is 2xx.\nA user row is persisted.\nPassword is stored hashed, not plain text.",
                        "Email is not already registered.",
                    ),
                    C(
                        "IT_AUTH_02",
                        "Verify duplicate register rejected via Controller.",
                        "1. Call POST /api/auth/register with an email that already exists via MockMvc.\n2. AuthController delegates to AuthService.register().\n3. AuthService checks uniqueness against UserRepository and rejects the request.",
                        "HTTP status is 4xx.\nNo second user row is created for the same email.",
                        "Email already exists in DB.",
                    ),
                    C(
                        "IT_AUTH_03",
                        "Verify email OTP verification via Controller.",
                        "1. Register a new unverified user, then call POST /api/auth/verify-email with a valid code via MockMvc.\n2. AuthController delegates to AuthService.verifyEmail().\n3. AuthService validates the token in AuthTokenRepository and updates the verified flag in UserRepository.",
                        "HTTP status is 200.\nUser is marked verified.\nThe OTP token is consumed or invalidated.",
                        "Unverified user and valid OTP are available.",
                    ),
                    C(
                        "IT_AUTH_04",
                        "Verify invalid OTP rejected via Controller.",
                        "1. Call POST /api/auth/verify-email with a wrong code via MockMvc.\n2. AuthController delegates to AuthService.verifyEmail().\n3. AuthService fails token validation against AuthTokenRepository.",
                        "HTTP status is 4xx.\nUser remains unverified.",
                        "An unverified user exists.",
                    ),
                ],
            ),
            M(
                2,
                "Login",
                [
                    C(
                        "IT_AUTH_05",
                        "Verify login returns access token via Controller.",
                        "1. Call POST /api/auth/login with valid email and password via MockMvc.\n2. AuthController delegates to AuthService.login().\n3. AuthService loads the user from UserRepository and issues a JWT accessToken.",
                        "HTTP status is 200 OK.\nJSON contains accessToken.",
                        "A verified user with known password exists.",
                    ),
                    C(
                        "IT_AUTH_06",
                        "Verify /api/user/me with JWT via Controller.",
                        "1. Login and obtain a JWT accessToken.\n2. Call GET /api/user/me via MockMvc with Authorization Bearer token.\n3. UserController delegates to UserService.getCurrentUser().\n4. UserService loads the profile from UserRepository.",
                        "HTTP status is 200 OK.\nJSON email matches the logged-in user.",
                        "A valid JWT from login is available.",
                    ),
                    C(
                        "IT_AUTH_07",
                        "Verify login with wrong password rejected via Controller.",
                        "1. Call POST /api/auth/login with a wrong password via MockMvc.\n2. AuthController delegates to AuthService.login().\n3. AuthService fails credential check against UserRepository.",
                        "HTTP status is 401 or 4xx.\nNo accessToken is issued.",
                        "An existing user account is available.",
                    ),
                ],
            ),
            M(
                3,
                "Reset password",
                [
                    C(
                        "IT_AUTH_08",
                        "Verify forgot-password request via Controller.",
                        "1. Call POST /api/auth/forgot-password with a registered email via MockMvc.\n2. AuthController delegates to AuthService.forgotPassword().\n3. AuthService creates a reset token through AuthTokenRepository.",
                        "HTTP status is 200 (or generic success).\nA reset token row exists for the user.",
                        "Registered email exists.",
                    ),
                    C(
                        "IT_AUTH_09",
                        "Verify reset password with token via Controller.",
                        "1. Call POST /api/auth/reset-password with a valid token and new password via MockMvc.\n2. AuthController delegates to AuthService.resetPassword().\n3. AuthService updates the password hash in UserRepository and invalidates the token in AuthTokenRepository.\n4. Login with the new password, then confirm the old password fails.",
                        "HTTP status is 200.\nLogin succeeds with the new password.\nLogin fails with the old password.",
                        "A valid reset token exists.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "USER",
        "sheet": "IT_USER",
        "function": "Manage profile",
        "ucs": ["UC-05 Manage profile"],
        "requirement": "IT for view/update profile (Controller-Service-Repository).",
        "components": "UserController, UserService, UserRepository",
        "groups": [
            M(
                1,
                "Manage profile",
                [
                    C(
                        "IT_USER_01",
                        "Verify fetching user profile via Controller.",
                        "1. Login and obtain a JWT accessToken.\n2. Call GET /api/user/me via MockMvc with Authorization Bearer token.\n3. UserController delegates to UserService.getCurrentUser() or findById().\n4. UserService retrieves the user from UserRepository.",
                        "HTTP status is 200 OK.\nReturned JSON contains correct user info (name, email).",
                        "User exists in DB.",
                    ),
                    C(
                        "IT_USER_02",
                        "Verify updating profile persists data into DB.",
                        "1. Login and obtain a JWT.\n2. Call PUT or PATCH /api/user/me with a valid JSON payload via MockMvc.\n3. UserController delegates to UserService.updateProfile().\n4. UserService saves changes through UserRepository.save().\n5. Reload GET /api/user/me or query DB to confirm persistence.",
                        "HTTP status is 200 OK.\nUpdated fields are persisted in DB.",
                        "Valid JWT and valid JSON payload.",
                    ),
                    C(
                        "IT_USER_03",
                        "Verify validation error on invalid profile payload.",
                        "1. Login and obtain a JWT.\n2. Call PUT or PATCH /api/user/me with an invalid field (for example bad email format) via MockMvc.\n3. UserController triggers bean validation before UserService save.\n4. Confirm UserRepository row is unchanged.",
                        "HTTP status is 400 Bad Request.\nError indicates invalid field.\nDB profile is unchanged.",
                        "Valid JWT.",
                    ),
                    C(
                        "IT_USER_04",
                        "Verify unauthenticated profile access rejected via Controller.",
                        "1. Call GET /api/user/me via MockMvc without Authorization header.\n2. Security filter rejects the call before UserController and UserService.",
                        "HTTP status is 401 or 403.",
                        "None.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "CLASS",
        "sheet": "IT_CLASS",
        "function": "Manage Classrooms",
        "ucs": ["UC-36a Create Classroom", "UC-36b View Classrooms", "UC-36c Update Classroom"],
        "requirement": "IT for staff classroom CRUD (Controller-Service-Repository).",
        "components": "StaffClassroomController, ClassroomOfferingService, ClassroomOfferingRepository",
        "groups": [
            M(
                1,
                "View Classrooms",
                [
                    C(
                        "IT_CLASS_01",
                        "Verify staff lists classrooms via Controller.",
                        "1. Login as STAFF and obtain a JWT.\n2. Call GET /api/staff/classrooms via MockMvc with Bearer token.\n3. StaffClassroomController delegates to ClassroomOfferingService.list().\n4. ClassroomOfferingService reads ClassroomOfferingRepository.",
                        "HTTP status is 200 OK.\nJSON contains a list of classroom offerings.",
                        "STAFF JWT is available. At least one classroom exists.",
                    ),
                ],
            ),
            M(
                2,
                "Create Classroom",
                [
                    C(
                        "IT_CLASS_02",
                        "Verify staff creates classroom proposal via Controller (create flow).",
                        "1. Login as STAFF and obtain a JWT.\n"
                        "2. Call GET /api/staff/classrooms/training-programs to pick a published TrainingProgram id.\n"
                        "3. Call POST /api/staff/classroom-proposals with title, courseOfferingId, capacity, dates, weekdays, session times via MockMvc.\n"
                        "4. StaffClassroomProposalController delegates to ClassroomProposalService.create(); draft proposal is saved.\n"
                        "Note: StaffClassroomController has no POST /api/staff/classrooms root create; opening a class goes through proposal approve.",
                        "HTTP status is 200 or 201.\n"
                        "JSON contains proposal id and approvalStatus=DRAFT (or equivalent).",
                        "Published TrainingProgram and STAFF JWT available.",
                    ),
                ],
            ),
            M(
                3,
                "Update Classroom",
                [
                    C(
                        "IT_CLASS_03",
                        "Verify staff updates classroom via Controller.",
                        "1. Login as STAFF and obtain a JWT.\n"
                        "2. Call GET /api/staff/classrooms and pick an updatable offering id (prefer OFFLINE without unapproved curriculum).\n"
                        "3. Call PUT /api/staff/classrooms/{id} with a valid CreateClassroomOfferingRequest body via MockMvc.\n"
                        "4. StaffClassroomController delegates to ClassroomOfferingService.updateOffering().\n"
                        "5. ClassroomOfferingService saves changes through ClassroomOfferingRepository.",
                        "HTTP status is 200 OK.\nUpdated fields are persisted in DB.",
                        "An existing updatable classroom id is available.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "ASNTEACH",
        "sheet": "IT_ASNTEACH",
        "function": "Assign Teacher to Classroom",
        "ucs": ["UC-37 Assign Teacher to Classroom"],
        "requirement": "IT for staff assigning teacher to classroom (Controller-Service-Repository).",
        "components": "StaffClassroomController, ClassroomOfferingService, ClassroomTeacherAssignmentRepository",
        "groups": [
            M(
                1,
                "Assign Teacher to Classroom",
                [
                    C(
                        "IT_ASNTEACH_01",
                        "Verify assigning teacher to classroom via Controller.",
                        "1. Login as STAFF and obtain a JWT.\n"
                        "2. Call POST /api/staff/classrooms/{id}/teachers/{teacherId}/assign via MockMvc.\n"
                        "3. StaffClassroomController delegates to ClassroomOfferingService.assignTeacher().\n"
                        "4. ClassroomOfferingService writes ClassroomTeacherAssignmentRepository.",
                        "HTTP status is 200 or 201.\n"
                        "A teacher assignment row exists for the classroom.",
                        "Classroom and TEACHER user exist.",
                    ),
                    C(
                        "IT_ASNTEACH_02",
                        "Verify assign teacher rejected for non-staff via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call POST /api/staff/classrooms/{id}/teachers/{teacherId}/assign via MockMvc.\n"
                        "3. Security or StaffClassroomController rejects the call before a service write.\n"
                        "4. Confirm ClassroomTeacherAssignmentRepository has no new row.",
                        "HTTP status is 403.\n"
                        "No assignment row is created.",
                        "Classroom exists.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "SCHEDULE",
        "sheet": "IT_SCHEDULE",
        "function": "View Teaching Schedule",
        "ucs": ["UC-22 View Teaching Schedule"],
        "requirement": "IT for teacher teaching schedule / assigned classrooms (Controller-Service-Repository).",
        "components": "TeacherClassroomController, ClassroomOfferingService, ClassroomSessionRepository",
        "groups": [
            M(
                1,
                "View Teaching Schedule",
                [
                    C(
                        "IT_SCHEDULE_01",
                        "Verify teacher lists assigned classrooms via Controller.",
                        "1. Login as TEACHER and obtain a JWT.\n2. Call GET /api/teacher/classrooms/assigned via MockMvc.\n3. TeacherClassroomController delegates to ClassroomOfferingService.listForTeacher().\n4. ClassroomOfferingService queries assignment and offering repositories.",
                        "HTTP status is 200 OK.\nOnly classrooms assigned to this teacher are returned.",
                        "TEACHER is assigned to at least one classroom.",
                    ),
                    C(
                        "IT_SCHEDULE_02",
                        "Verify teacher loads classroom sessions via Controller.",
                        "1. Login as TEACHER and obtain a JWT.\n2. Call GET /api/teacher/classrooms/{id}/sessions via MockMvc.\n3. TeacherClassroomController delegates to ClassroomOfferingService (session list method).\n4. Service reads ClassroomSessionRepository.",
                        "HTTP status is 200 OK.\nSession list matches DB for that offering.",
                        "Assigned classroom has sessions.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "ATTEND",
        "sheet": "IT_ATTEND",
        "function": "Manage Class Attendance",
        "ucs": ["UC-23a View Class Attendance", "UC-23b Record Class Attendance"],
        "requirement": "IT for teacher view/record class attendance (Controller-Service-Repository).",
        "components": "TeacherClassroomController, ClassroomAttendanceService, ClassroomAttendanceRepository",
        "groups": [
            M(
                1,
                "Record Class Attendance",
                [
                    C(
                        "IT_ATTEND_01",
                        "Verify teacher saves session attendance via Controller.",
                        "1. Login as TEACHER and obtain a JWT.\n2. Call POST /api/teacher/classrooms/attendance via MockMvc with attendance payload (sessionId + learner statuses).\n3. TeacherClassroomController delegates to ClassroomAttendanceService.saveAttendance().\n4. ClassroomAttendanceService upserts rows in ClassroomAttendanceRepository.",
                        "HTTP status is 200 OK.\nAttendance rows are persisted for learners in the session.",
                        "Session exists and teacher is authorized.",
                    ),
                ],
            ),
            M(
                2,
                "View Class Attendance",
                [
                    C(
                        "IT_ATTEND_02",
                        "Verify teacher loads attendance sheet via Controller.",
                        "1. Login as TEACHER and obtain a JWT.\n2. Call GET /api/teacher/classrooms/sessions/{sessionId}/attendance via MockMvc.\n3. TeacherClassroomController delegates to ClassroomAttendanceService.getAttendance().\n4. ClassroomAttendanceService reads ClassroomAttendanceRepository.",
                        "HTTP status is 200 OK.\nJSON matches stored attendance statuses.",
                        "Attendance data exists.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "MNGHW",
        "sheet": "IT_MNGHW",
        "function": "Manage Homework",
        "ucs": ["UC-26a Create Homework", "UC-26 Manage Homework"],
        "requirement": "IT for teacher create homework and grade submissions (Controller-Service-Repository). Distinct from learner IT_HOMEWORK (UC-13 Submit Homework).",
        "components": "TeacherClassroomController, ClassroomHomeworkService, ClassroomHomeworkRepository, ClassroomHomeworkSubmissionRepository",
        "groups": [
            M(
                1,
                "Create Homework",
                [
                    C(
                        "IT_MNGHW_01",
                        "Verify teacher creates homework via Controller.",
                        "1. Login as TEACHER and obtain a JWT.\n2. Call POST /api/teacher/classrooms/{id}/homework via MockMvc with homework payload.\n3. TeacherClassroomController delegates to ClassroomHomeworkService.create().\n4. ClassroomHomeworkService saves through ClassroomHomeworkRepository.",
                        "HTTP status is 200 or 201.\nA homework row is persisted for the classroom.",
                        "Teacher is assigned to the classroom.",
                    ),
                    C(
                        "IT_MNGHW_02",
                        "Verify teacher grades submission via Controller.",
                        "1. Login as TEACHER and obtain a JWT.\n2. Call POST /api/teacher/classrooms/homework/{homeworkId}/students/{studentId}/grade via MockMvc with score/feedback.\n3. TeacherClassroomController delegates to ClassroomHomeworkService.grade().\n4. ClassroomHomeworkService updates ClassroomHomeworkSubmissionRepository.",
                        "HTTP status is 200 OK.\nScore and feedback are persisted on the submission row.",
                        "A submission exists.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "TIMETABLE",
        "sheet": "IT_TIMETABLE",
        "function": "View Timetable",
        "ucs": ["UC-09 View Timetable"],
        "requirement": "IT for learner classroom list and timetable/sessions (Controller-Service-Repository).",
        "components": "StudentClassroomController, ClassroomOfferingService, ClassroomEnrollmentRepository, ClassroomSessionRepository",
        "groups": [
            M(
                1,
                "View Timetable",
                [
                    C(
                        "IT_TIMETABLE_01",
                        "Verify learner lists my classrooms via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call GET /api/student/classrooms via MockMvc.\n"
                        "3. StudentClassroomController delegates to ClassroomOfferingService.listForStudent().\n"
                        "4. ClassroomOfferingService reads ClassroomEnrollmentRepository.",
                        "HTTP status is 200 OK.\n"
                        "Only enrolled classrooms are returned.",
                        "LEARNER is enrolled in at least one class.",
                    ),
                    C(
                        "IT_TIMETABLE_02",
                        "Verify learner views timetable/sessions via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call GET /api/student/classrooms/{id}/sessions via MockMvc.\n"
                        "3. StudentClassroomController delegates to the session/offering service.\n"
                        "4. Service reads ClassroomSessionRepository for the enrolled offering.",
                        "HTTP status is 200 OK.\n"
                        "Sessions match DB for the learner class.",
                        "Enrolled classroom has sessions.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "MATERIAL",
        "sheet": "IT_MATERIAL",
        "function": "Access Classroom Learning Materials",
        "ucs": ["UC-11 Access Classroom Learning Materials"],
        "requirement": "IT for learner classroom materials (UC-11). Distinct from online-course materials IT_ACCESS (UC-48).",
        "components": "StudentClassroomController, ClassroomContentService, ClassroomMaterialRepository",
        "groups": [
            M(
                1,
                "Access Classroom Learning Materials",
                [
                    C(
                        "IT_MATERIAL_01",
                        "Verify learner accesses class materials via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call GET /api/student/classrooms/{id}/materials via MockMvc.\n"
                        "3. StudentClassroomController delegates to ClassroomContentService.listMaterials().\n"
                        "4. ClassroomContentService reads ClassroomMaterialRepository.",
                        "HTTP status is 200 OK.\n"
                        "Materials for the enrolled class are returned.\n"
                        "Non-enrolled access is denied.",
                        "Materials are published for the classroom.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "HOMEWORK",
        "sheet": "IT_HOMEWORK",
        "function": "Submit Homework",
        "ucs": ["UC-13 Submit Homework"],
        "requirement": "IT for learner homework submit and view submission (Controller-Service-Repository).",
        "components": "StudentClassroomController, ClassroomHomeworkService, ClassroomHomeworkSubmissionRepository",
        "groups": [
            M(
                1,
                "Submit Homework",
                [
                    C(
                        "IT_HOMEWORK_01",
                        "Verify learner submits homework via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call POST /api/student/classrooms/homework/{homeworkId}/submit via MockMvc with submission payload.\n"
                        "3. StudentClassroomController delegates to ClassroomHomeworkService.submit().\n"
                        "4. ClassroomHomeworkService saves through ClassroomHomeworkSubmissionRepository.",
                        "HTTP status is 200 or 201.\n"
                        "A submission row is persisted for homework and learner.",
                        "Open homework exists for an enrolled learner.",
                    ),
                    C(
                        "IT_HOMEWORK_02",
                        "Verify learner views own submission via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call GET /api/student/classrooms/my-homework via MockMvc (or reload submission from submit response id).\n"
                        "3. StudentClassroomController delegates to ClassroomHomeworkService.getMySubmission().\n"
                        "4. ClassroomHomeworkService reads ClassroomHomeworkSubmissionRepository.",
                        "HTTP status is 200 OK.\n"
                        "Returned submission matches DB.",
                        "A submission exists.",
                    ),
                    C(
                        "IT_HOMEWORK_03",
                        "Verify submit rejected after deadline or closed homework via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call POST /api/student/classrooms/homework/{homeworkId}/submit via MockMvc on a closed or past-due homework.\n"
                        "3. StudentClassroomController delegates to ClassroomHomeworkService.submit().\n"
                        "4. ClassroomHomeworkService rejects by business rule before invalid overwrite.",
                        "HTTP status is 4xx.\n"
                        "No new submission row violates the close/deadline rule.",
                        "Homework is closed or past due.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "QUIZ",
        "sheet": "IT_QUIZ",
        "function": "Take Quiz",
        "ucs": ["UC-15 Take Quiz", "UC-27a Create Quiz Practice"],
        "requirement": "IT for teacher quiz practice and learner take/submit (UC-15, UC-27a).",
        "components": "ClassroomQuizController, ClassroomQuizService, ClassroomQuizRepository, ClassroomQuizAttemptRepository",
        "groups": [
            M(
                1,
                "Create Quiz Practice",
                [
                    C(
                        "IT_QUIZ_01",
                        "Verify teacher creates quiz via Controller.",
                        "1. Login as TEACHER and obtain a JWT.\n2. Call POST /api/teacher/classrooms/{id}/quizzes via MockMvc.\n3. ClassroomQuizController delegates to ClassroomQuizService.create().\n4. ClassroomQuizService saves through ClassroomQuizRepository.",
                        "HTTP status is 200 or 201.\nA quiz row is persisted.",
                        "Teacher is assigned to the classroom.",
                    ),
                    C(
                        "IT_QUIZ_02",
                        "Verify teacher updates or deletes quiz via Controller.",
                        "1. Login as TEACHER and obtain a JWT.\n2. Call DELETE /api/teacher/quizzes/{quizId} via MockMvc (or PATCH /api/teacher/quizzes/{quizId}/open|/close).\n3. ClassroomQuizController delegates to ClassroomQuizService.update() or delete().\n4. ClassroomQuizService updates ClassroomQuizRepository.",
                        "HTTP status is 200 or 204.\nDB reflects the update or delete.",
                        "An existing quiz for the classroom is available.",
                    ),
                ],
            ),
            M(
                2,
                "Take Quiz",
                [
                    C(
                        "IT_QUIZ_03",
                        "Verify learner starts or takes quiz via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n2. Call GET /api/student/classrooms/quizzes via MockMvc to load quiz, then prepare answers for submit.\n3. ClassroomQuizController delegates to ClassroomQuizService.startAttempt().\n4. ClassroomQuizService creates a row in ClassroomQuizAttemptRepository.",
                        "HTTP status is 200 OK.\nAttempt is persisted.\nQuestions are returned without answerKey leak.",
                        "Published quiz exists and learner is enrolled.",
                    ),
                    C(
                        "IT_QUIZ_04",
                        "Verify learner submits quiz via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n2. Call POST /api/student/quizzes/{id}/submit via MockMvc with answers.\n3. ClassroomQuizController delegates to ClassroomQuizService.submit().\n4. ClassroomQuizService grades and updates ClassroomQuizAttemptRepository.",
                        "HTTP status is 200 OK.\nScore and status are persisted on the attempt.",
                        "An open quiz attempt exists.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "PLACEMENT",
        "sheet": "IT_PLACEMENT",
        "function": "Take Placement Exam",
        "ucs": ["UC-16 Take Placement Exam"],
        "requirement": "IT for placement exam start/submit/result (Controller-Service-Repository).",
        "components": "PlacementTestController, PlacementTestService, PlacementTestAttemptRepository",
        "groups": [
            M(
                1,
                "Take Placement Exam",
                [
                    C(
                        "IT_PLACEMENT_01",
                        "Verify learner starts placement exam via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n2. Call GET /api/student/placement-tests/current via MockMvc to start/load the placement exam.\n3. PlacementTestController delegates to PlacementTestService.start().\n4. PlacementTestService creates an attempt through PlacementTestAttemptRepository.",
                        "HTTP status is 200 or 201.\nAttempt row exists with IN_PROGRESS status.",
                        "LEARNER is eligible for placement.",
                    ),
                    C(
                        "IT_PLACEMENT_02",
                        "Verify learner submits placement answers via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n2. Call POST /api/student/placement-tests/current/submit via MockMvc with answers.\n3. PlacementTestController delegates to PlacementTestService.submit().\n4. PlacementTestService updates attempt and score in PlacementTestAttemptRepository.",
                        "HTTP status is 200 OK.\nAttempt is marked submitted and score is stored.",
                        "An in-progress attempt exists.",
                    ),
                    C(
                        "IT_PLACEMENT_03",
                        "Verify learner views placement result via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n2. Call GET /api/student/placement-tests/current via MockMvc after submit (result returned from submit/current payload).\n3. PlacementTestController delegates to PlacementTestService.getResult().\n4. PlacementTestService reads PlacementTestAttemptRepository.",
                        "HTTP status is 200 OK.\nResult JSON matches the stored score/report.",
                        "A submitted attempt exists.",
                    ),
                    C(
                        "IT_PLACEMENT_04",
                        "Verify unauthenticated placement access rejected via Controller.",
                        "1. Call GET /api/student/placement-tests/current via MockMvc without JWT.\n2. Security filter rejects before PlacementTestService.\n3. Confirm PlacementTestAttemptRepository has no new row.",
                        "HTTP status is 401 or 403.\nNo attempt row is created.",
                        "None.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "ONLINE",
        "sheet": "IT_ONLINE",
        "function": "Manage Online Courses",
        "ucs": ["UC-33a Create Online Course", "UC-33b View Online Courses", "UC-33c Update Online Course", "UC-33d Deactivate Online Course"],
        "requirement": "IT for Content Manager online course CRUD (Controller-Service-Repository).",
        "components": "ContentManagerOnlineCourseController, OnlineCourseService, OnlineCourseRepository",
        "groups": [
            M(
                1,
                "View Online Courses",
                [
                    C(
                        "IT_ONLINE_01",
                        "Verify CM lists online courses via Controller.",
                        "1. Login as Content Manager and obtain a JWT.\n2. Call GET /api/content-manager/online-courses via MockMvc.\n3. ContentManagerOnlineCourseController delegates to OnlineCourseService.listForCm().\n4. OnlineCourseService reads OnlineCourseRepository.",
                        "HTTP status is 200 OK.\nCourse list is returned (including draft if allowed for CM).",
                        "CM JWT is available.",
                    ),
                ],
            ),
            M(
                2,
                "Create Online Course",
                [
                    C(
                        "IT_ONLINE_02",
                        "Verify CM creates online course via Controller.",
                        "1. Login as Content Manager and obtain a JWT.\n2. Call POST /api/content-manager/online-courses via MockMvc with valid body.\n3. ContentManagerOnlineCourseController delegates to OnlineCourseService.create().\n4. OnlineCourseService saves through OnlineCourseRepository.",
                        "HTTP status is 200 or 201.\nA new online_courses row is persisted.",
                        "Valid create payload.",
                    ),
                ],
            ),
            M(
                3,
                "Update Online Course",
                [
                    C(
                        "IT_ONLINE_03",
                        "Verify CM updates online course via Controller.",
                        "1. Login as Content Manager and obtain a JWT.\n2. Call PUT /api/content-manager/online-courses/{id} via MockMvc.\n3. ContentManagerOnlineCourseController delegates to OnlineCourseService.update().\n4. OnlineCourseService saves through OnlineCourseRepository.",
                        "HTTP status is 200 OK.\nUpdated fields are persisted.",
                        "Existing course id.",
                    ),
                    C(
                        "IT_ONLINE_04",
                        "Verify non-CM cannot manage courses via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n2. Call GET /api/content-manager/online-courses via MockMvc (or POST /api/content-manager/online-courses).\n3. Security rejects before OnlineCourseService CM methods.\n4. Confirm no unauthorized insert or update.",
                        "HTTP status is 403.\nNo unauthorized insert or update occurs.",
                        "Course API requires CM role.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "SYLLABUS",
        "sheet": "IT_SYLLABUS",
        "function": "Manage Syllabus",
        "ucs": ["UC-32a Create Syllabus", "UC-32b View Syllabus", "UC-32c Update Syllabus", "UC-32d Delete Syllabus"],
        "requirement": "IT for Content Manager syllabus/curriculum programs (Controller-Service-Repository).",
        "components": "ContentManagerCurriculumController, CurriculumProgramService, CurriculumProgramRepository, CurriculumUnitRepository",
        "groups": [
            M(
                1,
                "View Syllabus",
                [
                    C(
                        "IT_SYLLABUS_01",
                        "Verify CM lists curriculum programs via Controller.",
                        "1. Login as Content Manager and obtain a JWT.\n2. Call GET /api/content-manager/curriculum-programs via MockMvc.\n3. ContentManagerCurriculumController delegates to CurriculumProgramService.list().\n4. CurriculumProgramService reads CurriculumProgramRepository.",
                        "HTTP status is 200 OK.",
                        "CM JWT is available.",
                    ),
                ],
            ),
            M(
                2,
                "Create Syllabus",
                [
                    C(
                        "IT_SYLLABUS_02",
                        "Verify CM creates syllabus or program via Controller.",
                        "1. Login as Content Manager and obtain a JWT.\n2. Call POST /api/content-manager/curriculum-programs via MockMvc.\n3. ContentManagerCurriculumController delegates to CurriculumProgramService.create().\n4. CurriculumProgramService saves through CurriculumProgramRepository.",
                        "HTTP status is 200 or 201.\nProgram row is persisted.",
                        "Valid payload.",
                    ),
                ],
            ),
            M(
                3,
                "Update Syllabus",
                [
                    C(
                        "IT_SYLLABUS_03",
                        "Verify CM updates syllabus via Controller.",
                        "1. Login as Content Manager and obtain a JWT.\n2. Call PUT /api/content-manager/curriculum-programs/{id} via MockMvc.\n3. ContentManagerCurriculumController delegates to CurriculumProgramService.update().\n4. CurriculumProgramService saves through CurriculumProgramRepository.",
                        "HTTP status is 200 OK.\nChanges are persisted in DB.",
                        "Existing program id.",
                    ),
                    C(
                        "IT_SYLLABUS_04",
                        "Verify CM manages units under syllabus via Controller.",
                        "1. Login as Content Manager and obtain a JWT.\n2. Call POST /api/content-manager/curriculum-programs/{programId}/units via MockMvc (or PUT /api/content-manager/curriculum-units/{unitId}).\n3. ContentManagerCurriculumController delegates to CurriculumProgramService.saveUnit().\n4. CurriculumProgramService writes CurriculumUnitRepository.",
                        "HTTP status is 200 or 201.\nUnit row is linked to the program in DB.",
                        "Program exists.",
                    ),
                ],
            ),
            M(
                4,
                "Delete Syllabus",
                [
                    C(
                        "IT_SYLLABUS_05",
                        "Verify CM deletes syllabus via Controller.",
                        "1. Login as Content Manager and obtain a JWT.\n2. Call DELETE /api/content-manager/curriculum-programs/{id} via MockMvc.\n3. ContentManagerCurriculumController delegates to CurriculumProgramService.delete().\n4. CurriculumProgramService removes or soft-deletes through CurriculumProgramRepository.",
                        "HTTP status is 200 or 204.\nProgram is no longer active in DB.",
                        "Program exists and is deletable.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "NOTIF",
        "sheet": "IT_NOTIF",
        "function": "View Notifications",
        "ucs": ["UC-06 View Notifications"],
        "requirement": "IT for learner/user inbox notifications list/read/preferences (UC-06). Not admin broadcasts.",
        "components": "StudentNotificationController, AppNotificationService, AppNotificationRepository, NotificationPreferenceRepository",
        "groups": [
            M(
                1,
                "View Notifications",
                [
                    C(
                        "IT_NOTIF_01",
                        "Verify listing inbox notifications via Controller.",
                        "1. Login as a system user and obtain a JWT.\n2. Call GET /api/student/notifications via MockMvc.\n3. StudentNotificationController delegates to AppNotificationService.list().\n4. AppNotificationService reads AppNotificationRepository for the current user.",
                        "HTTP status is 200 OK.\nOnly current user inbox notifications are returned.",
                        "At least one AppNotification exists for the user.",
                    ),
                    C(
                        "IT_NOTIF_02",
                        "Verify marking inbox notification as read via Controller.",
                        "1. Login and obtain a JWT.\n2. Call PATCH /api/student/notifications/{notificationId}/read via MockMvc.\n3. StudentNotificationController delegates to AppNotificationService.markRead().\n4. AppNotificationService updates AppNotificationRepository.",
                        "HTTP status is 200 OK.\nNotification read flag is persisted.",
                        "An unread notification exists.",
                    ),
                    C(
                        "IT_NOTIF_03",
                        "Verify mark-all-read via Controller.",
                        "1. Login and obtain a JWT.\n2. Call PATCH /api/student/notifications/read-all via MockMvc.\n3. StudentNotificationController delegates to AppNotificationService.markAllRead().\n4. AppNotificationService updates AppNotificationRepository for the user.",
                        "HTTP status is 200 OK.\nAll user notifications are marked read in DB.",
                        "Multiple unread notifications exist.",
                    ),
                    C(
                        "IT_NOTIF_04",
                        "Verify notification preferences via Controller.",
                        "1. Login and obtain a JWT.\n2. Call GET or PUT /api/user/me/notification-preferences via MockMvc.\n3. UserController delegates to NotificationPreferenceService.\n4. NotificationPreferenceService reads or writes NotificationPreferenceRepository.",
                        "HTTP status is 200 OK.\nPreferences are persisted.",
                        "Valid JWT.",
                    ),
                    C(
                        "IT_NOTIF_05",
                        "Verify unauthenticated inbox notifications rejected via Controller.",
                        "1. Call GET /api/student/notifications via MockMvc without JWT.\n2. Security rejects before AppNotificationService.",
                        "HTTP status is 401 or 403.",
                        "None.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "SUPPORT",
        "sheet": "IT_SUPPORT",
        "function": "Submit Support Ticket",
        "ucs": ["UC-07 Submit Support Ticket", "UC-44 Resolve Support Tickets"],
        "requirement": "IT for support ticket submit and staff resolve.",
        "components": "StudentSupportTicketController, ManagerSupportTicketController, SupportTicketService, SupportTicketRepository",
        "groups": [
            M(
                1,
                "Submit Support Ticket",
                [
                    C(
                        "IT_SUPPORT_01",
                        "Verify learner submits support ticket via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n2. Call POST /api/student/support-tickets via MockMvc with ticket payload.\n3. StudentSupportTicketController delegates to SupportTicketService.create().\n4. SupportTicketService saves through SupportTicketRepository.",
                        "HTTP status is 200 or 201.\nTicket row is persisted with OPEN status.",
                        "Valid ticket payload.",
                    ),
                    C(
                        "IT_SUPPORT_02",
                        "Verify learner lists own tickets via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n2. Call GET /api/student/support-tickets via MockMvc.\n3. StudentSupportTicketController delegates to SupportTicketService.listMine().\n4. SupportTicketService queries SupportTicketRepository by user.",
                        "HTTP status is 200 OK.\nOnly current user tickets are returned.",
                        "At least one ticket exists for the learner.",
                    ),
                ],
            ),
            M(
                2,
                "Resolve Support Tickets",
                [
                    C(
                        "IT_SUPPORT_03",
                        "Verify staff lists support queue via Controller.",
                        "1. Login as STAFF or Manager and obtain a JWT.\n2. Call GET /api/staff/support-tickets via MockMvc (or GET /api/manager/support-tickets).\n3. ManagerSupportTicketController delegates to SupportTicketService.listQueue().\n4. SupportTicketService reads SupportTicketRepository.",
                        "HTTP status is 200 OK.\nQueue includes open tickets.",
                        "STAFF or Manager JWT is available.",
                    ),
                    C(
                        "IT_SUPPORT_04",
                        "Verify staff resolves ticket via Controller.",
                        "1. Login as STAFF or Manager and obtain a JWT.\n2. Call PATCH /api/staff/support-tickets/{ticketId} via MockMvc with resolve/close status body (or /api/manager/support-tickets/{ticketId}).\n3. ManagerSupportTicketController delegates to SupportTicketService.resolve().\n4. SupportTicketService updates SupportTicketRepository status and may write a reply message.",
                        "HTTP status is 200 OK.\nTicket status is CLOSED or RESOLVED in DB.",
                        "An open ticket exists.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "ADMIN",
        "sheet": "IT_ADMIN",
        "function": "Manage User Accounts",
        "ucs": [
            "UC-42b View User Accounts",
            "UC-42d Lock/Unlock User Account",
        ],
        "requirement": "IT for admin user account directory and lock/unlock (Controller-Service-Repository).",
        "components": "AdminUserController, AdminUserService, UserRepository",
        "groups": [
            M(
                1,
                "View User Accounts",
                [
                    C(
                        "IT_ADMIN_01",
                        "Verify admin views user accounts via Controller.",
                        "1. Ensure at least two users exist in UserRepository (different roles if possible).\n"
                        "2. Login as ADMIN and obtain a JWT accessToken.\n"
                        "3. Call GET /api/admin/users via MockMvc with Authorization Bearer token.\n"
                        "4. AdminUserController delegates to AdminUserService.listUsers() (or equivalent list method).\n"
                        "5. AdminUserService reads the user directory from UserRepository.\n"
                        "6. Compare returned user emails/ids with a direct DB query.\n"
                        "7. Call the same GET as LEARNER and without Authorization.",
                        "ADMIN call: HTTP 200 OK with a JSON user directory (UC-42b View User Accounts).\n"
                        "Returned user identifiers match UserRepository (no silent missing/extra rows).\n"
                        "LEARNER call: HTTP 403.\n"
                        "No Authorization: HTTP 401 or 403.\n"
                        "No HTTP 5xx.",
                        "ADMIN role seeded. Users exist in DB.",
                    ),
                ],
            ),
            M(
                2,
                "Lock/Unlock User Account",
                [
                    C(
                        "IT_ADMIN_02",
                        "Verify admin locks or unlocks a user account via Controller.",
                        "1. Pick a non-admin target user that is currently enabled/unlocked.\n"
                        "2. Login as ADMIN and obtain a JWT.\n"
                        "3. Call PATCH /api/admin/users/{id}/status via MockMvc with body {enabled:false} and Bearer token.\n"
                        "4. AdminUserController delegates to AdminUserService.lock() / unlock() (or equivalent).\n"
                        "5. AdminUserService updates the user status flag in UserRepository.\n"
                        "6. Query UserRepository and confirm the status changed.\n"
                        "7. Attempt POST /api/auth/login with the locked user's credentials; then unlock and confirm login works again.\n"
                        "8. Repeat PATCH /api/admin/users/{id}/status as LEARNER.",
                        "Lock call: HTTP 200 OK; user status persisted as locked/disabled (UC-42d Lock/Unlock User Account).\n"
                        "Locked user login: HTTP 401/403 (or business auth failure); no accessToken.\n"
                        "Unlock restores login capability.\n"
                        "LEARNER lock attempt: HTTP 403; target user status unchanged.",
                        "Target user exists. ADMIN JWT available.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "BROADCAST",
        "sheet": "IT_BROADCAST",
        "function": "Manage System Notifications",
        "ucs": [
            "UC-43a Create System Notification",
            "UC-43b View System Notifications",
            "UC-43c Update System Notification",
            "UC-43d Delete System Notification",
        ],
        "requirement": "IT for ADMIN broadcast CRUD/cancel via /api/admin/broadcasts (UC-43). Distinct from learner inbox IT_NOTIF (UC-06).",
        "components": "AdminBroadcastController, AdminBroadcastServiceImpl, AdminBroadcastRepository",
        "groups": [
            M(
                1,
                "Create System Notification",
                [
                    C(
                        "IT_BROADCAST_01",
                        "Verify admin creates a broadcast via Controller.",
                        "1. Login as ADMIN and obtain a JWT accessToken.\n"
                        "2. Prepare a valid JSON body with title, message, sendInApp=true, sendEmail=false (both Booleans required by UpsertAdminBroadcastRequest).\n"
                        "3. Call POST /api/admin/broadcasts via MockMvc with Authorization Bearer token and Content-Type application/json.\n"
                        "4. AdminBroadcastController receives the request and delegates to AdminBroadcastService.create(principalEmail, request).\n"
                        "5. AdminBroadcastServiceImpl validates the payload and saves an AdminBroadcast row through AdminBroadcastRepository.save().\n"
                        "6. Query AdminBroadcastRepository (or reload GET /api/admin/broadcasts) and compare title, message, status with the request.\n"
                        "7. Repeat POST without Authorization, then repeat with LEARNER JWT.",
                        "Valid ADMIN create: HTTP 200 OK and JSON contains the new broadcast id, title, message, status.\n"
                        "A matching AdminBroadcast row is persisted in DB (UC-43a).\n"
                        "No Authorization: HTTP 401 or 403; no new broadcast row.\n"
                        "LEARNER JWT: HTTP 403; no new broadcast row.\n"
                        "Invalid/missing required fields: HTTP 400; no insert.",
                        "ADMIN role seeded. Valid UpsertAdminBroadcastRequest fields available.",
                    ),
                ],
            ),
            M(
                2,
                "View System Notifications",
                [
                    C(
                        "IT_BROADCAST_02",
                        "Verify admin lists broadcasts via Controller.",
                        "1. Seed at least one AdminBroadcast row (create via API or DB seed) with known title and status.\n"
                        "2. Login as ADMIN and obtain a JWT accessToken.\n"
                        "3. Call GET /api/admin/broadcasts?page=0&size=10 via MockMvc with Authorization Bearer token.\n"
                        "4. AdminBroadcastController delegates to AdminBroadcastService.list(status, pageable).\n"
                        "5. AdminBroadcastServiceImpl reads page data from AdminBroadcastRepository ordered by createdAt descending.\n"
                        "6. Compare returned page content (id, title, audience/target, status, createdAt) with DB rows.\n"
                        "7. Optionally call GET with a status filter; then call GET without Authorization.",
                        "ADMIN list: HTTP 200 OK with a page/list of AdminBroadcast rows (UC-43b).\n"
                        "Returned IDs match AdminBroadcastRepository for the filter.\n"
                        "Empty DB still returns HTTP 200 with empty content (empty-state allowed).\n"
                        "No Authorization: HTTP 401 or 403.",
                        "ADMIN JWT available. Prefer >=1 existing broadcast for non-empty assertion.",
                    ),
                ],
            ),
            M(
                3,
                "Update System Notification",
                [
                    C(
                        "IT_BROADCAST_03",
                        "Verify admin updates a draft/scheduled broadcast via Controller.",
                        "1. Create a broadcast via POST /api/admin/broadcasts and keep its id.\n"
                        "2. Login as ADMIN; call PUT /api/admin/broadcasts/{id} with updated title/message via MockMvc.\n"
                        "3. AdminBroadcastController delegates to AdminBroadcastService.update(principalEmail, id, request).\n"
                        "4. AdminBroadcastServiceImpl loads the entity from AdminBroadcastRepository, applies changes, and saves.\n"
                        "5. Reload GET /api/admin/broadcasts or query DB by id and confirm updated fields.\n"
                        "6. Call PUT with a non-existent id; call PUT as LEARNER.",
                        "Valid update: HTTP 200 OK; title/message in DB match the new payload (UC-43c).\n"
                        "Unknown id: HTTP 404 or business not-found; no unrelated row changed.\n"
                        "LEARNER: HTTP 403; broadcast unchanged.",
                        "An editable broadcast exists. ADMIN JWT available.",
                    ),
                ],
            ),
            M(
                4,
                "Delete System Notification",
                [
                    C(
                        "IT_BROADCAST_04",
                        "Verify admin cancels a broadcast via Controller.",
                        "1. Create (and optionally schedule) a broadcast that can be cancelled; keep its id.\n"
                        "2. Login as ADMIN; call POST /api/admin/broadcasts/{id}/cancel via MockMvc with Bearer token.\n"
                        "3. AdminBroadcastController delegates to AdminBroadcastService.cancel(principalEmail, id).\n"
                        "4. AdminBroadcastServiceImpl updates status to CANCELLED (or equivalent) through AdminBroadcastRepository.\n"
                        "5. Reload the broadcast from DB/API and confirm status change and that it is no longer sendable.\n"
                        "6. Call cancel again on the same id; call cancel as LEARNER.",
                        "Valid cancel: HTTP 200 OK; status becomes CANCELLED/inactive in DB (UC-43d).\n"
                        "Cancelled broadcast is not sent on subsequent send attempts.\n"
                        "LEARNER: HTTP 403; status unchanged.\n"
                        "Idempotent or business-safe behavior on double-cancel (4xx or same CANCELLED status; never 5xx).",
                        "Cancellable broadcast exists. ADMIN JWT available.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "GMEET",
        "sheet": "IT_GMEET",
        "function": "Join Online Meeting",
        "ucs": ["UC-10 Join Online Meeting"],
        "requirement": "IT for opening/joining Google Meet session (Controller-Service-Repository).",
        "components": "TeacherClassroomController, StudentClassroomController, VirtualMeetingService (GoogleMeetServiceImpl), ClassroomSessionRepository",
        "groups": [
            M(
                1,
                "Join Online Meeting",
                [
                    C(
                        "IT_GMEET_01",
                        "Verify teacher opens Google Meet via Controller.",
                        "1. Login as TEACHER and obtain a JWT.\n"
                        "2. Call GET /api/teacher/classrooms/assigned; pick a VIRTUAL classroom id.\n"
                        "3. Call GET /api/teacher/classrooms/{id}/sessions; pick a sessionId.\n"
                        "4. Call POST /api/teacher/classrooms/sessions/{sessionId}/open via MockMvc.\n"
                        "5. TeacherClassroomController delegates to VirtualMeetingService (GoogleMeetServiceImpl).",
                        "HTTP status is 200 OK and session meeting URL is persisted when Google Meet provider is enabled.\n"
                        "If provider is disabled/unavailable: HTTP 400/503 with business message (mark N/A for env limit).",
                        "Teacher has a VIRTUAL classroom with sessions. Google Meet provider optional.",
                    ),
                    C(
                        "IT_GMEET_02",
                        "Verify learner joins Google Meet via Controller.",
                        "1. Login as enrolled LEARNER and obtain a JWT.\n"
                        "2. Call GET /api/student/classrooms/my-classrooms; pick a VIRTUAL classroom id.\n"
                        "3. Call GET /api/student/classrooms/{id}/sessions; pick a sessionId.\n"
                        "4. Call POST /api/student/classrooms/sessions/{sessionId}/join via MockMvc.\n"
                        "5. StudentClassroomController delegates to VirtualMeetingService.join().",
                        "HTTP status is 200 OK with join URL when Google Meet provider is enabled.\n"
                        "If provider is disabled/unavailable: HTTP 400/503 with business message (mark N/A for env limit).",
                        "Learner enrolled in VIRTUAL classroom. Google Meet provider optional.",
                    ),
                    C(
                        "IT_GMEET_03",
                        "Verify non-enrolled learner cannot join Google Meet via Controller.",
                        "1. Login as LEARNER and obtain a JWT.\n"
                        "2. Call POST /api/student/classrooms/sessions/{unknownSessionId}/join via MockMvc.\n"
                        "3. StudentClassroomController / service rejects unknown or unauthorized session.\n"
                        "4. Confirm no Google Meet credential is leaked in the response.",
                        "HTTP status is 400, 403, or 404.\n"
                        "No meeting credential leak.",
                        "LEARNER JWT available.",
                    ),
                ],
            ),
        ],
    },

    {
        "code": "REPORT",
        "sheet": "IT_REPORT",
        "function": "View operational report",
        "ucs": ["UC-40 View operational report", "UC-41 View revenue analytic of online course"],
        "requirement": "IT for operational dashboard and revenue analytics.",
        "components": "StaffDashboardController, ContentManagerRevenueController, StaffOperationsService, PaymentService",
        "groups": [
            M(
                1,
                "View operational report",
                [
                    C(
                        "IT_REPORT_01",
                        "Verify staff loads operational dashboard via Controller.",
                        "1. Login as STAFF or Manager and obtain a JWT.\n2. Call GET /api/staff/dashboard via MockMvc.\n3. StaffDashboardController delegates to StaffOperationsService.getDashboard().\n4. StaffOperationsService aggregates metrics from operational source repositories.",
                        "HTTP status is 200 OK.\nDashboard metrics JSON is returned.",
                        "STAFF or Manager JWT is available. Source data is seeded.",
                    ),
                ],
            ),
            M(
                2,
                "View revenue analytic of online course",
                [
                    C(
                        "IT_REPORT_02",
                        "Verify revenue analytics via Controller.",
                        "1. Login as Content Manager or Manager and obtain a JWT.\n2. Call GET /api/content-manager/revenue via MockMvc.\n3. ContentManagerRevenueController delegates to PaymentService or revenue service.\n4. Service aggregates paid orders from PaymentOrderRepository.",
                        "HTTP status is 200 OK.\nRevenue figures are consistent with paid orders in DB.",
                        "Paid payment orders exist.",
                    ),
                ],
            ),
        ],
    }

]


def iter_cases(module):
    for g in module["groups"]:
        yield ("GROUP", g["name"], g.get("scope"), None)
        for c in g["cases"]:
            yield ("CASE", g["name"], g.get("scope"), c)


def all_case_ids():
    ids = []
    for m in MODULES:
        for kind, *_rest, c in iter_cases(m):
            if kind == "CASE":
                ids.append(c["id"])
    return ids
