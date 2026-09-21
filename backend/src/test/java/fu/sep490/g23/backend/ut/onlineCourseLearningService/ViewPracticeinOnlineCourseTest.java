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
    void ut01_studentNotEnrolled_throwsForbidden() {
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
}
