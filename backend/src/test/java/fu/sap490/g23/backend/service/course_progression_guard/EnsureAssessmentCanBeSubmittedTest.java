package fu.sap490.g23.backend.service.course_progression_guard;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.CourseAssessment;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnsureAssessmentCanBeSubmittedTest {

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

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);

        lesson1 = new Lesson();
        lesson1.setId(101L);

        module1 = new CourseModule();
        module1.setId(10L);
        module1.setDisplayOrder(1);
        module1.setLessons(List.of(lesson1));

        module2 = new CourseModule();
        module2.setId(20L);
        module2.setDisplayOrder(2);
        module2.setLessons(List.of());

        course = new OnlineCourse();
        course.setId(1L);
        course.setModules(List.of(module1, module2));
    }

    /**
     * Mục đích: Kiểm tra trường hợp học viên nộp bài đánh giá cuối Mô-đun khi đã học xong toàn bộ bài học trong Mô-đun.
     * Kỳ vọng: Thành công (không ném ngoại lệ).
     */
    @Test
    void ensureAssessmentCanBeSubmitted_ModuleAssessment_Success() {
        // Arrange
        CourseAssessment assessment = new CourseAssessment();
        assessment.setOnlineCourse(course);
        assessment.setModule(module1);

        // For module 1, no previous module to check.
        // It will just check if all lessons in module 1 are completed.
        when(lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                student, Set.of(101L), LessonProgressStatus.COMPLETED)
        ).thenReturn(List.of(new LessonProgress()));

        // Act & Assert
        guard.ensureAssessmentCanBeSubmitted(student, assessment);
    }

    /**
     * Mục đích: Kiểm tra việc hệ thống chặn nộp bài đánh giá Mô-đun nếu học viên chưa hoàn thành bài học trong đó.
     * Kỳ vọng: Ném ngoại lệ yêu cầu học viên hoàn thành hết bài học trước khi thi.
     */
    @Test
    void ensureAssessmentCanBeSubmitted_ModuleAssessment_LessonsNotCompleted_ThrowsException() {
        // Arrange
        CourseAssessment assessment = new CourseAssessment();
        assessment.setOnlineCourse(course);
        assessment.setModule(module1);

        when(lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                student, Set.of(101L), LessonProgressStatus.COMPLETED)
        ).thenReturn(List.of()); // Not completed

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            guard.ensureAssessmentCanBeSubmitted(student, assessment);
        });

        assertEquals("Bạn cần hoàn thành toàn bộ bài học trong mô-đun trước khi làm bài đánh giá cuối mô-đun.", exception.getMessage());
    }

    /**
     * Mục đích: Kiểm tra trường hợp nộp bài đánh giá Cuối Khóa (Course Assessment) khi đủ điều kiện.
     * Kỳ vọng: Thành công nếu tất cả bài học và tất cả Mô-đun test đã pass.
     */
    @Test
    void ensureAssessmentCanBeSubmitted_CourseAssessment_Success() {
        // Arrange
        CourseAssessment assessment = new CourseAssessment();
        assessment.setOnlineCourse(course);
        assessment.setModule(null); // Course assessment

        // Needs to ensure all lessons are completed
        when(lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                student, Set.of(101L), LessonProgressStatus.COMPLETED)
        ).thenReturn(List.of(new LessonProgress()));

        // Needs to ensure all module checks passed
        when(courseAssessmentRepository.findByModuleAndActiveTrueOrderByDisplayOrderAscIdAsc(any()))
                .thenReturn(List.of()); // No assessments in modules

        // Act & Assert
        guard.ensureAssessmentCanBeSubmitted(student, assessment);
    }

    /**
     * Mục đích: Kiểm tra việc chặn nộp bài đánh giá Cuối Khóa nếu học viên chưa hoàn thành hết tất cả bài học.
     * Kỳ vọng: Ném ngoại lệ chặn.
     */
    @Test
    void ensureAssessmentCanBeSubmitted_CourseAssessment_NotAllLessonsCompleted_ThrowsException() {
        // Arrange
        CourseAssessment assessment = new CourseAssessment();
        assessment.setOnlineCourse(course);
        assessment.setModule(null);

        // Lesson 1 not completed
        when(lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                student, Set.of(101L), LessonProgressStatus.COMPLETED)
        ).thenReturn(List.of());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            guard.ensureAssessmentCanBeSubmitted(student, assessment);
        });

        assertEquals("Bạn cần hoàn thành toàn bộ bài học trước khi làm bài đánh giá cuối khóa.", exception.getMessage());
    }

    /**
     * Mục đích: Đảm bảo luồng kiểm tra logic Module phụ thuộc. Phải qua Module 1 mới được nộp test Module 2.
     * Kỳ vọng: Ném ngoại lệ bắt buộc hoàn thành Module trước đó.
     */
    @Test
    void ensureAssessmentCanBeSubmitted_SecondModuleAssessment_PreviousModuleNotCompleted_ThrowsException() {
        // Arrange
        CourseAssessment assessment = new CourseAssessment();
        assessment.setOnlineCourse(course);
        assessment.setModule(module2); // Second module

        // Module 1 lessons not completed
        when(lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                student, Set.of(101L), LessonProgressStatus.COMPLETED)
        ).thenReturn(List.of());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            guard.ensureAssessmentCanBeSubmitted(student, assessment);
        });

        assertEquals("Bạn cần hoàn thành và đạt yêu cầu ở bài đánh giá cuối mô-đun trước đó trước khi làm bài đánh giá này.", exception.getMessage());
    }
}
