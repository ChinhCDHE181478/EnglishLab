# Integration Test Traceability (v2 full project)

Date: 2026-07-28

Modules: 24; designed for whole EnglishLab backend surface.

| SRS requirement ID | Requirement summary | Module | Test Case IDs | Coverage status |
|---|---|---|---|---|
| UC-01 | Register Account | AUTH | IT_AUTH_01, IT_AUTH_02, IT_AUTH_03, IT_AUTH_04, IT_AUTH_05, IT_AUTH_06, IT_AUTH_07, IT_AUTH_08, IT_AUTH_09 | Covered |
| UC-03 | Login | AUTH | IT_AUTH_05, IT_AUTH_06, IT_AUTH_07 | Covered |
| UC-04 | Reset password | AUTH | IT_AUTH_08, IT_AUTH_09, IT_AUTH_10 | Covered |
| UC-05 | Manage profile | USER | IT_USER_01, IT_USER_02, IT_USER_03, IT_USER_04, IT_USER_05 | Covered |
| UC-06 | View Notifications | NOTIF | IT_NOTIF_01, IT_NOTIF_02, IT_NOTIF_03, IT_NOTIF_04, IT_NOTIF_05 | Covered |
| UC-07 | Submit Support Ticket | SUPPORT | IT_SUPPORT_01, IT_SUPPORT_02, IT_SUPPORT_04 | Covered |
| UC-44 | Resolve Support Tickets | SUPPORT | IT_SUPPORT_03 | Covered |
| UC-02 | View public courses | COURSE | IT_COURSE_01, IT_COURSE_02 | Covered |
| UC-08 | Enroll in Course | PAYMENT/COURSE/ENROLLREQ | IT_PAYMENT_01–03, IT_COURSE_03–05, IT_ENROLLREQ_* | Covered |
| UC-45 | Wishlist | COMMERCE | IT_COMMERCE_02 | Covered |
| UC-46 | Cart | COMMERCE | IT_COMMERCE_01, IT_COMMERCE_02, IT_COMMERCE_03, IT_COMMERCE_04 | Covered |
| UC-47 | Checkout / PayOS | PAYMENT | IT_PAYMENT_01, IT_PAYMENT_02, IT_PAYMENT_03, IT_PAYMENT_04, IT_PAYMENT_05 | Covered |
| UC-48 | Access online materials | COURSE | IT_COURSE_03, IT_COURSE_04 | Covered |
| UC-49 | Discuss in Course | DISCUSS | IT_DISCUSS_01, IT_DISCUSS_02 | Covered |
| UC-50 | Report Discussion | DISCUSS | IT_DISCUSS_03, IT_DISCUSS_04, IT_DISCUSS_05 | Covered |
| UC-51 | Take Note | NOTES | IT_NOTES_01, IT_NOTES_02 | Covered |
| UC-09 | View Timetable | LEARNERCLS | IT_LEARNERCLS_01, IT_LEARNERCLS_02 | Covered |
| UC-10 | Join Online Meeting | LEARNERCLS/LARK | IT_LEARNERCLS_02, IT_LARK_01–03 | Covered |
| UC-11 | Access classroom materials | LEARNERCLS | IT_LEARNERCLS_04 | Covered |
| UC-12 | Download materials | LEARNERCLS | — | SRS/source mismatch — URL only, no download API |
| UC-13 | Submit Homework | LEARNERCLS/TEACH | IT_LEARNERCLS_03, IT_TEACH_01–02 | Covered |
| UC-14 | Academic report | LEARNERCLS/TEACH | IT_LEARNERCLS_06, IT_TEACH_04 | Covered |
| UC-15 | Take Quiz | QUIZ | IT_QUIZ_01, IT_QUIZ_02, IT_QUIZ_03, IT_QUIZ_04 | Covered |
| UC-16 | Placement exam | ASSESS | IT_ASSESS_01–03 | Partially covered (timer/auto-submit client-side) |
| UC-17–20 | Skill practice assessments | ASSESS | IT_ASSESS_04–06 | Partially covered |
| UC-22 | Teaching schedule | TEACH | IT_TEACH_01 | Partially covered |
| UC-23 | Attendance | TEACH/DISPUTE | IT_TEACH_03, IT_DISPUTE_* | Covered |
| UC-26 | Manage Homework | TEACH | IT_TEACH_01, IT_TEACH_02, IT_TEACH_03, IT_TEACH_04, IT_TEACH_05, IT_TEACH_06 | Covered |
| UC-27 | Manage Quiz content | QUIZ | IT_QUIZ_01, IT_QUIZ_02, IT_QUIZ_04 | Covered |
| UC-32 | Syllabus/curriculum | CURRICULUM | IT_CURRICULUM_01, IT_CURRICULUM_02, IT_CURRICULUM_03, IT_CURRICULUM_04, IT_CURRICULUM_05 | Covered |
| UC-33 | Online course CM | CONTENT/PACKAGE | IT_CONTENT_*, IT_PACKAGE_* | Covered |
| UC-36 | Manage Classrooms | CLASS/INFRA/PROPOSAL | IT_CLASS_*, IT_INFRA_*, IT_PROPOSAL_* | Covered |
| UC-37 | Assign Teacher | CLASS | IT_CLASS_07 | Covered |
| UC-38 | Assign Learner | CLASS/ENROLLREQ | IT_CLASS_03–06, IT_ENROLLREQ_* | Covered |
| UC-39 | Evaluate Teacher | — | — | Missing implementation |
| UC-40 | Operational report | REPORT | IT_REPORT_01 | Partially covered (fixed dashboard) |
| UC-41 | Revenue analytics | REPORT | IT_REPORT_02 | Partially covered |
| UC-42 | Manage User Accounts | ADMIN | IT_ADMIN_01, IT_ADMIN_02, IT_ADMIN_03, IT_ADMIN_04 | Covered |
| Notification prefs | Channel preferences | NOTIF | IT_NOTIF_01–03 | Covered |