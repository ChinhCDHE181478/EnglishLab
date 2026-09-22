package fu.sep490.g23.backend.ut.onlineCourseLearningService;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
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


@ExtendWith(MockitoExtension.class)
public class AccessOnlineCourseLessonsTest {

    @Mock
    private OnlineCourseEnrollmentRepository enrollmentRepository;

    private CourseEnrollmentAccessPolicy accessPolicy;

    @BeforeEach
    void setUp() {
        accessPolicy = new CourseEnrollmentAccessPolicyImpl(enrollmentRepository);
    }

    //Học viên chưa đăng ký khóa học
    @Test
    void accessLesson_whenStudentNotEnrolled_throwsForbidden() {
        // Arrange
        String userId = "user_01";
        String courseId = "course_01";
        String lessonId = "L-101";

        User student = User.builder().id(1L).email("user_01@test.com").build();
        OnlineCourse course = OnlineCourse.builder().id(1L).slug("course_01").build();

        when(enrollmentRepository.findByStudentAndOnlineCourse(student, course))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> {
                    accessPolicy.requireLearningAccess(student, course);
                    // lessonId chỉ là ngữ cảnh truy cập, không ảnh hưởng tới gate kiểm tra enrollment
                    System.out.println("Accessed lesson: " + lessonId);
                })
                .isInstanceOf(EnrollmentAccessException.class)
                .extracting("code").isEqualTo(EnrollmentErrorCode.NOT_ENROLLED);

        verify(enrollmentRepository, times(1)).findByStudentAndOnlineCourse(student, course);
    }
}
