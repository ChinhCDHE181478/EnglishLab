# Diagram Drawing Tracking Plan

Tracking này phản ánh file thực tế trong `C:\Users\Chinh\Downloads\sds-diagrams`. Phạm vi đã hoàn tất phần Mermaid source; các gap giữa SRS và code được giữ lại dưới dạng note trong diagram, không được xem là endpoint đã implement.

## Trạng thái

| Trạng thái | Ý nghĩa |
|---|---|
| Source đã vẽ | File `.mmd` hiện có |
| Không có SQ | Parent UC có trong SRS nhưng code không hỗ trợ đủ để tạo sequence code-aligned |
| Implemented | Main operation có boundary/code thực tế |
| Partial / SRS-only gap | Source đã vẽ nhưng phải ghi rõ hành vi SRS không có trong code |
| Render đã có | Có file SVG cùng basename với Mermaid source |

**Trạng thái chung:** **9/9 CD source đã vẽ**, **64/64 SQ source đã vẽ**, **73/73 SVG render**.

## Phase và CD numbering thực tế

| Phase | CD | Scope | Parent UC | SQ source |
|---|---|---|---:|---:|
| 1 | CD01 | Authentication and Common | 9 | 12 |
| 2 | CD02A/CD02B | Online Course, Commerce, and Learning | 9 | 10 |
| 3 | CD03 | Content Management | 3 | 12 |
| 4 | CD04 | Classroom Participation | 6 | 6 |
| 5 | CD05 | Classroom Practice | 7 | 7 |
| 6 | CD06 | Teacher Classroom Operations | 10 | 10 |
| 7 | CD07 | Training Management | 5 | 5 |
| 7 | CD08 | Reports | 2 | 2 |
| **Tổng** | **9 CD** |  | **51** | **64** |

Parent UC không bằng số SQ: UC-01, UC-03 và UC-04 được tách thành nhiều auth sequence; UC-47 có thêm PayOS webhook; UC-32/33/34 tách CRUD operation; UC-07/43/44 không có code-aligned SQ; UC-42 được bổ sung ở `SQ64`.

## Phase 1 - Authentication and Common

**CD:** `CD01-authentication-common.mmd` — **Source đã vẽ**.  
**Inventory:** auth/profile/notification/admin-user controllers; auth/token/social/profile/notification/admin-user/audit/role services; `User`, `Role`, `AuthToken`, `AppNotification`, `AuditLog` và repositories.

| UC | SQ source | Source | Code status |
|---|---|---|---|
| UC-01 Register Account | SQ01 Register Account; SQ02 Verify Email; SQ03 Resend Verification OTP | Đã vẽ | Implemented |
| UC-03 Login | SQ04 Login Email/Password; SQ05 Login Google; SQ06 Login Facebook | Đã vẽ | Implemented |
| UC-04 Forgot Password | SQ07 Forgot Password; SQ08 Reset Password | Đã vẽ | Implemented |
| UC-05 View/Update Profile | SQ09 View Profile; SQ10 Update Profile | Đã vẽ | Implemented |
| UC-06 View Notifications | SQ11 View Notifications | Đã vẽ | Implemented |
| UC-07 Submit Support Ticket | Không có | Không có SQ | **Unsupported** |
| UC-42 Manage User Accounts | SQ64 Manage User Accounts | **Đã vẽ/bổ sung** | **Implemented** |
| UC-43 Manage System Notifications | Không có | Không có SQ | **Unsupported:** không có admin broadcast/create/delete |
| UC-44 Resolve Support Tickets | Không có | Không có SQ | **Unsupported** |

## Phase 2 - Online Course, Commerce, and Learning

**CD:** `CD02A-course-commerce-enrollment.mmd`, `CD02B-online-learning-experience.mmd` — **Source và SVG đã vẽ**.  
**CD02A inventory:** public catalog/category, wishlist/cart, checkout/PayOS/discount, ownership, payment và enrollment; primary mapping SQ12-SQ17.  
**CD02B inventory:** enrolled modules/lessons/progress, progression assessment dependencies, discussion/reply/helpful/reaction/report/moderation, notes, access policy, notifications và audit; primary mapping SQ18, SQ61-SQ63. SQ17 được cross-reference qua shared enrollment/access classes.

