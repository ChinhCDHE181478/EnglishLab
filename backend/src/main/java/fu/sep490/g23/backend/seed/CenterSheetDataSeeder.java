package fu.sep490.g23.backend.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.request.curriculum.AssessmentBankItemRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CourseUnitContentRefRequest;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.entity.classroom.ClassroomAnnouncement;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.Room;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomAttendanceStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestSource;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.GoogleMeetStatus;
import fu.sep490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionSettlementType;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.CourseLesson;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.entity.teacher.TeacherPerformanceEvaluation;
import fu.sep490.g23.backend.entity.teacher.enums.TeacherEvaluationStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAnnouncementRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomChangeRequestRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkSubmissionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.repository.classroom.CourseRegistrationRequestRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.repository.teacher.TeacherPerformanceEvaluationRepository;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import fu.sep490.g23.backend.service.assessment.PlacementTestService;
import fu.sep490.g23.backend.service.classroom.ClassroomMaterialSyncService;
import fu.sep490.g23.backend.service.curriculum.InstructorLedCourseManagementService;
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
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseVersionRepository onlineCourseVersionRepository;
    private final OnlineCourseEnrollmentRepository packageEnrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final RoomRepository roomRepository;
    private final ClassSectionRepository offeringRepository;
    private final ClassScheduleRepository sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomGradebookEntryRepository gradebookEntryRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomChangeRequestRepository changeRequestRepository;
    private final CourseRegistrationRequestRepository enrollmentRequestRepository;
    private final InstructorLedCourseRepository instructorLedCourseRepository;
    private final CourseUnitRepository courseUnitRepository;
    private final TeacherPerformanceEvaluationRepository teacherEvaluationRepository;
    private final ClassroomAttendanceRepository attendanceRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomHomeworkSubmissionRepository homeworkSubmissionRepository;
    private final ClassroomAnnouncementRepository announcementRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final AssessmentBankItemRepository assessmentBankItemRepository;
    private final PlacementTestAttemptRepository placementAttemptRepository;
    private final PlacementTestDefinitionService placementTestDefinitionService;
    private final PlacementTestService placementTestService;
    private final InstructorLedCourseManagementService instructorLedCourseManagementService;
    private final ClassroomMaterialSyncService classroomMaterialSyncService;
    private final CenterMaterialLibraryItemRepository centerMaterialRepository;
    private final ExerciseBankItemRepository exerciseBankItemRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final CenterSheetCourseCatalog courseCatalog;
    private final DemoLearnerOnboardingSupport onboardingSupport;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.sheet.enabled:false}")
    private boolean sheetEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!sheetEnabled) {
            return;
        }
        log.info("[CenterSheet] Nap du lieu quy mo trung tam (local sheet DB), flag={}", sheetEnabled);
        try {
            User contentManager = ensureUser("content.manager@englishlab.vn", "Quản lý Content", RoleCodes.CONTENT_MANAGER);
        ensureUser("classroom.admin@englishlab.vn", "Nguyễn Admin", RoleCodes.ADMIN);
        ensureUser("classroom.manager@englishlab.vn", "Quản lý lớp học", RoleCodes.MANAGER);
        ensureUser("staff@englishlab.vn", "Nhân viên đào tạo", RoleCodes.STAFF);
        User staff = userRepository.findByEmail("staff@englishlab.vn").orElseThrow();

        User alien = ensureExistingOrCreate(TEACHER_EMAIL, "Trần Minh Huy", RoleCodes.TEACHER);
        List<User> teachers = new ArrayList<>();
        teachers.add(alien);
        for (int i = 0; i < TEACHER_NAMES.length; i++) {
            teachers.add(ensureUser("gv.sheet.%02d@englishlab.vn".formatted(i + 1), TEACHER_NAMES[i], RoleCodes.TEACHER));
        }

        User showcaseLearner = ensureExistingOrCreate(LEARNER_EMAIL, "Lê Ngọc Anh", RoleCodes.LEARNER);
        onboardingSupport.ensureReady(showcaseLearner);

        List<User> learners = new ArrayList<>();
        learners.add(showcaseLearner);
        for (int i = 1; i <= 299; i++) {
            String name = LAST_NAMES[i % LAST_NAMES.length] + " " + FIRST_NAMES[i % FIRST_NAMES.length] + " " + i;
            learners.add(ensureUser("hs.sheet.%03d@englishlab.vn".formatted(i), name, RoleCodes.LEARNER));
        }

        courseCatalog.seed(contentManager);
        try {
            seedPublishedMockTests();
        } catch (Exception ex) {
            log.warn("[CenterSheet] Khong xuat ban het mock test: {}", ex.getMessage());
        }
        seedPlacementViaApi(showcaseLearner);

        List<Room> rooms = ensureRooms();
        List<ClassSection> offerings = seedClasses(teachers, rooms, learners, showcaseLearner, alien);
        seedShowcaseOnlineProgress(showcaseLearner);
        seedAlienClassExtras(offerings, alien, showcaseLearner);
        seedStaffOperationsData(staff, teachers, learners, offerings, alien, showcaseLearner);
        log.info("[CenterSheet] Xong. GV {} | HV {} | lop {}", TEACHER_EMAIL, LEARNER_EMAIL, offerings.size());
        } catch (Exception ex) {
            log.error("[CenterSheet] Seeder gap, da giu phan data tao duoc trong transaction hien tai neu loi da duoc bat. Chi tiet:", ex);
        }
    }

    private List<Room> ensureRooms() {
        List<Room> rooms = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String name = "Phòng P%03d".formatted(i);
            Room room = roomRepository.findByActiveTrue().stream()
                    .filter(item -> name.equalsIgnoreCase(item.getName()))
                    .findFirst()
                    .orElseGet(() -> roomRepository.save(Room.builder()
                            .name(name)
                            .locationName(CAMPUS_NAME)
                            .locationAddress(ADDRESS)
                            .capacity(12)
                            .active(true)
                            .build()));
            rooms.add(room);
        }
        return rooms;
    }

    private List<ClassSection> seedClasses(
            List<User> teachers,
            List<Room> rooms,
            List<User> learners,
            User showcaseLearner,
            User alien
    ) {
        InstructorLedCourse ieltsOffline = ensureSheetTrainingProgram(
                "center-sheet-ielts-4skills", "IELTS 4 kỹ năng ca tối", ClassroomDeliveryMode.OFFLINE);
        InstructorLedCourse ieltsLive = ensureSheetTrainingProgram(
                "center-sheet-ielts-live", "IELTS 4 kỹ năng Google Meet", ClassroomDeliveryMode.VIRTUAL);
        InstructorLedCourse toeicOffline = ensureSheetTrainingProgram(
                "center-sheet-toeic-lr", "TOEIC Listening & Reading", ClassroomDeliveryMode.OFFLINE);
        List<ClassSection> offerings = new ArrayList<>();
        int learnerCursor = 1;
        int[] classOrder = demoFirstClassIndexes(30);
        for (int classIndex : classOrder) {
            boolean online = classIndex >= 24;
            int intake = classIndex % 3;
            LocalDate start = LocalDate.now().minusWeeks(10 - intake * 4L);
            LocalDate end = start.plusWeeks(12);
            boolean mwf = classScheduleMwf(classIndex);
            boolean eveningTwo = classScheduleEveningTwo(classIndex);
            Room room = online ? null : rooms.get(classIndex % 10);
            User teacher = (classIndex == 0 || classIndex == 24) ? alien : teachers.get(1 + (classIndex % 19));
            String slug = "center-sheet-class-%02d".formatted(classIndex + 1);
            boolean toeic = !online && intake == 2;
            String title = (online ? "IELTS Live Meet " : (toeic ? "TOEIC Center " : "IELTS Center "))
                    + (intake == 0 ? "K1 " : intake == 1 ? "K2 " : "K3 ")
                    + (mwf ? "T2-4-6 " : "T3-5-7 ")
                    + (eveningTwo ? "Ca 2" : "Ca 1");
            InstructorLedCourse program = online ? ieltsLive : (toeic ? toeicOffline : ieltsOffline);
            ClassSection offering = upsertOffering(
                    slug, title, online, start, end, teacher, room, eveningTwo, program);
            attachCourse(offering, program);
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
                ensureClassEnrollment(offering, learner, teacher, start);
                ensureGradebook(offering, learner, teacher, classIndex);
            }
            seedAttendance(offering, classLearners);
            offerings.add(offering);
        }
        offerings.sort(java.util.Comparator.comparing(ClassSection::getId, java.util.Comparator.nullsLast(Long::compareTo)));
        return offerings;
    }

    private int[] demoFirstClassIndexes(int total) {
        int[] order = new int[total];
        order[0] = 0;
        order[1] = 24;
        int cursor = 2;
        for (int index = 0; index < total; index++) {
            if (index != 0 && index != 24) {
                order[cursor++] = index;
            }
        }
        return order;
    }

    private ClassSection upsertOffering(
            String slug,
            String title,
            boolean online,
            LocalDate start,
            LocalDate end,
            User teacher,
            Room room,
            boolean eveningTwo,
            InstructorLedCourse instructorLedCourse
    ) {
        String cover = online ? "/course-covers/classroom-online.png" : "/course-covers/classroom-offline.png";
        return offeringRepository.findByInstructorLedCourseSlugOrCode(slug).map(existing -> {
            existing.setName(title);
            existing.setInstructorLedCourse(instructorLedCourse);
            existing.setPrimaryTeacher(teacher);
            existing.setRoom(room);
            return offeringRepository.save(existing);
        }).orElseGet(() -> {
            ClassSection offering = ClassSection.builder()
                    .instructorLedCourse(instructorLedCourse)
                    .name(title)
                    .code(slug)
                    .tuitionFeeVnd(BigDecimal.valueOf(4_690_000))
                    .deliveryMode(online ? ClassroomDeliveryMode.VIRTUAL : ClassroomDeliveryMode.OFFLINE)
                    .status(ClassroomOfferingStatus.ACTIVE)
                    .entryLevel("IELTS 5.0")
                    .targetOutcome("Đạt band 6.0-6.5 sau 36 buổi.")
                    .capacity(10)
                    .startDate(start)
                    .plannedEndDate(end)
                    .primaryTeacher(teacher)
                    .room(room)
                    .offlineAddress(online ? null : ADDRESS)
                    .googleMeetOwner(online ? teacher : null)
                    .googleMeetUrl(online ? "https://meet.google.com/englishlab-sheet-" + slug : null)
                    .googleMeetStatus(online ? GoogleMeetStatus.READY : GoogleMeetStatus.NOT_CREATED)
                    .syllabusSummary("36 buổi Listening-Reading-Writing-Speaking xoay vòng.")
                    .build();
            return offeringRepository.save(offering);
        });
    }

    private void ensureTeacherAssignment(ClassSection offering, User teacher) {
        if (teacherAssignmentRepository.findAllByClassSectionIdAndTeacherId(offering.getId(), teacher.getId()).isEmpty()) {
            teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                    .classSection(offering)
                    .teacher(teacher)
                    .role(ClassroomTeacherRole.PRIMARY)
                    .effectiveFrom(offering.getStartDate())
                    .reason("Phân công giáo viên sheet data")
                    .build());
        }
    }

    private boolean classScheduleMwf(int classIndex) {
        if (classIndex == 24) {
            return false;
        }
        return (classIndex / 3) % 2 == 0;
    }

    private boolean classScheduleEveningTwo(int classIndex) {
        if (classIndex == 24) {
            return true;
        }
        return classIndex % 2 == 1;
    }

    private void attachCourse(ClassSection offering, InstructorLedCourse curriculum) {
        offering.setInstructorLedCourse(curriculum);
        offering.setEntryLevel(curriculum.getEntryLevel());
        offering.setTargetOutcome(curriculum.getLearningOutcomes());
        offering.setSyllabusSummary(curriculum.getLearningOutcomes());
        offering.setProgramOutcomes(curriculum.getLearningOutcomes());
        offering.setTeacherGuide(curriculum.getTeacherGuide());
        offering.setInteractionActivities(null);
        offeringRepository.save(offering);
        User actor = offering.getPrimaryTeacher();
        if (actor == null) {
            actor = userRepository.findByEmail("content.manager@englishlab.vn").orElse(null);
        }
        classroomMaterialSyncService.synchronizeMandatoryMaterials(offering, actor);
    }

    private void seedSessions(
            ClassSection offering,
            User teacher,
            Room room,
            boolean mwf,
            boolean eveningTwo,
            boolean online
    ) {
        Set<DayOfWeek> days = mwf ? MWF : TTS;
        LocalTime start = eveningTwo ? SLOT_2_START : SLOT_1_START;
        LocalTime end = eveningTwo ? SLOT_2_END : SLOT_1_END;
        List<ClassSchedule> existing = sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId());
        List<LocalDate> dates = sessionDates(offering.getStartDate(), offering.getPlannedEndDate(), days);
        if (!existing.isEmpty()) {
            ClassSchedule first = existing.getFirst();
            boolean sameSlot = first.getStartTime().equals(start)
                    && days.contains(first.getSessionDate().getDayOfWeek());
            if (sameSlot) {
                refreshSessionStatuses(existing);
                return;
            }
            int limit = Math.min(existing.size(), dates.size());
            for (int i = 0; i < limit; i++) {
                ClassSchedule session = existing.get(i);
                LocalDate date = dates.get(i);
                session.setSessionDate(date);
                session.setStartTime(start);
                session.setEndTime(end);
                session.setTeacher(teacher);
                session.setRoom(room);
                session.setStatus(statusForSessionDate(date));
                sessionRepository.save(session);
            }
            return;
        }
        LocalDate cursor = offering.getStartDate();
        int index = 1;
        while (!cursor.isAfter(offering.getPlannedEndDate())) {
            if (days.contains(cursor.getDayOfWeek())) {
                ClassroomSessionStatus status = statusForSessionDate(cursor);
                sessionRepository.save(ClassSchedule.builder()
                        .classSection(offering)
                        .sessionDate(cursor)
                        .startTime(start)
                        .endTime(end)
                        .teacher(teacher)
                        .status(status)
                        .deliveryModeOverride(null)
                        .room(room)
                        .recordingStatus(RecordingSyncStatus.NOT_AVAILABLE)
                        .sessionContent("Buổi " + index + ": luyện 4 kỹ năng")
                        .build());
                index++;
            }
            cursor = cursor.plusDays(1);
        }
    }

    private void refreshSessionStatuses(List<ClassSchedule> schedules) {
        for (ClassSchedule session : schedules) {
            session.setStatus(statusForSessionDate(session.getSessionDate()));
            sessionRepository.save(session);
        }
    }

    private ClassroomSessionStatus statusForSessionDate(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            return ClassroomSessionStatus.COMPLETED;
        }
        if (date.equals(LocalDate.now())) {
            return ClassroomSessionStatus.IN_PROGRESS;
        }
        return ClassroomSessionStatus.SCHEDULED;
    }

    private void ensureClassEnrollment(ClassSection offering, User learner, User teacher, LocalDate start) {
        enrollmentRepository.findByStudentIdAndClassSectionId(learner.getId(), offering.getId())
                .orElseGet(() -> enrollmentRepository.save(ClassEnrollment.builder()
                        .student(learner)
                        .classSection(offering)
                        .status(ClassroomEnrollmentStatus.ENROLLED)
                        .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                        .agreedTuitionFeeVnd(offering.getTuitionFeeVnd())
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

    private void seedAttendance(ClassSection offering, List<User> classLearners) {
        List<ClassSchedule> schedules = sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId());
        int i = 0;
        for (ClassSchedule session : schedules) {
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
        if (packageEnrollmentRepository.findByStudentOrderByRegisteredAtDesc(learner).size() < 2) {
            completeCourseIfPresent(learner, "center-sheet-ielts-listening");
            enrollInProgress(learner, "center-sheet-ielts-reading", 40);
        }
        enrollInProgress(learner, "center-sheet-communication-work", 15);
    }

    private void completeCourseIfPresent(User learner, String slug) {
        onlineCourseRepository.findBySlug(slug).ifPresent(course -> {
            OnlineCourseEnrollment enrollment = packageEnrollmentRepository.findByStudentAndOnlineCourse(learner, course)
                    .orElseGet(() -> packageEnrollmentRepository.save(OnlineCourseEnrollment.builder()
                            .student(learner)
                            .onlineCourse(course)
                            .registeredAt(LocalDateTime.now().minusDays(40))
                            .build()));
            OnlineCourseVersion version = onlineCourseVersionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.PUBLISHED)
                    .or(() -> onlineCourseVersionRepository.findFirstByOnlineCourseOrderByVersionNumberDesc(course))
                    .orElse(null);
            enrollment.setCourseVersion(version);
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setProgressPercent(100);
            packageEnrollmentRepository.save(enrollment);
            if (version != null && version.getModules() != null) {
                for (var module : version.getModules()) {
                    for (OnlineLesson lesson : module.getLessons()) {
                        LessonProgress progress = lessonProgressRepository.findByStudentAndLesson(learner, lesson)
                                .orElseGet(() -> LessonProgress.builder().student(learner).lesson(lesson).enrollment(enrollment).build());
                        progress.setEnrollment(enrollment);
                        progress.setCourseVersion(version);
                        progress.setLessonKey(lesson.getLessonKey());
                        progress.setStatus(LessonProgressStatus.COMPLETED);
                        progress.setProgressPercent(100);
                        progress.setCompletedAt(LocalDateTime.now().minusDays(5));
                        progress.setLastAccessedAt(LocalDateTime.now().minusDays(2));
                        lessonProgressRepository.save(progress);
                    }
                }
            }
        });
    }

    private void enrollInProgress(User learner, String slug, int percent) {
        onlineCourseRepository.findBySlug(slug).ifPresent(course -> {
            OnlineCourseEnrollment enrollment = packageEnrollmentRepository.findByStudentAndOnlineCourse(learner, course)
                    .orElseGet(() -> packageEnrollmentRepository.save(OnlineCourseEnrollment.builder()
                            .student(learner)
                            .onlineCourse(course)
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

    private void seedAlienClassExtras(List<ClassSection> offerings, User teacher, User learner) {
        ClassSection offering = offerings.get(0);
        ClassroomHomework speaking = upsertSpeakingHomework(offering, teacher);
        if (homeworkRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId()).stream()
                .noneMatch(item -> "Writing Task 2 - Family".equalsIgnoreCase(item.getTitle()))) {
            ClassroomHomework overdue = saveHomework(offering, "Writing Task 2 - Family", LocalDateTime.now().minusDays(2));
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
        }
        homeworkSubmissionRepository.findByHomeworkIdAndStudentId(speaking.getId(), learner.getId())
                .ifPresentOrElse(existing -> {
                    existing.setAttachmentUrl("/sheet-speaking/sample-answer.wav");
                    existing.setTextAnswer(null);
                    existing.setStatus(HomeworkSubmissionStatus.SUBMITTED);
                    homeworkSubmissionRepository.save(existing);
                }, () -> homeworkSubmissionRepository.save(ClassroomHomeworkSubmission.builder()
                        .homework(speaking)
                        .student(learner)
                        .status(HomeworkSubmissionStatus.SUBMITTED)
                        .attachmentUrl("/sheet-speaking/sample-answer.wav")
                        .submittedAt(LocalDateTime.now().minusHours(2))
                        .build()));
        if (announcementRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId()).isEmpty()) {
            announcementRepository.save(ClassroomAnnouncement.builder()
                    .classSection(offering)
                    .title("Lịch ca tối tuần này")
                    .content("Lớp học 18:00-19:30, nghỉ Chủ nhật. Mang tài liệu Writing.")
                    .createdBy(teacher)
                    .build());
        }
        if (materialRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId()).isEmpty()) {
            materialRepository.save(ClassroomMaterial.builder()
                    .classSection(offering)
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

    private ClassroomHomework upsertSpeakingHomework(ClassSection offering, User teacher) {
        String title = "Speaking Part 1-2 ghi âm";
        String config = """
                {"parts":[
                  {"key":"part_1","title":"Part 1 · Interview","answerSeconds":60,"prompts":[
                    {"text":"Where are you from?","audioUrl":"/sheet-speaking/p1-q1.wav"},
                    {"text":"Who do you live with?","audioUrl":"/sheet-speaking/p1-q2.wav"}
                  ]},
                  {"key":"part_2","title":"Part 2 · Cue Card","prepSeconds":60,"answerSeconds":120,
                   "audioUrl":"/sheet-speaking/p2-cue.wav",
                   "cueCardTitle":"Describe an evening class you enjoy.",
                   "cueCardBullets":["What the class is about","When and where you take it","Who you study with","Why you enjoy it"]}
                ]}
                """;
        ClassroomHomework homework = homeworkRepository.findByClassSectionIdOrderByCreatedAtDesc(offering.getId()).stream()
                .filter(item -> title.equalsIgnoreCase(item.getTitle()) || "Speaking Part 2 Cue Card".equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .orElseGet(() -> ClassroomHomework.builder().classSection(offering).build());
        homework.setTitle(title);
        homework.setInstruction("Nghe câu hỏi ghi âm, rồi thu âm câu trả lời. Không gõ text thay cho bài nói.");
        homework.setDeadline(LocalDateTime.now().plusHours(8));
        homework.setMaxScore(BigDecimal.TEN);
        homework.setActivityType(HomeworkActivityType.TEXT_RESPONSE);
        homework.setActivityConfigJson(config);
        homework.setSkill(AssessmentSkill.SPEAKING);
        homework.setGradingMode(HomeworkGradingMode.TEACHER);
        homework.setStatus(HomeworkStatus.OPEN);
        homework.setCreatedBy(teacher);
        homework.setAllowResubmission(true);
        return homeworkRepository.save(homework);
    }

    private ClassroomHomework saveHomework(ClassSection offering, String title, LocalDateTime deadline) {
        return homeworkRepository.save(ClassroomHomework.builder()
                .classSection(offering)
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
        publishMock(
                "EnglishLab IELTS Speaking Mock 1",
                AssessmentSkill.SPEAKING,
                14,
                "sheet-data/englishlab-speaking-mock-1.json",
                false);
        JsonNode mockIndex = objectMapper.readTree(
                new ClassPathResource("sheet-data/iot-mocks-index.json").getInputStream());
        for (JsonNode item : mockIndex) {
            publishMock(
                    item.path("title").asText(),
                    AssessmentSkill.valueOf(item.path("skill").asText()),
                    item.path("minutes").asInt(),
                    item.path("resource").asText(),
                    item.path("needsKey").asBoolean(false));
        }
        JsonNode toeicMockIndex = objectMapper.readTree(
                new ClassPathResource("sheet-data/toeic-mocks-index.json").getInputStream());
        for (JsonNode item : toeicMockIndex) {
            publishMock(
                    item.path("title").asText(),
                    AssessmentSkill.valueOf(item.path("skill").asText()),
                    item.path("minutes").asInt(),
                    item.path("resource").asText(),
                    item.path("needsKey").asBoolean(false));
        }
    }

    private void publishMock(String title, AssessmentSkill skill, int minutes, String resource, boolean needsKey) throws Exception {
        String json = new String(new ClassPathResource(resource).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        var existing = assessmentBankItemRepository
                .findByTypeAndStatusAndActiveTrueOrderByDisplayOrderAscUpdatedAtDescIdDesc(AssessmentType.MOCK_TEST, "PUBLISHED")
                .stream()
                .filter(item -> title.equalsIgnoreCase(item.getTitle()))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().setUiConfigJson(json);
            existing.get().setTimeLimitMinutes(minutes);
            if (skill == AssessmentSkill.WRITING || skill == AssessmentSkill.SPEAKING) {
                existing.get().setAiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND);
            }
            if (needsKey) {
                JsonNode key = objectMapper.readTree(json).path("answerKey");
                existing.get().setObjectiveAnswerKey(key.isMissingNode() ? "{}" : objectMapper.writeValueAsString(key));
            }
            assessmentBankItemRepository.save(existing.get());
            return;
        }
        AssessmentBankItemRequest request = new AssessmentBankItemRequest();
        request.setTitle(title);
        request.setDescription("Đề thi thử xuất bản qua API kho đề, không hard-code trên frontend.");
        request.setType(AssessmentType.MOCK_TEST);
        request.setSkill(skill);
        request.setAiEvaluationMode(skill == AssessmentSkill.WRITING || skill == AssessmentSkill.SPEAKING
                ? AiEvaluationMode.ESTIMATED_BAND
                : AiEvaluationMode.NONE);
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
        instructorLedCourseManagementService.createAssessmentBankItem(request);
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

    private void seedStaffOperationsData(
            User staff,
            List<User> teachers,
            List<User> learners,
            List<ClassSection> offerings,
            User alien,
            User showcaseLearner
    ) {
        try {
            seedTeacherProfilesAndScores(staff, teachers);
        } catch (Exception ex) {
            log.warn("[CenterSheet] Khong nap duoc diem giao vien: {}", ex.getMessage());
        }
        try {
            seedEnrollmentRequests(staff, learners, offerings, showcaseLearner);
        } catch (Exception ex) {
            log.warn("[CenterSheet] Khong nap duoc ho so dang ky: {}", ex.getMessage(), ex);
        }
        try {
            seedChangeRequests(alien, offerings);
        } catch (Exception ex) {
            log.warn("[CenterSheet] Khong nap duoc yeu cau van hanh: {}", ex.getMessage());
        }
        try {
            seedUpcomingAlertClass(teachers, learners);
        } catch (Exception ex) {
            log.warn("[CenterSheet] Khong nap duoc lop sap khai giang: {}", ex.getMessage());
        }
    }

    private void seedTeacherProfilesAndScores(User staff, List<User> teachers) {
        String[] headlines = {
                "Giáo viên IELTS ca tối",
                "Giáo viên TOEIC và giao tiếp",
                "Giáo viên Writing và Speaking"
        };
        for (int i = 0; i < teachers.size(); i++) {
            User teacher = teachers.get(i);
            teacher.setTeacherHeadline(headlines[i % headlines.length]);
            teacher.setTeacherBiography("Giảng dạy ca tối tại EnglishLab Hai Bà Trưng, theo dõi tiến độ học viên từng buổi.");
            teacher.setTeacherSpecializations("IELTS, TOEIC, giao tiếp công sở");
            teacher.setTeacherTeachingLanguages("Tiếng Anh, tiếng Việt");
            teacher.setTeacherYearsOfExperience(3 + (i % 8));
            teacher.setTeacherHighestQualification(i % 2 == 0 ? "CELTA" : "IELTS 8.0");
            teacher.setTeacherPublicProfile(true);
            userRepository.save(teacher);

            if (teacherEvaluationRepository.findByTeacherIdOrderByPeriodEndDescIdDesc(teacher.getId()).isEmpty()) {
                BigDecimal delivery = BigDecimal.valueOf(3.8 + (i % 10) * 0.1);
                BigDecimal support = BigDecimal.valueOf(3.7 + ((i + 3) % 10) * 0.1);
                BigDecimal grading = BigDecimal.valueOf(3.6 + ((i + 5) % 10) * 0.1);
                BigDecimal professionalism = BigDecimal.valueOf(3.9 + ((i + 2) % 8) * 0.1);
                BigDecimal overall = delivery.add(support).add(grading).add(professionalism)
                        .divide(BigDecimal.valueOf(4), 2, java.math.RoundingMode.HALF_UP);
                teacherEvaluationRepository.save(TeacherPerformanceEvaluation.builder()
                        .teacher(teacher)
                        .evaluator(staff)
                        .periodStart(LocalDate.now().minusMonths(3))
                        .periodEnd(LocalDate.now().minusDays(7))
                        .lessonDeliveryScore(delivery)
                        .learnerSupportScore(support)
                        .gradingTimelinessScore(grading)
                        .professionalismScore(professionalism)
                        .overallScore(overall)
                        .strengths("Dẫn dắt lớp ca tối rõ ràng, phản hồi bài tập đúng hạn.")
                        .improvementAreas("Cần thêm ví dụ collocation trong buổi Speaking.")
                        .actionPlan("Bổ sung 1 hoạt động giao tiếp mỗi tuần.")
                        .status(TeacherEvaluationStatus.PUBLISHED)
                        .publishedAt(LocalDateTime.now().minusDays(6))
                        .build());
            }
        }
    }

    private void seedEnrollmentRequests(
            User staff,
            List<User> learners,
            List<ClassSection> offerings,
            User showcaseLearner
    ) {
        InstructorLedCourse ieltsProgram = ensureSheetTrainingProgram("center-sheet-ielts-4skills", "IELTS 4 kỹ năng ca tối", ClassroomDeliveryMode.OFFLINE);
        InstructorLedCourse toeicProgram = ensureSheetTrainingProgram("center-sheet-toeic-lr", "TOEIC Listening & Reading", ClassroomDeliveryMode.OFFLINE);
        if (enrollmentRequestRepository.count() > 0) {
            for (CourseRegistrationRequest existing : enrollmentRequestRepository.findAll()) {
                if (existing.getCourseOffering() == null) {
                    boolean toeic = "TOEIC_2_SKILLS".equals(existing.getConsultationTrack());
                    existing.setCourseOffering(toeic ? toeicProgram : ieltsProgram);
                    enrollmentRequestRepository.save(existing);
                }
            }
            return;
        }
        List<User> prospects = new ArrayList<>();
        for (int i = 1; i <= 36; i++) {
            String name = LAST_NAMES[i % LAST_NAMES.length] + " " + FIRST_NAMES[i % FIRST_NAMES.length] + " Tư vấn " + i;
            prospects.add(ensureUser("hs.consult.%03d@englishlab.vn".formatted(i), name, RoleCodes.LEARNER));
        }
        EnrollmentRequestStatus[] statuses = {
                EnrollmentRequestStatus.SUBMITTED,
                EnrollmentRequestStatus.SUBMITTED,
                EnrollmentRequestStatus.SUBMITTED,
                EnrollmentRequestStatus.SUBMITTED,
                EnrollmentRequestStatus.SUBMITTED,
                EnrollmentRequestStatus.SUBMITTED,
                EnrollmentRequestStatus.SUBMITTED,
                EnrollmentRequestStatus.SUBMITTED,
                EnrollmentRequestStatus.INVITATION_SENT,
                EnrollmentRequestStatus.INVITATION_SENT,
                EnrollmentRequestStatus.TEST_SCHEDULED,
                EnrollmentRequestStatus.TEST_SCHEDULED,
                EnrollmentRequestStatus.TEST_SCHEDULED,
                EnrollmentRequestStatus.PLACEMENT_TEST_COMPLETED,
                EnrollmentRequestStatus.UNDER_STAFF_REVIEW,
                EnrollmentRequestStatus.UNDER_STAFF_REVIEW,
                EnrollmentRequestStatus.UNDER_STAFF_REVIEW,
                EnrollmentRequestStatus.UNDER_STAFF_REVIEW,
                EnrollmentRequestStatus.UNDER_STAFF_REVIEW,
                EnrollmentRequestStatus.UNDER_STAFF_REVIEW,
                EnrollmentRequestStatus.WAITING_FOR_CLASS,
                EnrollmentRequestStatus.WAITING_FOR_CLASS,
                EnrollmentRequestStatus.WAITING_FOR_CLASS,
                EnrollmentRequestStatus.WAITING_FOR_CLASS,
                EnrollmentRequestStatus.WAITING_FOR_CLASS,
                EnrollmentRequestStatus.WAITING_FOR_CLASS,
                EnrollmentRequestStatus.WAITING_FOR_CLASS,
                EnrollmentRequestStatus.WAITING_FOR_CLASS,
                EnrollmentRequestStatus.CLASS_ASSIGNED,
                EnrollmentRequestStatus.CLASS_ASSIGNED,
                EnrollmentRequestStatus.CLASS_ASSIGNED,
                EnrollmentRequestStatus.CLASS_ASSIGNED,
                EnrollmentRequestStatus.CLASS_ASSIGNED,
                EnrollmentRequestStatus.CLASS_ASSIGNED,
                EnrollmentRequestStatus.REJECTED,
                EnrollmentRequestStatus.REJECTED
        };
        ClassSection assigned = offerings.isEmpty() ? null : offerings.getFirst();
        for (int i = 0; i < prospects.size(); i++) {
            User learner = prospects.get(i);
            EnrollmentRequestStatus status = statuses[i];
            CourseRegistrationRequest.CourseRegistrationRequestBuilder builder = CourseRegistrationRequest.builder()
                    .learner(learner)
                    .contactName(learner.getFullName())
                    .contactEmail(learner.getEmail())
                    .contactPhone("09" + String.format("%08d", 68000000 + i))
                    .consultationTrack(i % 2 == 0 ? "IELTS_4_SKILLS" : "TOEIC_2_SKILLS")
                    .studyWorkGoal("Muốn học ca tối để đi làm ban ngày.")
                    .preferredSchedule("Thứ 2-4-6 · Tối")
                    .campusPreference(CAMPUS_NAME)
                    .learnerNote("Đăng ký tư vấn từ lịch khai giảng sheet.")
                    .requestSource(EnrollmentRequestSource.ONLINE)
                    .courseOffering(i % 2 == 0 ? ieltsProgram : toeicProgram)
                    .status(status)
                    .version(0L)
                    .createdAt(LocalDateTime.now().minusDays(12 - (i % 10)))
                    .updatedAt(LocalDateTime.now().minusHours(i));
            if (status != EnrollmentRequestStatus.SUBMITTED) {
                builder.reviewedBy(staff).reviewedAt(LocalDateTime.now().minusDays(3));
                builder.invitationSentAt(LocalDateTime.now().minusDays(5));
                builder.staffNote("Đã gọi điện tư vấn ca tối và hướng dẫn bài xếp lớp.");
            }
            if (status == EnrollmentRequestStatus.TEST_SCHEDULED
                    || status == EnrollmentRequestStatus.PLACEMENT_TEST_COMPLETED
                    || status == EnrollmentRequestStatus.UNDER_STAFF_REVIEW
                    || status == EnrollmentRequestStatus.WAITING_FOR_CLASS
                    || status == EnrollmentRequestStatus.CLASS_ASSIGNED) {
                builder.testAppointmentAt(LocalDateTime.now().minusDays(2));
                builder.testLocation(ADDRESS);
            }
            if (status == EnrollmentRequestStatus.PLACEMENT_TEST_COMPLETED
                    || status == EnrollmentRequestStatus.UNDER_STAFF_REVIEW
                    || status == EnrollmentRequestStatus.WAITING_FOR_CLASS
                    || status == EnrollmentRequestStatus.CLASS_ASSIGNED) {
                builder.testCompletedAt(LocalDateTime.now().minusDays(1));
                builder.confirmedLevel(i % 3 == 0 ? PlacementLevel.BEGINNER : PlacementLevel.INTERMEDIATE);
            }
            if (status == EnrollmentRequestStatus.CLASS_ASSIGNED && assigned != null) {
                builder.assignedClassSection(assigned);
                builder.preferredClassSection(assigned);
            }
            if (status == EnrollmentRequestStatus.REJECTED) {
                builder.rejectionReason("Chưa phù hợp lịch ca tối hiện tại.");
            }
            enrollmentRequestRepository.save(builder.build());
        }
        if (showcaseLearner != null && assigned != null) {
            enrollmentRequestRepository.save(CourseRegistrationRequest.builder()
                    .learner(showcaseLearner)
                    .contactName(showcaseLearner.getFullName())
                    .contactEmail(showcaseLearner.getEmail())
                    .consultationTrack("IELTS_4_SKILLS")
                    .courseOffering(ieltsProgram)
                    .studyWorkGoal("Học viên showcase đã được tư vấn và xếp lớp.")
                    .status(EnrollmentRequestStatus.CLASS_ASSIGNED)
                    .requestSource(EnrollmentRequestSource.CENTER)
                    .assignedClassSection(assigned)
                    .preferredClassSection(assigned)
                    .reviewedBy(staff)
                    .reviewedAt(LocalDateTime.now().minusDays(8))
                    .confirmedLevel(PlacementLevel.INTERMEDIATE)
                    .staffNote("Đã tư vấn trực tiếp và xếp vào lớp 01.")
                    .version(0L)
                    .createdAt(LocalDateTime.now().minusDays(14))
                    .updatedAt(LocalDateTime.now().minusDays(8))
                    .build());
        }
    }

    private InstructorLedCourse ensureSheetTrainingProgram(String slug, String title, ClassroomDeliveryMode mode) {
        boolean toeic = slug.contains("toeic");
        return ensureSheetCurriculum(slug, title, mode, toeic);
    }

    private InstructorLedCourse ensureSheetCurriculum(
            String slug,
            String title,
            ClassroomDeliveryMode mode,
            boolean toeic
    ) {
        String codeBase = slug.replace("center-sheet-", "").replace("-", "_").replace("_curriculum", "").toUpperCase();
        InstructorLedCourse program = instructorLedCourseRepository.findBySlug(slug)
                .orElseGet(() -> instructorLedCourseRepository.save(InstructorLedCourse.builder()
                        .title(title)
                        .code("CS_" + codeBase)
                        .slug(slug)
                        .examType(toeic ? "TOEIC" : "IELTS")
                        .programTrack(toeic ? "TOEIC_2_SKILLS" : "IELTS_4_SKILLS")
                        .focusSkills(toeic ? "Listening, Reading" : "Listening, Reading, Writing, Speaking")
                        .entryLevel(toeic ? "TOEIC 350+" : "IELTS 5.0")
                        .learningOutcomes(toeic
                                ? "Hoàn thành 36 buổi TOEIC L&R: Part 1-7, chiến thuật làm bài, mục tiêu 650+."
                                : "Hoàn thành 36 buổi IELTS 4 kỹ năng: Listening, Reading, Writing, Speaking, mục tiêu 6.0-6.5.")
                        .teacherGuide("Mỗi unit gồm học liệu trung tâm, bài luyện tập, bộ flashcard và kế hoạch 4 buổi.")
                        .shortDescription(title)
                        .description("Giáo trình ca tối tại " + CAMPUS_NAME + ", 12 tuần / 36 buổi.")
                        .baseTuitionFeeVnd(BigDecimal.valueOf(toeic ? 8_900_000 : 12_500_000))
                        .durationLabel("12 tuần")
                        .publicationStatus(PackageStatus.PUBLISHED)
                        .displayOrder(1)
                        .featured(true)
                        .build()));
        if (courseUnitRepository.findByInstructorLedCourseIdOrderBySequenceNumberAscIdAsc(program.getId()).isEmpty()) {
            String[][] units = toeic ? toeicUnits() : ieltsUnits();
            for (int i = 0; i < units.length; i++) {
                CourseUnit unit = CourseUnit.builder()
                        .instructorLedCourse(program)
                        .sequenceNumber(i + 1)
                        .title(units[i][0])
                        .description(units[i][1])
                        .learningObjectives("Warm-up 10 phút; chiến thuật 25 phút; luyện có hướng dẫn 40 phút; chốt bài về nhà 15 phút.")
                        .build();
                for (int session = 1; session <= 4; session++) {
                    unit.addLesson(CourseLesson.builder()
                            .sequenceNumber(i * 4 + session)
                            .title("Buổi " + (i * 4 + session) + " · " + units[i][0])
                            .description(units[i][1])
                            .learningObjectives(units[i][2])
                            .build());
                }
                courseUnitRepository.save(unit);
            }
        }
        List<CourseUnit> units = courseUnitRepository.findByInstructorLedCourseIdOrderBySequenceNumberAscIdAsc(program.getId());
        User creator = userRepository.findByEmail("content.manager@englishlab.vn").orElse(null);
        attachSheetUnitResources(units, toeic, creator);
        return program;
    }

    private void attachSheetUnitResources(List<CourseUnit> units, boolean toeic, User creator) {
        String[][] catalog = toeic ? toeicUnits() : ieltsUnits();
        String exam = toeic ? "TOEIC" : "IELTS";
        for (int i = 0; i < Math.min(units.size(), catalog.length); i++) {
            int unitNumber = i + 1;
            CourseUnit unit = units.get(i);
            String title = catalog[i][0];
            String description = catalog[i][1];
            String skill = toeic ? toeicSkill(unitNumber) : ieltsSkill(unitNumber);
            CenterMaterialLibraryItem material = ensureSheetMaterial(exam, unitNumber, title, description, skill, creator);
            ExerciseBankItem exercise = ensureSheetExercise(exam, unitNumber, title, description, skill, creator);
            FlashcardSet flashcards = ensureSheetFlashcards(exam, unitNumber, title, skill);
            try {
                instructorLedCourseManagementService.attachMaterial(unit.getId(), sheetRef(material.getId(), "Học liệu chuẩn của unit"));
            } catch (Exception ignored) {}
            try {
                instructorLedCourseManagementService.attachExercise(unit.getId(), sheetRef(exercise.getId(), "Bài luyện tập trong giáo trình"));
            } catch (Exception ignored) {}
            try {
                instructorLedCourseManagementService.attachFlashcard(unit.getId(), sheetRef(flashcards.getId(), "Từ vựng ôn trước và sau buổi học"));
            } catch (Exception ignored) {}
        }
    }

    private CourseUnitContentRefRequest sheetRef(Long resourceId, String note) {
        CourseUnitContentRefRequest request = new CourseUnitContentRefRequest();
        request.setResourceId(resourceId);
        request.setDisplayOrder(1);
        request.setNote(note);
        return request;
    }

    private CenterMaterialLibraryItem ensureSheetMaterial(
            String exam,
            int unitNumber,
            String title,
            String description,
            String skill,
            User creator
    ) {
        String itemTitle = "Sheet " + exam + " · " + title;
        String fileUrl = "/sheet-materials/" + exam.toLowerCase() + "-unit-" + unitNumber + ".txt";
        return centerMaterialRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(item -> itemTitle.equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .orElseGet(() -> {
                    CenterMaterialLibraryItem.CenterMaterialLibraryItemBuilder builder = CenterMaterialLibraryItem.builder()
                            .title(itemTitle)
                            .description(description)
                            .fileUrl(fileUrl)
                            .fileType("TXT")
                            .materialType("LESSON_NOTE")
                            .provider("EnglishLab")
                            .examCategory(exam)
                            .skill(skill)
                            .tags("sheet," + exam.toLowerCase() + ",unit-" + unitNumber)
                            .status("PUBLISHED")
                            .createdBy(creator)
                            .updatedBy(creator);
                    if ("IELTS".equals(exam)) {
                        builder.ieltsBandMin(BigDecimal.valueOf(5.0)).ieltsBandMax(BigDecimal.valueOf(6.5));
                    } else {
                        builder.toeicScoreMin(350).toeicScoreMax(750);
                    }
                    return centerMaterialRepository.save(builder.build());
                });
    }

    private ExerciseBankItem ensureSheetExercise(
            String exam,
            int unitNumber,
            String title,
            String description,
            String skill,
            User creator
    ) {
        String itemTitle = "Sheet " + exam + " Unit " + unitNumber + " Practice";
        ExerciseBankItem exercise = exerciseBankItemRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(item -> itemTitle.equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .orElseGet(() -> ExerciseBankItem.builder()
                        .title(itemTitle)
                        .skill(skill)
                        .level("IELTS".equals(exam) ? "IELTS 5.0-6.5" : "TOEIC 350-650")
                        .prompt("Hoàn thành bài luyện tập " + title)
                        .answerKey("{\"1\":\"B\",\"2\":\"A\",\"3\":\"C\"}")
                        .explanation("Đối chiếu đáp án và ghi lại lỗi sai trước khi làm lại.")
                        .tags("sheet," + exam.toLowerCase() + ",unit-" + unitNumber)
                        .active(true)
                        .createdBy(creator)
                        .build());
        exercise.setExerciseType("PRACTICE");
        if (exercise.getPrompt() == null || !exercise.getPrompt().trim().startsWith("{")) {
            exercise.setPrompt(buildSheetPracticeConfig(exam, unitNumber, title, description, skill));
            exercise.setAnswerKey("{\"1\":\"B\",\"2\":\"A\",\"3\":\"C\"}");
        }
        return exerciseBankItemRepository.save(exercise);
    }

    private String buildSheetPracticeConfig(String exam, int unitNumber, String title, String description, String skill) {
        String type = switch (skill) {
            case "LISTENING" -> "ielts_listening_exam";
            case "READING" -> "ielts_reading_exam";
            case "WRITING" -> "ielts_writing_exam";
            case "SPEAKING" -> "ielts_speaking_exam";
            default -> "ielts_reading_exam";
        };
        return """
                {
                  "type":"%s",
                  "key":"sheet-%s-unit-%d-practice",
                  "title":"%s Unit %d Practice",
                  "durationMinutes":10,
                  "rules":["Mỗi câu chọn một đáp án","Có thể luyện lại nhiều lần","Kết quả không tính vào bảng điểm lớp"],
                  "parts":[{
                    "key":"part_1",
                    "partNumber":%d,
                    "title":"%s",
                    "questionRange":"Questions 1-3",
                    "passage":{"title":"Ngữ cảnh luyện tập","paragraphs":[{"label":"%s","text":"%s"}]},
                    "questionGroups":[{
                      "type":"single_choice",
                      "title":"Chọn đáp án đúng nhất",
                      "instructions":"Đọc kỹ yêu cầu rồi chọn A, B, C hoặc D.",
                      "questions":[
                        {"number":1,"prompt":"Chiến thuật quan trọng nhất của unit này là gì?","options":[{"value":"A","label":"Bỏ qua ngữ cảnh"},{"value":"B","label":"Xác định từ khóa và ngữ cảnh trước khi trả lời"},{"value":"C","label":"Chọn đáp án dài nhất"},{"value":"D","label":"Bỏ mọi câu khó"}]},
                        {"number":2,"prompt":"Bạn nên làm gì trước khi chốt đáp án?","options":[{"value":"A","label":"Đối chiếu lại bằng chứng trong bài"},{"value":"B","label":"Đổi đáp án ngẫu nhiên"},{"value":"C","label":"Chỉ nhìn một từ"},{"value":"D","label":"Để trống"}]},
                        {"number":3,"prompt":"Cách ôn hiệu quả sau bài luyện là gì?","options":[{"value":"A","label":"Bỏ qua câu sai"},{"value":"B","label":"Làm lại ngay mà không xem giải thích"},{"value":"C","label":"Phân loại lỗi và đọc lời giải"},{"value":"D","label":"Học thuộc thứ tự A B C D"}]}
                      ]
                    }]
                  }]
                }
                """.formatted(type, exam.toLowerCase(), unitNumber, exam, unitNumber, unitNumber, title, skill, description);
    }

    private FlashcardSet ensureSheetFlashcards(String exam, int unitNumber, String title, String skill) {
        String setTitle = "Sheet " + exam + " Unit " + unitNumber + " Flashcards";
        FlashcardSet set = flashcardSetRepository.findByTitleIgnoreCase(setTitle)
                .orElseGet(() -> FlashcardSet.builder().title(setTitle).build());
        set.setDescription("Từ vựng trọng tâm cho " + title);
        set.setExamCategory(exam);
        set.setSkill(skill);
        set.setTags("sheet," + exam.toLowerCase() + ",unit-" + unitNumber);
        set.setCardsJson("IELTS".equals(exam) ? ieltsFlashcardsJson(unitNumber) : toeicFlashcardsJson(unitNumber));
        set.setStatus("PUBLISHED");
        set.setDisplayOrder(unitNumber);
        return flashcardSetRepository.save(set);
    }

    private String ieltsSkill(int unitNumber) {
        if (unitNumber <= 2) {
            return "LISTENING";
        }
        if (unitNumber <= 4) {
            return "READING";
        }
        if (unitNumber <= 6) {
            return "WRITING";
        }
        return "SPEAKING";
    }

    private String toeicSkill(int unitNumber) {
        return unitNumber <= 3 ? "LISTENING" : "READING";
    }

    private String ieltsFlashcardsJson(int unitNumber) {
        return switch (unitNumber) {
            case 1 -> """
                    [{"front":"accommodation","back":"chỗ ở","example":"Student accommodation is close to campus."},
                     {"front":"reservation","back":"đặt chỗ","example":"I confirmed my table reservation."},
                     {"front":"departure","back":"khởi hành","example":"The departure was delayed by fog."},
                     {"front":"itinerary","back":"lịch trình","example":"Check the itinerary before you leave."},
                     {"front":"confirmation","back":"xác nhận","example":"You will receive a confirmation email."},
                     {"front":"spelling","back":"chính tả","example":"Check spelling on names and addresses."}]
                    """;
            case 2 -> """
                    [{"front":"methodology","back":"phương pháp nghiên cứu","example":"The methodology uses surveys."},
                     {"front":"hypothesis","back":"giả thuyết","example":"The data supported the hypothesis."},
                     {"front":"empirical","back":"dựa trên quan sát","example":"We need empirical evidence."},
                     {"front":"lecture","back":"bài giảng","example":"The lecture focused on climate data."},
                     {"front":"paraphrase","back":"diễn đạt lại","example":"Listen for paraphrase, not the same word."},
                     {"front":"signpost","back":"từ dẫn dắt","example":"However is a useful signpost."}]
                    """;
            case 3 -> """
                    [{"front":"contradiction","back":"mâu thuẫn","example":"False means there is a contradiction."},
                     {"front":"not given","back":"không có thông tin","example":"Choose NG when the text is silent."},
                     {"front":"proposition","back":"mệnh đề","example":"Compare the proposition with the paragraph."},
                     {"front":"keyword","back":"từ khóa","example":"Underline keywords before scanning."},
                     {"front":"skim","back":"đọc lướt","example":"Skim for the main idea first."},
                     {"front":"scan","back":"đọc tìm chi tiết","example":"Scan for dates and names."}]
                    """;
            case 4 -> """
                    [{"front":"heading","back":"tiêu đề đoạn","example":"Match the heading to the whole paragraph."},
                     {"front":"distractor","back":"lựa chọn nhiễu","example":"A distractor repeats one word only."},
                     {"front":"summary","back":"bản tóm tắt","example":"Complete the summary with paraphrases."},
                     {"front":"topic sentence","back":"câu chủ đề","example":"The topic sentence often carries the idea."},
                     {"front":"narrow","back":"quá hẹp","example":"Avoid headings that are too narrow."},
                     {"front":"overview","back":"cái nhìn tổng quát","example":"The heading should give an overview."}]
                    """;
            case 5 -> """
                    [{"front":"overview","back":"tổng quan biểu đồ","example":"Write a clear overview of two trends."},
                     {"front":"peak","back":"đỉnh","example":"Sales peaked in July."},
                     {"front":"fluctuate","back":"dao động","example":"The figure fluctuated throughout the year."},
                     {"front":"remain stable","back":"ổn định","example":"Unemployment remained stable."},
                     {"front":"significant","back":"đáng kể","example":"There was a significant rise in exports."},
                     {"front":"compare","back":"so sánh","example":"Compare the highest and lowest figures."}]
                    """;
            case 6 -> """
                    [{"front":"opinion","back":"quan điểm","example":"State your opinion in the introduction."},
                     {"front":"discussion","back":"thảo luận hai phía","example":"A discussion essay covers both views."},
                     {"front":"cohesion","back":"liên kết ý","example":"Use linking words for cohesion."},
                     {"front":"example","back":"ví dụ","example":"Support each idea with an example."},
                     {"front":"paraphrase","back":"viết lại đề","example":"Paraphrase the question, do not copy it."},
                     {"front":"outline","back":"dàn ý","example":"Spend five minutes on an outline."}]
                    """;
            case 7 -> """
                    [{"front":"cue card","back":"thẻ gợi ý Part 2","example":"Use one minute to plan the cue card."},
                     {"front":"extend","back":"kéo dài câu trả lời","example":"Extend answers with reasons and examples."},
                     {"front":"fluency","back":"độ trôi chảy","example":"Keep fluency by using fillers naturally."},
                     {"front":"hesitation","back":"ngập ngừng","example":"Too much hesitation lowers the band."},
                     {"front":"personal example","back":"ví dụ cá nhân","example":"A personal example makes Part 1 longer."},
                     {"front":"follow-up","back":"câu hỏi nối","example":"Expect a follow-up after Part 1."}]
                    """;
            default -> """
                    [{"front":"abstract","back":"trừu tượng","example":"Part 3 asks more abstract questions."},
                     {"front":"justify","back":"giải thích quan điểm","example":"Justify your view with a reason."},
                     {"front":"compare","back":"so sánh","example":"Compare city life and rural life."},
                     {"front":"implication","back":"hệ quả","example":"Discuss the implications for education."},
                     {"front":"band descriptor","back":"mô tả band điểm","example":"Check fluency against the band descriptor."},
                     {"front":"self-assess","back":"tự chấm","example":"Self-assess after the mock speaking."}]
                    """;
        };
    }

    private String toeicFlashcardsJson(int unitNumber) {
        return switch (unitNumber) {
            case 1 -> """
                    [{"front":"in the foreground","back":"ở tiền cảnh","example":"A bicycle is in the foreground."},
                     {"front":"be seated","back":"đang ngồi","example":"People are seated around a table."},
                     {"front":"stacked","back":"xếp chồng","example":"Boxes are stacked by the wall."},
                     {"front":"railing","back":"lan can","example":"He is leaning against a railing."},
                     {"front":"pedestrian","back":"người đi bộ","example":"Pedestrians are crossing the street."},
                     {"front":"question-response","back":"hỏi đáp Part 2","example":"Listen to the question word first."}]
                    """;
            case 2 -> """
                    [{"front":"available","back":"rảnh, có sẵn","example":"Is the manager available today?"},
                     {"front":"reschedule","back":"đổi lịch","example":"Can we reschedule the meeting?"},
                     {"front":"extension","back":"số máy lẻ","example":"What is her extension?"},
                     {"front":"department","back":"phòng ban","example":"Which department handles refunds?"},
                     {"front":"confirm","back":"xác nhận","example":"Please confirm the delivery date."},
                     {"front":"appointment","back":"cuộc hẹn","example":"When is your appointment?"}]
                    """;
            case 3 -> """
                    [{"front":"announcement","back":"thông báo","example":"The announcement concerns a gate change."},
                     {"front":"shipment","back":"lô hàng","example":"The shipment arrives on Friday."},
                     {"front":"invoice","back":"hóa đơn","example":"The invoice was emailed yesterday."},
                     {"front":"venue","back":"địa điểm tổ chức","example":"The venue has changed."},
                     {"front":"delay","back":"trì hoãn","example":"We apologize for the delay."},
                     {"front":"proceed to","back":"đi tới","example":"Please proceed to platform six."}]
                    """;
            case 4 -> """
                    [{"front":"efficiently","back":"một cách hiệu quả","example":"The system processes orders efficiently."},
                     {"front":"provided that","back":"với điều kiện","example":"A refund is available provided that you have a receipt."},
                     {"front":"responsible for","back":"chịu trách nhiệm","example":"She is responsible for training."},
                     {"front":"prior to","back":"trước khi","example":"Submit the form prior to departure."},
                     {"front":"approximately","back":"xấp xỉ","example":"The repair takes approximately two hours."},
                     {"front":"word form","back":"dạng từ","example":"Check the word form before you choose."}]
                    """;
            case 5 -> """
                    [{"front":"furthermore","back":"hơn nữa","example":"Furthermore, delivery is free."},
                     {"front":"therefore","back":"vì vậy","example":"The road is closed; therefore use the east gate."},
                     {"front":"in response to","back":"để phản hồi","example":"I am writing in response to your inquiry."},
                     {"front":"enclosed","back":"đính kèm","example":"Please find the enclosed form."},
                     {"front":"regarding","back":"về việc","example":"We received your note regarding the invoice."},
                     {"front":"otherwise","back":"nếu không thì","example":"Pay by Friday; otherwise a fee applies."}]
                    """;
            case 6 -> """
                    [{"front":"according to","back":"theo như","example":"According to the notice, the store closes at six."},
                     {"front":"indicate","back":"cho biết","example":"The article indicates that sales rose."},
                     {"front":"imply","back":"ngụ ý","example":"What is implied about the policy?"},
                     {"front":"most likely","back":"có khả năng nhất","example":"Who most likely wrote the email?"},
                     {"front":"purpose","back":"mục đích","example":"What is the purpose of the memo?"},
                     {"front":"attached","back":"được đính kèm","example":"The schedule is attached."}]
                    """;
            case 7 -> """
                    [{"front":"cross-text","back":"nối nhiều văn bản","example":"A cross-text item links email and schedule."},
                     {"front":"not mentioned","back":"không được nhắc","example":"Choose the option that is not mentioned."},
                     {"front":"correspond","back":"tương ứng","example":"Which date corresponds to the meeting?"},
                     {"front":"refer to","back":"đề cập tới","example":"The second email refers to the invoice."},
                     {"front":"schedule","back":"lịch","example":"Check the schedule against the email."},
                     {"front":"notice","back":"thông báo","example":"The notice lists the new hours."}]
                    """;
            default -> """
                    [{"front":"pacing","back":"phân bổ tốc độ","example":"Good pacing leaves time for Part 7."},
                     {"front":"eliminate","back":"loại trừ","example":"Eliminate choices that contradict the text."},
                     {"front":"distractor","back":"đáp án nhiễu","example":"A distractor repeats a word from the audio."},
                     {"front":"review","back":"xem lại","example":"Review marked questions before submitting."},
                     {"front":"accuracy","back":"độ chính xác","example":"Balance speed with accuracy."},
                     {"front":"time allocation","back":"phân bổ thời gian","example":"Plan time allocation for every part."}]
                    """;
        };
    }

    private String[][] ieltsUnits() {
        return new String[][]{
                {"Unit 1 · Listening Section 1-2", "Form completion, map labelling, everyday conversations.", "Nghe lấy thông tin cụ thể và điền form không mất điểm spelling."},
                {"Unit 2 · Listening Section 3-4", "Academic dialogue và lecture notes.", "Ghi chú bài giảng, nhận biết paraphrase trong lecture."},
                {"Unit 3 · Reading True/False/Not Given", "Skimming, scanning, phân biệt NG với False.", "Tìm keyword và đối chiếu proposition với đoạn văn."},
                {"Unit 4 · Reading Matching Headings", "Matching headings, summary completion.", "Tóm ý đoạn và loại heading nhiễu."},
                {"Unit 5 · Writing Task 1", "Biểu đồ, quy trình, so sánh số liệu.", "Viết overview và chọn số liệu then chốt."},
                {"Unit 6 · Writing Task 2", "Opinion, discussion, problem-solution.", "Lập dàn ý 4 đoạn và paraphrase đề."},
                {"Unit 7 · Speaking Part 1-2", "Câu hỏi đời thường và cue card 2 phút.", "Kéo dài câu trả lời bằng example và reason."},
                {"Unit 8 · Speaking Part 3 và mock test", "Câu hỏi trừu tượng, mock 4 kỹ năng.", "Phản hồi abstract idea và tự chấm theo band."}
        };
    }

    private String[][] toeicUnits() {
        return new String[][]{
                {"Unit 1 · Photographs & Q-R", "Part 1 photographs, Part 2 question-response.", "Loại đáp án nhiễu về thì và từ đồng âm."},
                {"Unit 2 · Conversations", "Part 3 hội thoại 3 người, bảng biểu.", "Nghe mục đích, chi tiết và implied meaning."},
                {"Unit 3 · Talks", "Part 4 announcement, excerpt, voicemail.", "Bắt topic sentence và số liệu trong talk."},
                {"Unit 4 · Incomplete sentences", "Part 5 grammar: thì, giới từ, word form.", "Chọn từ loại đúng trong 20 giây/câu."},
                {"Unit 5 · Text completion", "Part 6 điền từ trong email/thông báo.", "Đọc ngữ cảnh trước-sau chỗ trống."},
                {"Unit 6 · Reading single passages", "Part 7 bài đơn: email, article, advert.", "Câu inference và vocabulary in context."},
                {"Unit 7 · Reading double-triple", "Part 7 bộ 2-3 văn bản.", "Nối thông tin giữa các văn bản."},
                {"Unit 8 · Mini mock 650+", "Đề rút gọn Listening + Reading.", "Quản lý thời gian và review lỗi sai."}
        };
    }

    private List<LocalDate> sessionDates(LocalDate start, LocalDate end, Set<DayOfWeek> days) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            if (days.contains(cursor.getDayOfWeek())) {
                dates.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private void seedChangeRequests(User alien, List<ClassSection> offerings) {
        if (offerings.isEmpty() || changeRequestRepository.count() > 0) {
            return;
        }
        ClassSection offering = offerings.getFirst();
        List<ClassSchedule> schedules = sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId());
        ClassSchedule session = schedules.isEmpty() ? null : schedules.getFirst();
        changeRequestRepository.save(ClassroomChangeRequest.builder()
                .requestType(ClassroomChangeRequestType.RESCHEDULE_SESSION)
                .requester(alien)
                .requesterRole(RoleCodes.TEACHER)
                .classSection(offering)
                .targetClassSchedule(session)
                .reason("Trùng lịch họp phụ huynh, xin dời buổi học ca 1.")
                .status(ClassroomChangeRequestStatus.PENDING)
                .build());
        changeRequestRepository.save(ClassroomChangeRequest.builder()
                .requestType(ClassroomChangeRequestType.CHANGE_ROOM)
                .requester(alien)
                .requesterRole(RoleCodes.TEACHER)
                .classSection(offering)
                .reason("Phòng P001 đang bảo trì loa, xin chuyển phòng.")
                .status(ClassroomChangeRequestStatus.PENDING)
                .build());
    }

    private void seedUpcomingAlertClass(List<User> teachers, List<User> learners) {
        if (offeringRepository.findByInstructorLedCourseSlugOrCode("center-sheet-class-31").isPresent()) {
            return;
        }
        if (teachers.size() < 2 || learners.size() < 4) {
            return;
        }
        User teacher = teachers.get(1);
        InstructorLedCourse program = ensureSheetTrainingProgram(
                "center-sheet-ielts-4skills", "IELTS 4 kỹ năng ca tối", ClassroomDeliveryMode.OFFLINE);
        ClassSection offering = upsertOffering(
                "center-sheet-class-31",
                "IELTS Center K4 T2-4-6 Ca 1",
                false,
                LocalDate.now().plusDays(6),
                LocalDate.now().plusWeeks(12),
                teacher,
                roomRepository.findByActiveTrue().stream().findFirst().orElse(null),
                false,
                program
        );
        offering.setCapacity(10);
        offering.setStatus(ClassroomOfferingStatus.UPCOMING);
        offeringRepository.save(offering);
        attachCourse(offering, program);
        ensureTeacherAssignment(offering, teacher);
        ensureClassEnrollment(offering, learners.get(1), teacher, LocalDate.now());
        ensureClassEnrollment(offering, learners.get(2), teacher, LocalDate.now());
        ensureGradebook(offering, learners.get(1), teacher, 30);
        ensureGradebook(offering, learners.get(2), teacher, 30);
    }

    private void ensureGradebook(ClassSection offering, User learner, User teacher, int salt) {
        BigDecimal homework = BigDecimal.valueOf(6.5 + (salt + learner.getId().intValue()) % 30 * 0.1)
                .min(BigDecimal.TEN);
        BigDecimal quiz = BigDecimal.valueOf(6.8 + (salt * 2 + learner.getId().intValue()) % 28 * 0.1)
                .min(BigDecimal.TEN);
        BigDecimal attendance = BigDecimal.valueOf(78 + (salt + learner.getId().intValue()) % 20);
        BigDecimal participation = BigDecimal.valueOf(7.0 + (salt + 4) % 25 * 0.1).min(BigDecimal.TEN);
        BigDecimal finalResult = homework.add(quiz).add(participation)
                .divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);
        ClassroomGradebookEntry entry = gradebookEntryRepository
                .findByClassSectionIdAndStudentId(offering.getId(), learner.getId())
                .orElseGet(() -> ClassroomGradebookEntry.builder()
                        .classSection(offering)
                        .student(learner)
                        .build());
        entry.setHomeworkScore(homework);
        entry.setQuizScore(quiz);
        entry.setAttendancePercent(attendance);
        entry.setParticipationScore(participation);
        entry.setFinalResult(finalResult);
        entry.setTeacherComment("Tiến độ đều, cần giữ phong độ bài tập về nhà.");
        entry.setStatus(GradebookEntryStatus.PUBLISHED);
        entry.setUpdatedBy(teacher);
        gradebookEntryRepository.save(entry);
    }

    private User ensureUser(String email, String fullName, String roleCode) {
        return userRepository.findByEmail(email).map(existing -> {
            userRoleService.ensureRole(existing, roleCode);
            existing.setFullName(fullName);
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
            userRoleService.assignRole(created, roleCode);
            return userRepository.save(created);
        });
    }

    private User ensureExistingOrCreate(String email, String fullName, String roleCode) {
        return ensureUser(email, fullName, roleCode);
    }
}
