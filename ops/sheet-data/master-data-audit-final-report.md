# MASTER DEMO DATASET — Final Audit Report (AU/AV)

**Reference:** `2026-09-16` · **Seed:** `20260916` · **Window:** Jun–Oct 2026  
**Gap report:** `ops/sheet-data/master-data-gap-report.md`  
**Validation:** `ops/sheet-data/master-data-validation-report.json` → `ok: true`, `issueCount: 0`  
**Compile:** `MasterDemoSemanticValidatorTest` PASS · `mvn -DskipTests compile` PASS  
**Live browser UI:** **NOT RUN** this session — role marks below are **DATA_READY** assessments from schema/API/JSON coverage, not PASS from clicked UI.

---

## 1. Tables / entities audited

| Scope | Count |
|---|---|
| JPA `@Entity` | 63 |
| Non-JPA facades | 8 |
| TABLE_MATRIX rows | 41+ |
| MASTER `world` domains after fill | 36 |

## 2. Frontend features audited

~75 routed pages across ADMIN / CONTENT_MANAGER / STAFF / MANAGER / TEACHER / LEARNER (see route inventory). Focus: badges, counters, moderation, discussion, cart/wishlist, support threads, tuition proofs, change requests, disputes, broadcasts, teacher feedback/performance, flashcards, materials.

## 3–6. Gaps by severity (post-fill)

| Severity | Count | Notes |
|---|---|---|
| **CRITICAL** | **0 open** | Discussion/reaction/report/review/feedback/announcement/ticket-messages filled |
| **HIGH** | **2 residual** | (1) Live UI not verified; (2) demo flashcard↔lesson refs thin |
| **MEDIUM** | **4** | Learning-path ordering; certificate runtime verify; persona tagging explicitness; proposal schedule-item depth |
| **INTENTIONAL_EMPTY** | **5** | `auth_tokens`, `system_audit_logs`, mock attempts optional, Google Meet OAuth tokens, protected placement/mock regen |

## 7. Discussion coverage

PASS (data): 50 threads + 123 replies; realistic VN Q&A; Jun–Sep spread; OPEN/RESOLVED mix; tied to PUBLISHED demo courses.

## 8. Reaction / emotion coverage

PASS (data): 1099 reactions; UNIQUE (post,user); types LIKE/LOVE/CARE/…; natural uneven counts.

## 9. Report / moderation coverage

PASS (data): 12 reports; PENDING/DISMISSED/ACTION_TAKEN; realistic reasons; CM reviewer + chronology.

## 10. Course review / rating coverage

PASS (data): 16 reviews on eligible enrollments; ratings 3–5; meaningful VN comments; not every enrollment reviews.

## 11. Teacher feedback coverage

PASS (data): 30 `teacher_course_feedback` rows; multi-score + pace + written strengths/improvements; enrolled learners only.

## 12. Teacher performance coverage

PASS (data): 12 manager evaluations + 30 learner feedbacks. Preserved teacher `alien1062004@gmail.com` not overwritten.

## 13. Support lifecycle coverage

PASS (data): 28 tickets + 81 messages; learner↔staff chains; RESOLVED vs OPEN; subjects realistic.

## 14. Notification coverage

PASS (data): 100 notifications; 8 event titles; `actionPath` set; `readAt` only when read.

## 15. Classroom request / dispute coverage

PASS (data): 20 change requests (multi-type, multi-status); 12 attendance disputes on ABSENT rows.

## 16. Content / material / flashcard coverage

PARTIAL→PASS-leaning: 40 class materials + 25 center library + 8 MASTER flashcard sets (8–12 cards). **Gap:** lesson flashcard refs for demo courses still sparse. Protected banks untouched.

## 17. Payment edge-case coverage

PASS (data): 65 orders — PAID + FAILED/retry + CANCELLED + EXPIRED; tuition 60 payments + 27 proofs PENDING/CONFIRMED/REJECTED.

## 18. Certificate / learning-path coverage

PARTIAL: 5 enrollments COMPLETED @ 100% for cert eligibility. Learning paths remain SYSTEM defaults (ordering not fully MASTER-owned). Certificate issuance needs post-import runtime check.

