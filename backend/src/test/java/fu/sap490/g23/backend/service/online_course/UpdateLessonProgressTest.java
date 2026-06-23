package fu.sap490.g23.backend.service.online_course;

import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.*;
import fu.sap490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.LessonProgressRepository;
import fu.sap490.g23.backend.repository.course.LessonRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.service.course.CourseProgressService;
import fu.sap490.g23.backend.service.course.CourseProgressionGuard;
import fu.sap490.g23.backend.service.course.OnlineCourseMapper;
import fu.sap490.g23.backend.service.course.OnlineCourseServiceImpl;
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
class UpdateLessonProgressTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OnlineCourseRepository onlineCourseRepository;

    @Mock
    private PackageEnrollmentRepository enrollmentRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private CourseProgressionGuard courseProgressionGuard;

    @Mock
    private CourseProgressService courseProgressService;

    @Mock
    private OnlineCourseMapper mapper;

    @InjectMocks
    private OnlineCourseServiceImpl onlineCourseService;

    private User student;
    private OnlineCourse course;
    private LearningPackage learningPackage;
    private PackageEnrollment enrollment;
    private CourseModule module;
    private Lesson lesson;
    private LessonProgress progress;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setEmail("student@test.com");

        learningPackage = new LearningPackage();
        learningPackage.setId(1L);
        learningPackage.setDeleted(false);

        course = new OnlineCourse();
        course.setId(10L);
        course.setLearningPackage(learningPackage);

        module = new CourseModule();
        module.setId(100L);
        module.setOnlineCourse(course);
        course.setModules(List.of(module));

        lesson = new Lesson();
        lesson.setId(1000L);
        lesson.setModule(module);
        module.setLessons(List.of(lesson));

        enrollment = new PackageEnrollment();
        enrollment.setStudent(student);
        enrollment.setLearningPackage(learningPackage);

        progress = new LessonProgress();
        progress.setStudent(student);
        progress.setLesson(lesson);
    }

    /**
     * Mục đích: Kiểm tra trường hợp học viên hoàn thành bài học (completed = true).
     * Kỳ vọng: Gọi CourseProgressionGuard để check điều kiện, đánh dấu trạng thái COMPLETED (100%), lưu lại tiến độ và cập nhật tiến độ chung của toàn khóa học.
     */
    @Test
    void updateLessonProgress_CompleteLesson_Success_UpdatesAndRefreshesProgress() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(lessonProgressRepository.findByStudentAndLesson(student, lesson)).thenReturn(Optional.of(progress));
        when(courseProgressService.refreshEnrollmentProgress(enrollment, course, student)).thenReturn(enrollment);
        when(mapper.toEnrollmentResponse(enrollment)).thenReturn(new PackageEnrollmentResponse());

        // Act
        onlineCourseService.updateLessonProgress(course.getId(), lesson.getId(), true, student.getEmail());

        // Assert
        verify(courseProgressionGuard, times(1)).ensureLessonCanBeCompleted(student, course, lesson);
        verify(lessonProgressRepository, times(1)).save(progress);
        verify(courseProgressService, times(1)).refreshEnrollmentProgress(enrollment, course, student);
        
        assertEquals(LessonProgressStatus.COMPLETED, progress.getStatus());
        assertEquals(100, progress.getProgressPercent());
        assertNotNull(progress.getCompletedAt());
        assertNotNull(progress.getLastAccessedAt());
    }

    /**
     * Mục đích: Kiểm tra trường hợp học viên bỏ đánh dấu hoàn thành bài học (completed = false).
     * Kỳ vọng: Gọi CourseProgressionGuard để check xem có được phép bỏ hoàn thành không, đưa trạng thái về IN_PROGRESS (0%), xóa ngày completedAt.
     */
    @Test
    void updateLessonProgress_IncompleteLesson_Success_UpdatesAndRefreshesProgress() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(lessonProgressRepository.findByStudentAndLesson(student, lesson)).thenReturn(Optional.of(progress));
        when(courseProgressService.refreshEnrollmentProgress(enrollment, course, student)).thenReturn(enrollment);
        when(mapper.toEnrollmentResponse(enrollment)).thenReturn(new PackageEnrollmentResponse());

        // Act
        onlineCourseService.updateLessonProgress(course.getId(), lesson.getId(), false, student.getEmail());

        // Assert
        verify(courseProgressionGuard, times(1)).ensureLessonCanBeMarkedIncomplete(student, course, lesson);
        verify(lessonProgressRepository, times(1)).save(progress);
        verify(courseProgressService, times(1)).refreshEnrollmentProgress(enrollment, course, student);
        
        assertEquals(LessonProgressStatus.IN_PROGRESS, progress.getStatus());
        assertEquals(0, progress.getProgressPercent());
        assertNull(progress.getCompletedAt());
        assertNotNull(progress.getLastAccessedAt());
    }

    /**
     * Mục đích: Kiểm tra trường hợp học viên cập nhật bài học nhưng email không tồn tại trong hệ thống.
     * Kỳ vọng: Ném ngoại lệ "Student not found".
     */
    @Test
    void updateLessonProgress_Failure_StudentNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            onlineCourseService.updateLessonProgress(course.getId(), lesson.getId(), true, "notfound@test.com");
        });
        assertEquals("Student not found", exception.getMessage());
    }

    /**
     * Mục đích: Ngăn chặn học viên cập nhật tiến độ cho một khóa học mà họ chưa đăng ký.
     * Kỳ vọng: Ném ngoại lệ yêu cầu học viên phải được enrolled (đăng ký) trước.
     */
    @Test
    void updateLessonProgress_Failure_NotEnrolled_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            onlineCourseService.updateLessonProgress(course.getId(), lesson.getId(), true, student.getEmail());
        });
        assertEquals("You are not enrolled in this course", exception.getMessage());
    }

    /**
     * Mục đích: Bảo mật chặn hành vi cập nhật tiến độ bằng cách truyền ID bài học của một khóa học khác vào.
     * Kỳ vọng: Ném ngoại lệ do bài học không thuộc về khóa học tương ứng.
     */
    @Test
    void updateLessonProgress_Failure_LessonNotBelongToCourse_ThrowsException() {
        // Arrange
        OnlineCourse otherCourse = new OnlineCourse();
        otherCourse.setId(99L);
        CourseModule otherModule = new CourseModule();
        otherModule.setOnlineCourse(otherCourse);
        
        Lesson otherLesson = new Lesson();
        otherLesson.setId(999L);
        otherLesson.setModule(otherModule); // Thuộc về course 99L, trong khi req gọi course 10L

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.findById(otherLesson.getId())).thenReturn(Optional.of(otherLesson));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            onlineCourseService.updateLessonProgress(course.getId(), otherLesson.getId(), true, student.getEmail());
        });
        assertEquals("Lesson does not belong to this course", exception.getMessage());
    }
}
