package fu.sap490.g23.backend.seed;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.*;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.PackageType;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import fu.sap490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Seeder tạo dữ liệu test đầy đủ cho account chinhcdhe181478@fpt.edu.vn và các học viên cùng lớp.
 * Bao gồm 1 lớp học IELTS duy nhất với tất cả trạng thái có thể:
 * - Buổi học: COMPLETED, IN_PROGRESS (hôm nay + join meeting), SCHEDULED
 * - Bài tập: QUÁ HẠN, hạn hôm nay, ngày mai, ngày kia, đã nộp, đã chấm điểm (nhiều đầu điểm)
 * - Học viên: Chinh + 3 học viên khác cùng tham gia lớp
 * - Điểm danh: có mặt, vắng, đến muộn
 * - Bảng điểm: đã publish với đầy đủ đầu điểm và điểm trung bình
 * - Tài liệu: PDF, slide, link
 * - Thông báo: nhiều loại
 * - Học phí: lịch sử thanh toán
 *
 * Bật seeder: APP_SEED_CHINH_TEST_ENABLED=true
 */
@Slf4j
@Component
@Order(220)
@RequiredArgsConstructor
public class ChinhTestClassroomSeeder implements CommandLineRunner {

    // ── Identities ──────────────────────────────────────────────────────────
    static final String LEARNER_EMAIL = "chinhcdhe181478@fpt.edu.vn";
    private static final String TEACHER_EMAIL = "classroom.teacher1@englishlab.vn";
    private static final String PACKAGE_SLUG = "ielts-intensive-chinh-test-v1";
    private static final String CLASS_TITLE = "IELTS Intensive 6.5+ – Lớp Test Chinh";

