package fu.sep490.g23.backend.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.request.curriculum.AssessmentBankItemRequest;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.classroom.ClassroomAnnouncement;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sep490.g23.backend.entity.classroom.ClassroomCampus;
import fu.sep490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.ClassroomRoom;
import fu.sep490.g23.backend.entity.classroom.ClassroomSession;
import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomAttendanceStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionSettlementType;
import fu.sep490.g23.backend.entity.course.Lesson;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.PackageEnrollment;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.PackageType;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAnnouncementRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomCampusRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.PackageTypeRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import fu.sep490.g23.backend.service.assessment.PlacementTestService;
import fu.sep490.g23.backend.service.curriculum.CurriculumProgramService;
import fu.sep490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Order(320)
@RequiredArgsConstructor
@Slf4j
public class CenterSheetDataSeeder implements CommandLineRunner {

    static final String TEACHER_EMAIL = "alien1062004@gmail.com";
    static final String LEARNER_EMAIL = "0386852628z@gmail.com";
    private static final String PASSWORD = "Password123!";
    private static final String CAMPUS_NAME = "EnglishLab Hai Bà Trưng";
    private static final String ADDRESS = "123 Phố Huế, Hai Bà Trưng, Hà Nội";
    private static final LocalTime SLOT_1_START = LocalTime.of(18, 0);
    private static final LocalTime SLOT_1_END = LocalTime.of(19, 30);
    private static final LocalTime SLOT_2_START = LocalTime.of(19, 45);
    private static final LocalTime SLOT_2_END = LocalTime.of(21, 15);
    private static final Set<DayOfWeek> MWF = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    private static final Set<DayOfWeek> TTS = EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY);
    private static final String[] TEACHER_NAMES = {
            "Nguyễn Minh Trí", "Trần Thu Hà", "Lê Quang Huy", "Phạm Ngọc Anh", "Hoàng Mai Linh",
            "Vũ Đình Nam", "Đặng Thảo Nhi", "Bùi Tuấn Kiệt", "Đỗ Mỹ Dung", "Ngô Thanh Sơn",
            "Lý Khánh Vân", "Cao Đức Long", "Mai Phương Thảo", "Trịnh Gia Bảo", "Phan Hà My",
            "Đinh Việt Anh", "Lâm Quỳnh Chi", "Huỳnh Tấn Phát", "Võ Bảo Ngọc"
    };
    private static final String[] FIRST_NAMES = {
            "An", "Bình", "Chi", "Dũng", "Giang", "Hà", "Khánh", "Linh", "Minh", "Nam",
            "Oanh", "Phúc", "Quỳnh", "Sơn", "Tâm", "Uyên", "Vy", "Yến", "Huy", "My"
    };
    private static final String[] LAST_NAMES = {
            "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Vũ", "Đặng", "Bùi", "Đỗ", "Ngô"
    };

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final PackageTypeRepository packageTypeRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final PackageEnrollmentRepository packageEnrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ClassroomCampusRepository campusRepository;
    private final ClassroomRoomRepository roomRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomAttendanceRepository attendanceRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository homeworkSubmissionRepository;
    private final ClassroomAnnouncementRepository announcementRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final AssessmentBankItemRepository assessmentBankItemRepository;
    private final PlacementTestAttemptRepository placementAttemptRepository;
    private final PlacementTestDefinitionService placementTestDefinitionService;
    private final PlacementTestService placementTestService;
    private final CurriculumProgramService curriculumProgramService;
    private final CenterSheetCourseCatalog courseCatalog;
    private final DemoLearnerOnboardingSupport onboardingSupport;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.sheet.enabled:false}")
    private boolean sheetEnabled;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!sheetEnabled) {
            return;
        }
        log.info("[CenterSheet] Nap du lieu quy mo trung tam (local sheet DB), flag={}", sheetEnabled);
        try {
            seedPackageTypes();
            User contentManager = ensureUser("content.manager@englishlab.vn", "Quản lý Content", RoleEnum.CONTENT_MANAGER);
        ensureUser("classroom.admin@englishlab.vn", "Nguyễn Admin", RoleEnum.ADMIN);
        ensureUser("classroom.manager@englishlab.vn", "Quản lý lớp học", RoleEnum.MANAGER);
        ensureUser("staff@englishlab.vn", "Nhân viên đào tạo", RoleEnum.STAFF);

        User alien = ensureExistingOrCreate(TEACHER_EMAIL, "Alien Teacher", RoleEnum.TEACHER);
        List<User> teachers = new ArrayList<>();
        teachers.add(alien);
        for (int i = 0; i < TEACHER_NAMES.length; i++) {
            teachers.add(ensureUser("gv.sheet.%02d@englishlab.vn".formatted(i + 1), TEACHER_NAMES[i], RoleEnum.TEACHER));
        }

        User showcaseLearner = ensureExistingOrCreate(LEARNER_EMAIL, "Lê Học viên Showcase", RoleEnum.LEARNER);
        onboardingSupport.ensureReady(showcaseLearner);

        List<User> learners = new ArrayList<>();
        learners.add(showcaseLearner);
        for (int i = 1; i <= 299; i++) {
            String name = LAST_NAMES[i % LAST_NAMES.length] + " " + FIRST_NAMES[i % FIRST_NAMES.length] + " " + i;
            learners.add(ensureUser("hs.sheet.%03d@englishlab.vn".formatted(i), name, RoleEnum.LEARNER));
        }

        courseCatalog.seed(contentManager);
        try {
            seedPublishedMockTests();
        } catch (Exception ex) {
            log.warn("[CenterSheet] Khong xuat ban het mock test: {}", ex.getMessage());
        }
        seedPlacementViaApi(showcaseLearner);

        ClassroomCampus campus = ensureCampus();
        List<ClassroomRoom> rooms = ensureRooms(campus);
        List<ClassroomOffering> offerings = seedClasses(teachers, rooms, learners, showcaseLearner, alien);
        seedShowcaseOnlineProgress(showcaseLearner);
        seedAlienClassExtras(offerings, alien, showcaseLearner);
        log.info("[CenterSheet] Xong. GV {} | HV {} | lop {}", TEACHER_EMAIL, LEARNER_EMAIL, offerings.size());
        } catch (Exception ex) {
            log.error("[CenterSheet] Seeder gap, da giu phan data tao duoc trong transaction hien tai neu loi da duoc bat. Chi tiet:", ex);
        }
    }

    private void seedPackageTypes() {
        upsertType(PackageTypeCode.ONLINE_COURSE, "Online Course");
        upsertType(PackageTypeCode.CLASSROOM, "Classroom");
        upsertType(PackageTypeCode.MOCK_TEST, "Mock Test");
        upsertType(PackageTypeCode.SUBSCRIPTION, "Subscription");
        upsertType(PackageTypeCode.BUNDLE, "Bundle");
    }

    private void upsertType(PackageTypeCode code, String name) {
        if (!packageTypeRepository.existsByCode(code)) {
            packageTypeRepository.save(PackageType.builder().code(code).name(name).description(name).active(true).build());
        }
    }

    private ClassroomCampus ensureCampus() {
        return campusRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(item -> CAMPUS_NAME.equalsIgnoreCase(item.getName()))
                .findFirst()
                .orElseGet(() -> campusRepository.save(ClassroomCampus.builder()
                        .name(CAMPUS_NAME)
                        .address(ADDRESS)
                        .note("10 phòng cố định, 2 ca tối, nghỉ Chủ nhật")
                        .active(true)
                        .build()));
    }

    private List<ClassroomRoom> ensureRooms(ClassroomCampus campus) {
        List<ClassroomRoom> rooms = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String name = "Phòng P%03d".formatted(i);
            ClassroomRoom room = roomRepository.findByActiveTrue().stream()
                    .filter(item -> name.equalsIgnoreCase(item.getName()))
                    .findFirst()
                    .orElseGet(() -> roomRepository.save(ClassroomRoom.builder()
                            .name(name)
                            .campus(campus)
                            .capacity(12)
                            .active(true)
                            .build()));
            rooms.add(room);
        }
        return rooms;
    }

    private List<ClassroomOffering> seedClasses(
            List<User> teachers,
            List<ClassroomRoom> rooms,
            List<User> learners,
            User showcaseLearner,
            User alien
    ) {
        PackageType classroomType = packageTypeRepository.findByCode(PackageTypeCode.CLASSROOM)
                .orElseThrow(() -> new IllegalStateException("CLASSROOM package type missing"));
        List<ClassroomOffering> offerings = new ArrayList<>();
        int learnerCursor = 1;
        for (int classIndex = 0; classIndex < 30; classIndex++) {
            boolean online = classIndex >= 24;
            int intake = classIndex % 3;
            LocalDate start = LocalDate.now().minusWeeks(10 - intake * 4L);
            LocalDate end = start.plusWeeks(12);
            boolean mwf = (classIndex / 3) % 2 == 0;
            boolean eveningTwo = classIndex % 2 == 1;
            ClassroomRoom room = online ? null : rooms.get(classIndex % 10);
            User teacher = (classIndex == 0 || classIndex == 24) ? alien : teachers.get(1 + (classIndex % 19));
            String slug = "center-sheet-class-%02d".formatted(classIndex + 1);
            String title = (online ? "IELTS Live Meet " : "IELTS Center ")
                    + (intake == 0 ? "K1 " : intake == 1 ? "K2 " : "K3 ")
                    + (mwf ? "T2-4-6 " : "T3-5-7 ")
                    + (eveningTwo ? "Ca 2" : "Ca 1");
            ClassroomOffering offering = upsertOffering(
                    classroomType, slug, title, online, start, end, teacher, room, eveningTwo);
            ensureTeacherAssignment(offering, teacher);
            seedSessions(offering, teacher, room, mwf, eveningTwo, online);
            List<User> classLearners = new ArrayList<>();
            if (classIndex == 0 || classIndex == 24) {
                classLearners.add(showcaseLearner);
            }
            while (classLearners.size() < 10) {
                User next = learners.get(learnerCursor % learners.size());
                learnerCursor++;
                if (!classLearners.contains(next)) {
                    classLearners.add(next);
                }
            }
            for (User learner : classLearners) {
                ensureClassroomEnrollment(offering, learner, teacher, start);
            }
            seedAttendance(offering, classLearners);
            offerings.add(offering);
        }
        return offerings;
    }

    private ClassroomOffering upsertOffering(
            PackageType classroomType,
            String slug,
            String title,
            boolean online,
            LocalDate start,
            LocalDate end,
            User teacher,
            ClassroomRoom room,
            boolean eveningTwo
    ) {
        String cover = online ? "/course-covers/classroom-online.png" : "/course-covers/classroom-offline.png";
        return offeringRepository.findByLearningPackageSlug(slug).map(existing -> {
            LearningPackage pack = existing.getLearningPackage();
            pack.setThumbnailUrl(cover);
            learningPackageRepository.save(pack);
            return existing;
        }).orElseGet(() -> {
            LearningPackage pack = LearningPackage.builder()
                    .packageType(classroomType)
                    .slug(slug)
                    .title(title)
                    .shortDescription("Lớp 3 tháng, 3 buổi/tuần, sĩ số 10.")
                    .description("Chương trình lớp học trung tâm EnglishLab, nghỉ Chủ nhật, 2 ca tối.")
                    .duration("3 tháng")
                    .studyMode(online ? "Google Meet" : "Tại trung tâm")
                    .price(BigDecimal.valueOf(4_690_000))
                    .thumbnailUrl(cover)
                    .status(PackageStatus.PUBLISHED)
                    .displayOrder(100)
                    .deleted(false)
                    .build();
            ClassroomOffering offering = ClassroomOffering.builder()
                    .learningPackage(pack)
                    .deliveryMode(online ? ClassroomDeliveryMode.VIRTUAL : ClassroomDeliveryMode.OFFLINE)
                    .status(ClassroomOfferingStatus.ACTIVE)
                    .entryLevel("IELTS 5.0")
                    .targetOutcome("Đạt band 6.0-6.5 sau 36 buổi.")
                    .maxCapacity(10)
                    .startDate(start)
                    .endDate(end)
                    .primaryTeacher(teacher)
                    .virtualMeetingOwner(online ? teacher : null)
                    .defaultRoom(room)
                    .offlineAddress(online ? null : ADDRESS)
                    .defaultLarkMeetingUrl(online ? "https://meet.google.com/englishlab-sheet-" + slug : null)
                    .larkMeetingStatus(online ? LarkMeetingStatus.SCHEDULED : LarkMeetingStatus.NOT_CREATED)
                    .syllabusSummary("36 buổi Listening-Reading-Writing-Speaking xoay vòng.")
                    .build();
            return offeringRepository.save(offering);
        });
    }

    private void ensureTeacherAssignment(ClassroomOffering offering, User teacher) {
        if (teacherAssignmentRepository.findAllByClassroomOfferingIdAndTeacherId(offering.getId(), teacher.getId()).isEmpty()) {
            teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                    .classroomOffering(offering)
                    .teacher(teacher)
                    .role(ClassroomTeacherRole.PRIMARY)
                    .effectiveFrom(offering.getStartDate())
                    .reason("Phân công giáo viên sheet data")
                    .build());
        }
    }

    private void seedSessions(
            ClassroomOffering offering,
            User teacher,
            ClassroomRoom room,
            boolean mwf,
            boolean eveningTwo,
            boolean online
    ) {
        if (sessionRepository.countByClassroomOfferingId(offering.getId()) > 0) {
            return;
        }
        Set<DayOfWeek> days = mwf ? MWF : TTS;
        LocalTime start = eveningTwo ? SLOT_2_START : SLOT_1_START;
        LocalTime end = eveningTwo ? SLOT_2_END : SLOT_1_END;
        LocalDate cursor = offering.getStartDate();
        int index = 1;
        while (!cursor.isAfter(offering.getEndDate())) {
            if (days.contains(cursor.getDayOfWeek())) {
                ClassroomSessionStatus status;
                if (cursor.isBefore(LocalDate.now())) {
                    status = ClassroomSessionStatus.COMPLETED;
                } else if (cursor.equals(LocalDate.now())) {
                    status = ClassroomSessionStatus.IN_PROGRESS;
                } else {
                    status = ClassroomSessionStatus.SCHEDULED;
                }
                sessionRepository.save(ClassroomSession.builder()
                        .classroomOffering(offering)
                        .sessionDate(cursor)
                        .startTime(start)
                        .endTime(end)
                        .teacher(teacher)
                        .status(status)
                        .deliveryMode(online ? ClassroomDeliveryMode.VIRTUAL : ClassroomDeliveryMode.OFFLINE)
                        .room(room)
                        .larkMeetingUrl(online ? offering.getDefaultLarkMeetingUrl() : null)
                        .larkMeetingStatus(online ? LarkMeetingStatus.SCHEDULED : LarkMeetingStatus.NOT_CREATED)
                        .recordingProvider(online ? "GOOGLE_MEET" : null)
                        .recordingSyncStatus(RecordingSyncStatus.NOT_AVAILABLE)
                        .sessionContent("Buổi " + index + ": luyện 4 kỹ năng")
                        .build());
                index++;
            }
            cursor = cursor.plusDays(1);
        }
    }

    private void ensureClassroomEnrollment(ClassroomOffering offering, User learner, User teacher, LocalDate start) {
        enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offering.getId())
                .orElseGet(() -> enrollmentRepository.save(ClassroomEnrollment.builder()
                        .student(learner)
                        .classroomOffering(offering)
                        .status(ClassroomEnrollmentStatus.ENROLLED)
                        .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                        .tuitionAmountDue(BigDecimal.valueOf(4_690_000))
                        .tuitionAmountPaid(BigDecimal.valueOf(4_690_000))
                        .tuitionDepositPaid(BigDecimal.valueOf(1_000_000))
                        .tuitionSettlementType(TuitionSettlementType.NONE)
                        .enrolledAt(start.atTime(9, 0))
                        .assignedAt(start.atTime(9, 0))
                        .assignedBy(teacher)
                        .confirmedAt(start.atTime(9, 0))
                        .confirmedBy(teacher)
                        .build()));
    }

    private void seedAttendance(ClassroomOffering offering, List<User> classLearners) {
        List<ClassroomSession> sessions = sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId());
        int i = 0;
        for (ClassroomSession session : sessions) {
            if (session.getStatus() != ClassroomSessionStatus.COMPLETED && session.getStatus() != ClassroomSessionStatus.IN_PROGRESS) {
                continue;
            }
            for (User learner : classLearners) {
                if (attendanceRepository.findBySessionIdAndStudentId(session.getId(), learner.getId()).isPresent()) {
                    continue;
                }
                ClassroomAttendanceStatus status = (i % 11 == 0)
                        ? ClassroomAttendanceStatus.LATE
                        : (i % 17 == 0 ? ClassroomAttendanceStatus.ABSENT : ClassroomAttendanceStatus.PRESENT);
                attendanceRepository.save(ClassroomAttendance.builder()
                        .session(session)
                        .student(learner)
                        .status(status)
                        .build());
                i++;
            }
        }
    }

    private void seedShowcaseOnlineProgress(User learner) {
        completeCourseIfPresent(learner, "ielts-master-vocabulary-band-7-plus");
        enrollInProgress(learner, "e2-ielts-practice-tests", 35);
        if (packageEnrollmentRepository.findByStudentOrderByRegisteredAtDesc(learner).size() < 2) {
            completeCourseIfPresent(learner, "center-sheet-ielts-listening");
            enrollInProgress(learner, "center-sheet-ielts-reading", 40);
        }
        enrollInProgress(learner, "center-sheet-communication-work", 15);
    }

    private void completeCourseIfPresent(User learner, String slug) {
        learningPackageRepository.findBySlugAndDeletedFalse(slug).ifPresent(pack -> {
            PackageEnrollment enrollment = packageEnrollmentRepository.findByStudentAndLearningPackage(learner, pack)
                    .orElseGet(() -> packageEnrollmentRepository.save(PackageEnrollment.builder()
                            .student(learner)
                            .learningPackage(pack)
                            .registeredAt(LocalDateTime.now().minusDays(40))
                            .build()));
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setProgressPercent(100);
            packageEnrollmentRepository.save(enrollment);
            onlineCourseRepository.findByLearningPackage(pack).ifPresent(course -> {
                for (var module : course.getModules()) {
                    for (Lesson lesson : module.getLessons()) {
                        LessonProgress progress = lessonProgressRepository.findByStudentAndLesson(learner, lesson)
                                .orElseGet(() -> LessonProgress.builder().student(learner).lesson(lesson).enrollment(enrollment).build());
                        progress.setEnrollment(enrollment);
                        progress.setStatus(LessonProgressStatus.COMPLETED);
                        progress.setProgressPercent(100);
                        progress.setCompletedAt(LocalDateTime.now().minusDays(5));
                        progress.setLastAccessedAt(LocalDateTime.now().minusDays(2));
                        lessonProgressRepository.save(progress);
                    }
                }
            });
        });
    }

    private void enrollInProgress(User learner, String slug, int percent) {
        learningPackageRepository.findBySlugAndDeletedFalse(slug).ifPresent(pack -> {
            PackageEnrollment enrollment = packageEnrollmentRepository.findByStudentAndLearningPackage(learner, pack)
                    .orElseGet(() -> packageEnrollmentRepository.save(PackageEnrollment.builder()
                            .student(learner)
                            .learningPackage(pack)
                            .registeredAt(LocalDateTime.now().minusDays(12))
                            .build()));
            if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
                return;
            }
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setProgressPercent(percent);
            packageEnrollmentRepository.save(enrollment);
        });
    }

    private void seedAlienClassExtras(List<ClassroomOffering> offerings, User teacher, User learner) {
        ClassroomOffering offering = offerings.get(0);
        if (homeworkRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offering.getId()).isEmpty()) {
            ClassroomHomework overdue = saveHomework(offering, "Writing Task 2 - Family", LocalDateTime.now().minusDays(2));
            ClassroomHomework today = saveHomework(offering, "Speaking Part 2 Cue Card", LocalDateTime.now().plusHours(8));
            saveHomework(offering, "Listening Section 1 Form", LocalDateTime.now().plusDays(5));
            homeworkSubmissionRepository.save(ClassroomHomeworkSubmission.builder()
                    .homework(overdue)
                    .student(learner)
                    .status(HomeworkSubmissionStatus.GRADED)
                    .textAnswer("Family support still matters in urban life because many students rely on their immediate family.")
                    .score(BigDecimal.valueOf(8.5))
                    .teacherFeedback("Collocation tốt, cần thêm ví dụ cụ thể.")
                    .gradedBy(teacher)
                    .submittedAt(LocalDateTime.now().minusDays(3))
                    .gradedAt(LocalDateTime.now().minusDays(1))
                    .build());
            homeworkSubmissionRepository.save(ClassroomHomeworkSubmission.builder()
                    .homework(today)
                    .student(learner)
                    .status(HomeworkSubmissionStatus.SUBMITTED)
                    .textAnswer("I would talk about a teacher who helped me prepare for evening classes.")
                    .submittedAt(LocalDateTime.now().minusHours(2))
                    .build());
        }
        if (announcementRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offering.getId()).isEmpty()) {
            announcementRepository.save(ClassroomAnnouncement.builder()
                    .classroomOffering(offering)
                    .title("Lịch ca tối tuần này")
                    .content("Lớp học 18:00-19:30, nghỉ Chủ nhật. Mang tài liệu Writing.")
                    .createdBy(teacher)
                    .build());
        }
        if (materialRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offering.getId()).isEmpty()) {
            materialRepository.save(ClassroomMaterial.builder()
                    .classroomOffering(offering)
                    .title("Đề cương 36 buổi")
                    .description("Syllabus lớp IELTS 3 tháng")
                    .fileUrl("https://cdn.englishlab.vn/sheet/ielts-36-sessions.pdf")
                    .fileType("pdf")
                    .materialType("SYLLABUS")
                    .provider("EnglishLab")
                    .visibility("LEARNERS_IN_CLASS")
                    .build());
        }
    }

    private ClassroomHomework saveHomework(ClassroomOffering offering, String title, LocalDateTime deadline) {
        return homeworkRepository.save(ClassroomHomework.builder()
                .classroomOffering(offering)
                .title(title)
                .instruction("Làm bài và nộp trước hạn. Dùng từ vựng đã học trên lớp.")
                .deadline(deadline)
                .maxScore(BigDecimal.TEN)
                .activityType(HomeworkActivityType.TEXT_RESPONSE)
                .status(HomeworkStatus.OPEN)
                .build());
    }

    private void seedPublishedMockTests() throws Exception {
        publishMock(
                "EnglishLab IELTS Listening Mock 1",
                AssessmentSkill.LISTENING,
                32,
                "assessment-data/ielts_mock_2025_january_listening_test_1.json",
                true);
        publishMock(
                "EnglishLab IELTS Reading Mock 1",
                AssessmentSkill.READING,
                60,
                "assessment-data/ielts_mock_2025_january_reading_test_1.json",
                true);
        publishMock(
                "EnglishLab IELTS Writing Mock 1",
                AssessmentSkill.WRITING,
                60,
                "assessment-data/ielts_mock_2025_january_writing_test_1.json",
                false);
        publishMock(
                "EnglishLab Academic Listening Mini 1",
                AssessmentSkill.LISTENING,
                12,
                "sheet-data/englishlab-academic-listening-mini.json",
                true);
    }

    private void publishMock(String title, AssessmentSkill skill, int minutes, String resource, boolean needsKey) throws Exception {
        boolean exists = assessmentBankItemRepository
                .findByTypeAndStatusAndActiveTrueOrderByDisplayOrderAscUpdatedAtDescIdDesc(AssessmentType.MOCK_TEST, "PUBLISHED")
                .stream()
                .anyMatch(item -> title.equalsIgnoreCase(item.getTitle()));
        if (exists) {
            return;
        }
        String json = new String(new ClassPathResource(resource).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        AssessmentBankItemRequest request = new AssessmentBankItemRequest();
        request.setTitle(title);
        request.setDescription("Đề thi thử xuất bản qua API kho đề, không hard-code trên frontend.");
        request.setType(AssessmentType.MOCK_TEST);
        request.setSkill(skill);
        request.setAiEvaluationMode(skill == AssessmentSkill.WRITING ? AiEvaluationMode.ESTIMATED_BAND : AiEvaluationMode.NONE);
        request.setInstructions("Làm bài theo đúng giao diện thi thử EnglishLab.");
        request.setUiConfigJson(json);
        if (needsKey) {
            JsonNode key = objectMapper.readTree(json).path("answerKey");
            request.setObjectiveAnswerKey(key.isMissingNode() ? "{}" : objectMapper.writeValueAsString(key));
        }
        request.setPassingScore(BigDecimal.valueOf(6.0));
        request.setMaxScore(BigDecimal.valueOf(9.0));
        request.setTimeLimitMinutes(minutes);
        request.setStatus("PUBLISHED");
        request.setDisplayOrder(10);
        curriculumProgramService.createAssessmentBankItem(request);
    }

    private void seedPlacementViaApi(User learner) {
        placementTestDefinitionService.getDefinition();
        try {
            placementTestService.getTest(learner.getEmail());
            if (placementAttemptRepository.existsByStudentAndTestCode(learner, PlacementTestDefinitionService.TEST_CODE)) {
                return;
            }
            JsonNode listening = placementTestDefinitionService.getConfig(placementTestDefinitionService.getDefinition(), "listening");
            JsonNode reading = placementTestDefinitionService.getConfig(placementTestDefinitionService.getDefinition(), "reading");
            PlacementTestSubmissionRequest request = new PlacementTestSubmissionRequest();
            request.setTestCode(PlacementTestDefinitionService.TEST_CODE);
            request.setExamType("IELTS");
            request.setListeningAnswers(toAnswerMap(listening.path("answerKey")));
            request.setReadingAnswers(toAnswerMap(reading.path("answerKey")));
            request.setWritingAnswers(Map.of(
                    "task1", "The diagram shows the production of ethanol fuel from corn through milling, cooking, fermentation and purification stages.",
                    "task2", "Physical training and mental strength both matter for athletes. Success in sport depends on practice, competition and support from coaches."
            ));
            request.setSpeakingTranscript("I live with my family at home. In my free time I watch films and enjoy leisure activities with parents and other adults.");
            placementTestService.submit(request, learner.getEmail());
        } catch (RuntimeException ex) {
            log.warn("[CenterSheet] Khong submit duoc placement qua API: {}", ex.getMessage());
        }
    }

    private Map<String, Object> toAnswerMap(JsonNode answerKey) {
        Map<String, Object> answers = new LinkedHashMap<>();
        if (answerKey == null || !answerKey.isObject()) {
            return answers;
        }
        answerKey.fields().forEachRemaining(entry -> answers.put(entry.getKey(), entry.getValue().asText()));
        return answers;
    }

    private User ensureUser(String email, String fullName, RoleEnum role) {
        return userRepository.findByEmail(email).map(existing -> {
            userRoleService.ensureRole(existing, role);
            if (existing.getFullName() == null || existing.getFullName().isBlank()) {
                existing.setFullName(fullName);
            }
            existing.setEmailVerified(true);
            return userRepository.save(existing);
        }).orElseGet(() -> {
            User created = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .password(passwordEncoder.encode(PASSWORD))
                    .emailVerified(true)
                    .profileCompleted(true)
                    .passwordSet(true)
                    .build();
            userRoleService.assignRole(created, role);
            return userRepository.save(created);
        });
    }

    private User ensureExistingOrCreate(String email, String fullName, RoleEnum role) {
        return ensureUser(email, fullName, role);
    }
}
