# MASTER DEMO DATASET — GAP REPORT

**Repo:** `ChinhCDHE181478/EnglishLab`  
**Reference date:** `2026-09-16`  
**Window:** `2026-06-01` → `2026-10-31`  
**Audit date:** 2026-09-16  
**Sources:** Flyway + 63 JPA entities + repositories/services/controllers + ~75 frontend routes + `master-dataset.json` + `MasterDemoDataSeeder` + `01_TABLE_MATRIX`

## Executive summary

MASTER hiện **mạnh ở classroom ops lớn** (classes/sessions/attendance/homework/payments/enrollments) nhưng **yếu/trống ở hầu hết feature nhỏ tạo trải nghiệm UI**: discussion, reaction, report/moderation, course review, teacher feedback, announcements, broadcasts, support message lifecycle, change requests, attendance disputes, cart/wishlist, credentials, tuition proofs, practice attempts, certificate stories, learning-path progression diversity.

`01_TABLE_MATRIX` đánh dấu nhiều bảng là `MASTER`, nhưng **generator/JSON `world` và importer chưa tạo** các domain đó → matrix đang **aspirational**, không phản ánh thực tế dataset.

**Preserved accounts** (`0386852628z@gmail.com`, `alien1062004@gmail.com`) được tôn trọng trong importer (lookup-only). Showcase completion đã được gate khi master on / khi đã có E2 progress.

---

## Inventory counts

| Scope | Count |
|---|---|
| JPA `@Entity` | 63 |
| Non-JPA domain facades | 8 |
| Frontend routed pages (approx.) | ~75 |
| MASTER JSON `world` domains present | 18 |
| Matrix rows claiming MASTER | 41 |
| Domains in matrix MASTER but missing from JSON/importer | ≥15 |

---

## Current MASTER `world` keys (actual)

| Domain | Rows | Quality note |
|---|---|---|
| accounts | 139 | Vietnamese names OK; personas thin (mostly same pattern) |
| onlineCourses | 11 (+2 protected) | Catalog OK; modules/lessons sparse |
| classes / sessions / enrollments | 16 / 384 / 160 | Strong |
| attendance | 860 | Strong; no disputes |
| homework / submissions / gradebook | 41 / 309 / 130 | Titles OK; feedback short but OK |
| onlineEnrollments / lessonProgressPlan / payments | 50 / 50 / 50 | All PAID happy-path; no fail/retry |
| tickets | 28 | Subject OK; **no messages** |
| proposals / registrations | 20 / 30 | Rows only; thin lifecycle |
| notifications | 100 | **Only 2 titles**; no `actionPath` / event diversity |
| evaluations | 12 | Manager→teacher only |
| materials / discountCodes / rooms | 40 / 3 / 8 | Basic |

---

## GAP TABLE