    // ── Repositories ─────────────────────────────────────────────────────────
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final DemoLearnerOnboardingSupport onboardingSupport;
    private final PackageTypeRepository packageTypeRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomAttendanceRepository attendanceRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository submissionRepository;
    private final ClassroomGradebookEntryRepository gradebookRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final ClassroomAnnouncementRepository announcementRepository;
    private final ClassroomSyllabusItemRepository syllabusRepository;
    private final ClassroomTuitionPaymentRepository tuitionPaymentRepository;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.seed.chinh-test.enabled:true}")
    private boolean chinhTestEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled || !chinhTestEnabled) {
            return;
        }

        log.info("[ChinhTestSeeder] Bắt đầu seed / cập nhật dữ liệu test cho {}...", LEARNER_EMAIL);

        PackageType classroomType = packageTypeRepository.findByCode(PackageTypeCode.CLASSROOM)
                .orElseThrow(() -> new IllegalStateException("CLASSROOM package type chưa tồn tại. Hãy chạy OnlineCourseDataSeeder trước."));

        User learner = ensureUser(LEARNER_EMAIL, "Chinh CDHE181478", RoleEnum.LEARNER);
        User teacher = ensureUser(TEACHER_EMAIL, "Nguyễn Văn Teacher", RoleEnum.TEACHER);

        Optional<ClassroomOffering> existingOffering = offeringRepository.findByLearningPackageSlug(PACKAGE_SLUG);
        ClassroomOffering offering;
        if (existingOffering.isPresent()) {
            offering = existingOffering.get();
            log.info("[ChinhTestSeeder] Lớp học đã tồn tại (ID: {}), tiến hành đồng bộ học viên và điểm số mới...", offering.getId());
        } else {
            offering = createOffering(classroomType, teacher);
        }

        ensureTeacherAssignment(offering, teacher);
        ClassroomEnrollment enrollment = ensureEnrollment(offering, learner, teacher);
        ensureTuitionPayments(enrollment, teacher);

        // Tạo thêm 3 học sinh cùng tham gia lớp
        ensureAdditionalStudents(offering, teacher);

        List<ClassroomSession> sessions;
        if (existingOffering.isPresent()) {
            sessions = sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId());
            if (sessions.isEmpty()) {
                sessions = createSessions(offering, teacher);
                createSyllabus(offering, sessions);
                createAttendance(sessions, learner, teacher);
                createMaterials(offering, sessions, teacher);
                createAnnouncements(offering, teacher);
            }
        } else {
            sessions = createSessions(offering, teacher);
            createSyllabus(offering, sessions);
            createAttendance(sessions, learner, teacher);
            createMaterials(offering, sessions, teacher);
            createAnnouncements(offering, teacher);
        }

        createHomework(offering, sessions, teacher, learner);
        createGradebook(offering, learner, teacher);

        log.info("[ChinhTestSeeder] ✅ Seed/Cập nhật hoàn tất! Email chính: {} | Mật khẩu: Password123!", LEARNER_EMAIL);
    }

    // ── Offering ──────────────────────────────────────────────────────────────

    private ClassroomOffering createOffering(PackageType classroomType, User teacher) {
        LearningPackage pkg = learningPackageRepository.save(LearningPackage.builder()
                .packageType(classroomType)
                .title(CLASS_TITLE)
                .slug(PACKAGE_SLUG)
                .shortDescription("Lớp học IELTS 6.5+ dành để test đầy đủ tính năng.")
                .description("Lớp test cho chinh: có bài tập quá hạn, bài mới, buổi học đang diễn ra và sắp tới. Bao gồm điểm danh, bảng điểm, học phí, tài liệu, thông báo.")
                .targetScore("IELTS 6.5+")
                .duration("8 tuần")
                .studyMode("Virtual · Google Meet")
                .price(BigDecimal.valueOf(5_200_000))
                .salePrice(BigDecimal.valueOf(4_690_000))
                .status(PackageStatus.PUBLISHED)
                .featured(true)
                .createdBy(teacher)
                .build());

        return offeringRepository.save(ClassroomOffering.builder()
                .learningPackage(pkg)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .status(ClassroomOfferingStatus.ACTIVE)
                .entryLevel("IELTS 5.0+ hoặc CEFR B1")
                .targetOutcome("Đạt IELTS 6.5+; thành thạo cả 4 kỹ năng; có chiến lược thi thực tế.")
                .maxCapacity(20)
                .startDate(LocalDate.now().minusWeeks(5))
                .endDate(LocalDate.now().plusWeeks(3))
                .primaryTeacher(teacher)
                .larkMeetingStatus(LarkMeetingStatus.NOT_CREATED)
                .recordingVisible(false)
                .syllabusSummary("8 buổi bám sát 4 kỹ năng IELTS: Listening, Reading, Writing, Speaking. Mỗi buổi gồm lý thuyết + luyện tập + feedback cá nhân.")
                .programOutcomes("Đạt band 6.5 IELTS tổng. Viết task 1 và task 2 đạt band 6.0+. Nói liên tục 2 phút không dừng.")
                .teacherGuide("Mỗi buổi: review 10 phút + dạy chiến lược 30 phút + luyện tập có hướng dẫn 40 phút + Q&A 10 phút.")
                .interactionActivities("Mock test, pair speaking, error log review, timed writing, peer feedback.")
                .build());
    }

    // ── Teacher assignment ────────────────────────────────────────────────────

    private void ensureTeacherAssignment(ClassroomOffering offering, User teacher) {
        teacherAssignmentRepository.findByClassroomOfferingIdAndTeacherId(offering.getId(), teacher.getId())
                .orElseGet(() -> teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                        .classroomOffering(offering)
                        .teacher(teacher)
                        .role(ClassroomTeacherRole.PRIMARY)
                        .effectiveFrom(offering.getStartDate())
                        .reason("Giáo viên phụ trách lớp IELTS test Chinh")
                        .build()));
    }

    // ── Enrollment & tuition ──────────────────────────────────────────────────

    private ClassroomEnrollment ensureEnrollment(ClassroomOffering offering, User learner, User teacher) {
        return enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offering.getId())
                .orElseGet(() -> enrollmentRepository.save(ClassroomEnrollment.builder()
                        .student(learner)
                        .classroomOffering(offering)
                        .status(ClassroomEnrollmentStatus.ENROLLED)
                        .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                        .tuitionAmountDue(BigDecimal.valueOf(4_690_000))
                        .tuitionAmountPaid(BigDecimal.valueOf(4_690_000))
                        .tuitionDepositPaid(BigDecimal.valueOf(1_000_000))
                        .tuitionSettlementType(TuitionSettlementType.NONE)
                        .enrolledAt(LocalDateTime.now().minusWeeks(5))
                        .assignedAt(LocalDateTime.now().minusWeeks(5))
                        .assignedBy(teacher)
                        .confirmedAt(LocalDateTime.now().minusWeeks(5))
                        .confirmedBy(teacher)
                        .assignmentNote("Học viên tham gia lớp: " + learner.getEmail())
                        .build()));
    }

    private void ensureAdditionalStudents(ClassroomOffering offering, User teacher) {
        String[][] studentsData = {
                {"student.test1@englishlab.vn", "Nguyễn Hoàng Nam"},
                {"student.test2@englishlab.vn", "Trần Thị Mai"},
                {"student.test3@englishlab.vn", "Lê Minh Khang"}
        };
        for (String[] data : studentsData) {
            User student = ensureUser(data[0], data[1], RoleEnum.LEARNER);
            ensureEnrollment(offering, student, teacher);
        }
    }

    private void ensureTuitionPayments(ClassroomEnrollment enrollment, User teacher) {
        if (!tuitionPaymentRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId()).isEmpty()) {
            return;
        }
        // Đặt cọc lần 1 (5 tuần trước)
        tuitionPaymentRepository.save(ClassroomTuitionPayment.builder()
                .enrollment(enrollment)
                .amount(BigDecimal.valueOf(1_000_000))
                .paymentKind(TuitionPaymentKind.DEPOSIT)
                .note("Đặt cọc giữ chỗ – chuyển khoản Vietcombank")
                .recordedBy(teacher)
                .build());
        // Thanh toán phần còn lại (3 tuần trước)
        tuitionPaymentRepository.save(ClassroomTuitionPayment.builder()
                .enrollment(enrollment)
                .amount(BigDecimal.valueOf(3_690_000))
                .paymentKind(TuitionPaymentKind.PARTIAL)
                .note("Thanh toán phần còn lại – MoMo QR")
                .recordedBy(teacher)
                .build());
    }

    // ── Sessions ──────────────────────────────────────────────────────────────

    /**
     * Tạo 8 buổi học với các trạng thái:
     *  S1-S4: COMPLETED (4 buổi đã qua, từ 5 tuần → 2 tuần trước)
     *  S5: OPEN (hôm nay 19:30–21:00 — có thể Join Meeting)
     *  S6: SCHEDULED (ngày mai)
     *  S7: SCHEDULED (3 ngày nữa)
     *  S8: SCHEDULED (1 tuần nữa)
     */
    private List<ClassroomSession> createSessions(ClassroomOffering offering, User teacher) {
        LocalDate today = LocalDate.now();
        ClassroomSession s1 = saveSession(offering, teacher, today.minusWeeks(5), 19, 21, ClassroomSessionStatus.COMPLETED,
                "Buổi 1: Tổng quan IELTS & Listening Section 1-2");
        ClassroomSession s2 = saveSession(offering, teacher, today.minusWeeks(4), 19, 21, ClassroomSessionStatus.COMPLETED,
                "Buổi 2: Listening Section 3-4 & chiến thuật dự đoán đáp án");
        ClassroomSession s3 = saveSession(offering, teacher, today.minusWeeks(3), 19, 21, ClassroomSessionStatus.COMPLETED,
                "Buổi 3: Reading – Matching Headings & True/False/Not Given");
        ClassroomSession s4 = saveSession(offering, teacher, today.minusWeeks(2), 19, 21, ClassroomSessionStatus.COMPLETED,
                "Buổi 4: Writing Task 1 – Bar chart & Line graph");
        ClassroomSession s5 = saveSession(offering, teacher, today, 19, 21, ClassroomSessionStatus.OPEN,
                "Buổi 5: Writing Task 2 – Opinion essay (Đang diễn ra – có thể Join)");
        ClassroomSession s6 = saveSession(offering, teacher, today.plusDays(1), 19, 21, ClassroomSessionStatus.SCHEDULED,
                "Buổi 6: Speaking Part 1 & 2 – chiến thuật mở rộng ý");
        ClassroomSession s7 = saveSession(offering, teacher, today.plusDays(3), 19, 21, ClassroomSessionStatus.SCHEDULED,
                "Buổi 7: Speaking Part 3 – thảo luận chủ đề xã hội");
        ClassroomSession s8 = saveSession(offering, teacher, today.plusWeeks(1), 19, 21, ClassroomSessionStatus.SCHEDULED,
                "Buổi 8: Mock test toàn phần & tổng kết khóa học");
        return List.of(s1, s2, s3, s4, s5, s6, s7, s8);
    }

    private ClassroomSession saveSession(ClassroomOffering offering, User teacher,
                                          LocalDate date, int startHour, int endHour,
                                          ClassroomSessionStatus status, String content) {
        return sessionRepository.save(ClassroomSession.builder()
                .classroomOffering(offering)
                .sessionDate(date)
                .startTime(LocalTime.of(startHour, 30))
                .endTime(LocalTime.of(endHour, 0))
                .teacher(teacher)
                .status(status)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingStatus(LarkMeetingStatus.NOT_CREATED)
                .larkSyncStatus("PENDING")
                .recordingVisible(false)
                .sessionContent(content)
                .note("Lớp test chinhcdhe181478 – buổi " + date)
                .build());
    }

    // ── Syllabus ──────────────────────────────────────────────────────────────

    private void createSyllabus(ClassroomOffering offering, List<ClassroomSession> sessions) {
        String[][] syllabusData = {
                {"Buổi 1 – Listening Section 1–2", "Nghe hội thoại ngắn, điền form và ghi chú.", "Warm-up nghe thụ động; chiến thuật đọc câu hỏi trước; guided practice 2 đề; sửa lỗi.", "Luyện thêm 1 đề Section 1-2 tại nhà."},
                {"Buổi 2 – Listening Section 3–4", "Nghe thảo luận học thuật và bài giảng.", "Phân tích cấu trúc Section 3-4; chiến thuật dự đoán; luyện 2 đề.", "Tóm tắt 5 lỗi phổ biến nhất của cá nhân."},
                {"Buổi 3 – Reading Matching & T/F/NG", "Chiến thuật làm nhanh dạng Matching Headings và True/False/Not Given.", "Scanning kỹ thuật; không đọc toàn văn; luyện 1 passage dài.", "Hoàn thành bài Reading Section 1 nộp file."},
                {"Buổi 4 – Writing Task 1", "Viết mô tả biểu đồ Bar chart và Line graph.", "Cấu trúc 4 đoạn; từ vựng mô tả xu hướng; luyện viết tại lớp 20 phút.", "Viết 1 bài Task 1 hoàn chỉnh (150 từ) nộp online."},
                {"Buổi 5 – Writing Task 2 (Hôm nay)", "Opinion essay – lập luận và ví dụ minh chứng.", "Cấu trúc 5 đoạn; cách mở bài paraphrase; luyện outline 15 phút.", "Viết bài Task 2 hoàn chỉnh (250 từ) – deadline ngày mai."},
                {"Buổi 6 – Speaking Part 1 & 2", "Mở rộng câu trả lời Part 1; kỹ thuật cue card Part 2.", "Role-play hỏi đáp; chiến thuật PREP; luyện cue card 4 chủ đề.", "Ghi âm 1 cue card và nộp link."},
                {"Buổi 7 – Speaking Part 3", "Thảo luận chủ đề xã hội – giáo dục, công nghệ, môi trường.", "Cách đưa ra quan điểm có căn cứ; từ nối học thuật; luyện pair speaking.", "Ghi âm trả lời 3 câu Part 3 theo chủ đề môi trường."},
                {"Buổi 8 – Mock Test & Tổng kết", "Làm toàn bộ 4 phần IELTS trong điều kiện thi thực tế.", "Full test 2h45m; chữa bài; phân tích điểm yếu từng kỹ năng.", "Hoàn thiện error log cá nhân và nộp trước tốt nghiệp."}
        };

        for (int i = 0; i < syllabusData.length; i++) {
            String[] row = syllabusData[i];
            ClassroomSession session = i < sessions.size() ? sessions.get(i) : null;
            syllabusRepository.save(ClassroomSyllabusItem.builder()
                    .classroomOffering(offering)
                    .title(row[0])
                    .description(row[1])
                    .displayOrder(i + 1)
                    .sessionNumber(i + 1)
                    .linkedSessionId(session != null ? session.getId() : null)
                    .sessionPlan(row[2])
                    .homeworkNotes(row[3])
                    .quizNotes("Quiz ôn bài nằm trong mục Bài tập của buổi học.")
                    .teacherNotes("Review error log đầu buổi kế tiếp; gọi 2-3 học viên trình bày.")
                    .reviewStatus(ContentReviewStatus.APPROVED)
                    .status("PUBLISHED")
                    .build());
        }
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    /**
     * Điểm danh cho 4 buổi đã hoàn thành + 1 buổi đang diễn ra:
     *  S1: CÓ MẶT, đúng giờ
     *  S2: CÓ MẶT, đúng giờ
     *  S3: ĐẾN MUỘN 15 phút
     *  S4: VẮNG MẶT (có lý do)
     *  S5: CÓ MẶT (buổi hôm nay, vừa join)
     */
    private void createAttendance(List<ClassroomSession> sessions, User learner, User teacher) {
        if (sessions.isEmpty()) return;
        LocalDate today = LocalDate.now();

        if (sessions.size() > 0) {
            saveAttendance(sessions.get(0), learner, teacher, ClassroomAttendanceStatus.PRESENT,
                    sessions.get(0).getSessionDate().atTime(19, 32), sessions.get(0).getSessionDate().atTime(21, 0), 88, null);
        }
        if (sessions.size() > 1) {
            saveAttendance(sessions.get(1), learner, teacher, ClassroomAttendanceStatus.PRESENT,
                    sessions.get(1).getSessionDate().atTime(19, 28), sessions.get(1).getSessionDate().atTime(21, 0), 92, null);
        }
        if (sessions.size() > 2) {
            saveAttendance(sessions.get(2), learner, teacher, ClassroomAttendanceStatus.LATE,
                    sessions.get(2).getSessionDate().atTime(19, 45), sessions.get(2).getSessionDate().atTime(21, 0), 75, "Đến trễ 15 phút do kẹt xe");
        }
        if (sessions.size() > 3) {
            saveAttendance(sessions.get(3), learner, teacher, ClassroomAttendanceStatus.ABSENT,
                    null, null, null, "Vắng có phép – báo giáo viên trước qua Zalo");
        }
        if (sessions.size() > 4) {
            saveAttendance(sessions.get(4), learner, teacher, ClassroomAttendanceStatus.PRESENT,
                    today.atTime(19, 33), null, null, "Tham gia buổi hôm nay qua Google Meet");
        }
    }

    private void saveAttendance(ClassroomSession session, User learner, User teacher,
                                 ClassroomAttendanceStatus status,
                                 LocalDateTime joinTime, LocalDateTime leaveTime,
                                 Integer durationMinutes, String note) {
        if (attendanceRepository.findBySessionIdAndStudentId(session.getId(), learner.getId()).isPresent()) {
            return;
        }
        ClassroomAttendance.ClassroomAttendanceBuilder builder = ClassroomAttendance.builder()
                .session(session)
                .student(learner)
                .status(status)
                .teacherConfirmed(true)
                .markedBy(teacher)
                .note(note);
        if (joinTime != null) builder.joinTime(joinTime);
        if (leaveTime != null) builder.leaveTime(leaveTime);
        if (durationMinutes != null) builder.durationMinutes(durationMinutes);
        attendanceRepository.save(builder.build());
    }

    // ── Materials ─────────────────────────────────────────────────────────────

    private void createMaterials(ClassroomOffering offering, List<ClassroomSession> sessions, User teacher) {
        // Tài liệu cấp lớp (không gắn buổi cụ thể)
        saveMaterial(offering, null, teacher, "Tài liệu tổng quan IELTS 2024",
                "https://cdn.englishlab.vn/test/ielts-overview-2024.pdf", "PDF",
                "Hướng dẫn tổng quan cấu trúc đề thi IELTS Academic 2024", "DOCUMENT");
        saveMaterial(offering, null, teacher, "Bảng từ vựng IELTS theo chủ đề",
                "https://cdn.englishlab.vn/test/ielts-vocab-topics.pdf", "PDF",
                "500 từ vựng IELTS chia theo 10 chủ đề: environment, technology, education, health...", "DOCUMENT");
        saveMaterial(offering, null, teacher, "Template lập Error Log cá nhân",
                "https://cdn.englishlab.vn/test/error-log-template.xlsx", "XLSX",
                "File Excel để ghi lại lỗi sai theo từng kỹ năng và buổi học", "TEMPLATE");

        // Tài liệu từng buổi
        if (sessions.size() > 0) {
            saveMaterial(offering, sessions.get(0), teacher, "Slide Buổi 1 – Listening Overview",
                    "https://cdn.englishlab.vn/test/session1-listening.pdf", "PDF",
                    "Slide giảng dạy buổi 1 – Listening Part 1 & 2", "SLIDE");
        }
        if (sessions.size() > 1) {
            saveMaterial(offering, sessions.get(1), teacher, "Đề luyện Listening Section 3-4",
                    "https://cdn.englishlab.vn/test/listening-practice-s3s4.pdf", "PDF",
                    "2 đề luyện tập Section 3-4 với đáp án chi tiết", "EXERCISE");
        }
        if (sessions.size() > 2) {
            saveMaterial(offering, sessions.get(2), teacher, "Slide Buổi 3 – Reading Strategies",
                    "https://cdn.englishlab.vn/test/session3-reading.pdf", "PDF",
                    "Chiến thuật Matching Headings và True/False/Not Given", "SLIDE");
        }
        if (sessions.size() > 3) {
            saveMaterial(offering, sessions.get(3), teacher, "Writing Task 1 – Mẫu bài band 7.0",
                    "https://cdn.englishlab.vn/test/writing-task1-sample-band7.pdf", "PDF",
                    "3 bài mẫu band 7.0 với annotations chi tiết từ giáo viên", "SAMPLE");
        }
        if (sessions.size() > 4) {
            saveMaterial(offering, sessions.get(4), teacher, "Writing Task 2 – Outline Framework",
                    "https://cdn.englishlab.vn/test/writing-task2-framework.pdf", "PDF",
                    "Khung dàn bài 5 đoạn cho Opinion, Discussion, Problem-Solution essays", "SLIDE");
        }
    }

    private void saveMaterial(ClassroomOffering offering, ClassroomSession session, User teacher,
                               String title, String url, String fileType, String description, String materialType) {
        materialRepository.save(ClassroomMaterial.builder()
                .classroomOffering(offering)
                .session(session)
                .title(title)
                .fileUrl(url)
                .fileType(fileType)
                .description(description)
                .materialType(materialType)
                .visibility("LEARNERS_IN_CLASS")
                .sourceType("CLASSROOM_UPLOAD")
                .uploadedBy(teacher)
                .reviewStatus(ContentReviewStatus.APPROVED)
                .build());
    }

    // ── Announcements ─────────────────────────────────────────────────────────

    private void createAnnouncements(ClassroomOffering offering, User teacher) {
        saveAnnouncement(offering, teacher,
                "📚 Chào mừng đến lớp IELTS Intensive 6.5+",
                "Chào các bạn! Lớp học bắt đầu từ tuần này. Tài liệu, bài tập và flashcard của từng buổi sẽ được mở lần lượt. Hãy vào mục Tài liệu để tải slide và vào Bài tập để hoàn thành bài trước deadline nhé!");
        saveAnnouncement(offering, teacher,
                "📝 Nhắc nhở: Nộp bài Writing Task 1 trước hôm nay 23:59",
                "Bài Writing Task 1 (Buổi 4) đến hạn hôm nay lúc 23:59. Các bạn chưa nộp vui lòng hoàn thành gấp trong mục Bài tập → Buổi 4. Bài nộp trễ vẫn được nhận nhưng trừ 2 điểm.");
        saveAnnouncement(offering, teacher,
                "🎯 Buổi 5 tối nay – Writing Task 2 Opinion Essay",
                "Tối nay 19:30 chúng ta học Writing Task 2. Các bạn chuẩn bị sẵn 1 chủ đề yêu thích để luyện outline ngay tại lớp. Link Google Meet sẽ được mở lúc 19:25.");
        saveAnnouncement(offering, teacher,
                "🔔 Lịch thi thử Mock Test – Buổi 8",
                "Buổi 8 sẽ là mock test toàn phần 2h45m theo format thi thật. Các bạn cần chuẩn bị: tai nghe, bút chì (cho bản giấy) hoặc chỉ cần laptop nếu làm online. Chi tiết sẽ thông báo sau Buổi 7.");
    }

    private void saveAnnouncement(ClassroomOffering offering, User teacher, String title, String content) {
        announcementRepository.save(ClassroomAnnouncement.builder()
                .classroomOffering(offering)
                .title(title)
                .content(content)
                .createdBy(teacher)
                .build());
    }

    // ── Homework ──────────────────────────────────────────────────────────────

    /**
     * Tạo bài tập đầy đủ trạng thái và nhiều đầu điểm:
     *
     *  HW1 – Buổi 1 – QUÁ HẠN (deadline 4 tuần trước) – CHƯA NỘP (—)
     *  HW2 – Buổi 2 – QUÁ HẠN – ĐÃ NỘP & ĐÃ CHẤM (Điểm: 8.5 / 10)
     *  HW3 – Buổi 3 – QUÁ HẠN – ĐÃ NỘP & ĐÃ CHẤM (Điểm: 7.0 / 10)
     *  HW4 – Buổi 4 – Hạn HÔM NAY – ĐÃ LÀM QUIZ & ĐÃ CHẤM (Điểm: 9.0 / 10)
     *  HW5 – Buổi 5 – Hạn NGÀY MAI 23:59 – chưa nộp (—)
     *  HW6 – Buổi 5 – Hạn NGÀY MAI 23:59 (dạng quiz) – chưa nộp (—)
     *  HW7 – Buổi 6 – Hạn NGÀY KIA – chưa nộp (—)
     *  HW8 – Buổi 7 – Hạn 5 ngày nữa – chưa nộp (—)
     *  HW9 – Buổi 8 – Hạn 1 tuần nữa – chưa nộp (—)
     */
    private void createHomework(ClassroomOffering offering, List<ClassroomSession> sessions,
                                 User teacher, User learner) {
        LocalDate today = LocalDate.now();

        ClassroomSession s1 = sessions.size() > 0 ? sessions.get(0) : null;
        ClassroomSession s2 = sessions.size() > 1 ? sessions.get(1) : null;
        ClassroomSession s3 = sessions.size() > 2 ? sessions.get(2) : null;
        ClassroomSession s4 = sessions.size() > 3 ? sessions.get(3) : null;
        ClassroomSession s5 = sessions.size() > 4 ? sessions.get(4) : null;
        ClassroomSession s6 = sessions.size() > 5 ? sessions.get(5) : null;
        ClassroomSession s7 = sessions.size() > 6 ? sessions.get(6) : null;
        ClassroomSession s8 = sessions.size() > 7 ? sessions.get(7) : null;

        // ── HW1: Quá hạn – chưa nộp ─────────────────────────────────────────
        saveHomework(offering, s1, teacher,
                "HW1 – Nghe Section 1-2 và điền bảng (QUÁ HẠN)",
                "Nghe audio Section 1-2, điền form và trả lời 10 câu. Tải file đề tại Tài liệu Buổi 1.",
                today.minusWeeks(4).atTime(23, 59),
                HomeworkActivityType.FILE_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.LISTENING, null, null, false);

        // ── HW2: Quá hạn – đã nộp & đã chấm (8.5 / 10) ─────────────────────
        ClassroomHomework hw2 = saveHomework(offering, s2, teacher,
                "HW2 – Tóm tắt lỗi Listening (QUÁ HẠN – đã chấm)",
                "Viết 150-200 từ tóm tắt 5 lỗi Listening hay mắc nhất của bản thân và cách khắc phục.",
                today.minusWeeks(3).atTime(23, 59),
                HomeworkActivityType.TEXT_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.LISTENING, null, null, true);
        saveSubmission(hw2, learner,
                "1. Nghe nhầm số (số nhiều/đơn). 2. Bỏ sót thông tin khi chuyển section. 3. Không đọc câu hỏi trước. 4. Viết sai chính tả. 5. Không kiểm tra lại đáp án.",
                null, HomeworkSubmissionStatus.GRADED,
                BigDecimal.valueOf(8.5), "Bài làm tốt, phân tích sâu. Cần bổ sung cách khắc phục cụ thể hơn cho lỗi số 3.", teacher);

        // ── HW3: Quá hạn – đã nộp & đã chấm (7.0 / 10) ─────────────────────
        ClassroomHomework hw3 = saveHomework(offering, s3, teacher,
                "HW3 – Nộp bài Reading Section 1 (QUÁ HẠN – đã chấm)",
                "Tải đề Reading Section 1 ở Tài liệu Buổi 3, làm và scan nộp lại file PDF/ảnh.",
                today.minusWeeks(2).atTime(23, 59),
                HomeworkActivityType.FILE_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.READING,
                "https://cdn.englishlab.vn/test/reading-section1-exercise.pdf", null, true);
        saveSubmission(hw3, learner, "Em đã hoàn thành 10 câu Reading Section 1 theo thời gian quy định.",
                "https://cdn.englishlab.vn/test/submissions/chinh-hw3-reading.pdf",
                HomeworkSubmissionStatus.GRADED,
                BigDecimal.valueOf(7.0), "Làm đúng 7/10 câu Reading Section 1. Chú ý bẫy True/False/Not Given ở câu 5 và 8.", teacher);

        // ── HW4: Hạn HÔM NAY – quiz trắc nghiệm đã làm (9.0 / 10) ───────────
        String quizConfigWritingTask1 = """
                {"questions":[
                  {"number":1,"prompt":"Which of the following BEST describes the trend in the bar chart?","options":[{"value":"A","label":"The data shows a steady decline over 5 years."},{"value":"B","label":"There was a sharp increase followed by a slight decrease."},{"value":"C","label":"All categories remained stable throughout the period."},{"value":"D","label":"The figures fluctuated with no clear trend."}]},
                  {"number":2,"prompt":"In Writing Task 1, the introduction should _____.","options":[{"value":"A","label":"copy the question directly"},{"value":"B","label":"paraphrase the question using different words"},{"value":"C","label":"include your opinion about the data"},{"value":"D","label":"describe all the data in detail"}]},
                  {"number":3,"prompt":"The phrase 'rose sharply' means the data _____.","options":[{"value":"A","label":"decreased significantly"},{"value":"B","label":"remained unchanged"},{"value":"C","label":"increased dramatically"},{"value":"D","label":"fluctuated slightly"}]},
                  {"number":4,"prompt":"Which sentence correctly describes an overview?","options":[{"value":"A","label":"In 2010, the value was 45 million."},{"value":"B","label":"Overall, the most notable feature is the upward trend in Category A."},{"value":"C","label":"Furthermore, the data shows various figures."},{"value":"D","label":"The chart is about population growth."}]}
                ],"answerKey":{"1":"B","2":"B","3":"C","4":"B"}}
                """;
        ClassroomHomework hw4 = saveHomework(offering, s4, teacher,
                "HW4 – Quiz Writing Task 1 (Hạn HÔM NAY 23:59)",
                "Trả lời 4 câu trắc nghiệm về Writing Task 1 – Bar chart. Hoàn thành trực tiếp trên website.",
                today.atTime(23, 59),
                HomeworkActivityType.SKILL_PRACTICE, HomeworkStatus.OPEN,
                AssessmentSkill.WRITING, null, quizConfigWritingTask1, false);
        saveSubmission(hw4, learner, "{\"1\":\"B\",\"2\":\"B\",\"3\":\"C\",\"4\":\"B\"}",
                null, HomeworkSubmissionStatus.GRADED,
                BigDecimal.valueOf(9.0), "Trả lời đúng 4/4 câu quiz trắc nghiệm Writing Task 1. Nắm rất vững cấu trúc và từ vựng mô tả biểu đồ.", teacher);

        // ── HW5: Hạn NGÀY MAI – bài viết tự do (chưa nộp) ──────────────────
        saveHomework(offering, s5, teacher,
                "HW5 – Viết Writing Task 2 hoàn chỉnh (Hạn NGÀY MAI)",
                "Viết bài Opinion Essay 250+ từ theo đề: 'Some people think that technology has made our lives more complex. To what extent do you agree or disagree?' Nộp trực tiếp vào ô text phía dưới.",
                today.plusDays(1).atTime(23, 59),
                HomeworkActivityType.TEXT_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.WRITING, null, null, true);

        // ── HW6: Hạn NGÀY MAI – quiz kỹ năng (chưa nộp) ────────────────────
        String quizConfigWritingTask2 = """
                {"durationMinutes":12,"questions":[
                  {"number":1,"prompt":"In an Opinion Essay, the thesis statement should appear in the _____.","options":[{"value":"A","label":"body paragraph 1"},{"value":"B","label":"introduction"},{"value":"C","label":"conclusion"},{"value":"D","label":"body paragraph 2"}]},
                  {"number":2,"prompt":"Which linking word best introduces a contrasting idea?","options":[{"value":"A","label":"Furthermore"},{"value":"B","label":"Therefore"},{"value":"C","label":"However"},{"value":"D","label":"Additionally"}]},
                  {"number":3,"prompt":"A well-structured IELTS Task 2 essay should have how many paragraphs?","options":[{"value":"A","label":"2"},{"value":"B","label":"3"},{"value":"C","label":"4 to 5"},{"value":"D","label":"6 or more"}]},
                  {"number":4,"prompt":"'The government should invest in renewable energy.' This is an example of _____.","options":[{"value":"A","label":"a fact"},{"value":"B","label":"an opinion"},{"value":"C","label":"a statistic"},{"value":"D","label":"an example"}]},
                  {"number":5,"prompt":"Which sentence is the best paraphrase of the essay question?","options":[{"value":"A","label":"Technology makes life harder."},{"value":"B","label":"Some argue that technology has increased the complexity of modern life."},{"value":"C","label":"I agree that technology is bad."},{"value":"D","label":"Technology is a complex topic."}]}
                ],"answerKey":{"1":"B","2":"C","3":"C","4":"B","5":"B"}}
                """;
        saveHomework(offering, s5, teacher,
                "HW6 – Quiz chiến thuật Task 2 (Hạn NGÀY MAI)",
                "5 câu trắc nghiệm về cấu trúc và chiến thuật Writing Task 2. Hoàn thành trong 12 phút.",
                today.plusDays(1).atTime(23, 59),
                HomeworkActivityType.SKILL_PRACTICE, HomeworkStatus.OPEN,
                AssessmentSkill.WRITING, null, quizConfigWritingTask2, false);

        // ── HW7: Hạn NGÀY KIA – ghi âm Speaking (chưa nộp) ─────────────────
        saveHomework(offering, s6, teacher,
                "HW7 – Ghi âm Speaking Part 1 (Hạn NGÀY KIA)",
                "Ghi âm trả lời 3 câu hỏi Speaking Part 1 theo chủ đề Hometown (1-2 phút mỗi câu). Nộp link Google Drive hoặc file âm thanh vào ô text bên dưới.",
                today.plusDays(2).atTime(23, 59),
                HomeworkActivityType.TEXT_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.SPEAKING, null, null, true);

        // ── HW8: Hạn 5 ngày nữa – ghi âm Speaking Part 3 (chưa nộp) ────────
        saveHomework(offering, s7, teacher,
                "HW8 – Ghi âm Speaking Part 3 – Environment (Hạn 5 ngày)",
                "Ghi âm trả lời 2 câu Speaking Part 3 về chủ đề Environment (tối thiểu 2 phút mỗi câu). Câu hỏi: (1) How can individuals help protect the environment? (2) Do you think governments are doing enough to fight climate change?",
                today.plusDays(5).atTime(23, 59),
                HomeworkActivityType.TEXT_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.SPEAKING, null, null, true);

        // ── HW9: Hạn 1 tuần nữa – nộp file Error Log (chưa nộp) ────────────
        saveHomework(offering, s8, teacher,
                "HW9 – Nộp Error Log cá nhân (Hạn 1 tuần nữa)",
                "Hoàn chỉnh bảng Error Log theo template ở Tài liệu. Ghi đầy đủ lỗi của từng kỹ năng qua 8 buổi học. Scan hoặc xuất PDF và nộp bên dưới.",
                today.plusWeeks(1).atTime(23, 59),
                HomeworkActivityType.FILE_RESPONSE, HomeworkStatus.OPEN,
                AssessmentSkill.MIXED,
                "https://cdn.englishlab.vn/test/error-log-template.xlsx", null, true);
    }

    private ClassroomHomework saveHomework(ClassroomOffering offering, ClassroomSession session,
                                            User teacher, String title, String instruction,
                                            LocalDateTime deadline,
                                            HomeworkActivityType activityType,
                                            HomeworkStatus status,
                                            AssessmentSkill skill,
                                            String attachmentUrl, String activityConfigJson,
                                            boolean aiReviewEnabled) {
        Optional<ClassroomHomework> existing = homeworkRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offering.getId())
                .stream().filter(h -> title.equalsIgnoreCase(h.getTitle())).findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        ClassroomHomework.ClassroomHomeworkBuilder builder = ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(session)
                .title(title)
                .instruction(instruction)
                .deadline(deadline)
                .maxScore(BigDecimal.TEN)
                .activityType(activityType)
                .gradingMode(HomeworkGradingMode.TEACHER)
                .skill(skill)
                .status(status)
                .allowResubmission(true)
                .aiReviewEnabled(aiReviewEnabled)
                .createdBy(teacher);
        if (attachmentUrl != null) builder.attachmentUrl(attachmentUrl);
        if (activityConfigJson != null) builder.activityConfigJson(activityConfigJson);
        return homeworkRepository.save(builder.build());
    }

    private ClassroomHomeworkSubmission saveSubmission(ClassroomHomework homework, User learner,
                                                        String textAnswer, String attachmentUrl,
                                                        HomeworkSubmissionStatus status,
                                                        BigDecimal score, String feedback,
                                                        User gradedBy) {
        Optional<ClassroomHomeworkSubmission> existingOpt = submissionRepository.findByHomeworkIdAndStudentId(homework.getId(), learner.getId());
        if (existingOpt.isPresent()) {
            ClassroomHomeworkSubmission sub = existingOpt.get();
            if (score != null) {
                sub.setScore(score);
                sub.setStatus(status);
                sub.setTeacherFeedback(feedback);
                sub.setGradedBy(gradedBy);
                sub.setGradedAt(LocalDateTime.now());
                return submissionRepository.save(sub);
            }
            return sub;
        }

        ClassroomHomeworkSubmission.ClassroomHomeworkSubmissionBuilder builder = ClassroomHomeworkSubmission.builder()
                .homework(homework)
                .student(learner)
                .status(status)
                .submittedAt(homework.getDeadline() != null
                        ? homework.getDeadline().minusDays(1)
                        : LocalDateTime.now().minusDays(1));
        if (textAnswer != null) builder.textAnswer(textAnswer);
        if (attachmentUrl != null) builder.attachmentUrl(attachmentUrl);
        if (score != null) {
            builder.score(score);
            builder.gradedAt(homework.getDeadline() != null
                    ? homework.getDeadline().plusDays(1)
                    : LocalDateTime.now());
        }
        if (feedback != null) builder.teacherFeedback(feedback);
        if (gradedBy != null) builder.gradedBy(gradedBy);
        return submissionRepository.save(builder.build());
    }

    // ── Gradebook ─────────────────────────────────────────────────────────────

    private void createGradebook(ClassroomOffering offering, User learner, User teacher) {
        Optional<ClassroomGradebookEntry> existingOpt = gradebookRepository.findByClassroomOfferingIdAndStudentId(offering.getId(), learner.getId());
        if (existingOpt.isPresent()) {
            ClassroomGradebookEntry entry = existingOpt.get();
            entry.setHomeworkScore(BigDecimal.valueOf(8.2));
            entry.setQuizScore(BigDecimal.valueOf(9.0));
            entry.setAttendancePercent(BigDecimal.valueOf(80.0));
            entry.setParticipationScore(BigDecimal.valueOf(8.5));
            entry.setFinalResult(BigDecimal.valueOf(8.4));
            entry.setTeacherComment("Học viên học nghiêm túc, tham gia tốt. Đã hoàn thành 3 bài tập và đạt điểm số ấn tượng (TB: 8.2). Cần tiếp tục duy trì phong độ cho các bài tập sắp tới!");
            entry.setStatus(GradebookEntryStatus.PUBLISHED);
            entry.setUpdatedBy(teacher);
            gradebookRepository.save(entry);
            return;
        }

        gradebookRepository.save(ClassroomGradebookEntry.builder()
                .classroomOffering(offering)
                .student(learner)
                .homeworkScore(BigDecimal.valueOf(8.2))   // trung bình các bài đã chấm (8.5, 7.0, 9.0)
                .quizScore(BigDecimal.valueOf(9.0))       // điểm quiz
                .attendancePercent(BigDecimal.valueOf(80.0)) // 4/5 buổi = 80%
                .participationScore(BigDecimal.valueOf(8.5)) // tham gia tích cực
                .finalResult(BigDecimal.valueOf(8.4))
                .teacherComment("Học viên học nghiêm túc, tham gia tốt. Đã hoàn thành 3 bài tập và đạt điểm số ấn tượng (TB: 8.2). Cần tiếp tục duy trì phong độ cho các bài tập sắp tới!")
                .status(GradebookEntryStatus.PUBLISHED)
                .updatedBy(teacher)
                .build());
    }

    // ── User helper ───────────────────────────────────────────────────────────

    private User ensureUser(String email, String fullName, RoleEnum role) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User created = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .password(passwordEncoder.encode("Password123!"))
                    .emailVerified(true)
                    .build();
            userRoleService.assignRole(created, role);
            return userRepository.save(created);
        });
        userRoleService.ensureRole(user, role);
        return role == RoleEnum.LEARNER ? onboardingSupport.ensureReady(user) : user;
    }
}
