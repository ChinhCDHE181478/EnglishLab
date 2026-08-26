package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.Room;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
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
    private final RoomRepository roomRepository;
    private final InstructorLedCourseRepository instructorLedCourseRepository;
    private final ClassSectionRepository offeringRepository;
    private final ClassScheduleRepository sessionRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    @Value("${app.seed.review.enabled:false}")
    private boolean enabled;

    @Value("${app.seed.sheet.enabled:false}")
    private boolean sheetEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || sheetEnabled) {
            return;
        }

        Room ieltsRoom = ensureRoom("IELTS 01", 24);
        Room toeicRoom = ensureRoom("TOEIC 01", 30);
        ensureRoom("Speaking Studio", 12);

        User manager = ensureUser("review.manager@englishlab.vn", "Nguyễn Hoài An", RoleCodes.MANAGER);
        ensureUser("review.staff@englishlab.vn", "Vũ Ngọc Mai", RoleCodes.STAFF);
        User ieltsTeacher = ensureUser("review.teacher.ielts@englishlab.vn", "Nguyễn Khánh Linh", RoleCodes.TEACHER);
        User toeicTeacher = ensureUser("review.teacher.toeic@englishlab.vn", "Trần Minh Quân", RoleCodes.TEACHER);
        ensureUser("review.teacher.speaking@englishlab.vn", "Phạm Thu Hương", RoleCodes.TEACHER);
        ensureUser("review.teacher.communication@englishlab.vn", "Lê Hoàng Nam", RoleCodes.TEACHER);
        List<User> ieltsLearners = List.of(
                ensureUser("review.learner.minhanh@englishlab.vn", "Phạm Minh Anh", RoleCodes.LEARNER),
                ensureUser("review.learner.giahuy@englishlab.vn", "Hoàng Gia Huy", RoleCodes.LEARNER),
                ensureUser("review.learner.ngocmai@englishlab.vn", "Trần Ngọc Mai", RoleCodes.LEARNER)
        );
        List<User> toeicLearners = List.of(
                ensureUser("review.learner.quanghuy@englishlab.vn", "Nguyễn Quang Huy", RoleCodes.LEARNER),
                ensureUser("review.learner.thuha@englishlab.vn", "Lê Thu Hà", RoleCodes.LEARNER),
                ensureUser("review.learner.baolong@englishlab.vn", "Trần Bảo Long", RoleCodes.LEARNER)
        );

        ClassSection ieltsClass = ensureClassroom(
                manager, ieltsTeacher, ieltsRoom,
                "Review IELTS Intermediate - 2-4-6", "review-ielts-intermediate-246",
                "IELTS 5.0 - 6.0", "Nâng kỹ năng Reading và Writing học thuật.", 5_900_000
        );
        ClassSection toeicClass = ensureClassroom(
                manager, toeicTeacher, toeicRoom,
                "Review TOEIC 650 - 3-5-7", "review-toeic-650-357",
                "TOEIC 550 - 650", "Củng cố Listening và Reading để đạt 650+.", 4_900_000
        );

        ensureEnrollment(ieltsClass, ieltsLearners, manager);
        ensureEnrollment(toeicClass, toeicLearners, manager);
        log.info("[ReviewDataSeeder] Đã đồng bộ dữ liệu review cho một cơ sở EnglishLab.");
    }

    private Room ensureRoom(String name, int capacity) {
        return roomRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(room -> name.equalsIgnoreCase(room.getName()))
                .findFirst()
                .orElseGet(() -> roomRepository.save(Room.builder()
                        .name(name)
                        .locationName(CAMPUS_NAME)
                        .locationAddress("EnglishLab Center, Hà Nội")
                        .capacity(capacity)
                        .active(true)
                        .build()));
    }

    private User ensureUser(String email, String fullName, String roleCode) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .password(passwordEncoder.encode(PASSWORD))
                    .emailVerified(true)
                    .profileCompleted(!RoleCodes.LEARNER.equals(roleCode))
                    .build();
            userRoleService.assignRole(user, roleCode);
            return userRepository.save(user);
        });
    }

    private ClassSection ensureClassroom(
            User manager,
            User teacher,
            Room room,
            String title,
            String slug,
            String entryLevel,
            String outcome,
            long price
    ) {
        return offeringRepository.findByCode(slug).orElseGet(() -> {
            InstructorLedCourse course = instructorLedCourseRepository.findBySlug(slug + "-course")
                    .orElseGet(() -> instructorLedCourseRepository.save(InstructorLedCourse.builder()
                    .code(("REVIEW-" + slug).toUpperCase())
                    .slug(slug + "-course")
                    .title(title)
                    .shortDescription("Dữ liệu review cho lớp học tại EnglishLab.")
                    .description("Lớp học dùng để review các luồng vận hành, giáo viên và học viên.")
                    .baseTuitionFeeVnd(BigDecimal.valueOf(price))
                    .publicationStatus(PackageStatus.PUBLISHED)
                    .durationLabel("10 buổi")
                    .createdBy(manager)
                    .build()));
            LocalDate startDate = LocalDate.now().plusDays(7);
            ClassSection offering = offeringRepository.save(ClassSection.builder()
                    .name(title)
                    .code(slug)
                    .instructorLedCourse(course)
                    .tuitionFeeVnd(BigDecimal.valueOf(price))
                    .status(ClassroomOfferingStatus.UPCOMING)
                    .entryLevel(entryLevel)
                    .targetOutcome(outcome)
                    .capacity(room.getCapacity())
                    .startDate(startDate)
                    .plannedEndDate(startDate.plusWeeks(8))
                    .primaryTeacher(teacher)
                    .room(room)
                    .offlineAddress("EnglishLab Center, Hà Nội")
                    .syllabusSummary("Lộ trình 10 buổi có bài tập và phản hồi từ giáo viên.")
                    .build());
            teacherAssignmentRepository.save(ClassroomTeacherAssignment.builder()
                    .classSection(offering)
                    .teacher(teacher)
                    .role(ClassroomTeacherRole.PRIMARY)
                    .effectiveFrom(startDate)
                    .build());
            sessionRepository.save(ClassSchedule.builder()
                    .classSection(offering)
                    .sessionDate(startDate)
                    .startTime(LocalTime.of(19, 30))
                    .endTime(LocalTime.of(21, 0))
                    .teacher(teacher)
                    .room(room)
                    .status(ClassroomSessionStatus.SCHEDULED)
                    .sessionContent("Buổi 1: Orientation và đánh giá mục tiêu học tập.")
                    .build());
            return offering;
        });
    }

    private void ensureEnrollment(ClassSection offering, List<User> learners, User manager) {
        learners.forEach(learner -> {
            if (enrollmentRepository.existsByStudentIdAndClassSectionId(learner.getId(), offering.getId())) {
                return;
            }
            enrollmentRepository.save(ClassEnrollment.builder()
                    .student(learner)
                    .classSection(offering)
                    .status(ClassroomEnrollmentStatus.ENROLLED)
                    .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                    .tuitionAmountDue(offering.getPrice())
                    .tuitionAmountPaid(offering.getPrice())
                    .enrolledAt(LocalDateTime.now())
                    .assignedAt(LocalDateTime.now())
                    .assignedBy(manager)
                    .confirmedAt(LocalDateTime.now())
                    .confirmedBy(manager)
                    .build());
        });
    }
}