| UC | SQ source | Source | Code status |
|---|---|---|---|
| UC-02 View Public Courses | SQ12 | Đã vẽ — CD02A | Implemented |
| UC-45 Wishlist Courses | SQ13 | Đã vẽ — CD02A | Implemented; cart-to-wishlist không có |
| UC-46 Add Courses to Cart | SQ14 | Đã vẽ — CD02A | Implemented |
| UC-47 Checkout | SQ15 Checkout; SQ16 PayOS Webhook | Đã vẽ — CD02A | Implemented |
| UC-08 Enroll in Course | SQ17 | Đã vẽ — CD02A primary/CD02B cross-reference | Implemented |
| UC-48 Access Online Learning Materials | SQ18 | Đã vẽ — CD02B | Partial: authorized material URL, không có download endpoint riêng |
| UC-49 Discuss in Course | **SQ61** | **Đã vẽ — CD02B** | Read/create/reply/reaction/helpful/resolve implemented; **thiếu edit/delete thread/reply** |
| UC-50 Report Discussion | **SQ62** | **Đã vẽ — CD02B** | Learner report **create-only**; moderation list/hide/dismiss implemented |
| UC-51 Take Note in Course | **SQ63** | **Đã vẽ — CD02B** | **List/create/update/delete**, không có **GET-one** |

## Phase 3 - Content Management

**CD:** `CD03-content-management.mmd` — **Source đã vẽ**.  
**Inventory:** course/curriculum/placement controllers and services; learning package/course/module/lesson, curriculum, assessment-bank và placement definition/attempt entities/repositories.

| Parent UC | SQ source | Source | Code status / SRS-only operations |
|---|---|---|---|
| UC-32 Manage Syllabus | SQ19-SQ22 Create/View/Update/Delete Syllabus | Đã vẽ | Curriculum CRUD có code; active course/classroom delete-blocking phải ghi SRS-only nếu không được enforce |
| UC-33 Manage Online Course | SQ23-SQ26 Create/View/Update/Delete Online Course | Đã vẽ | CRUD có code; delete là soft delete/archive, **không có SRS active-enrollment guard** |
| UC-34 Manage Placement Exam | SQ27-SQ30 Create/View/Update/Delete Placement Exam | Đã vẽ | View/update có code; **create/import và DELETE là SRS-only unsupported**; GET lazy-seed và PUT `active=false` là behavior thực tế |

## Phase 4 - Classroom Participation

**CD:** `CD04-learner-classroom-participation.mmd` — **Source đã vẽ**.  
**Inventory:** student classroom controller; offering/homework/gradebook/content/material-sync/storage/attendance services; classroom offering/enrollment/session/material/syllabus/announcement/homework/submission/attendance/gradebook entities/repositories.

| UC | SQ source | Source | Code status |
|---|---|---|---|
| UC-09 View Timetable | SQ31 | Đã vẽ | Implemented |
| UC-10 Join Online Meeting | SQ32 | Đã vẽ | Implemented |
| UC-11 Access Learning Materials | SQ33 | Đã vẽ | Implemented |
| UC-12 Download Learning Materials | SQ34 | Đã vẽ | Partial: trả `fileUrl`, không có backend download/downloadability check |
| UC-13 Submit Homework | SQ35 | Đã vẽ | Implemented |
| UC-14 View Academic Report | SQ36 | Đã vẽ | Partial: published gradebook entry + attendance endpoint riêng |

## Phase 5 - Classroom Practice

**CD:** `CD05-classroom-practice-assessment.mmd` — **Source đã vẽ**.  
**Inventory:** quiz/assessment/placement/flashcard/course controllers and services; progression/access policies; quiz, assessment/rubric/submission, placement-attempt, course và vocabulary-progress entities/repositories.

| UC | SQ source | Source | Code status / SRS-only operations |
|---|---|---|---|
| UC-15 Take Quiz | SQ37 | Đã vẽ | Submit implemented; **countdown/forced auto-submit unsupported** |
| UC-16 Take Placement Exam | SQ38 | Đã vẽ | Read/submit implemented; timer/draft client-side, không có start/autosave API |
| UC-17 Practice Writing | SQ39 | Đã vẽ | Implemented; **resumable periodic backend autosave unsupported** |
| UC-18 Practice Listening | SQ40 | Đã vẽ | Implemented; timer/auto-submit client-side |
| UC-19 Practice Speaking | SQ41 | Đã vẽ | Implemented; **SRS five-minute cap không được backend enforce** |
| UC-20 Practice Reading | SQ42 | Đã vẽ | Implemented; timer/auto-submit client-side |
| UC-21 Practice Flashcard | SQ43 | Đã vẽ | Implemented qua read boundary và vocabulary-progress boundary |

