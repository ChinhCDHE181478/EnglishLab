# Integration Test Validation Report

Date: 2026-07-28

Workbook: `C:\Users\phong\Downloads\intergration test\SEP490_G23_Report5.2_Integration Test_COMPLETED_v4.xlsx`

Sheets: ['Cover', 'Test Cases', 'Test Statistics', 'IT - Auth', 'IT - User', 'IT - Notif', 'IT - Commerce', 'IT - Payment', 'IT - Course', 'IT - Discuss', 'IT - Content', 'IT - Package', 'IT - Curriculum', 'IT - EnrollReq', 'IT - Classroom', 'IT - LearnerCls', 'IT - Teacher', 'IT - Quiz', 'IT - Assess', 'IT - Support', 'IT - Admin', 'IT - Lark', 'IT - Infra', 'IT - Report', 'IT - Proposal', 'IT - Dispute', 'IT - Notes']

## Totals
- Modules: 24
- Test cases: 111
- Duplicate IDs: None

## Per module

| Module | Count |
|---|---:|
| AUTH | 10 |
| USER | 5 |
| NOTIF | 5 |
| COMMERCE | 4 |
| PAYMENT | 5 |
| COURSE | 6 |
| DISCUSS | 5 |
| CONTENT | 4 |
| PACKAGE | 3 |
| CURRICULUM | 5 |
| ENROLLREQ | 5 |
| CLASS | 8 |
| LEARNERCLS | 6 |
| TEACH | 6 |
| QUIZ | 4 |
| ASSESS | 6 |
| SUPPORT | 4 |
| ADMIN | 4 |
| LARK | 3 |
| INFRA | 3 |
| REPORT | 2 |
| PROPOSAL | 3 |
| DISPUTE | 3 |
| NOTES | 2 |

## Endpoints covered (by design of IT cases)

- /api/auth/* (register, login, verify, forgot/reset)
- /api/user/me* (profile, password, notification-preferences)
- /api/student/commerce/* , /api/student/payments/* , /api/payos/webhook
- /api/online-courses/** , /api/student/online-courses/**
- /api/classroom-offerings/** , /api/training-manager/classrooms/** , /api/student/classrooms/** , /api/teacher/classrooms/**
- /api/student/placement-tests/**
- /api/student/support-tickets/** , manager/staff support ticket APIs
- /api/admin/users/**
- /api/student/notifications/**

## Special environments

- PayOS, Mail, Gemini, Lark should be stubbed for deterministic IT.
- Waitlist HTTP mapping gap: validate service method; restore controller if product requires.
- No Testcontainers dependency today — use dedicated test PostgreSQL or profile.

## Assumptions

- UC IDs from SRS 1.3.2 are the primary requirement anchors.
- Role names match `RoleEnum`.
- Round statuses remain Pending until executed.

## Checks performed

1. Unique IT_* IDs
2. Sheets exist for every Test Cases row
3. Statistics formulas retargeted to module sheets
4. No UI-click procedures
5. Cover metadata filled from project code SEP490_G23 / EnglishLab