## 19. Admin / manager / staff activity coverage

PASS (data): broadcasts 8; proposals 20; registrations 30; change requests 20; tickets+messages; tuition proofs; teacher evaluations/feedback; CM discussion reports + flashcards + center library.

## 20. Preserved account verification

| Account | Status |
|---|---|
| `0386852628z@gmail.com` | Reference-only in accounts; seeder lookup-only; no progress/password overwrite |
| `alien1062004@gmail.com` | Reference-only; Google Meet / credentials not MASTER-owned |

## 21. Semantic validator result

`ok: true`, `issueCount: 0` after regenerate. Extended Java `MasterDemoSemanticValidator` covers gap domains when present. Unit test PASS.

## 22. UI role-by-role audit (DATA_READY — live UI NOT RUN)

Legend: **DATA_READY** = dataset+API expected to populate; **EMPTY_INTENTIONAL**; **PARTIAL**; **LIVE_UI_PENDING**.

### ADMIN
| Page | Mark |
|---|---|
| Dashboard | DATA_READY |
| Users | DATA_READY |
| Broadcasts | DATA_READY |
| Monitoring / Backups / Settings | EMPTY_INTENTIONAL / SYSTEM |
| Audit logs | EMPTY_INTENTIONAL |
| Inherited manager/staff/CM | DATA_READY (see below) |

### CONTENT_MANAGER
| Page | Mark |
|---|---|
| Dashboard / Courses / ILC / Categories / Discounts | DATA_READY |
| Materials / Flashcards | DATA_READY (flashcard lesson wiring PARTIAL) |
| Listening/Reading/Writing/Speaking/Placement/Mock banks | PROTECTED / PARTIAL |
| Discussion moderation | DATA_READY |
| Learning paths | PARTIAL (SYSTEM defaults) |
| Publication / Analytics | PARTIAL |

### STAFF
| Page | Mark |
|---|---|
| Dashboard | DATA_READY |
| Classrooms / detail | DATA_READY |
| Enrollment requests | DATA_READY |
| Classroom proposals | DATA_READY |
| Change requests | DATA_READY |
| Infrastructure (rooms) | DATA_READY |
| Teachers / credentials | DATA_READY |
| Support tickets (threads) | DATA_READY |
| Tuition proofs (class tabs) | DATA_READY |

### MANAGER
| Page | Mark |
|---|---|
| Proposal review | DATA_READY |
| Online enrollments | DATA_READY |
| Teacher performance | DATA_READY |
| Support tickets | DATA_READY |

### TEACHER
| Page | Mark |
|---|---|
| Dashboard / Schedule / Classrooms / Sessions | DATA_READY |
| Announcements / Homework / Attendance | DATA_READY |
| Disputes | DATA_READY |
| Change requests | DATA_READY |
| Professional profile / credentials | DATA_READY |
| Google Meet (preserved teacher) | PRESERVED — do not overwrite |

### LEARNER
| Page | Mark |
|---|---|
| Courses / My courses / Learn / Discussion | DATA_READY |
| Cart / Wishlist | DATA_READY |
| Classrooms / Homework / Practice / Schedule | DATA_READY |
| Support (conversation) | DATA_READY |
| Notifications | DATA_READY |
| Teacher feedback | DATA_READY |
| Attendance dispute | DATA_READY |
| Certificates | PARTIAL (5 completed@100%; runtime issue) |
| Learning path personal | PARTIAL |

## 23. Remaining genuine gaps

1. **Live browser role audit** still required before claiming UI PASS.  
2. Wire **MASTER flashcard sets → demo lesson refs**.  
3. Confirm **certificate** generation after master import for the 5 completed enrollments.  
4. Optionally enrich **learning_path_courses** order for demo catalog.  
5. Keep **INTENTIONAL_EMPTY** system tables empty.  
6. Do not claim coverage solely from row counts — verify counters (unread, pending reports, pending proofs, open tickets) in UI after import with `app.seed.master.enabled=true`.

---

## Enable import (ops)

```
app.seed.master.enabled=true
app.seed.master.cleanup-before-import=true   # MASTER-only cleanup
```

Password for MASTER accounts only: `Password123!`  
Never reset preserved Gmail passwords.
