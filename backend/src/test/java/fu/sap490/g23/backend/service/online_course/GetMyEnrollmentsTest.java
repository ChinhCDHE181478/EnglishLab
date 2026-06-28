package fu.sap490.g23.backend.service.online_course;

import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.service.course.CourseProgressService;
import fu.sap490.g23.backend.service.course.OnlineCourseMapper;
import fu.sap490.g23.backend.service.course.impl.OnlineCourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetMyEnrollmentsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PackageEnrollmentRepository enrollmentRepository;

    @Mock
    private OnlineCourseRepository onlineCourseRepository;

    @Mock
    private CourseProgressService courseProgressService;

    @Mock
    private OnlineCourseMapper mapper;

    @InjectMocks
    private OnlineCourseServiceImpl onlineCourseService;

    private User student;
    private LearningPackage activePackage;
    private LearningPackage deletedPackage;
    private OnlineCourse activeCourse;
    private PackageEnrollment activeEnrollment;
    private PackageEnrollment deletedEnrollment;
    private PackageEnrollmentResponse enrollmentResponse;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setEmail("student@test.com");

        activePackage = new LearningPackage();
        activePackage.setDeleted(false);

        deletedPackage = new LearningPackage();
        deletedPackage.setDeleted(true);

        activeCourse = new OnlineCourse();
        activeCourse.setLearningPackage(activePackage);

        activeEnrollment = new PackageEnrollment();
        activeEnrollment.setLearningPackage(activePackage);

        deletedEnrollment = new PackageEnrollment();
        deletedEnrollment.setLearningPackage(deletedPackage);

        enrollmentResponse = new PackageEnrollmentResponse();
    }

    /**
     * Mục đích: Kiểm tra trường hợp lấy danh sách khóa học thành công.
     * Kỳ vọng: Trả về danh sách khóa học mà học viên đã đăng ký, cập nhật lại tiến độ học tập thông qua courseProgressService.
     */
    @Test
    void getMyEnrollments_Success_ReturnsEnrollments() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentOrderByRegisteredAtDesc(student)).thenReturn(List.of(activeEnrollment));
        when(onlineCourseRepository.findByLearningPackage(activePackage)).thenReturn(Optional.of(activeCourse));
        when(mapper.toEnrollmentResponse(activeEnrollment)).thenReturn(enrollmentResponse);

        // Act
        List<PackageEnrollmentResponse> result = onlineCourseService.getMyEnrollments(student.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(enrollmentResponse, result.get(0));
    }

    /**
     * Mục đích: Đảm bảo không trả về các khóa học mà đã bị xóa (deleted = true) khỏi hệ thống.
     * Kỳ vọng: Danh sách trả về không chứa khóa học bị xóa.
     */
    @Test
    void getMyEnrollments_Success_IgnoresDeletedPackages() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentOrderByRegisteredAtDesc(student)).thenReturn(List.of(activeEnrollment, deletedEnrollment));
        when(onlineCourseRepository.findByLearningPackage(activePackage)).thenReturn(Optional.of(activeCourse));
        when(mapper.toEnrollmentResponse(activeEnrollment)).thenReturn(enrollmentResponse);

        // Act
        List<PackageEnrollmentResponse> result = onlineCourseService.getMyEnrollments(student.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(enrollmentResponse, result.get(0));
        verify(onlineCourseRepository, never()).findByLearningPackage(deletedPackage);
    }

    /**
     * Mục đích: Kiểm tra trường hợp gọi hàm lấy khóa học nhưng email không tồn tại.
     * Kỳ vọng: Ném ra ngoại lệ "Student not found".
     */
    @Test
    void getMyEnrollments_Failure_StudentNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            onlineCourseService.getMyEnrollments("notfound@test.com");
        });
        assertEquals("Student not found", exception.getMessage());
        verify(enrollmentRepository, never()).findByStudentOrderByRegisteredAtDesc(any());
    }
}