## Phase 6 - Teacher Classroom Operations

**CD:** `CD06-teacher-classroom-operations.mmd` — **Source đã vẽ**.  
**Inventory:** teacher-classroom, classroom-quiz, exercise-bank controllers; offering/attendance/homework/content/quiz/exercise-bank services; offering/assignment/session/attendance/homework/submission/material/quiz/question/exercise-bank entities/repositories.

| UC | SQ source | Source | Code status / SRS-only operations |
|---|---|---|---|
| UC-22 View Teaching Schedule | SQ44 | Đã vẽ | Assigned + sessions; aggregate schedule/date filter không có |
| UC-23 Manage Class Attendance | SQ45 | Đã vẽ | Implemented với ownership/lock limitations |
| UC-24 View Syllabus | SQ46 | Đã vẽ | **Dedicated teacher syllabus endpoint unsupported** |
| UC-25 Download Syllabus | SQ47 | Đã vẽ | **SRS-only unsupported:** không có generation/download endpoint |
| UC-26 Manage Homework | SQ48 | Đã vẽ | Implemented |
| UC-27 Manage Quiz Practice Content | SQ49 | Đã vẽ | List/create/open/close/delete; **content update unsupported** |
| UC-28 Manage Writing Content | SQ50 | Đã vẽ | **Teacher operation unsupported; content-manager-only CRUD** |
| UC-29 Manage Speaking Content | SQ51 | Đã vẽ | **Teacher operation unsupported; content-manager-only CRUD** |
| UC-30 Manage Reading Content | SQ52 | Đã vẽ | **Teacher operation unsupported; content-manager-only CRUD** |
| UC-31 Manage Listening Content | SQ53 | Đã vẽ | **Teacher operation unsupported; không có audio storage/upload** |

## Phase 7 - Training Management

**CD:** `CD07-training-management.mmd` — **Source đã vẽ**.  
**Inventory:** training-manager classroom controller, offering/conflict services; user/offering/assignment/enrollment/session/tuition/room entities/repositories.

| UC | SQ source | Source | Code status / SRS-only operations |
|---|---|---|---|
| UC-35 View Teacher Profiles | SQ54 | Đã vẽ | Picker only; **profile detail/search/paging/history unsupported** |
| UC-36 Manage Classrooms | SQ55 | Đã vẽ | List/get/create/update/publish/close; **delete/archive/filter/paging unsupported** |
| UC-37 Assign Teacher | SQ56 | Đã vẽ | Implemented assign/replace |
| UC-38 Assign Learner | SQ57 | Đã vẽ | Implemented |
| UC-39 Evaluate Teacher Performance | SQ58 | Đã vẽ | **SRS-only unsupported:** không có evaluation subsystem |

## Phase 7 - Reports

**CD:** `CD08-reports.mmd` — **Source đã vẽ**.  
**Inventory:** training-manager dashboard/content-manager revenue controllers, ops/payment services, offering/enrollment/change-request/session/payment repositories/entities và response models.

| UC | SQ source | Source | Code status / SRS-only operations |
|---|---|---|---|
| UC-40 View Operational Report | SQ59 | Đã vẽ | Fixed dashboard; **date/filter/detail/export unsupported** |
| UC-41 View Revenue Analytics | SQ60 | Đã vẽ | Fixed analytics; **date/course filter, period compare, detail/export unsupported** |

## Tổng hợp cuối

| Item | Final count |
|---|---:|
| Parent UC | **51** |
| Sequence source | **64** |
| Class source | **9** |
| Mermaid source tổng cộng | **73** |
| Source chưa vẽ | **0** |
| SVG render hiện có | **73** |

Không dùng `58` làm tổng UC: đó là số dòng flow khi tách `UC-05a/b` và các CRUD `UC-32a-d`, `UC-33a-d`, `UC-34a-d`, không phải số parent UC.