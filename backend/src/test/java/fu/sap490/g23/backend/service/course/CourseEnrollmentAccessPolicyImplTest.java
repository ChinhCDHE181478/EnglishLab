package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sap490.g23.backend.exception.EnrollmentAccessException;
import fu.sap490.g23.backend.exception.EnrollmentErrorCode;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.service.course.impl.CourseEnrollmentAccessPolicyImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseEnrollmentAccessPolicyImplTest {

    @Mock
    private PackageEnrollmentRepository enrollmentRepository;

    private CourseEnrollmentAccessPolicy policy;
    private User student;
    private LearningPackage learningPackage;
    private OnlineCourse course;
    private PackageEnrollment enrollment;

    @BeforeEach
    void setUp() {
        policy = new CourseEnrollmentAccessPolicyImpl(enrollmentRepository);
        student = User.builder().email("learner@example.com").build();
        learningPackage = LearningPackage.builder().id(10L).build();
        course = OnlineCourse.builder().id(5L).learningPackage(learningPackage).build();
        enrollment = PackageEnrollment.builder()
                .id(100L)
                .student(student)
                .learningPackage(learningPackage)
                .status(EnrollmentStatus.ACTIVE)
                .registeredAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    void requireLearningAccess_allowsActiveEnrollment() {
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.of(enrollment));

        PackageEnrollment result = policy.requireLearningAccess(student, course);

        assertSame(enrollment, result);
    }

    @Test
    void hasLearningAccess_allowsCompletedEnrollment() {
        enrollment.setStatus(EnrollmentStatus.COMPLETED);

        assertTrue(policy.hasLearningAccess(enrollment));
        assertTrue(policy.hasAssessmentAccess(enrollment));
    }

    @Test
    void hasLearningAccess_deniesMissingStatus() {
        enrollment.setStatus(null);

        assertFalse(policy.hasLearningAccess(enrollment));
        assertFalse(policy.hasAssessmentAccess(enrollment));
    }

    @Test
    void requireLearningAccess_blocksMissingEnrollment() {
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.empty());

        EnrollmentAccessException exception = assertThrows(EnrollmentAccessException.class,
                () -> policy.requireLearningAccess(student, course));

        assertEquals(EnrollmentErrorCode.NOT_ENROLLED, exception.getCode());
        assertEquals("Bạn cần đăng ký khóa học trước khi xem nội dung.", exception.getMessage());
    }

    @Test
    void requireAssessmentAccess_blocksCancelledEnrollment() {
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.of(enrollment));

        EnrollmentAccessException exception = assertThrows(EnrollmentAccessException.class,
                () -> policy.requireAssessmentAccess(student, course));

        assertEquals(EnrollmentErrorCode.ENROLLMENT_CANCELLED, exception.getCode());
        assertEquals("Bạn đã hủy đăng ký khóa học này. Vui lòng đăng ký lại để tiếp tục.", exception.getMessage());
    }

    @Test
    void requireAssessmentAccess_blocksUnknownStatus() {
        enrollment.setStatus(null);
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.of(enrollment));

        EnrollmentAccessException exception = assertThrows(EnrollmentAccessException.class,
                () -> policy.requireAssessmentAccess(student, course));

        assertEquals(EnrollmentErrorCode.ENROLLMENT_INVALID_STATUS, exception.getCode());
        assertEquals("Không thể xác định trạng thái đăng ký. Vui lòng đăng ký lại khóa học.", exception.getMessage());
    }

    @Test
    void reactivateCancelledEnrollment_restoresLearningAccessAndPersists() {
        LocalDateTime previousRegisteredAt = enrollment.getRegisteredAt();
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        PackageEnrollment result = policy.reactivateCancelledEnrollment(enrollment);

        assertSame(enrollment, result);
        assertEquals(EnrollmentStatus.ACTIVE, enrollment.getStatus());
        assertTrue(enrollment.getRegisteredAt().isAfter(previousRegisteredAt));
        assertTrue(policy.hasLearningAccess(enrollment));
        verify(enrollmentRepository).save(enrollment);
    }
}
