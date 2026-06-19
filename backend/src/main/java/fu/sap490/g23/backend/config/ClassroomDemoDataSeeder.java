package fu.sap490.g23.backend.config;

import fu.sap490.g23.backend.entity.Role;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.course.*;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import fu.sap490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class ClassroomDemoDataSeeder implements CommandLineRunner {

    private static final String OFFLINE_UPCOMING_TITLE = "IELTS Foundation - Tại trung tâm";
    private static final String VIRTUAL_UPCOMING_TITLE = "IELTS Speaking Live - Lark";
    private static final String OFFLINE_IN_PROGRESS_TITLE = "IELTS Intermediate - Đang học";
    private static final String VIRTUAL_IN_PROGRESS_TITLE = "TOEIC Communication - Đang học (Lark)";
    private static final String REGISTRATION_PIPELINE_TITLE = "IELTS Mới - Chờ xử lý đăng ký";
    private static final String COMPLETED_TITLE = "IELTS Foundation - Đã kết thúc";

    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomAttendanceRepository attendanceRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository homeworkSubmissionRepository;
    private final ClassroomGradebookEntryRepository gradebookEntryRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final ClassroomAnnouncementRepository announcementRepository;
    private final ClassroomSyllabusItemRepository syllabusItemRepository;
    private final ClassroomChangeRequestRepository changeRequestRepository;
    private final ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    private final CampusRepository campusRepository;
    private final ClassroomRoomRepository roomRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        PackageType classroomType = packageTypeRepository.findByCode(PackageTypeCode.CLASSROOM)
                .orElseThrow(() -> new IllegalStateException("CLASSROOM package type is missing"));

        User teacher1 = ensureUser("classroom.teacher1@englishlab.vn", "Nguyễn Văn Teacher", Role.TEACHER);
        User teacher2 = ensureUser("classroom.teacher2@englishlab.vn", "Trần Thị Teacher", Role.TEACHER);
        User learner1 = ensureUser("classroom.learner1@englishlab.vn", "Lê Học Viên Một", Role.LEARNER);
        User learner2 = ensureUser("classroom.learner2@englishlab.vn", "Phạm Học Viên Hai", Role.LEARNER);
        User learner3 = ensureUser("classroom.learner3@englishlab.vn", "Hoàng Học Viên Ba", Role.LEARNER);
        User learner4 = ensureUser("classroom.learner4@englishlab.vn", "Trần Học Viên Bốn", Role.LEARNER);
        User manager = ensureUser("classroom.manager@englishlab.vn", "Quản Lý Lớp Học", Role.MANAGER);
        User trainingManager = ensureUser("training.manager@englishlab.vn", "Quản Lý Đào Tạo", Role.TRAINING_MANAGER);

        Campus campus = campusRepository.findByNameIgnoreCase("EnglishLab Hà Nội")
                .orElseGet(() -> campusRepository.save(Campus.builder()
                        .name("EnglishLab Hà Nội")
                        .address("123 Phố Huế, Hai Bà Trưng, Hà Nội")
                        .active(true)
                        .build()));

        ClassroomRoom roomA = roomRepository.findByCampusIdAndActiveTrue(campus.getId()).stream()
                .filter(room -> "Phòng A101".equalsIgnoreCase(room.getName()))
                .findFirst()
                .orElseGet(() -> roomRepository.save(ClassroomRoom.builder()
                        .campus(campus)
                        .name("Phòng A101")
                        .capacity(24)
                        .active(true)
                        .build()));

        ClassroomRoom roomB = roomRepository.findByCampusIdAndActiveTrue(campus.getId()).stream()
                .filter(room -> "Phòng B203".equalsIgnoreCase(room.getName()))
                .findFirst()
                .orElseGet(() -> roomRepository.save(ClassroomRoom.builder()
                        .campus(campus)
                        .name("Phòng B203")
                        .capacity(18)
                        .active(true)
                        .build()));

        seedIfMissing(OFFLINE_UPCOMING_TITLE, () -> {
            ClassroomOffering offering = createOfflineOffering(
                    classroomType,
                    OFFLINE_UPCOMING_TITLE,
                    "ielts-foundation-tai-trung-tam",
                    ClassroomOfferingStatus.UPCOMING,
                    LocalDate.now().plusDays(14),
                    LocalDate.now().plusMonths(2),
                    teacher1,
                    campus,
                    roomA,
                    manager,
                    BigDecimal.valueOf(4_500_000),
                    BigDecimal.valueOf(3_990_000)
            );
            seedOfflineUpcomingData(offering, teacher1, learner1, learner2, learner3, manager);
        });

        seedIfMissing(VIRTUAL_UPCOMING_TITLE, () -> {
            ClassroomOffering offering = createVirtualOffering(
                    classroomType,
                    VIRTUAL_UPCOMING_TITLE,
                    "ielts-speaking-live-lark",
                    ClassroomOfferingStatus.UPCOMING,
                    LocalDate.now().plusDays(7),
                    LocalDate.now().plusWeeks(6),
                    teacher2,
                    manager,
                    BigDecimal.valueOf(2_800_000),
                    BigDecimal.valueOf(2_490_000)
            );
            seedVirtualUpcomingData(offering, teacher2, learner1, learner2);
        });

        seedIfMissing(OFFLINE_IN_PROGRESS_TITLE, () -> {
            ClassroomOffering offering = createOfflineOffering(
                    classroomType,
                    OFFLINE_IN_PROGRESS_TITLE,
                    "ielts-intermediate-dang-hoc",
                    ClassroomOfferingStatus.ACTIVE,
                    LocalDate.now().minusWeeks(4),
                    LocalDate.now().plusWeeks(4),
                    teacher1,
                    campus,
                    roomB,
                    manager,
                    BigDecimal.valueOf(5_200_000),
                    BigDecimal.valueOf(4_690_000)
            );
            seedOfflineInProgressData(offering, teacher1, learner1, learner2, manager);
        });

        seedIfMissing(VIRTUAL_IN_PROGRESS_TITLE, () -> {
            ClassroomOffering offering = createVirtualOffering(
                    classroomType,
                    VIRTUAL_IN_PROGRESS_TITLE,
                    "toeic-communication-dang-hoc-lark",
                    ClassroomOfferingStatus.ACTIVE,
                    LocalDate.now().minusWeeks(3),
                    LocalDate.now().plusWeeks(3),
                    teacher2,
                    manager,
                    BigDecimal.valueOf(3_100_000),
                    BigDecimal.valueOf(2_790_000)
            );
            seedVirtualInProgressData(offering, teacher2, learner1, learner3, manager);
        });

        seedIfMissing(REGISTRATION_PIPELINE_TITLE, () -> {
            ClassroomOffering offering = createOfflineOffering(
                    classroomType,
                    REGISTRATION_PIPELINE_TITLE,
                    "ielts-moi-cho-xu-ly-dang-ky",
                    ClassroomOfferingStatus.UPCOMING,
                    LocalDate.now().plusDays(21),
                    LocalDate.now().plusMonths(3),
                    teacher1,
                    campus,
                    roomA,
                    manager,
                    BigDecimal.valueOf(4_800_000),
                    null
            );
            seedRegistrationPipelineData(offering, learner2, learner3, learner4, manager);
        });

        seedIfMissing(COMPLETED_TITLE, () -> {
            ClassroomOffering offering = createOfflineOffering(
                    classroomType,
                    COMPLETED_TITLE,
                    "ielts-foundation-da-ket-thuc",
                    ClassroomOfferingStatus.COMPLETED,
                    LocalDate.now().minusMonths(3),
                    LocalDate.now().minusWeeks(2),
                    teacher1,
                    campus,
                    roomA,
                    manager,
                    BigDecimal.valueOf(4_200_000),
                    BigDecimal.valueOf(3_800_000)
            );
            seedCompletedClassData(offering, teacher1, learner1, manager);
        });

        publishDemoGradebooks();
    }

    private void publishDemoGradebooks() {
        gradebookEntryRepository.findAll().stream()
                .filter(entry -> entry.getStatus() == GradebookEntryStatus.GRADED)
                .forEach(entry -> entry.setStatus(GradebookEntryStatus.PUBLISHED));
    }

    private void seedIfMissing(String title, Runnable seeder) {
        if (offeringRepository.existsByLearningPackage_TitleIgnoreCase(title)) {
            return;
        }
        seeder.run();
    }

    private void seedOfflineUpcomingData(
            ClassroomOffering offering,
            User teacher,
            User learner1,
            User learner2,
            User learner3,
            User manager
    ) {
        ClassroomSession session1 = sessionRepository.save(ClassroomSession.builder()
                .classroomOffering(offering)
                .sessionDate(LocalDate.now().plusDays(16))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .campus(offering.getDefaultCampus())
                .room(offering.getDefaultRoom())
                .status(ClassroomSessionStatus.SCHEDULED)
                .sessionContent("Giới thiệu IELTS Foundation và kỹ năng Listening cơ bản")
                .build());

        ClassroomSession conflictSession = sessionRepository.save(ClassroomSession.builder()
                .classroomOffering(offering)
                .sessionDate(LocalDate.now().plusDays(17))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .campus(offering.getDefaultCampus())
                .room(offering.getDefaultRoom())
                .status(ClassroomSessionStatus.SCHEDULED)
                .sessionContent("Buổi học mẫu để kiểm tra xung đột lịch giáo viên")
                .build());

        enrollAssigned(offering, learner1, manager);
        enrollAssigned(offering, learner2, manager);
        enrollAssigned(offering, learner3, manager);

        homeworkRepository.save(ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(session1)
                .title("Chuẩn bị trước khai giảng")
                .instruction("Đọc tài liệu giới thiệu và ghi chú 3 mục tiêu học tập.")
                .deadline(LocalDateTime.now().plusDays(12))
                .maxScore(BigDecimal.TEN)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        announcementRepository.save(ClassroomAnnouncement.builder()
                .classroomOffering(offering)
                .title("Chào mừng lớp IELTS Foundation")
                .content("Các bạn nhớ mang sách giáo trình và đến sớm 10 phút để làm quen phòng học.")
                .createdBy(teacher)
                .build());

        changeRequestRepository.save(ClassroomChangeRequest.builder()
                .requestType(ClassroomChangeRequestType.RESCHEDULE_SESSION)
                .requester(teacher)
                .requesterRole(teacher.getRole())
                .classroomOffering(offering)
                .targetSession(conflictSession)
                .oldValuesJson("{\"sessionDate\":\"" + conflictSession.getSessionDate() + "\"}")
                .newValuesJson("{\"sessionDate\":\"" + conflictSession.getSessionDate().plusDays(1)
                        + "\",\"startTime\":\"14:00\",\"endTime\":\"16:00\",\"teacherId\":" + teacher.getId()
                        + ",\"roomId\":" + offering.getDefaultRoom().getId() + "}")
                .reason("Giáo viên có lịch họp nội bộ, đề nghị dời buổi học.")
                .status(ClassroomChangeRequestStatus.PENDING)
                .build());
    }

    private void seedVirtualUpcomingData(ClassroomOffering offering, User teacher, User learner1, User learner2) {
        sessionRepository.save(ClassroomSession.builder()
                .classroomOffering(offering)
                .sessionDate(LocalDate.now().plusDays(8))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 30))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingUrl("https://meet.larksuite.com/demo/ielts-speaking-live")
                .larkMeetingStatus(LarkMeetingStatus.SCHEDULED)
                .status(ClassroomSessionStatus.SCHEDULED)
                .sessionContent("Speaking Part 1 & 2 practice")
                .build());

        enrollAssigned(offering, learner1, null);
        enrollAssigned(offering, learner2, null);

        announcementRepository.save(ClassroomAnnouncement.builder()
                .classroomOffering(offering)
                .title("Hướng dẫn vào lớp Lark")
                .content("Vào lớp trước 5 phút, bật mic khi giáo viên yêu cầu.")
                .createdBy(teacher)
                .build());
    }

    private void seedOfflineInProgressData(
            ClassroomOffering offering,
            User teacher,
            User learner1,
            User learner2,
            User manager
    ) {
        ClassroomSession completed1 = saveOfflineSession(offering, teacher, LocalDate.now().minusWeeks(3), 9, 11,
                ClassroomSessionStatus.COMPLETED, "Listening: Section 1-2");
        ClassroomSession completed2 = saveOfflineSession(offering, teacher, LocalDate.now().minusWeeks(2), 9, 11,
                ClassroomSessionStatus.COMPLETED, "Reading: True/False/Not Given");
        ClassroomSession completed3 = saveOfflineSession(offering, teacher, LocalDate.now().minusWeeks(1), 9, 11,
                ClassroomSessionStatus.COMPLETED, "Writing Task 1 overview");
        ClassroomSession inProgress = saveOfflineSession(offering, teacher, LocalDate.now(), 14, 16,
                ClassroomSessionStatus.IN_PROGRESS, "Speaking mock test nhóm nhỏ");
        ClassroomSession scheduled = saveOfflineSession(offering, teacher, LocalDate.now().plusDays(4), 9, 11,
                ClassroomSessionStatus.SCHEDULED, "Listening: Section 3-4");

        enrollAssigned(offering, learner1, manager);
        enrollAssigned(offering, learner2, manager);

        attendanceRepository.save(ClassroomAttendance.builder()
                .session(completed1)
                .student(learner1)
                .status(ClassroomAttendanceStatus.PRESENT)
                .note("Có mặt đúng giờ")
                .teacherConfirmed(true)
                .markedBy(teacher)
                .build());
        attendanceRepository.save(ClassroomAttendance.builder()
                .session(completed2)
                .student(learner1)
                .status(ClassroomAttendanceStatus.PRESENT)
                .teacherConfirmed(true)
                .markedBy(teacher)
                .build());
        attendanceRepository.save(ClassroomAttendance.builder()
                .session(completed3)
                .student(learner2)
                .status(ClassroomAttendanceStatus.LATE)
                .note("Đến trễ 10 phút")
                .teacherConfirmed(true)
                .markedBy(teacher)
                .build());

        ClassroomHomework homework = homeworkRepository.save(ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(inProgress)
                .title("Bài tập Writing Task 2")
                .instruction("Viết bài 250 từ về chủ đề giáo dục.")
                .deadline(LocalDateTime.now().plusDays(4))
                .maxScore(BigDecimal.TEN)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        homeworkSubmissionRepository.save(ClassroomHomeworkSubmission.builder()
                .homework(homework)
                .student(learner1)
                .textAnswer("Đã hoàn thành bản nháp Task 2.")
                .submittedAt(LocalDateTime.now().minusHours(2))
                .status(HomeworkSubmissionStatus.SUBMITTED)
                .build());

        gradebookEntryRepository.save(ClassroomGradebookEntry.builder()
                .classroomOffering(offering)
                .student(learner1)
                .homeworkScore(BigDecimal.valueOf(8.0))
                .attendancePercent(BigDecimal.valueOf(100))
                .participationScore(BigDecimal.valueOf(8.5))
                .finalResult(BigDecimal.valueOf(8.2))
                .status(GradebookEntryStatus.PUBLISHED)
                .updatedBy(teacher)
                .build());

        materialRepository.save(ClassroomMaterial.builder()
                .classroomOffering(offering)
                .title("Slide tuần 4 - Speaking")
                .fileUrl("https://cdn.englishlab.vn/materials/ielts-intermediate-speaking-week4.pdf")
                .fileType("PDF")
                .uploadedBy(teacher)
                .build());

        syllabusItemRepository.save(ClassroomSyllabusItem.builder()
                .classroomOffering(offering)
                .title("Tuần 4 - Speaking intensive")
                .description("Luyện Part 2 và feedback cá nhân")
                .displayOrder(4)
                .sessionPlan("Buổi hôm nay: mock test + sửa lỗi phát âm")
                .status("PUBLISHED")
                .build());
    }

    private void seedVirtualInProgressData(
            ClassroomOffering offering,
            User teacher,
            User learner1,
            User learner3,
            User manager
    ) {
        ClassroomSession completed = sessionRepository.save(ClassroomSession.builder()
                .classroomOffering(offering)
                .sessionDate(LocalDate.now().minusDays(7))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 30))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingUrl("https://meet.larksuite.com/demo/toeic-live-week1")
                .larkMeetingStatus(LarkMeetingStatus.ENDED)
                .status(ClassroomSessionStatus.COMPLETED)
                .sessionContent("TOEIC Listening Part 1-2")
                .build());

        ClassroomSession liveSession = sessionRepository.save(ClassroomSession.builder()
                .classroomOffering(offering)
                .sessionDate(LocalDate.now())
                .startTime(LocalTime.of(19, 30))
                .endTime(LocalTime.of(21, 0))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingUrl("https://meet.larksuite.com/demo/toeic-live-today")
                .larkMeetingStatus(LarkMeetingStatus.OPEN)
                .status(ClassroomSessionStatus.OPEN)
                .sessionContent("TOEIC Speaking practice hôm nay")
                .build());

        sessionRepository.save(ClassroomSession.builder()
                .classroomOffering(offering)
                .sessionDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 30))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingUrl("https://meet.larksuite.com/demo/toeic-live-next")
                .larkMeetingStatus(LarkMeetingStatus.SCHEDULED)
                .status(ClassroomSessionStatus.SCHEDULED)
                .sessionContent("Role-play giao tiếp công sở")
                .build());

        enrollAssigned(offering, learner1, manager);
        enrollAssigned(offering, learner3, manager);

        attendanceRepository.save(ClassroomAttendance.builder()
                .session(completed)
                .student(learner1)
                .status(ClassroomAttendanceStatus.PRESENT)
                .joinTime(completed.getSessionDate().atTime(19, 2))
                .leaveTime(completed.getSessionDate().atTime(20, 28))
                .durationMinutes(86)
                .teacherConfirmed(true)
                .markedBy(teacher)
                .build());

        homeworkRepository.save(ClassroomHomework.builder()
                .classroomOffering(offering)
                .session(liveSession)
                .title("Ghi âm phản hồi Speaking")
                .instruction("Ghi âm 90 giây mô tả dự án gần nhất và nộp link.")
                .deadline(LocalDateTime.now().plusDays(2))
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());
    }

    private void seedRegistrationPipelineData(
            ClassroomOffering offering,
            User learner2,
            User learner3,
            User learner4,
            User manager
    ) {
        seedEnrollment(offering, learner2, ClassroomRegistrationStatus.PENDING_CONFIRMATION,
                BigDecimal.ZERO, BigDecimal.ZERO, false);
        seedEnrollment(offering, learner3, ClassroomRegistrationStatus.PARTIALLY_PAID,
                BigDecimal.valueOf(2_400_000), BigDecimal.valueOf(1_200_000), false);
        seedEnrollment(offering, learner4, ClassroomRegistrationStatus.WAITLIST,
                BigDecimal.valueOf(4_800_000), BigDecimal.valueOf(4_800_000), true);

        ClassroomEnrollment partial = enrollmentRepository
                .findByStudentIdAndClassroomOfferingId(learner3.getId(), offering.getId())
                .orElseThrow();
        tuitionPaymentRepository.save(ClassroomTuitionPayment.builder()
                .enrollment(partial)
                .amount(BigDecimal.valueOf(1_200_000))
                .paymentKind(TuitionPaymentKind.PARTIAL)
                .note("Đặt cọc 25% học phí")
                .recordedBy(manager)
                .build());
    }

    private void seedCompletedClassData(ClassroomOffering offering, User teacher, User learner1, User manager) {
        ClassroomSession completedSession = saveOfflineSession(offering, teacher, LocalDate.now().minusWeeks(3), 9, 11,
                ClassroomSessionStatus.COMPLETED, "Tổng kết khóa và review lỗi sai");

        enrollAssigned(offering, learner1, manager);

        attendanceRepository.save(ClassroomAttendance.builder()
                .session(completedSession)
                .student(learner1)
                .status(ClassroomAttendanceStatus.PRESENT)
                .teacherConfirmed(true)
                .markedBy(teacher)
                .build());

        gradebookEntryRepository.save(ClassroomGradebookEntry.builder()
                .classroomOffering(offering)
                .student(learner1)
                .homeworkScore(BigDecimal.valueOf(9.0))
                .attendancePercent(BigDecimal.valueOf(95))
                .participationScore(BigDecimal.valueOf(9.5))
                .finalResult(BigDecimal.valueOf(9.1))
                .status(GradebookEntryStatus.PUBLISHED)
                .updatedBy(teacher)
                .build());
    }

    private ClassroomSession saveOfflineSession(
            ClassroomOffering offering,
            User teacher,
            LocalDate date,
            int startHour,
            int endHour,
            ClassroomSessionStatus status,
            String content
    ) {
        return sessionRepository.save(ClassroomSession.builder()
                .classroomOffering(offering)
                .sessionDate(date)
                .startTime(LocalTime.of(startHour, 0))
                .endTime(LocalTime.of(endHour, 0))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .campus(offering.getDefaultCampus())
                .room(offering.getDefaultRoom())
                .status(status)
                .sessionContent(content)
                .build());
    }

    private ClassroomOffering createOfflineOffering(
            PackageType classroomType,
            String title,
            String slug,
            ClassroomOfferingStatus status,
            LocalDate startDate,
            LocalDate endDate,
            User teacher,
            Campus campus,
            ClassroomRoom room,
            User manager,
            BigDecimal price,
            BigDecimal salePrice
    ) {
        LearningPackage learningPackage = learningPackageRepository.save(LearningPackage.builder()
                .packageType(classroomType)
                .title(title)
                .slug(slug)
                .shortDescription("Lớp IELTS học trực tiếp tại trung tâm.")
                .description("Khóa luyện IELTS với lịch cố định tại cơ sở Hà Nội.")
                .price(price)
                .salePrice(salePrice)
                .status(PackageStatus.PUBLISHED)
                .duration("8 tuần")
                .studyMode("Offline")
                .createdBy(manager)
                .build());

        ClassroomOffering offering = offeringRepository.save(ClassroomOffering.builder()
                .learningPackage(learningPackage)
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .status(status)
                .entryLevel("4.0 - 6.0")
                .targetOutcome("Đạt band 5.5-6.5")
                .maxCapacity(20)
                .startDate(startDate)
                .endDate(endDate)
                .primaryTeacher(teacher)
                .defaultCampus(campus)
                .defaultRoom(room)
                .offlineAddress(campus.getAddress())
                .locationNote(room.getName() + ", tầng 2")
                .syllabusSummary("Listening, Reading, Writing & Speaking theo lộ trình 8 tuần")
                .build());

        teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                .classroomOffering(offering)
                .teacher(teacher)
                .role(ClassroomTeacherRole.PRIMARY)
                .effectiveFrom(startDate)
                .build());
        return offering;
    }

    private ClassroomOffering createVirtualOffering(
            PackageType classroomType,
            String title,
            String slug,
            ClassroomOfferingStatus status,
            LocalDate startDate,
            LocalDate endDate,
            User teacher,
            User manager,
            BigDecimal price,
            BigDecimal salePrice
    ) {
        LearningPackage learningPackage = learningPackageRepository.save(LearningPackage.builder()
                .packageType(classroomType)
                .title(title)
                .slug(slug)
                .shortDescription("Lớp trực tuyến qua Lark với giảng viên.")
                .description("Luyện kỹ năng trực tuyến theo nhóm nhỏ.")
                .price(price)
                .salePrice(salePrice)
                .status(PackageStatus.PUBLISHED)
                .duration("6 tuần")
                .studyMode("Virtual")
                .createdBy(manager)
                .build());

        ClassroomOffering offering = offeringRepository.save(ClassroomOffering.builder()
                .learningPackage(learningPackage)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .status(status)
                .entryLevel("5.0+")
                .targetOutcome("Tự tin giao tiếp và luyện thi")
                .maxCapacity(12)
                .startDate(startDate)
                .endDate(endDate)
                .primaryTeacher(teacher)
                .defaultLarkMeetingUrl("https://meet.larksuite.com/demo/" + slug)
                .larkMeetingStatus(LarkMeetingStatus.SCHEDULED)
                .recordingVisible(true)
                .syllabusSummary("Buổi live + bài tập + feedback cá nhân")
                .build());

        teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                .classroomOffering(offering)
                .teacher(teacher)
                .role(ClassroomTeacherRole.PRIMARY)
                .effectiveFrom(startDate)
                .build());
        return offering;
    }

    private void enrollAssigned(ClassroomOffering offering, User learner, User assignedBy) {
        seedEnrollment(offering, learner, ClassroomRegistrationStatus.ASSIGNED,
                tuitionDue(offering), tuitionDue(offering), true, assignedBy);
    }

    private void seedEnrollment(
            ClassroomOffering offering,
            User learner,
            ClassroomRegistrationStatus registrationStatus,
            BigDecimal tuitionPaid,
            BigDecimal tuitionDueAmount,
            boolean withAssignmentMeta
    ) {
        seedEnrollment(offering, learner, registrationStatus, tuitionPaid, tuitionDueAmount, withAssignmentMeta, null);
    }

    private void seedEnrollment(
            ClassroomOffering offering,
            User learner,
            ClassroomRegistrationStatus registrationStatus,
            BigDecimal tuitionPaid,
            BigDecimal tuitionDueAmount,
            boolean withAssignmentMeta,
            User assignedBy
    ) {
        if (enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                learner.getId(),
                offering.getId(),
                ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS
        )) {
            return;
        }
        BigDecimal due = tuitionDueAmount != null ? tuitionDueAmount : tuitionDue(offering);
        BigDecimal paid = tuitionPaid != null ? tuitionPaid : BigDecimal.ZERO;
        ClassroomEnrollment enrollment = ClassroomEnrollment.builder()
                .student(learner)
                .classroomOffering(offering)
                .registrationStatus(registrationStatus)
                .status(registrationStatus == ClassroomRegistrationStatus.ASSIGNED
                        ? ClassroomEnrollmentStatus.ENROLLED
                        : ClassroomEnrollmentStatus.WAITING)
                .tuitionAmountDue(due)
                .tuitionAmountPaid(paid)
                .tuitionDepositPaid(registrationStatus == ClassroomRegistrationStatus.DEPOSIT_PAID ? paid : BigDecimal.ZERO)
                .tuitionSettlementType(TuitionSettlementType.NONE)
                .enrolledAt(LocalDateTime.now().minusDays(3))
                .build();
        if (withAssignmentMeta && registrationStatus == ClassroomRegistrationStatus.ASSIGNED) {
            enrollment.setAssignedAt(LocalDateTime.now().minusDays(1));
            enrollment.setAssignedBy(assignedBy);
            enrollment.setConfirmedAt(LocalDateTime.now().minusDays(2));
            enrollment.setConfirmedBy(assignedBy);
            enrollment.setTuitionRecordedAt(LocalDateTime.now().minusDays(1));
            enrollment.setTuitionRecordedBy(assignedBy);
        }
        enrollmentRepository.save(enrollment);
    }

    private BigDecimal tuitionDue(ClassroomOffering offering) {
        if (offering.getLearningPackage().getSalePrice() != null) {
            return offering.getLearningPackage().getSalePrice();
        }
        return offering.getLearningPackage().getPrice() == null
                ? BigDecimal.ZERO
                : offering.getLearningPackage().getPrice();
    }

    private User ensureUser(String email, String fullName, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> userRepository.save(User.builder()
                .email(email)
                .fullName(fullName)
                .password(passwordEncoder.encode("Password123!"))
                .role(role)
                .emailVerified(true)
                .build()));
    }
}