| DOMAIN | TABLE/ENTITY | FRONTEND FEATURE | CURRENT MASTER | EXPECTED | ISSUE | SEVERITY | RECOMMENDED FIX |
|---|---|---|---|---|---|---|---|
| Discussion | `course_discussion_posts` | CourseDetail / CourseHome forums / WorkspaceLessonDiscussion | **0** | 40–80 threads + replies Jun–Sep | Matrix says MASTER; JSON/importer missing | **CRITICAL** | Generate Q&A + replies tied to published demo courses; realistic VN/EN content |
| Reaction | `course_discussion_reactions` | DiscussionReactions UI, counters | **0** | Natural distribution across posts/replies | Feature exists end-to-end; no seed | **CRITICAL** | Seed LIKE/HELPFUL etc. without duplicates; varied counts |
| Moderation | `course_discussion_reports` | CM Discussion Moderation page | **0** (KEEP_EMPTY wrongly) | 8–15 reports: PENDING/RESOLVED/DISMISSED | CM moderation empty | **CRITICAL** | Seed realistic reasons + chronology |
| Course review | `online_course_enrollments.review_*` | Catalog averageRating/reviewCount, CourseHome rating | **0 reviews** | ~30–40% of enrollments review after progress | Cards show 0 rating | **CRITICAL** | Set reviewRating/reviewComment on eligible enrollments |
| Teacher feedback | `teacher_course_feedback` | Learner TeacherFeedbackPage; Manager teacher-performance | **0** | 20–40 feedbacks across classes | Manager/learner feedback empty | **CRITICAL** | Seed after class enrollment; mix 3–5 stars + written comments |
| Announcement | `classroom_announcements` | Learner/Teacher/Staff classroom tabs | **0** | 30–50 announcements | Classroom overview empty notices | **CRITICAL** | Seed per active/completed class; context-aware titles |
| Broadcast | `admin_broadcasts` | Admin Broadcasts page | **0** | 5–10 DRAFT/SCHEDULED/SENT | Admin broadcast empty | **HIGH** | Seed with role targets + schedule consistency |
| Support lifecycle | `support_ticket_messages` | Support ticket conversation | tickets=28, **messages=0** | 2–4 messages/ticket | Ticket list only; thread empty | **CRITICAL** | Seed learner↔staff message chains |
| Change request | `classroom_change_requests` | Teacher/Staff requests pages | **0** | 15–25 PENDING/APPROVED/REJECTED | Ops queue empty | **HIGH** | Seed transfer/withdraw/schedule reasons |
| Attendance dispute | dispute fields / facade | Learner dispute + TeacherAttendanceDisputesSection | **0** | 8–15 disputes | Dispute tabs empty | **HIGH** | Seed ABSENT→dispute→review stories |
| Cart/Wishlist | `course_list_items` | CartPage / WishlistPage / header | **0** | 15–30 wishlist + 8–15 cart | Commerce empty for demo learners | **HIGH** | Seed CourseListType WISHLIST/CART |
| Tuition | `tuition_payments`, `tuition_payment_proofs` | Classroom tuition tab | **0** | 40–80 payments + proofs PENDING/VERIFIED/REJECTED | Class fee UI empty | **HIGH** | Align with class enrollments amounts |
| Practice history | `classroom_practice_attempt_history` | Classroom practice tab | **0** | 40–80 attempts improving scores | Practice history empty | **MEDIUM** | Seed multi-attempt progressions |
| Teacher credential | `teacher_credentials` | Teacher professional profile | **0** | 12–20 credentials | Profiles thin | **MEDIUM** | IELTS/TESOL/CELTA/TOEIC realistic docs metadata |
| Lesson notes | `learner_lesson_notes` | Course workspace notes | **0** | 20–40 notes | Notes empty | **MEDIUM** | Seed for online learners with progress |
| Center library | `learning_resources` | Staff/CM materials library | only class `class_resources` | 20–40 center items | Center bank thin | **MEDIUM** | Seed shared library titles by exam/skill |
| Flashcards | `content_bank_items` FLASHCARD | CM flashcards + lesson practice | PROTECTED only | 5–10 MASTER sets + refs | Demo courses lack flashcard UX | **HIGH** | Create MASTER flashcard sets (do not touch protected) |
| Certificate | derived completion cert | MyCourses / CertificateVerify | not seeded; depends on completion | 5–10 cert-ready learners | May stay empty if progress incomplete | **HIGH** | Ensure some enrollments COMPLETED + progress 100% + cert eligibility |
| Learning path | `learning_paths` / `learning_path_courses` | LearningPath pages | SYSTEM defaults only | Ordered path + learner progress | Path demo weak | **MEDIUM** | Attach demo courses in logical order; optional learner path state |
| Payment edge | `payment_orders` | Transaction history / support stories | 50 PAID only | Include FAILED→retry→PAID, CANCELLED, EXPIRED | No edge cases | **HIGH** | Add 10–15 non-happy payment stories |
| Proposal depth | `classroom_proposal_schedule_items` | Staff/Manager proposals | proposals only | schedule items + statuses diversity | Thin proposal UX | **MEDIUM** | Expand approved/rejected/pending with slots |
| Registration depth | `course_registration_requests` | Staff enrollment requests | 30 rows | PENDING/APPROVED/ASSIGNED/REJECTED + assignment | Status diversity unclear | **MEDIUM** | Align statuses with real enums + assigned section |
| Notification quality | `app_notifications` | Header badge + NotificationsPage | 100 rows / **2 titles** | Multi-event types + actionPath | Looks fake / weak deep-link | **HIGH** | Diversify events; set action_path; read_at consistency |
| Content mismatch risk | materials titles vs class program | Classroom materials | 40 materials | Title matches IELTS/TOEIC class | Titles rotate but may mismatch class exam | **MEDIUM** | Bind material bank by program category |
| Persona diversity | users + cross-table stories | All roles | accounts exist | Distinct learner/teacher personas | Learners largely homogeneous | **HIGH** | Tag personas and drive discussion/support/late HW/dispute stories |
| CM activity | online course drafts/published | CM dashboard/courses | 2 DRAFT + 9 PUBLISHED | Enough publish/moderation/analytics | Partial | **MEDIUM** | Keep; add discussion reports + flashcards |
| Staff activity | tickets/reg/proposals | Staff pages | partial | Full ops queues | Change request/tuition/announce missing | **HIGH** | Fill related gaps above |
| Manager activity | evaluations + proposals | Manager pages | evaluations=12 | + feedback aggregates + proposal reviews | Feedback missing | **HIGH** | Seed teacher_course_feedback |
| Admin activity | broadcasts/users | Admin pages | users only | Broadcasts (+ optional audit KEEP_EMPTY) | Broadcast empty | **HIGH** | Seed admin_broadcasts |
| Auth tokens | `auth_tokens` | — | 0 | KEEP_EMPTY | Intentional | **INTENTIONAL_EMPTY** | Keep empty |
| Audit logs | `system_audit_logs` | Admin audit | 0 | KEEP_EMPTY unless runtime | Intentional | **INTENTIONAL_EMPTY** | Keep empty (runtime-generated) |
| Mock attempts | `mock_test_attempts` | Mock tests | 0 | Optional | Intentional empty / protected bank | **INTENTIONAL_EMPTY** | Optional later; don’t fabricate protected keys |
| Google Meet tokens | `teacher_google_meet_connections` | Virtual class | PRESERVED | Preserve only | Must not overwrite alien | **INTENTIONAL_EMPTY** for MASTER tokens | Reference only |
| Placement/Mock content | `content_bank_items` | Placement/Mock | PROTECTED | Preserve | Must not regenerate | **INTENTIONAL_EMPTY** for MASTER regen | Keep protected seeders |
| E2 / Vocabulary | online courses protected | Learning | PROTECTED | Preserve | Correct | — | Reuse only |
| Matrix honesty | `01_TABLE_MATRIX` | Docs | Claims MASTER for missing domains | Match reality | Misleading coverage | **HIGH** | Update matrix after fills; distinguish planned vs seeded |

