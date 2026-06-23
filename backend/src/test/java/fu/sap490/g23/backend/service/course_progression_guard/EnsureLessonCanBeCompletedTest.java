package fu.sap490.g23.backend.service.course_progression_guard;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.CourseAssessment;
import fu.sap490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sap490.g23.backend.entity.course.CourseModule;
import fu.sap490.g23.backend.entity.course.Lesson;
import fu.sap490.g23.backend.entity.course.LessonProgress;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sap490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.course.LessonProgressRepository;
import fu.sap490.g23.backend.service.course.CourseProgressionGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnsureLessonCanBeCompletedTest {

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private CourseAssessmentRepository courseAssessmentRepository;

    @Mock
    private AssessmentSubmissionRepository assessmentSubmissionRepository;

    @InjectMocks
    private CourseProgressionGuard guard;

    private User student;
    private OnlineCourse course;
    private CourseModule module1;
    private CourseModule module2;
    private Lesson lesson1;
    private Lesson lesson2;
    private Lesson lesson3;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);

        lesson1 = new Lesson();
        lesson1.setId(101L);
        lesson1.setDisplayOrder(1);

        lesson2 = new Lesson();
        lesson2.setId(102L);
        lesson2.setDisplayOrder(2);

        lesson3 = new Lesson();
        lesson3.setId(103L);
        lesson3.setDisplayOrder(1);

        module1 = new CourseModule();
        module1.setId(10L);
        module1.setDisplayOrder(1);
        module1.setLessons(List.of(lesson1, lesson2));

        module2 = new CourseModule();
        module2.setId(20L);
        module2.setDisplayOrder(2);
        module2.setLessons(List.of(lesson3));

        course = new OnlineCourse();
        course.setId(1L);
        course.setModules(List.of(module1, module2));
    }

    /**
     * Mục đích: Kiểm tra trường hợp học viên hoàn thành bài học đầu tiên của khóa học.
     * Kỳ vọng: Thành công (không ném ngoại lệ) do không có bài học nào phía trước yêu cầu phải học trước.
     */
    @Test
    void ensureLessonCanBeCompleted_FirstLesson_Success() {
        // Arrange
        // No previous lesson, so it should just return void without throwing exception
        
        // Act & Assert
        guard.ensureLessonCanBeCompleted(student, course, lesson1);
    }

    /**
     * Mục đích: Kiểm tra trường hợp học viên hoàn thành bài học số 2, và bài số 1 đã hoàn thành.
     * Kỳ vọng: Cho phép tiếp tục (không ném ngoại lệ).
     */
    @Test
    void ensureLessonCanBeCompleted_SecondLesson_PreviousCompleted_Success() {
        // Arrange
        LessonProgress completedProgress = new LessonProgress();
        when(lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                student, Set.of(101L), LessonProgressStatus.COMPLETED)
        ).thenReturn(List.of(completedProgress));

        // Act & Assert
        guard.ensureLessonCanBeCompleted(student, course, lesson2);
    }

    /**
     * Mục đích: Kiểm tra logic ngăn chặn nhảy cóc bài học (học bài 2 khi chưa học xong bài 1).
     * Kỳ vọng: Ném ngoại lệ yêu cầu học viên phải hoàn thành bài trước đó.
     */
    @Test
    void ensureLessonCanBeCompleted_SecondLesson_PreviousNotCompleted_ThrowsException() {
        // Arrange
        when(lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                student, Set.of(101L), LessonProgressStatus.COMPLETED)
        ).thenReturn(List.of());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            guard.ensureLessonCanBeCompleted(student, course, lesson2);
        });

        assertEquals("Bạn cần hoàn thành bài học trước đó trước khi tiếp tục.", exception.getMessage());
    }

    /**
     * Mục đích: Kiểm tra trường hợp truyền vào bài học không thuộc khóa học.
     * Kỳ vọng: Ném ngoại lệ báo bài học không thuộc khóa học.
     */
    @Test
    void ensureLessonCanBeCompleted_LessonNotInCourse_ThrowsException() {
        // Arrange
        Lesson unknownLesson = new Lesson();
        unknownLesson.setId(999L);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            guard.ensureLessonCanBeCompleted(student, course, unknownLesson);
        });

        assertEquals("Bài học không thuộc khóa học hiện tại.", exception.getMessage());
    }

    /**
     * Mục đích: Kiểm tra logic vượt qua Module mới (ví dụ học bài đầu tiên của Module 2).
     * Kỳ vọng: Bắt buộc người học phải làm xong toàn bộ bài đánh giá cuối Module 1 mới được học bài đầu của Module 2.
     */
    @Test
    void ensureLessonCanBeCompleted_FirstLessonOfSecondModule_PreviousModuleNotPassed_ThrowsException() {
        // Arrange
        // Even if the previous lesson was not completed, the guard checks previous module completion first.
        // We will make it so that module 1 is not fully completed.
        when(lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                eq(student), any(), eq(LessonProgressStatus.COMPLETED))
        ).thenReturn(List.of()); // Not completed

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            guard.ensureLessonCanBeCompleted(student, course, lesson3);
        });

        assertEquals("Bạn cần hoàn thành và đạt yêu cầu ở bài đánh giá cuối mô-đun trước khi mở mô-đun tiếp theo.", exception.getMessage());
    }
}
