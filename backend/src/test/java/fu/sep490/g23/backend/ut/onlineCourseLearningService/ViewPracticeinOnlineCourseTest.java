package fu.sep490.g23.backend.ut.onlineCourseLearningService;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.exception.EnrollmentAccessException;
import fu.sep490.g23.backend.exception.EnrollmentErrorCode;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.impl.CourseEnrollmentAccessPolicyImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * UC-24: View Practice in Online Course - test ngắn gọn với mock data.
 */
@ExtendWith(MockitoExtension.class)
public class ViewPracticeinOnlineCourseTest {

    @Mock
    private OnlineCourseEnrollmentRepository enrollmentRepository;

    private CourseEnrollmentAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new CourseEnrollmentAccessPolicyImpl(enrollmentRepository);
    }

    // UT-01: Học viên chưa đăng ký khóa học (PRE-3) -> ForbiddenException (403)
    @Test
    void viewPracticeInOnlineCourse_UTC01_studentNotEnrolled() {
        User student = User.builder().id(1L).email("user_01@test.com").build();
        OnlineCourse course = OnlineCourse.builder().id(1L).slug("course_01").build();

        when(enrollmentRepository.findByStudentAndOnlineCourse(student, course))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policy.requireLearningAccess(student, course))
                .isInstanceOf(EnrollmentAccessException.class)
                .extracting("code").isEqualTo(EnrollmentErrorCode.NOT_ENROLLED);

        verify(enrollmentRepository, times(1)).findByStudentAndOnlineCourse(student, course);
    }

    // UT-02: Module đang bị khóa (BR-57 / PRE-4) -> ForbiddenException
    @Test
    void ut02_moduleLocked_throwsForbidden() {
        User student = User.builder().id(1L).email("user_01@test.com").build();
        OnlineCourse course = OnlineCourse.builder().id(2L).slug("course_01").build();

        when(enrollmentRepository.findByStudentAndOnlineCourse(student, course))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policy.requireLearningAccess(student, course))
                .isInstanceOf(EnrollmentAccessException.class);

        verify(enrollmentRepository, times(1)).findByStudentAndOnlineCourse(student, course);
    }

    // UT-03: Học viên đã đăng ký nhưng enrollment bị CANCELLED (PRE-5 / BR-58)
    //        -> EnrollmentAccessException voi code ENROLLMENT_CANCELLED
    //        -> Thong bao yeu cau dang ky lai
    @Test
    void viewPracticeInOnlineCourse_UTC03_enrollmentCancelled_throwsAccessDenied() {
        // Arrange
        User student = User.builder().id(3L).email("user_03@test.com").build();
        OnlineCourse course = OnlineCourse.builder().id(3L).slug("course_03").build();
        OnlineCourseEnrollment cancelledEnrollment = OnlineCourseEnrollment.builder()
                .id(30L)
                .student(student)
                .onlineCourse(course)
                .status(EnrollmentStatus.CANCELLED)
                .build();

        when(enrollmentRepository.findByStudentAndOnlineCourse(student, course))
                .thenReturn(Optional.of(cancelledEnrollment));

        // Act & Assert: phai nem EnrollmentAccessException voi code ENROLLMENT_CANCELLED
        assertThatThrownBy(() -> policy.requireLearningAccess(student, course))
                .isInstanceOf(EnrollmentAccessException.class)
                .extracting("code").isEqualTo(EnrollmentErrorCode.ENROLLMENT_CANCELLED);

        // Verify thong bao loi yeu cau dang ky lai (MSG lien quan den CANCELLED)
        assertThatThrownBy(() -> policy.requireLearningAccess(student, course))
                .isInstanceOf(EnrollmentAccessException.class)
                .hasMessageContaining("đã hủy đăng ký");

        // Verify repository chi duoc goi 1 lan (khi findEnrollment)
        verify(enrollmentRepository, times(2)).findByStudentAndOnlineCourse(student, course);
        // Verify KHONG save enrollment moi khi bi CANCELLED
        verify(enrollmentRepository, never()).save(any(OnlineCourseEnrollment.class));
    }
}
