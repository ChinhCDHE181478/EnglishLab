# SRS Use Case Diagram Map

Danh sách này bám theo phần **Use Case Specification** trong file SRS. Các sequence được gom theo class diagram chung để dễ vẽ domain classes liên quan.

## CD01 - Account, Profile, Notification, Support

**Class chính:** `User`, `Role`, `AuthToken`, `UserProfile fields`, `AppNotification`, `SupportTicket`, `TicketReply`

| UC | Use Case | Sequence cần vẽ theo spec |
|---|---|---|
| UC-01 | Register Account | register + verify OTP + resend OTP |
| UC-03 | Login | email/password login + Google login + invalid credentials + account locked |
| UC-04 | Forgot Password | request reset OTP + verify OTP + reset password + resend OTP |
| UC-05a | View Profile | retrieve and display current profile |
| UC-05b | Update Profile | update profile + alternative change password |
| UC-06 | View Notifications | list notifications + mark shown notifications as read |
| UC-07 | Submit Support Ticket | create support ticket with status Open |
| UC-42 | Manage User Accounts | search/select user + lock/unlock + create internal user + update internal user |
| UC-43 | Manage System Notifications | create/broadcast notification + delete notification |
| UC-44 | Resolve Support Tickets | list open tickets + view ticket + reply and close |

## CD02 - Course Commerce and Online Learning

**Class chính:** `OnlineCourse`, `CourseCategory`, `WishlistItem`, `CartItem`, `PaymentOrder`, `PaymentTransaction`, `Enrollment`, `Lesson`, `LessonProgress`, `CourseMaterial`

| UC | Use Case | Sequence cần vẽ theo spec |
|---|---|---|
| UC-02 | View Public Courses | browse/search/filter course catalog + view course detail |
| UC-08 | Enroll in Course | validate course/enrollment + checkout/payment + create enrollment + grant access |
| UC-45 | Wishlist Courses | add wishlist + view wishlist + remove wishlist + add to cart from wishlist |
| UC-46 | Add Courses to Cart | add cart + view cart + remove item + move item to wishlist + continue to payment |
| UC-47 | Checkout | review order + apply coupon + PayOS payment + free order + cancel/failure/timeout |
| UC-48 | Access Online Learning Materials | open enrolled course + select unlocked lesson + render content + mark complete + open attached material |

## CD03 - Learner Classroom and Academic Report

**Class chính:** `Classroom`, `ClassroomSession`, `ClassroomEnrollment`, `ClassroomMaterial`, `Homework`, `HomeworkSubmission`, `AttendanceRecord`, `GradebookEntry`, `AcademicReport`

| UC | Use Case | Sequence cần vẽ theo spec |
|---|---|---|
| UC-09 | View Timetable | select week/month + load enrolled classroom sessions + view session detail |
| UC-10 | Join Online Meeting | open virtual session + join Lark link + link not ready |
| UC-11 | Access Learning Materials | open classroom material + render embedded viewer + material unavailable |
| UC-12 | Download Learning Materials | verify downloadable classroom material + trigger download + restricted online-course material |
| UC-13 | Submit Homework | upload file + validate file + submit + resubmit before deadline |
| UC-14 | View Academic Report | load grades/quiz scores/attendance/progress + calculate average/progress + gradebook not published |

## CD04 - Assessment and Learner Practice

**Class chính:** `Quiz`, `Question`, `AnswerOption`, `AnswerKey`, `Attempt`, `AnswerSubmission`, `WritingSubmission`, `SpeakingSubmission`, `AudioSubmission`, `Flashcard`, `FlashcardProgress`

| UC | Use Case | Sequence cần vẽ theo spec |
|---|---|---|
| UC-15 | Take Quiz | start quiz + countdown + answer + submit + auto-submit when timer expires |
| UC-16 | Take Placement Exam | start placement exam + sections + submit confirmation + score/result + auto-submit |
| UC-17 | Practice Writing Skill | display prompt + auto-save draft + submit for grading |
| UC-18 | Practice Listening Skill | load audio/questions + submit answers + score + timer auto-submit |
| UC-19 | Practice Speaking Skill | mic check + record + auto-stop at 5 minutes + playback + upload |
| UC-20 | Practice Reading Skill | display passage/questions + submit + evaluate + show correct/incorrect |
| UC-21 | Practice Flashcard | flip card + mark mastered/not mastered + study/match mode |

