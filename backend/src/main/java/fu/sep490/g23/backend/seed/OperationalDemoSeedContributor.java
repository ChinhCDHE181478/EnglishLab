package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomCampus;
import fu.sep490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.ClassroomRoom;
import fu.sep490.g23.backend.entity.classroom.ClassroomSession;
import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.PackageType;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomCampusRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.PackageTypeRepository;
import fu.sep490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OperationalDemoSeedContributor implements SeedDataContributor {

    private static final String CAMPUS_NAME = "EnglishLab";

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final ClassroomCampusRepository campusRepository;
    private final ClassroomRoomRepository roomRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;

    @Value("${app.seed.demo.password:}")
    private String demoPassword;

    @Override
    public int order() {
        return 300;
    }

    @Override
    public String name() {
        return "Dữ liệu demo vận hành";
    }

    @Override
    public Set<SeedMode> supportedModes() {
        return Set.of(SeedMode.TEST, SeedMode.REVIEW, SeedMode.SHEET);
    }

    @Override
    public void seed(Set<SeedMode> activeModes) {
        if (demoPassword == null || demoPassword.isBlank()) {
            throw new IllegalStateException(
                    "APP_SEED_DEMO_PASSWORD phải được cấu hình khi bật seed TEST, REVIEW hoặc SHEET."
            );
        }

        ClassroomCampus campus = ensureCampus();
        ClassroomRoom ieltsRoom = ensureRoom(campus, "IELTS 01", 24);
        ClassroomRoom toeicRoom = ensureRoom(campus, "TOEIC 01", 30);
        ensureRoom(campus, "Speaking Studio", 12);

        User admin = ensureUser("demo.admin@englishlab.vn", "Quản trị EnglishLab", RoleEnum.ADMIN);
        User manager = ensureUser("demo.manager@englishlab.vn", "Nguyễn Hoài An", RoleEnum.MANAGER);
        ensureUser("demo.staff@englishlab.vn", "Vũ Ngọc Mai", RoleEnum.STAFF);
        ensureUser("demo.content@englishlab.vn", "Lê Minh Châu", RoleEnum.CONTENT_MANAGER);
        User ieltsTeacher = ensureUser(
                "demo.teacher.ielts@englishlab.vn",
                "Nguyễn Khánh Linh",
                RoleEnum.TEACHER
        );
        User toeicTeacher = ensureUser(
                "demo.teacher.toeic@englishlab.vn",
                "Trần Minh Quân",
                RoleEnum.TEACHER
        );

        List<User> ieltsLearners = List.of(
                ensureLearner("demo.learner.minhanh@englishlab.vn", "Phạm Minh Anh", "IELTS", "6.5", 5.0),
                ensureLearner("demo.learner.giahuy@englishlab.vn", "Hoàng Gia Huy", "IELTS", "6.5", 5.5),
                ensureLearner("demo.learner.ngocmai@englishlab.vn", "Trần Ngọc Mai", "IELTS", "6.5", 5.0)
        );
        List<User> toeicLearners = List.of(
                ensureLearner("demo.learner.quanghuy@englishlab.vn", "Nguyễn Quang Huy", "TOEIC", "650", 4.5),
                ensureLearner("demo.learner.thuha@englishlab.vn", "Lê Thu Hà", "TOEIC", "650", 5.0),
                ensureLearner("demo.learner.baolong@englishlab.vn", "Trần Bảo Long", "TOEIC", "650", 4.5)
        );

        PackageType classroomType = packageTypeRepository.findByCode(PackageTypeCode.CLASSROOM)
                .orElseThrow(() -> new IllegalStateException("Thiếu loại sản phẩm CLASSROOM."));
        ClassroomOffering ieltsClass = ensureClassroom(
                classroomType,
                admin,
                ieltsTeacher,
                ieltsRoom,
                "IELTS Intermediate T2-4-6",
                "demo-ielts-intermediate-246",
                "IELTS 5.0",
                "Hướng tới IELTS 6.0 với trọng tâm Reading và Writing.",
                BigDecimal.valueOf(5_900_000)
        );
        ClassroomOffering toeicClass = ensureClassroom(
                classroomType,
                manager,
                toeicTeacher,
                toeicRoom,
                "TOEIC 650 T3-5-7",
                "demo-toeic-650-357",
                "TOEIC 500",
                "Củng cố Listening và Reading để đạt TOEIC 650+.",
                BigDecimal.valueOf(4_900_000)
        );

        ensureEnrollments(ieltsClass, ieltsLearners, manager);
        ensureEnrollments(toeicClass, toeicLearners, manager);
    }

    private ClassroomCampus ensureCampus() {
        ClassroomCampus campus = campusRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(item -> CAMPUS_NAME.equalsIgnoreCase(item.getName()))
                .findFirst()
                .orElseGet(ClassroomCampus::new);
        campus.setName(CAMPUS_NAME);
        campus.setAddress("EnglishLab Center, Hà Nội");
        campus.setNote("Cơ sở đào tạo EnglishLab.");
        campus.setActive(true);
        return campusRepository.save(campus);
    }

    private ClassroomRoom ensureRoom(ClassroomCampus campus, String name, int capacity) {
        ClassroomRoom room = roomRepository.findByCampusIdAndActiveTrueOrderByNameAsc(campus.getId()).stream()
                .filter(item -> name.equalsIgnoreCase(item.getName()))
                .findFirst()
                .orElseGet(ClassroomRoom::new);
        room.setCampus(campus);
        room.setName(name);
        room.setCapacity(capacity);
        room.setActive(true);
        return roomRepository.save(room);
    }

    private User ensureUser(String email, String fullName, RoleEnum role) {
        User user = userRepository.findByEmail(email).orElseGet(() -> User.builder()
                .email(email)
                .fullName(fullName)
                .password(passwordEncoder.encode(demoPassword))
                .passwordSet(true)
                .emailVerified(true)
                .profileCompleted(true)
                .build());
        user.setFullName(fullName);
        user.setEmailVerified(true);
        user.setProfileCompleted(true);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(demoPassword));
            user.setPasswordSet(true);
        }
        userRoleService.ensureRole(user, role);
        return userRepository.save(user);
    }

    private User ensureLearner(
            String email,
            String fullName,
            String targetExam,
            String targetScore,
            double currentBand
    ) {
        User learner = ensureUser(email, fullName, RoleEnum.LEARNER);
        learner.setTargetExam(targetExam);
        learner.setTargetScore(targetScore);
        learner.setCurrentBand(currentBand);
        learner.setStudyGoal("Cải thiện năng lực tiếng Anh theo lộ trình cá nhân.");
        return userRepository.save(learner);
    }

    private ClassroomOffering ensureClassroom(
            PackageType classroomType,
            User creator,
            User teacher,
            ClassroomRoom room,
            String title,
            String slug,
            String entryLevel,
            String outcome,
            BigDecimal price
    ) {
        LearningPackage learningPackage = learningPackageRepository.findBySlug(slug)
                .orElseGet(LearningPackage::new);
        learningPackage.setPackageType(classroomType);
        learningPackage.setTitle(title);
        learningPackage.setSlug(slug);
        learningPackage.setShortDescription("Lớp học có giáo viên phụ trách tại EnglishLab.");
        learningPackage.setDescription("Dữ liệu demo cho quy trình vận hành, giảng dạy và ghi danh.");
        learningPackage.setPrice(price);
        learningPackage.setStatus(PackageStatus.PUBLISHED);
        learningPackage.setDuration("10 buổi");
        learningPackage.setStudyMode("Offline");
        learningPackage.setCreatedBy(creator);
        learningPackage.setDeleted(false);
        learningPackage = learningPackageRepository.save(learningPackage);

        LocalDate startDate = LocalDate.now().plusDays(7);
        ClassroomOffering offering = offeringRepository.findByLearningPackageId(learningPackage.getId())
                .orElseGet(ClassroomOffering::new);
        if (offering.getStartDate() != null) {
            startDate = offering.getStartDate();
        }
        offering.setLearningPackage(learningPackage);
        offering.setDeliveryMode(ClassroomDeliveryMode.OFFLINE);
        offering.setStatus(ClassroomOfferingStatus.UPCOMING);
        offering.setEntryLevel(entryLevel);
        offering.setTargetOutcome(outcome);
        offering.setMaxCapacity(room.getCapacity());
        offering.setStartDate(startDate);
        offering.setEndDate(startDate.plusWeeks(8));
        offering.setPrimaryTeacher(teacher);
        offering.setDefaultRoom(room);
        offering.setOfflineAddress("EnglishLab Center, Hà Nội");
        offering.setLarkMeetingStatus(LarkMeetingStatus.NOT_CREATED);
        offering.setSyllabusSummary("Lộ trình 10 buổi có bài tập và phản hồi từ giáo viên.");
        offering = offeringRepository.save(offering);

        ensurePrimaryTeacher(offering, teacher, startDate);
        ensureFirstSession(offering, teacher, room, startDate);
        return offering;
    }

    private void ensurePrimaryTeacher(ClassroomOffering offering, User teacher, LocalDate effectiveFrom) {
        ClassroomTeacherAssignment assignment = teacherAssignmentRepository
                .findAllByClassroomOfferingIdAndTeacherId(offering.getId(), teacher.getId())
                .stream()
                .filter(item -> item.getClassroomSession() == null)
                .findFirst()
                .orElseGet(ClassroomTeacherAssignment::new);
        assignment.setClassroomOffering(offering);
        assignment.setTeacher(teacher);
        assignment.setRole(ClassroomTeacherRole.PRIMARY);
        assignment.setEffectiveFrom(effectiveFrom);
        assignment.setEffectiveTo(null);
        teacherAssignmentRepository.save(assignment);
    }

    private void ensureFirstSession(
            ClassroomOffering offering,
            User teacher,
            ClassroomRoom room,
            LocalDate sessionDate
    ) {
        ClassroomSession session = sessionRepository
                .findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId())
                .stream()
                .findFirst()
                .orElseGet(ClassroomSession::new);
        session.setClassroomOffering(offering);
        session.setSessionDate(sessionDate);
        session.setStartTime(LocalTime.of(19, 30));
        session.setEndTime(LocalTime.of(21, 0));
        session.setTeacher(teacher);
        session.setRoom(room);
        session.setDeliveryMode(ClassroomDeliveryMode.OFFLINE);
        session.setStatus(ClassroomSessionStatus.SCHEDULED);
        session.setLarkMeetingStatus(LarkMeetingStatus.NOT_CREATED);
        session.setSessionContent("Buổi 1: Định hướng và đánh giá mục tiêu học tập.");
        sessionRepository.save(session);
    }

    private void ensureEnrollments(ClassroomOffering offering, List<User> learners, User staff) {
        for (User learner : learners) {
            ClassroomEnrollment enrollment = enrollmentRepository
                    .findByStudentIdAndClassroomOfferingId(learner.getId(), offering.getId())
                    .orElseGet(ClassroomEnrollment::new);
            enrollment.setStudent(learner);
            enrollment.setClassroomOffering(offering);
            enrollment.setStatus(ClassroomEnrollmentStatus.ENROLLED);
            enrollment.setRegistrationStatus(ClassroomRegistrationStatus.ASSIGNED);
            enrollment.setTuitionAmountDue(offering.getLearningPackage().getPrice());
            enrollment.setTuitionAmountPaid(offering.getLearningPackage().getPrice());
            enrollment.setEnrolledAt(defaultDateTime(enrollment.getEnrolledAt()));
            enrollment.setAssignedAt(defaultDateTime(enrollment.getAssignedAt()));
            enrollment.setAssignedBy(staff);
            enrollment.setConfirmedAt(defaultDateTime(enrollment.getConfirmedAt()));
            enrollment.setConfirmedBy(staff);
            enrollmentRepository.save(enrollment);
        }
    }

    private LocalDateTime defaultDateTime(LocalDateTime value) {
        return value == null ? LocalDateTime.now() : value;
    }
}
