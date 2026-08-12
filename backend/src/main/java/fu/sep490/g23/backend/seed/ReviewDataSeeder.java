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

@Slf4j
@Component
@Order(400)
@RequiredArgsConstructor
public class ReviewDataSeeder implements CommandLineRunner {

    private static final String PASSWORD = "Password123!";
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

    @Value("${app.seed.review.enabled:false}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        ClassroomCampus campus = ensureCampus();
        ClassroomRoom ieltsRoom = ensureRoom("IELTS 01", campus, 24);
        ClassroomRoom toeicRoom = ensureRoom("TOEIC 01", campus, 30);
        ensureRoom("Speaking Studio", campus, 12);

        User manager = ensureUser("review.manager@englishlab.vn", "Nguyễn Hoài An", RoleEnum.MANAGER);
        ensureUser("review.staff@englishlab.vn", "Vũ Ngọc Mai", RoleEnum.STAFF);
        User ieltsTeacher = ensureUser("review.teacher.ielts@englishlab.vn", "Nguyễn Khánh Linh", RoleEnum.TEACHER);
        User toeicTeacher = ensureUser("review.teacher.toeic@englishlab.vn", "Trần Minh Quân", RoleEnum.TEACHER);
        ensureUser("review.teacher.speaking@englishlab.vn", "Phạm Thu Hương", RoleEnum.TEACHER);
        ensureUser("review.teacher.communication@englishlab.vn", "Lê Hoàng Nam", RoleEnum.TEACHER);
        List<User> ieltsLearners = List.of(
                ensureUser("review.learner.minhanh@englishlab.vn", "Phạm Minh Anh", RoleEnum.LEARNER),
                ensureUser("review.learner.giahuy@englishlab.vn", "Hoàng Gia Huy", RoleEnum.LEARNER),
                ensureUser("review.learner.ngocmai@englishlab.vn", "Trần Ngọc Mai", RoleEnum.LEARNER)
        );
        List<User> toeicLearners = List.of(
                ensureUser("review.learner.quanghuy@englishlab.vn", "Nguyễn Quang Huy", RoleEnum.LEARNER),
                ensureUser("review.learner.thuha@englishlab.vn", "Lê Thu Hà", RoleEnum.LEARNER),
                ensureUser("review.learner.baolong@englishlab.vn", "Trần Bảo Long", RoleEnum.LEARNER)
        );

        PackageType classroomType = packageTypeRepository.findByCode(PackageTypeCode.CLASSROOM)
                .orElseGet(() -> packageTypeRepository.save(PackageType.builder()
                        .code(PackageTypeCode.CLASSROOM)
                        .name("Lớp học")
                        .description("Lớp học có giảng viên.")
                        .active(true)
                        .build()));

        ClassroomOffering ieltsClass = ensureClassroom(
                classroomType, manager, ieltsTeacher, ieltsRoom,
                "Review IELTS Intermediate - 2-4-6", "review-ielts-intermediate-246",
                "IELTS 5.0 - 6.0", "Nâng kỹ năng Reading và Writing học thuật.", 5_900_000
        );
        ClassroomOffering toeicClass = ensureClassroom(
                classroomType, manager, toeicTeacher, toeicRoom,
                "Review TOEIC 650 - 3-5-7", "review-toeic-650-357",
                "TOEIC 550 - 650", "Củng cố Listening và Reading để đạt 650+.", 4_900_000
        );

        ensureEnrollment(ieltsClass, ieltsLearners, manager);
        ensureEnrollment(toeicClass, toeicLearners, manager);
        log.info("[ReviewDataSeeder] Đã đồng bộ dữ liệu review cho một cơ sở EnglishLab.");
    }

    private ClassroomCampus ensureCampus() {
        List<ClassroomCampus> activeCampuses = campusRepository.findByActiveTrueOrderByNameAsc();
        ClassroomCampus campus = activeCampuses.stream()
                .filter(item -> CAMPUS_NAME.equalsIgnoreCase(item.getName()))
                .findFirst()
                .orElseGet(() -> activeCampuses.stream().findFirst().orElseGet(ClassroomCampus::new));
        campus.setName(CAMPUS_NAME);
        campus.setAddress("EnglishLab Center, Hà Nội");
        campus.setNote("Cơ sở duy nhất của EnglishLab.");
        campus.setActive(true);
        ClassroomCampus savedCampus = campusRepository.save(campus);
        activeCampuses.stream()
                .filter(item -> !item.getId().equals(savedCampus.getId()))
                .forEach(item -> {
                    item.setActive(false);
                    campusRepository.save(item);
                });
        return savedCampus;
    }

