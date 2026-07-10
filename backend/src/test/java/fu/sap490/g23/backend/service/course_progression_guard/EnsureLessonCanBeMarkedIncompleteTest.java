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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnsureLessonCanBeMarkedIncompleteTest {

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
    private CourseModule module;
    private Lesson lesson1;
    private Lesson lesson2;

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

        module = new CourseModule();
        module.setId(10L);
        module.setDisplayOrder(1);
        module.setLessons(List.of(lesson1, lesson2));

        course = new OnlineCourse();
        course.setId(1L);
        course.setModules(List.of(module));
    }

    /**
     * Mục đích: Kiểm tra trường hợp cho phép học viên đánh dấu "chưa hoàn thành" bài học khi họ chưa học các bài tiếp theo.
     * Kỳ vọng: Thành công (không ném ngoại lệ).
     */
    @Test
    void ensureLessonCanBeMarkedIncomplete_Success_NoLaterLessonsCompleted() {
        // Arrange
        // Later lesson is lesson2 (id 102)
        when(lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                student, Set.of(102L), LessonProgressStatus.COMPLETED)
        ).thenReturn(List.of());

        when(courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course))
                .thenReturn(List.of());

        // Act & Assert
        guard.ensureLessonCanBeMarkedIncomplete(student, course, lesson1);
    }

    /**
     * Mục đích: Ngăn chặn học viên gian lận quay lại hủy hoàn thành bài 1 trong khi đã học bài 2.
     * Kỳ vọng: Ném ngoại lệ cảnh báo không thể bỏ hoàn thành bài này.
     */
    @Test
    void ensureLessonCanBeMarkedIncomplete_Failure_LaterLessonCompleted_ThrowsException() {
        // Arrange
        when(lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                student, Set.of(102L), LessonProgressStatus.COMPLETED)
        ).thenReturn(List.of(new LessonProgress()));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            guard.ensureLessonCanBeMarkedIncomplete(student, course, lesson1);
        });

        assertEquals("Không thể bỏ hoàn thành bài này vì bạn đã học xong các bài phía sau.", exception.getMessage());
    }

    /**
     * Mục đích: Ngăn chặn học viên hủy bài học khi đã nộp bài đánh giá liên quan (bài test cuối module).
     * Kỳ vọng: Ném ngoại lệ vì hệ thống ghi nhận điểm test đã phụ thuộc vào bài học này.
     */
    @Test
    void ensureLessonCanBeMarkedIncomplete_Failure_AssessmentSubmitted_ThrowsException() {
        // Arrange
        when(lessonProgressRepository.findByStudentAndLessonIdInAndStatus(
                student, Set.of(102L), LessonProgressStatus.COMPLETED)
        ).thenReturn(List.of());

        CourseAssessment assessment = new CourseAssessment();
        assessment.setId(500L);
        assessment.setModule(module);

        when(courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course))
                .thenReturn(List.of(assessment));

        when(assessmentSubmissionRepository.existsByAssessmentInAndStudent(List.of(assessment), student))
                .thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            guard.ensureLessonCanBeMarkedIncomplete(student, course, lesson1);
        });

        assertEquals("Không thể bỏ hoàn thành bài này vì đã có bài đánh giá liên quan được nộp.", exception.getMessage());
    }
}