---

## Severity rollup

| Severity | Approx. gaps |
|---|---|
| CRITICAL | Discussion, reactions, reports, course reviews, teacher feedback, announcements, support messages |
| HIGH | Broadcasts, change requests, disputes, cart/wishlist, tuition proofs, flashcards MASTER, payment edge cases, notification diversity, persona diversity, staff/manager/admin queues |
| MEDIUM | Practice attempts, credentials, lesson notes, center library, learning path ordering, proposal/registration depth, material-category alignment |
| INTENTIONAL_EMPTY | auth_tokens, system_audit_logs, mock_test_attempts (optional), Google Meet OAuth tokens, protected content regen |

---

## Role UI risk (pre-fix)

| Role | Likely FAIL pages without fill |
|---|---|
| LEARNER | Forums, reactions, cart/wishlist, teacher feedback, announcements, support thread, disputes, certificate verify (if none completed) |
| TEACHER | Announcements empty-ish, disputes empty, change-request history thin |
| STAFF | Change requests empty, tuition proofs empty, announcements empty |
| MANAGER | Teacher feedback aggregates empty |
| CONTENT_MANAGER | Discussion moderation empty; flashcard bank thin for demo courses |
| ADMIN | Broadcasts empty |

PASS-leaning today (if import succeeds): classroom list/schedule/attendance/homework grading/online enrollments/payments happy-path.

---

## Next step (after this report)

1. Extend `ops/sheet-data/master/generate.mjs` with missing `world` domains + realistic Vietnamese content.  
2. Extend `MasterDemoDataSeeder` importers.  
3. Extend semantic validator for new rules.  
4. Regenerate workbook/JSON/validation report.  
5. Update TABLE_MATRIX to match seeded reality.  
6. Re-run compile/tests; produce final AV checklist.

**No preserved account overwrite. No protected placement/mock/E2/vocab content regeneration.**

---

## Post-fill status (2026-09-16)

Generator (`lib/world-gap-fill.mjs`) + `MasterDemoDataSeeder` importers landed. Regenerated JSON/xlsx with `ok: true`.

| Domain | Seeded rows | Status |
|---|---|---|
| discussions + replies | 50 + 123 | FILLED |
| reactions | 1099 | FILLED |
| discussion reports | 12 | FILLED |
| course reviews | 16 | FILLED (was under-target; bumped) |
| teacher feedback | 30 | FILLED |
| announcements | 35 | FILLED |
| broadcasts | 8 | FILLED |
| ticket messages | 81 | FILLED |
| change requests | 20 | FILLED |
| attendance disputes | 12 | FILLED |
| cart/wishlist | 35 | FILLED |
| tuition + proofs | 60 + 27 | FILLED |
| teacher credentials | 16 | FILLED |
| practice attempts | 50 | FILLED |
| flashcard sets MASTER | 8 | FILLED (lesson refs still thin) |
| center library | 25 | FILLED |
| lesson notes | 27 | FILLED |
| notifications diversity | 100 / 8 titles | FILLED |
| payment edge cases | +15 non-PAID stories | FILLED |
| completed 100% enrollments | 5 | FILLED |

### Remaining genuine gaps
- Live role-by-role browser UI audit **not executed** this session (`DATA_READY` only).
- Flashcard → lesson refs for demo courses still sparse.
- Learning-path course ordering still mostly SYSTEM defaults.
- Certificate issuance is runtime/derived — verify after import with master flag on.
- Explicit persona tags across accounts still implicit (stories exist via tables).
- Placement/Mock assessment banks remain PROTECTED (intentional).