    private ClassroomRoom ensureRoom(String name, ClassroomCampus campus, int capacity) {
        return roomRepository.findByCampusIdAndActiveTrueOrderByNameAsc(campus.getId()).stream()
                .filter(room -> name.equalsIgnoreCase(room.getName()))
                .findFirst()
                .orElseGet(() -> roomRepository.save(ClassroomRoom.builder()
                        .name(name)
                        .campus(campus)
                        .capacity(capacity)
                        .active(true)
                        .build()));
    }

    private User ensureUser(String email, String fullName, RoleEnum role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .password(passwordEncoder.encode(PASSWORD))
                    .emailVerified(true)
                    .profileCompleted(role != RoleEnum.LEARNER)
                    .build();
            userRoleService.assignRole(user, role);
            return userRepository.save(user);
        });
    }

    private ClassroomOffering ensureClassroom(
            PackageType classroomType,
            User manager,
            User teacher,
            ClassroomRoom room,
            String title,
            String slug,
            String entryLevel,
            String outcome,
            long price
    ) {
        return offeringRepository.findByLearningPackageSlug(slug).orElseGet(() -> {
            LearningPackage learningPackage = learningPackageRepository.save(LearningPackage.builder()
                    .packageType(classroomType)
                    .title(title)
                    .slug(slug)
                    .shortDescription("Dữ liệu review cho lớp học tại EnglishLab.")
                    .description("Lớp học dùng để review các luồng vận hành, giáo viên và học viên.")
                    .price(BigDecimal.valueOf(price))
                    .status(PackageStatus.PUBLISHED)
                    .duration("10 buổi")
                    .studyMode("Offline")
                    .createdBy(manager)
                    .build());
            LocalDate startDate = LocalDate.now().plusDays(7);
            ClassroomOffering offering = offeringRepository.save(ClassroomOffering.builder()
                    .learningPackage(learningPackage)
                    .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                    .status(ClassroomOfferingStatus.UPCOMING)
                    .entryLevel(entryLevel)
                    .targetOutcome(outcome)
                    .maxCapacity(room.getCapacity())
                    .startDate(startDate)
                    .endDate(startDate.plusWeeks(8))
                    .primaryTeacher(teacher)
                    .defaultRoom(room)
                    .offlineAddress("EnglishLab Center, Hà Nội")
                    .larkMeetingStatus(LarkMeetingStatus.NOT_CREATED)
                    .syllabusSummary("Lộ trình 10 buổi có bài tập và phản hồi từ giáo viên.")
                    .build());
            teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                    .classroomOffering(offering)
                    .teacher(teacher)
                    .role(ClassroomTeacherRole.PRIMARY)
                    .effectiveFrom(startDate)
                    .build());
            sessionRepository.save(ClassroomSession.builder()
                    .classroomOffering(offering)
                    .sessionDate(startDate)
                    .startTime(LocalTime.of(19, 30))
                    .endTime(LocalTime.of(21, 0))
                    .teacher(teacher)
                    .room(room)
                    .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                    .status(ClassroomSessionStatus.SCHEDULED)
                    .sessionContent("Buổi 1: Orientation và đánh giá mục tiêu học tập.")
                    .build());
            return offering;
        });
    }

    private void ensureEnrollment(ClassroomOffering offering, List<User> learners, User manager) {
        learners.forEach(learner -> {
            if (enrollmentRepository.existsByStudentIdAndClassroomOfferingId(learner.getId(), offering.getId())) {
                return;
            }
            enrollmentRepository.save(ClassroomEnrollment.builder()
                    .student(learner)
                    .classroomOffering(offering)
                    .status(ClassroomEnrollmentStatus.ENROLLED)
                    .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                    .tuitionAmountDue(offering.getLearningPackage().getPrice())
                    .tuitionAmountPaid(offering.getLearningPackage().getPrice())
                    .enrolledAt(LocalDateTime.now())
                    .assignedAt(LocalDateTime.now())
                    .assignedBy(manager)
                    .confirmedAt(LocalDateTime.now())
                    .confirmedBy(manager)
                    .build());
        });
    }
}
