package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomGradebookEntryRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomTuitionPaymentRepository;
import fu.sap490.g23.backend.repository.classroom.LarkMeetingParticipantRepository;
import fu.sap490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumProgramRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.impl.ClassroomOfferingServiceImpl;
import fu.sap490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sap490.g23.backend.service.notification.ClassroomNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomOfferingServiceImplTest {

    @Mock
    private ClassroomOfferingRepository offeringRepository;
    @Mock
    private ClassroomSessionRepository sessionRepository;
    @Mock
    private ClassroomEnrollmentRepository enrollmentRepository;
    @Mock
    private ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    @Mock
    private ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    @Mock
    private ClassroomGradebookEntryRepository gradebookEntryRepository;
    @Mock
    private LearningPackageRepository learningPackageRepository;
    @Mock
    private PackageTypeRepository packageTypeRepository;
    @Mock
    private PackageEnrollmentRepository packageEnrollmentRepository;
    @Mock
    private CurriculumProgramRepository curriculumProgramRepository;
    @Mock
    private TrainingProgramRepository trainingProgramRepository;
    @Mock
    private ClassroomMaterialRepository materialRepository;
    @Mock
    private ClassroomRoomRepository roomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClassroomMapper mapper;
    @Mock
    private ClassroomConflictService conflictService;
    @Mock
    private LarkMeetingService larkMeetingService;
    @Mock
    private ClassroomAccessHelper accessHelper;
    @Mock
    private ClassroomNotificationService notificationService;
    @Mock
    private LarkMeetingParticipantRepository larkParticipantRepository;
    @Mock
    private CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    @Mock
    private VirtualAttendanceService virtualAttendanceService;

    private ClassroomOfferingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassroomOfferingServiceImpl(
                offeringRepository,
                sessionRepository,
                enrollmentRepository,
                tuitionPaymentRepository,
                teacherAssignmentRepository,
                gradebookEntryRepository,
                learningPackageRepository,
                packageTypeRepository,
                packageEnrollmentRepository,
                curriculumProgramRepository,
                trainingProgramRepository,
                materialRepository,
                roomRepository,
                userRepository,
                mapper,
                conflictService,
                larkMeetingService,
                accessHelper,
                notificationService,
                larkParticipantRepository,
                courseEnrollmentAccessPolicy,
                virtualAttendanceService
        );
    }

    @Test
    void ensurePackageEnrollment_reactivatesCancelledEnrollmentForClassroomReRegistration() {
        User learner = User.builder().id(7L).email("learner@example.com").build();
        LearningPackage learningPackage = LearningPackage.builder().id(10L).title("IELTS Foundation").build();
        ClassroomOffering offering = ClassroomOffering.builder()
                .id(20L)
                .learningPackage(learningPackage)
                .build();
        PackageEnrollment cancelledEnrollment = PackageEnrollment.builder()
                .id(30L)
                .student(learner)
                .learningPackage(learningPackage)
                .status(EnrollmentStatus.CANCELLED)
                .registeredAt(LocalDateTime.now().minusDays(3))
                .build();
        PackageEnrollment reactivatedEnrollment = PackageEnrollment.builder()
                .id(30L)
                .student(learner)
                .learningPackage(learningPackage)
                .status(EnrollmentStatus.ACTIVE)
                .registeredAt(LocalDateTime.now())
                .build();

        when(packageEnrollmentRepository.findByStudentAndLearningPackage(learner, learningPackage))
                .thenReturn(Optional.of(cancelledEnrollment));
        when(courseEnrollmentAccessPolicy.hasLearningAccess(cancelledEnrollment)).thenReturn(false);
        when(courseEnrollmentAccessPolicy.reactivateCancelledEnrollment(cancelledEnrollment))
                .thenReturn(reactivatedEnrollment);

        PackageEnrollment result = ReflectionTestUtils.invokeMethod(
                service,
                "ensurePackageEnrollment",
                learner,
                offering
        );

        assertSame(reactivatedEnrollment, result);
        assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());
    }

    @Test
    void tryAssignEnrollment_putsLearnerOnWaitlistWhenClassIsFullUnderLock() {
        User learner = User.builder().id(7L).build();
        ClassroomOffering offering = ClassroomOffering.builder().id(20L).maxCapacity(1).build();
        ClassroomEnrollment enrollment = ClassroomEnrollment.builder().student(learner).build();

        when(offeringRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.countByOfferingAndRegistrationStatuses(
                eq(20L),
                eq(ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT)
        )).thenReturn(1L);

        ReflectionTestUtils.invokeMethod(
                service,
                "tryAssignEnrollment",
                enrollment,
                offering,
                learner,
                null,
                "Xếp lớp"
        );

        assertEquals(ClassroomRegistrationStatus.WAITLIST, enrollment.getRegistrationStatus());
    }
}