## CD05 - Syllabus, Online Course, Placement Exam Management

**Class chính:** `Syllabus`, `CoursebookFile`, `OnlineCourse`, `CourseModule`, `Lesson`, `PlacementExam`, `PlacementQuestion`, `QuestionOption`, `AnswerKey`

| UC | Use Case | Sequence cần vẽ theo spec |
|---|---|---|
| UC-32a | Create Syllabus | create syllabus |
| UC-32b | View Syllabus | view/search syllabus list + view detail |
| UC-32c | Update Syllabus | update syllabus |
| UC-32d | Delete Syllabus | delete syllabus + block if linked to active course/classroom |
| UC-33a | Create Online Course | create online course |
| UC-33b | View Online Courses | view/search course list + view detail/module structure |
| UC-33c | Update Online Course | update course + module/lesson structure |
| UC-33d | Delete Online Course | delete/archive course + block if active enrolled learners |
| UC-34a | Create Placement Exam | create question/exam + import questions from file |
| UC-34b | View Placement Exams | view/search/filter placement bank + view detail |
| UC-34c | Update Placement Exam | update exam/question + cancel edit + restrict if existing attempts affected |
| UC-34d | Delete Placement Exam | delete/deactivate exam/question + cancel deletion + preserve attempts |

## CD06 - Teacher Classroom Operations and Practice Content

**Class chính:** `TeacherAssignment`, `ClassroomSession`, `ClassroomAttendance`, `Syllabus`, `Homework`, `HomeworkSubmission`, `GradebookEntry`, `QuizPracticeContent`, `WritingPracticeContent`, `SpeakingPracticeContent`, `ReadingPracticeContent`, `ListeningPracticeContent`

| UC | Use Case | Sequence cần vẽ theo spec |
|---|---|---|
| UC-22 | View Teaching Schedule | select week/month/today + load assigned sessions |
| UC-23 | Manage Class Attendance | load roster + set attendance status + save/update attendance |
| UC-24 | View Syllabus | load linked syllabus + render viewer |
| UC-25 | Download Syllabus | prepare syllabus file + trigger download |
| UC-26 | Manage Homework | create homework + update homework + delete homework + grade/update score |
| UC-27 | Manage Quiz Practice Content | create quiz + update quiz + delete quiz |
| UC-28 | Manage Writing Practice Content | SRS section is duplicated as UC-27 quiz content; no distinct writing flow is present in extracted spec |
| UC-29 | Manage Speaking Practice Content | create speaking exercise + update speaking exercise + delete speaking exercise |
| UC-30 | Manage Reading Practice Content | create reading exercise + update reading exercise + delete reading exercise |
| UC-31 | Manage Listening Practice Content | create listening exercise + update listening exercise + delete listening exercise |

## CD07 - Training Management

**Class chính:** `TeacherProfile`, `TeacherPerformanceMetric`, `Classroom`, `TeacherAssignment`, `ClassroomEnrollment`

| UC | Use Case | Sequence cần vẽ theo spec |
|---|---|---|
| UC-35 | View Teacher Profiles | search teacher + view teacher profile with schedule/performance metrics |
| UC-36 | Manage Classrooms | create classroom + update classroom + delete classroom |
| UC-37 | Assign Teacher to Classroom | search/select teacher + check schedule conflict + save assignment |
| UC-38 | Assign Learner to Classroom | select learners + check capacity + add learners to roster |
| UC-39 | Evaluate Teacher Performance | aggregate metrics + enter feedback/rating + save evaluation |

## CD08 - Reports

**Class chính:** `OperationalReportData`, `RevenueAnalyticsData`, `RevenueExportFile`

| UC | Use Case | Sequence cần vẽ theo spec |
|---|---|---|
| UC-40 | View Operational Report | select filters/date range + query operational data + render charts/tables |
| UC-41 | View Revenue Analytic of Online Course | select filters/date range + calculate revenue metrics + compare periods + drill down + export report |
