package fu.sep490.g23.backend.seed;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionSettlementType;
import fu.sep490.g23.backend.entity.classroom.ClassroomSyllabusItem;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomAttendanceStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomTuitionPayment;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.ClassroomAnnouncement;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.Room;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.repository.classroom.ClassroomChangeRequestRepository;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTuitionPaymentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSyllabusItemRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAnnouncementRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;

import fu.sep490.g23.backend.entity.classroom.*;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.*;
import fu.sep490.g23.backend.entity.classroom.enums.*;
import fu.sep490.g23.backend.entity.course.*;
import fu.sep490.g23.backend.entity.course.enums.*;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.*;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassroomDemoDataSeeder implements CommandLineRunner {

    private static final String TEACHER2_EMAIL = "bas1c.no04@gmail.com";
    private static final String LEGACY_TEACHER2_EMAIL = "classroom.teacher2@englishlab.vn";
    private static final String TEACHER2_FULL_NAME = "Trần Thị Teacher";

    private static final String OFFLINE_UPCOMING_TITLE = "IELTS Foundation - Tại trung tâm";
    private static final String VIRTUAL_UPCOMING_TITLE = "IELTS Speaking Live - Google Meet";
    private static final String OFFLINE_IN_PROGRESS_TITLE = "IELTS Intermediate - Đang học";
    private static final String VIRTUAL_IN_PROGRESS_TITLE = "TOEIC Communication - Đang học (Google Meet)";
    private static final String REGISTRATION_PIPELINE_TITLE = "IELTS Mới - Chờ xử lý đăng ký";
    private static final String COMPLETED_TITLE = "IELTS Foundation - Đã kết thúc";

    private static final String SLUG_OFFLINE_UPCOMING = "ielts-foundation-offline";
    private static final String SLUG_VIRTUAL_UPCOMING = "ielts-speaking-live";
    private static final String SLUG_OFFLINE_IN_PROGRESS = "ielts-intermediate-live";
    private static final String SLUG_VIRTUAL_IN_PROGRESS = "toeic-communication-live";
    private static final String SLUG_REGISTRATION_PIPELINE = "ielts-registration-pipeline";
    private static final String SLUG_COMPLETED = "ielts-foundation-completed";
    private static final String DEFAULT_OFFLINE_ADDRESS = "123 Phố Huế, Hai Bà Trưng, Hà Nội";
    private static final String LEGACY_DEMO_LARK_URL_SPEAKING = "https://meet.larksuite.com/s/englishlab-ielts-speaking-live";
    private static final String LEGACY_DEMO_LARK_URL_TOEIC = "https://meet.larksuite.com/s/englishlab-toeic-communication-live";

    private final ClassSectionRepository offeringRepository;
    private final ClassScheduleRepository sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
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
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;
    private final JdbcTemplate jdbcTemplate;
    private final DemoLearnerOnboardingSupport demoLearnerOnboardingSupport;

    @Value("${app.seed.test.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.seed.sheet.enabled:false}")
    private boolean sheetEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled && !sheetEnabled) {
            return;
        }

        syncEnglishSlugs();
        syncGoogleMeetLabels();
        syncTeacher2Account();
        clearLegacyDemoLarkLinks();
        syncTodayTeacher1Data();

        if (!seedEnabled || sheetEnabled) {
            return;
        }

        User teacher1 = ensureUser("classroom.teacher1@englishlab.vn", "Nguyễn Văn Teacher", RoleCodes.TEACHER);
        User teacher2 = ensureUser(TEACHER2_EMAIL, TEACHER2_FULL_NAME, RoleCodes.TEACHER);
        User learner1 = ensureUser("0386852628z@gmail.com", "Lê Ngọc Anh", RoleCodes.LEARNER);
        User learner2 = ensureUser("classroom.learner2@englishlab.vn", "Phạm Minh Châu", RoleCodes.LEARNER);
        User learner3 = ensureUser("classroom.learner3@englishlab.vn", "Hoàng Gia Huy", RoleCodes.LEARNER);
        User learner4 = ensureUser("classroom.learner4@englishlab.vn", "Trần Ngọc Mai", RoleCodes.LEARNER);
        User manager = ensureUser("classroom.manager@englishlab.vn", "Quản Lý Lớp Học", RoleCodes.MANAGER);
        ensureUser("staff@englishlab.vn", "Nhân Viên Đào Tạo", RoleCodes.STAFF);
        User contentManager = ensureUser("content.manager@englishlab.vn", "Quản Lý Content", RoleCodes.CONTENT_MANAGER);
        User admin = ensureUser("classroom.admin@englishlab.vn", "Nguyễn Admin", RoleCodes.ADMIN);

        Room roomA = roomRepository.findByActiveTrue().stream()
                .filter(room -> "Phòng A101".equalsIgnoreCase(room.getName()))
                .findFirst()
                .orElseGet(() -> roomRepository.save(Room.builder()
                        .name("Phòng A101")
                        .capacity(24)
                        .active(true)
                        .build()));

        Room roomB = roomRepository.findByActiveTrue().stream()
                .filter(room -> "Phòng B203".equalsIgnoreCase(room.getName()))
                .findFirst()
                .orElseGet(() -> roomRepository.save(Room.builder()
                        .name("Phòng B203")
                        .capacity(18)
                        .active(true)
                        .build()));

        seedIfMissing(OFFLINE_UPCOMING_TITLE, SLUG_OFFLINE_UPCOMING, () -> {
            ClassSection offering = createOfflineOffering(
                    OFFLINE_UPCOMING_TITLE,
                    SLUG_OFFLINE_UPCOMING,
                    ClassroomOfferingStatus.UPCOMING,
                    LocalDate.now().plusDays(14),
                    LocalDate.now().plusMonths(2),
                    teacher1,
                    roomA,
                    manager,
                    BigDecimal.valueOf(4_500_000),
                    BigDecimal.valueOf(3_990_000)
            );
            seedOfflineUpcomingData(offering, teacher1, learner1, learner2, learner3, manager);
        });

        seedIfMissing(VIRTUAL_UPCOMING_TITLE, SLUG_VIRTUAL_UPCOMING, () -> {
            ClassSection offering = createVirtualOffering(
                    VIRTUAL_UPCOMING_TITLE,
                    SLUG_VIRTUAL_UPCOMING,
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

        seedIfMissing(OFFLINE_IN_PROGRESS_TITLE, SLUG_OFFLINE_IN_PROGRESS, () -> {
            ClassSection offering = createOfflineOffering(
                    OFFLINE_IN_PROGRESS_TITLE,
                    SLUG_OFFLINE_IN_PROGRESS,
                    ClassroomOfferingStatus.ACTIVE,
                    LocalDate.now().minusWeeks(4),
                    LocalDate.now().plusWeeks(4),
                    teacher1,
                    roomB,
                    manager,
                    BigDecimal.valueOf(5_200_000),
                    BigDecimal.valueOf(4_690_000)
            );
            seedOfflineInProgressData(offering, teacher1, learner1, learner2, manager);
        });

        seedIfMissing(VIRTUAL_IN_PROGRESS_TITLE, SLUG_VIRTUAL_IN_PROGRESS, () -> {
            ClassSection offering = createVirtualOffering(
                    VIRTUAL_IN_PROGRESS_TITLE,
                    SLUG_VIRTUAL_IN_PROGRESS,
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

        seedIfMissing(REGISTRATION_PIPELINE_TITLE, SLUG_REGISTRATION_PIPELINE, () -> {
            ClassSection offering = createOfflineOffering(
                    REGISTRATION_PIPELINE_TITLE,
                    SLUG_REGISTRATION_PIPELINE,
                    ClassroomOfferingStatus.UPCOMING,
                    LocalDate.now().plusDays(21),
                    LocalDate.now().plusMonths(3),
                    teacher1,
                    roomA,
                    manager,
                    BigDecimal.valueOf(4_800_000),
                    null
            );
            seedRegistrationPipelineData(offering, learner2, learner3, learner4, manager);
        });

        seedIfMissing(COMPLETED_TITLE, SLUG_COMPLETED, () -> {
            ClassSection offering = createOfflineOffering(
                    COMPLETED_TITLE,
                    SLUG_COMPLETED,
                    ClassroomOfferingStatus.COMPLETED,
                    LocalDate.now().minusMonths(3),
                    LocalDate.now().minusWeeks(2),
                    teacher1,
                    roomA,
                    manager,
                    BigDecimal.valueOf(4_200_000),
                    BigDecimal.valueOf(3_800_000)
            );
            seedCompletedClassData(offering, teacher1, learner1, manager);
        });

        publishDemoGradebooks();
        repairRegistrationPipelineOffering();
    }

    private void repairRegistrationPipelineOffering() {
        offeringRepository.findByInstructorLedCourseSlugOrCode(SLUG_REGISTRATION_PIPELINE)
                .or(() -> offeringRepository.findByNameIgnoreCase(REGISTRATION_PIPELINE_TITLE))
                .ifPresent(offering -> {
                    if (offering.getStatus() == ClassroomOfferingStatus.CANCELLED) {
                        offering.setStatus(ClassroomOfferingStatus.UPCOMING);
                        offeringRepository.save(offering);
                    }
                });
    }

    private void publishDemoGradebooks() {
        gradebookEntryRepository.findAll().stream()
                .filter(entry -> entry.getStatus() == GradebookEntryStatus.GRADED)
                .forEach(entry -> entry.setStatus(GradebookEntryStatus.PUBLISHED));
    }

    private void syncEnglishSlugs() {
        updateSlugIfNeeded(OFFLINE_UPCOMING_TITLE, SLUG_OFFLINE_UPCOMING);
        updateSlugIfNeeded(VIRTUAL_UPCOMING_TITLE, SLUG_VIRTUAL_UPCOMING);
        updateSlugIfNeeded(OFFLINE_IN_PROGRESS_TITLE, SLUG_OFFLINE_IN_PROGRESS);
        updateSlugIfNeeded(VIRTUAL_IN_PROGRESS_TITLE, SLUG_VIRTUAL_IN_PROGRESS);
        updateSlugIfNeeded(REGISTRATION_PIPELINE_TITLE, SLUG_REGISTRATION_PIPELINE);
        updateSlugIfNeeded(COMPLETED_TITLE, SLUG_COMPLETED);
    }

    private void syncGoogleMeetLabels() {
        renameVirtualPackage(SLUG_VIRTUAL_UPCOMING, VIRTUAL_UPCOMING_TITLE);
        renameVirtualPackage(SLUG_VIRTUAL_IN_PROGRESS, VIRTUAL_IN_PROGRESS_TITLE);
    }

    private void renameVirtualPackage(String slug, String title) {
        offeringRepository.findByInstructorLedCourseSlugOrCode(slug).ifPresent(offering -> {
            if (!title.equals(offering.getName())) {
                offering.setName(title);
                offeringRepository.save(offering);
            }
        });
    }

    private void updateSlugIfNeeded(String title, String slug) {
        offeringRepository.findByNameIgnoreCase(title).ifPresent(offering -> {
            if (slug.equals(offering.getCode())) {
                return;
            }
            offering.setCode(slug);
            offeringRepository.save(offering);
        });
    }

    private void seedIfMissing(String title, String slug, Runnable seeder) {
        if (offeringRepository.findByInstructorLedCourseSlugOrCode(slug).isPresent()) {
            return;
        }
        if (offeringRepository.findByNameIgnoreCase(title).isPresent()) {
            updateSlugIfNeeded(title, slug);
            return;
        }
        seeder.run();
    }

    private void seedOfflineUpcomingData(
            ClassSection offering,
            User teacher,
            User learner1,
            User learner2,
            User learner3,
            User manager
    ) {
        ClassSchedule session1 = sessionRepository.save(ClassSchedule.builder()
                .classSection(offering)
                .sessionDate(LocalDate.now().plusDays(16))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .room(offering.getRegularRoom())
                .status(ClassroomSessionStatus.SCHEDULED)
                .sessionContent("Giới thiệu IELTS Foundation và kỹ năng Listening cơ bản")
                .build());

        ClassSchedule conflictSession = sessionRepository.save(ClassSchedule.builder()
                .classSection(offering)
                .sessionDate(LocalDate.now().plusDays(17))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .room(offering.getRegularRoom())
                .status(ClassroomSessionStatus.SCHEDULED)
                .sessionContent("Buổi học mẫu để kiểm tra xung đột lịch giáo viên")
                .build());

        enrollAssigned(offering, learner1, manager);
        enrollAssigned(offering, learner2, manager);
        enrollAssigned(offering, learner3, manager);

        homeworkRepository.save(ClassroomHomework.builder()
                .classSection(offering)
                .session(session1)
                .title("Chuẩn bị trước khai giảng")
                .instruction("Đọc tài liệu giới thiệu và ghi chú 3 mục tiêu học tập.")
                .deadline(LocalDateTime.now().plusDays(12))
                .maxScore(BigDecimal.TEN)
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());

        announcementRepository.save(ClassroomAnnouncement.builder()
                .classSection(offering)
                .title("Chào mừng lớp IELTS Foundation")
                .content("Các bạn nhớ mang sách giáo trình và đến sớm 10 phút để làm quen phòng học.")
                .createdBy(teacher)
                .build());

        changeRequestRepository.save(ClassroomChangeRequest.builder()
                .requestType(ClassroomChangeRequestType.RESCHEDULE_SESSION)
                .requester(teacher)
                .requesterRole(teacher.getPrimaryRoleCode())
                .classSection(offering)
                .targetClassSchedule(conflictSession)
                .oldValuesJson("{\"sessionDate\":\"" + conflictSession.getSessionDate() + "\"}")
                .newValuesJson("{\"sessionDate\":\"" + conflictSession.getSessionDate().plusDays(1)
                        + "\",\"startTime\":\"14:00\",\"endTime\":\"16:00\",\"teacherId\":" + teacher.getId()
                        + ",\"roomId\":" + offering.getRegularRoom().getId() + "}")
                .reason("Giáo viên có lịch họp nội bộ, đề nghị dời buổi học.")
                .status(ClassroomChangeRequestStatus.PENDING)
                .build());
    }

    private void seedVirtualUpcomingData(ClassSection offering, User teacher, User learner1, User learner2) {
        sessionRepository.save(ClassSchedule.builder()
                .classSection(offering)
                .sessionDate(LocalDate.now().plusDays(8))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 30))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingStatus(LarkMeetingStatus.NOT_CREATED)
                .larkSyncStatus("PENDING")
                .status(ClassroomSessionStatus.SCHEDULED)
                .sessionContent("Speaking Part 1 & 2 practice")
                .build());

        enrollAssigned(offering, learner1, null);
        enrollAssigned(offering, learner2, null);

        announcementRepository.save(ClassroomAnnouncement.builder()
                .classSection(offering)
                .title("Hướng dẫn vào lớp Google Meet")
                .content("Vào lớp trước 5 phút, bật mic khi giáo viên yêu cầu.")
                .createdBy(teacher)
                .build());
    }

    private void seedOfflineInProgressData(
            ClassSection offering,
            User teacher,
            User learner1,
            User learner2,
            User manager
    ) {
        ClassSchedule completed1 = saveOfflineSession(offering, teacher, LocalDate.now().minusWeeks(3), 9, 11,
                ClassroomSessionStatus.COMPLETED, "Listening: Section 1-2");
        ClassSchedule completed2 = saveOfflineSession(offering, teacher, LocalDate.now().minusWeeks(2), 9, 11,
                ClassroomSessionStatus.COMPLETED, "Reading: True/False/Not Given");
        ClassSchedule completed3 = saveOfflineSession(offering, teacher, LocalDate.now().minusWeeks(1), 9, 11,
                ClassroomSessionStatus.COMPLETED, "Writing Task 1 overview");
        ClassSchedule inProgress = saveOfflineSession(offering, teacher, LocalDate.now(), 14, 16,
                ClassroomSessionStatus.IN_PROGRESS, "Speaking mock test nhóm nhỏ");
        ClassSchedule scheduled = saveOfflineSession(offering, teacher, LocalDate.now().plusDays(4), 9, 11,
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
                .classSection(offering)
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
                .classSection(offering)
                .student(learner1)
                .homeworkScore(BigDecimal.valueOf(8.0))
                .attendancePercent(BigDecimal.valueOf(100))
                .participationScore(BigDecimal.valueOf(8.5))
                .finalResult(BigDecimal.valueOf(8.2))
                .status(GradebookEntryStatus.PUBLISHED)
                .updatedBy(teacher)
                .build());

        materialRepository.save(ClassroomMaterial.builder()
                .classSection(offering)
                .title("Slide tuần 4 - Speaking")
                .fileUrl("https://cdn.englishlab.vn/materials/ielts-intermediate-speaking-week4.pdf")
                .fileType("PDF")
                .uploadedBy(teacher)
                .build());

        syllabusItemRepository.save(ClassroomSyllabusItem.builder()
                .classSection(offering)
                .title("Tuần 4 - Speaking intensive")
                .description("Luyện Part 2 và feedback cá nhân")
                .displayOrder(4)
                .sessionPlan("Buổi hôm nay: mock test + sửa lỗi phát âm")
                .status("PUBLISHED")
                .build());
    }

    private void seedVirtualInProgressData(
            ClassSection offering,
            User teacher,
            User learner1,
            User learner3,
            User manager
    ) {
        ClassSchedule completed = sessionRepository.save(ClassSchedule.builder()
                .classSection(offering)
                .sessionDate(LocalDate.now().minusDays(7))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 30))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingStatus(LarkMeetingStatus.NOT_CREATED)
                .larkSyncStatus("PENDING")
                .status(ClassroomSessionStatus.COMPLETED)
                .sessionContent("TOEIC Listening Part 1-2")
                .build());

        ClassSchedule liveSession = sessionRepository.save(ClassSchedule.builder()
                .classSection(offering)
                .sessionDate(LocalDate.now())
                .startTime(LocalTime.of(19, 30))
                .endTime(LocalTime.of(21, 0))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingStatus(LarkMeetingStatus.NOT_CREATED)
                .larkSyncStatus("PENDING")
                .status(ClassroomSessionStatus.OPEN)
                .sessionContent("TOEIC Speaking practice hôm nay")
                .build());

        sessionRepository.save(ClassSchedule.builder()
                .classSection(offering)
                .sessionDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 30))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingStatus(LarkMeetingStatus.NOT_CREATED)
                .larkSyncStatus("PENDING")
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
                .classSection(offering)
                .session(liveSession)
                .title("Ghi âm phản hồi Speaking")
                .instruction("Ghi âm 90 giây mô tả dự án gần nhất và nộp link.")
                .deadline(LocalDateTime.now().plusDays(2))
                .status(HomeworkStatus.OPEN)
                .createdBy(teacher)
                .build());
    }

    private void seedRegistrationPipelineData(
            ClassSection offering,
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

        ClassEnrollment partial = enrollmentRepository
                .findByStudentIdAndClassSectionId(learner3.getId(), offering.getId())
                .orElseThrow();
        tuitionPaymentRepository.save(ClassroomTuitionPayment.builder()
                .enrollment(partial)
                .amount(BigDecimal.valueOf(1_200_000))
                .paymentKind(TuitionPaymentKind.PARTIAL)
                .note("Đặt cọc 25% học phí")
                .recordedBy(manager)
                .build());
    }

    private void seedCompletedClassData(ClassSection offering, User teacher, User learner1, User manager) {
        ClassSchedule completedSession = saveOfflineSession(offering, teacher, LocalDate.now().minusWeeks(3), 9, 11,
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
                .classSection(offering)
                .student(learner1)
                .homeworkScore(BigDecimal.valueOf(9.0))
                .attendancePercent(BigDecimal.valueOf(95))
                .participationScore(BigDecimal.valueOf(9.5))
                .finalResult(BigDecimal.valueOf(9.1))
                .status(GradebookEntryStatus.PUBLISHED)
                .updatedBy(teacher)
                .build());
    }

    private ClassSchedule saveOfflineSession(
            ClassSection offering,
            User teacher,
            LocalDate date,
            int startHour,
            int endHour,
            ClassroomSessionStatus status,
            String content
    ) {
        return sessionRepository.save(ClassSchedule.builder()
                .classSection(offering)
                .sessionDate(date)
                .startTime(LocalTime.of(startHour, 0))
                .endTime(LocalTime.of(endHour, 0))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .room(offering.getRegularRoom())
                .status(status)
                .sessionContent(content)
                .build());
    }

    private ClassSection createOfflineOffering(
            String title,
            String slug,
            ClassroomOfferingStatus status,
            LocalDate startDate,
            LocalDate endDate,
            User teacher,
            Room room,
            User manager,
            BigDecimal price,
            BigDecimal salePrice
    ) {
        ClassSection offering = offeringRepository.save(ClassSection.builder()
                .name(title)
                .code(slug)
                .tuitionFeeVnd(salePrice != null ? salePrice : price)
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .status(status)
                .entryLevel("4.0 - 6.0")
                .targetOutcome("Đạt band 5.5-6.5")
                .capacity(20)
                .startDate(startDate)
                .plannedEndDate(endDate)
                .primaryTeacher(teacher)
                .regularRoom(room)
                .offlineAddress(DEFAULT_OFFLINE_ADDRESS)
                .locationNote(room.getName() + ", tầng 2")
                .syllabusSummary("Listening, Reading, Writing & Speaking theo lộ trình 8 tuần")
                .build());

        teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                .classSection(offering)
                .teacher(teacher)
                .role(ClassroomTeacherRole.PRIMARY)
                .effectiveFrom(startDate)
                .build());
        return offering;
    }

    private ClassSection createVirtualOffering(
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
        ClassSection offering = offeringRepository.save(ClassSection.builder()
                .name(title)
                .code(slug)
                .tuitionFeeVnd(salePrice != null ? salePrice : price)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .status(status)
                .entryLevel("5.0+")
                .targetOutcome("Tự tin giao tiếp và luyện thi")
                .capacity(12)
                .startDate(startDate)
                .plannedEndDate(endDate)
                .primaryTeacher(teacher)
                .larkMeetingStatus(LarkMeetingStatus.NOT_CREATED)
                .recordingVisible(true)
                .syllabusSummary("Buổi live + bài tập + feedback cá nhân")
                .build());

        teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                .classSection(offering)
                .teacher(teacher)
                .role(ClassroomTeacherRole.PRIMARY)
                .effectiveFrom(startDate)
                .build());
        return offering;
    }

    private void enrollAssigned(ClassSection offering, User learner, User assignedBy) {
        seedEnrollment(offering, learner, ClassroomRegistrationStatus.ASSIGNED,
                tuitionDue(offering), tuitionDue(offering), true, assignedBy);
    }

    private void seedEnrollment(
            ClassSection offering,
            User learner,
            ClassroomRegistrationStatus registrationStatus,
            BigDecimal tuitionPaid,
            BigDecimal tuitionDueAmount,
            boolean withAssignmentMeta
    ) {
        seedEnrollment(offering, learner, registrationStatus, tuitionPaid, tuitionDueAmount, withAssignmentMeta, null);
    }

    private void seedEnrollment(
            ClassSection offering,
            User learner,
            ClassroomRegistrationStatus registrationStatus,
            BigDecimal tuitionPaid,
            BigDecimal tuitionDueAmount,
            boolean withAssignmentMeta,
            User assignedBy
    ) {
        if (enrollmentRepository.existsByStudentIdAndClassSectionIdAndRegistrationStatusIn(
                learner.getId(),
                offering.getId(),
                ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS
        )) {
            return;
        }
        BigDecimal due = tuitionDueAmount != null ? tuitionDueAmount : tuitionDue(offering);
        BigDecimal paid = tuitionPaid != null ? tuitionPaid : BigDecimal.ZERO;
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .student(learner)
                .classSection(offering)
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

    private BigDecimal tuitionDue(ClassSection offering) {
        if (offering.getSalePrice() != null) {
            return offering.getSalePrice();
        }
        return offering.getPrice() == null
                ? BigDecimal.ZERO
                : offering.getPrice();
    }

    private void clearLegacyDemoLarkLinks() {
        clearOfferingDemoLarkLinks(VIRTUAL_UPCOMING_TITLE, LEGACY_DEMO_LARK_URL_SPEAKING);
        clearOfferingDemoLarkLinks(VIRTUAL_IN_PROGRESS_TITLE, LEGACY_DEMO_LARK_URL_TOEIC);
    }

    private void clearOfferingDemoLarkLinks(String offeringTitle, String legacyDemoLarkUrl) {
        offeringRepository.findByNameIgnoreCase(offeringTitle).ifPresent(offering -> {
            if (legacyDemoLarkUrl.equalsIgnoreCase(offering.getDefaultLarkMeetingUrl())) {
                offering.setDefaultLarkMeetingUrl(null);
                offering.setLarkMeetingStatus(LarkMeetingStatus.NOT_CREATED);
                offeringRepository.save(offering);
            }

            sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId())
                    .forEach(session -> {
                        boolean hasLegacyUrl = legacyDemoLarkUrl.equalsIgnoreCase(session.getLarkMeetingUrl());
                        boolean hasDemoStatus = "DEMO".equalsIgnoreCase(session.getLarkSyncStatus());
                        if (!hasLegacyUrl && !hasDemoStatus) {
                            return;
                        }
                        session.setLarkMeetingUrl(null);
                        session.setLarkMeetingStatus(LarkMeetingStatus.NOT_CREATED);
                        session.setLarkSyncStatus("PENDING");
                        session.setLarkSyncError(null);
                        sessionRepository.save(session);
                    });
        });
    }

    private void syncTeacher2Account() {
        Optional<User> legacyTeacher = userRepository.findByEmail(LEGACY_TEACHER2_EMAIL);
        Optional<User> targetEmailUser = userRepository.findByEmail(TEACHER2_EMAIL);

        if (targetEmailUser.isPresent()) {
            User target = targetEmailUser.get();
            if (legacyTeacher.isPresent() && !Objects.equals(target.getId(), legacyTeacher.get().getId())) {
                log.info("Removing conflicting account {} before assigning demo teacher email.", TEACHER2_EMAIL);
                removeUserAccount(target.getId());
                targetEmailUser = Optional.empty();
            } else if (legacyTeacher.isEmpty()) {
                finalizeTeacherAccount(target);
                return;
            }
        }

        if (legacyTeacher.isPresent()) {
            User teacher = legacyTeacher.get();
            teacher.setEmail(TEACHER2_EMAIL);
            finalizeTeacherAccount(teacher);
            log.info("Updated demo teacher account email to {}.", TEACHER2_EMAIL);
        }
    }

    private void finalizeTeacherAccount(User teacher) {
        teacher.setFullName(TEACHER2_FULL_NAME);
        teacher.setEmailVerified(true);
        if (teacher.getPassword() == null || teacher.getPassword().isBlank()) {
            teacher.setPassword(passwordEncoder.encode("Password123!"));
        }
        userRoleService.replaceRoles(teacher, RoleCodes.TEACHER);
        userRepository.save(teacher);
    }

    private void removeUserAccount(Long userId) {
        jdbcTemplate.update(
                "delete from classroom_tuition_payments where enrollment_id in (select id from classroom_enrollments where student_id = ?)",
                userId
        );
        jdbcTemplate.update("delete from classroom_attendance_records where student_id = ?", userId);
        jdbcTemplate.update("delete from classroom_homework_submissions where student_id = ?", userId);
        jdbcTemplate.update("delete from classroom_gradebook_entries where student_id = ?", userId);
        jdbcTemplate.update("delete from classroom_enrollments where student_id = ?", userId);
        jdbcTemplate.update("delete from lesson_progress where student_id = ?", userId);
        jdbcTemplate.update("delete from vocabulary_progress where student_id = ?", userId);
        jdbcTemplate.update("delete from assessment_submissions where student_id = ?", userId);
        jdbcTemplate.update("delete from placement_test_attempts where student_id = ?", userId);
        jdbcTemplate.update("delete from package_enrollments where student_id = ?", userId);
        jdbcTemplate.update("delete from payment_orders where student_id = ?", userId);
        jdbcTemplate.update("delete from course_discussion_reply_votes where user_id = ?", userId);
        jdbcTemplate.update("delete from course_discussion_reactions where user_id = ?", userId);
        jdbcTemplate.update("delete from course_discussion_reports where reporter_id = ?", userId);
        jdbcTemplate.update(
                """
                        delete from course_discussion_reply_votes
                        where reply_id in (select id from course_discussion_replies where author_id = ?)
                        """,
                userId
        );
        jdbcTemplate.update("delete from course_discussion_replies where author_id = ?", userId);
        jdbcTemplate.update("delete from course_discussion_threads where author_id = ?", userId);
        jdbcTemplate.update("delete from app_notifications where user_id = ?", userId);
        jdbcTemplate.update("delete from auth_tokens where user_id = ?", userId);
        jdbcTemplate.update("delete from user_roles where user_id = ?", userId);
        jdbcTemplate.update("delete from users where id = ?", userId);
    }

    /*
     * Session-template persistence was removed from the current classroom model.
     * Keep the former demo data below disabled until the feature is reintroduced.
    private void seedSessionTemplates() {
        User staff = userRepository.findByEmail("staff@englishlab.vn").orElse(null);

        ensureSessionTemplate(
                "Tối 246 (18:30–20:30)",
                """
                [{"dayOfWeek":1,"startTime":"18:30","endTime":"20:30"},{"dayOfWeek":3,"startTime":"18:30","endTime":"20:30"},{"dayOfWeek":5,"startTime":"18:30","endTime":"20:30"}]
                """.trim(),
                "Lịch tối Thứ 2-4-6. Dùng khi demo sinh lịch cho lớp IELTS/TOEIC tại trung tâm.",
                "Warm-up 10 phút → giảng mới 50 phút → luyện tập 40 phút → nhận xét & giao bài 20 phút.",
                "Pair work, role-play, error correction board.",
                "Làm bài tập củng cố trong workbook và nộp trước buổi kế tiếp.",
                120,
                staff
        );

        ensureSessionTemplate(
                "Tối 357 (18:30–20:30)",
                """
                [{"dayOfWeek":2,"startTime":"18:30","endTime":"20:30"},{"dayOfWeek":4,"startTime":"18:30","endTime":"20:30"},{"dayOfWeek":6,"startTime":"18:30","endTime":"20:30"}]
                """.trim(),
                "Lịch tối Thứ 3-5-7. Phù hợp lớp ca tối xen kẽ với ca 246.",
                "Ôn nhanh 10 phút → input 45 phút → practice 45 phút → wrap-up 20 phút.",
                "Group discussion, peer feedback, mini presentation.",
                "Ôn từ vựng buổi học và làm 1 bài listening ngắn.",
                120,
                staff
        );

        ensureSessionTemplate(
                "Sáng cuối tuần (08:00–10:00)",
                """
                [{"dayOfWeek":6,"startTime":"08:00","endTime":"10:00"},{"dayOfWeek":7,"startTime":"08:00","endTime":"10:00"}]
                """.trim(),
                "Lịch sáng Thứ 7 và Chủ nhật. Phù hợp học viên đi học cuối tuần.",
                "Check-in 10 phút → skill focus 60 phút → workshop 40 phút → homework brief 10 phút.",
                "Speaking circle, writing clinic, mock quiz.",
                "Hoàn thành worksheet cuối tuần và mang lại buổi kế tiếp.",
                120,
                staff
        );

        ensureSessionTemplate(
                "Chiều 246 (14:00–16:00)",
                """
                [{"dayOfWeek":1,"startTime":"14:00","endTime":"16:00"},{"dayOfWeek":3,"startTime":"14:00","endTime":"16:00"},{"dayOfWeek":5,"startTime":"14:00","endTime":"16:00"}]
                """.trim(),
                "Lịch chiều Thứ 2-4-6. Dùng cho lớp học sinh/sinh viên học ca chiều.",
                "Review homework 15 phút → giảng mới 50 phút → practice 40 phút → Q&A 15 phút.",
                "Board race, vocabulary games, short reading race.",
                "Làm 1 unit workbook và ghi lại 5 lỗi cần sửa.",
                120,
                staff
        );

        log.info("Classroom session templates are ready for demo.");
    }

    private void ensureSessionTemplate(
            String name,
            String slotsJson,
            String description,
            String teacherGuide,
            String interactionActivities,
            String postSessionHomework,
            Integer defaultDurationMinutes,
            User createdBy
    ) {
        sessionTemplateRepository.findByNameIgnoreCase(name).ifPresentOrElse(existing -> {
            existing.setSlotsJson(slotsJson);
            existing.setDescription(description);
            existing.setTeacherGuide(teacherGuide);
            existing.setInteractionActivities(interactionActivities);
            existing.setPostSessionHomework(postSessionHomework);
        User staff = userRepository.findByEmail("staff@englishlab.vn").orElse(null);

        ensureSessionTemplate(
                "Tối 246 (18:30–20:30)",
                """
                [{"dayOfWeek":1,"startTime":"18:30","endTime":"20:30"},{"dayOfWeek":3,"startTime":"18:30","endTime":"20:30"},{"dayOfWeek":5,"startTime":"18:30","endTime":"20:30"}]
                """.trim(),
                "Lịch tối Thứ 2-4-6. Dùng khi demo sinh lịch cho lớp IELTS/TOEIC tại trung tâm.",
                "Warm-up 10 phút → giảng mới 50 phút → luyện tập 40 phút → nhận xét & giao bài 20 phút.",
                "Pair work, role-play, error correction board.",
                "Làm bài tập củng cố trong workbook và nộp trước buổi kế tiếp.",
                120,
                staff
        );

        ensureSessionTemplate(
                "Tối 357 (18:30–20:30)",
                """
                [{"dayOfWeek":2,"startTime":"18:30","endTime":"20:30"},{"dayOfWeek":4,"startTime":"18:30","endTime":"20:30"},{"dayOfWeek":6,"startTime":"18:30","endTime":"20:30"}]
                """.trim(),
                "Lịch tối Thứ 3-5-7. Phù hợp lớp ca tối xen kẽ với ca 246.",
                "Ôn nhanh 10 phút → input 45 phút → practice 45 phút → wrap-up 20 phút.",
                "Group discussion, peer feedback, mini presentation.",
                "Ôn từ vựng buổi học và làm 1 bài listening ngắn.",
                120,
                staff
        );

        ensureSessionTemplate(
                "Sáng cuối tuần (08:00–10:00)",
                """
                [{"dayOfWeek":6,"startTime":"08:00","endTime":"10:00"},{"dayOfWeek":7,"startTime":"08:00","endTime":"10:00"}]
                """.trim(),
                "Lịch sáng Thứ 7 và Chủ nhật. Phù hợp học viên đi học cuối tuần.",
                "Check-in 10 phút → skill focus 60 phút → workshop 40 phút → homework brief 10 phút.",
                "Speaking circle, writing clinic, mock quiz.",
                "Hoàn thành worksheet cuối tuần và mang lại buổi kế tiếp.",
                120,
                staff
        );

        ensureSessionTemplate(
                "Chiều 246 (14:00–16:00)",
                """
                [{"dayOfWeek":1,"startTime":"14:00","endTime":"16:00"},{"dayOfWeek":3,"startTime":"14:00","endTime":"16:00"},{"dayOfWeek":5,"startTime":"14:00","endTime":"16:00"}]
                """.trim(),
                "Lịch chiều Thứ 2-4-6. Dùng cho lớp học sinh/sinh viên học ca chiều.",
                "Review homework 15 phút → giảng mới 50 phút → practice 40 phút → Q&A 15 phút.",
                "Board race, vocabulary games, short reading race.",
                "Làm 1 unit workbook và ghi lại 5 lỗi cần sửa.",
                120,
                staff
        );

        log.info("Classroom session templates are ready for demo.");
    }

    private void ensureSessionTemplate(
            String name,
            String slotsJson,
            String description,
            String teacherGuide,
            String interactionActivities,
            String postSessionHomework,
            Integer defaultDurationMinutes,
            User createdBy
    ) {
        sessionTemplateRepository.findByNameIgnoreCase(name).ifPresentOrElse(existing -> {
            existing.setSlotsJson(slotsJson);
            existing.setDescription(description);
            existing.setTeacherGuide(teacherGuide);
            existing.setInteractionActivities(interactionActivities);
            existing.setPostSessionHomework(postSessionHomework);
            existing.setDefaultDurationMinutes(defaultDurationMinutes);
            existing.setActive(true);
            if (existing.getCreatedBy() == null && createdBy != null) {
                existing.setCreatedBy(createdBy);
            }
            sessionTemplateRepository.save(existing);
        }, () -> sessionTemplateRepository.save(ClassScheduleTemplate.builder()
                .name(name)
                .slotsJson(slotsJson)
                .description(description)
                .teacherGuide(teacherGuide)
                .interactionActivities(interactionActivities)
                .postSessionHomework(postSessionHomework)
                .defaultDurationMinutes(defaultDurationMinutes)
                .active(true)
                .createdBy(createdBy)
                .build()));
    }

    }
    */

    private User ensureUser(String email, String fullName, String roleCode) {
        User user = userRepository.findByEmail(email).map(existing -> {
            userRoleService.replaceRoles(existing, roleCode);
            existing.setFullName(fullName);
            return userRepository.save(existing);
        }).orElseGet(() -> {
            User created = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .password(passwordEncoder.encode("Password123!"))
                    .emailVerified(true)
                    .build();
            userRoleService.assignRole(created, roleCode);
            return userRepository.save(created);
        });
        if (RoleCodes.LEARNER.equals(roleCode)) {
            return demoLearnerOnboardingSupport.ensureReady(user);
        }
        return user;
    }

    private void syncTodayTeacher1Data() {
        try {
            User teacher1 = userRepository.findByEmail("classroom.teacher1@englishlab.vn").orElse(null);
            if (teacher1 == null) return;

            User manager = userRepository.findByEmail("classroom.manager@englishlab.vn").orElse(null);
            User learner1 = ensureUser("0386852628z@gmail.com", "Lê Ngọc Anh", RoleCodes.LEARNER);
            User learner2 = ensureUser("classroom.learner2@englishlab.vn", "Phạm Minh Châu", RoleCodes.LEARNER);
            User learner3 = ensureUser("classroom.learner3@englishlab.vn", "Hoàng Gia Huy", RoleCodes.LEARNER);

            Optional<ClassSection> offeringOpt = offeringRepository.findByInstructorLedCourseSlugOrCode(SLUG_OFFLINE_IN_PROGRESS);
            if (offeringOpt.isEmpty()) return;

            ClassSection offering = offeringOpt.get();

            boolean assigned = teacherAssignmentRepository.findByClassSectionId(offering.getId()).stream()
                    .anyMatch(a -> a.getTeacher().getId().equals(teacher1.getId()));
            if (!assigned) {
                teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                        .classSection(offering)
                        .teacher(teacher1)
                        .role(ClassroomTeacherRole.PRIMARY)
                        .effectiveFrom(LocalDate.now().minusWeeks(4))
                        .build());
            }

            enrollAssigned(offering, learner1, manager);
            enrollAssigned(offering, learner2, manager);
            enrollAssigned(offering, learner3, manager);

            LocalDate today = LocalDate.now();
            java.util.List<ClassSchedule> sessions = sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId());
            boolean hasTodaySession = sessions.stream().anyMatch(s -> today.equals(s.getSessionDate()));

            if (!hasTodaySession) {
                if (sessions.isEmpty()) {
                    saveOfflineSession(offering, teacher1, today, 19, 21, ClassroomSessionStatus.OPEN, "Buổi 5: Writing Task 2 – Opinion essay (Hôm nay)");
                } else {
                    ClassSchedule targetClassSchedule = sessions.stream()
                            .filter(s -> s.getStatus() == ClassroomSessionStatus.OPEN || s.getStatus() == ClassroomSessionStatus.SCHEDULED)
                            .findFirst()
                            .orElse(sessions.get(0));
                    targetClassSchedule.setSessionDate(today);
                    targetClassSchedule.setStatus(ClassroomSessionStatus.OPEN);
                    targetClassSchedule.setSessionContent("Buổi 5: Writing Task 2 – Opinion essay (Hôm nay)");
                    sessionRepository.save(targetClassSchedule);
                }
            }

            seedRichSubmissionsForAllHomeworks(offering, learner1, learner2, learner3);
            offeringRepository.findByInstructorLedCourseSlugOrCode("ielts-intensive-chinh-test-v1")
                    .ifPresent(chinhOffering -> seedRichSubmissionsForAllHomeworks(chinhOffering, learner1, learner2, learner3));
        } catch (Exception ex) {
            log.warn("syncTodayTeacher1Data warning: {}", ex.getMessage());
        }
    }

    private void seedRichSubmissionsForAllHomeworks(ClassSection offering, User learner1, User learner2, User learner3) {
        java.util.List<ClassroomHomework> homeworks = homeworkRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId());
        if (homeworks.isEmpty()) return;

        String essay1 = """
                In the contemporary era, technological advancements have reshaped human society in unprecedented ways. While some individuals argue that these innovations have complicated daily routines, I firmly believe that technology ultimately simplifies modern living by improving communication, automating labor-intensive tasks, and enhancing global accessibility.

                On the one hand, critics contend that rapid technological proliferation introduces unnecessary complexity into our lives. Constant notifications, social media overload, and the steep learning curve required to master new software can cause mental fatigue and anxiety. For instance, elderly populations often struggle to navigate online banking platforms or digital government services, leading to a sense of exclusion and confusion. Furthermore, the blur between work and personal life caused by instant messaging applications often results in elevated stress levels among modern professionals.

                On the other hand, the primary function of technology is to streamline human endeavors and optimize productivity. Firstly, modern communication channels such as video conferencing and electronic mail allow individuals to collaborate across geographic boundaries seamlessly. Secondly, household automation—ranging from smart appliances to online grocery delivery—saves considerable time and physical effort, allowing people to focus on personal development and leisure activities. Finally, healthcare technological breakthroughs have significantly improved diagnostic accuracy and treatment efficiency, saving millions of lives worldwide.

                In conclusion, although the misapplication of digital devices can occasionally generate stress and confusion, the overarching benefits of technology far outweigh its drawbacks. By establishing healthy digital boundaries, society can harness technological progress to foster a more efficient, connected, and convenient world.
                """.trim();

        String essay2 = """
                The primary objective of higher education has been a topic of intense debate in recent years. While some argue that universities should exclusively train students for immediate employment skills, I am convinced that a balanced curriculum integrating both theoretical foundations and practical applications is essential for long-term career success and personal growth.

                Proponents of vocational training argue that the modern job market demands specialized skills. In rapidly evolving industries such as software engineering, data analytics, and digital marketing, employers prioritize candidates who possess hands-on proficiency with tools and frameworks over those who only understand abstract concepts. Consequently, universities that emphasize practical workshops and industry internships help graduates secure employment faster and reduce retraining costs for corporations.

                However, theoretical knowledge remains the cornerstone of critical thinking and adaptability. Without a solid understanding of fundamental principles, professionals risk becoming obsolete when technology or market demands shift. For example, a civil engineer must thoroughly comprehend structural mechanics and physics before applying design software; otherwise, catastrophic structural failures could occur. Furthermore, academic research and theoretical exploration drive innovation, inspiring breakthroughs that transform entire industries rather than merely maintaining the status quo.

                To conclude, prioritizing job skills at the expense of theoretical learning would be short-sighted. Educational institutions should strive to offer a holistic education that equips students with both fundamental theoretical insights and practical competencies, ensuring they remain versatile and innovative throughout their professional careers.
                """.trim();

        String essay3 = """
                Infrastructure investment is a crucial responsibility of modern governments. While expanding road networks may temporarily relieve traffic congestion, I strongly advocate for prioritizing public transportation funding because it offers a more sustainable solution to urban traffic problems, reduces environmental pollution, and promotes social equity.

                Firstly, investing heavily in public transit systems like subways, light rail, and electric buses provides a high-capacity solution to urban mobility. Constructing new highways often triggers induced demand—a phenomenon where new lanes attract more private vehicle users, quickly returning congestion to previous levels. In contrast, efficient metro systems can transport tens of thousands of commuters per hour without clogging city streets, thereby significantly reducing travel times for urban residents.

                Secondly, public transportation investment yields substantial environmental and economic advantages. Private vehicles are major contributors to greenhouse gas emissions and urban air pollution. By transitioning commuters to clean electric transit, cities can dramatically lower carbon footprints and improve public health outcomes. Furthermore, accessible public transportation enables lower-income citizens to access employment and educational opportunities across the city without the burdensome costs of vehicle ownership and fuel.

                In conclusion, while road maintenance remains necessary, allocating primary financial resources toward public transportation is a superior strategy for urban development. A robust public transit network creates cleaner, more efficient, and inclusive cities for future generations.
                """.trim();

        for (ClassroomHomework hw : homeworks) {
            saveOrUpdateSubmission(hw, learner1, essay1, HomeworkSubmissionStatus.SUBMITTED);
            saveOrUpdateSubmission(hw, learner2, essay2, HomeworkSubmissionStatus.SUBMITTED);
            saveOrUpdateSubmission(hw, learner3, essay3, HomeworkSubmissionStatus.SUBMITTED);
        }
    }

    private void saveOrUpdateSubmission(ClassroomHomework hw, User student, String content, HomeworkSubmissionStatus status) {
        if (student == null) return;
        ClassroomHomeworkSubmission sub = homeworkSubmissionRepository.findByHomeworkIdAndStudentId(hw.getId(), student.getId())
                .orElseGet(() -> ClassroomHomeworkSubmission.builder()
                        .homework(hw)
                        .student(student)
                        .build());
        sub.setTextAnswer(content);
        sub.setStatus(status);
        sub.setSubmittedAt(LocalDateTime.now().minusHours(2));
        homeworkSubmissionRepository.save(sub);
    }
}
