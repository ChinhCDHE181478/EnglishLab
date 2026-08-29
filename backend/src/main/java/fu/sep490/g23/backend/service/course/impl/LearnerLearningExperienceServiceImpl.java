package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.request.course.LearnerLessonNoteRequest;
import fu.sep490.g23.backend.dto.response.course.LearnerLessonNoteResponse;
import fu.sep490.g23.backend.dto.response.course.LearnerLessonReviewFlagResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearnerLessonNote;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.LearnerLessonNoteRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineLessonRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.LearnerLearningExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LearnerLearningExperienceServiceImpl implements LearnerLearningExperienceService {
    private final LearnerLessonNoteRepository noteRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineLessonRepository lessonRepository;
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;



    private LessonProgress ensureLessonProgress(OnlineCourseEnrollment enrollment, OnlineLesson lesson, User user) {
        return lessonProgressRepository.findByEnrollmentAndLesson(enrollment, lesson)
                .or(() -> lessonProgressRepository.findByStudentAndLesson(user, lesson))
                .orElseGet(() -> LessonProgress.builder()
                        .student(user)
                        .lesson(lesson)
                        .enrollment(enrollment)
                        .courseVersion(enrollment.getCourseVersion())
                        .lessonKey(lesson.getLessonKey())
                        .status(LessonProgressStatus.NOT_STARTED)
                        .progressPercent(0)
                        .needsReview(false)
                        .build());
    }

    private LearningContext findLearningContext(Long courseId, Long lessonId, User user) {
        OnlineCourse course = onlineCourseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        OnlineLesson lesson = lessonRepository.findByIdAndModuleOnlineCourseVersionOnlineCourseId(lessonId, courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học trong khóa học này."));
        OnlineCourseEnrollment enrollment = courseEnrollmentAccessPolicy.requireLearningAccess(user, course);
        return new LearningContext(course, lesson, enrollment);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LearnerLessonNoteResponse toNoteResponse(LearnerLessonNote note) {
        return LearnerLessonNoteResponse.builder()
                .id(note.getId())
                .courseId(note.getCourse().getId())
                .lessonId(note.getLesson().getId())
                .lessonTitle(note.getLesson().getTitle())
                .courseTitle(note.getCourse().getTitle())
                .content(note.getContent())
                .selectedText(note.getSelectedText())
                .transcriptStartSeconds(note.getTranscriptStartSeconds())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }

    private LearnerLessonReviewFlagResponse toFlagResponse(LessonProgress progress) {
        OnlineCourse course = progress.getEnrollment() != null && progress.getEnrollment().getOnlineCourse() != null
                ? progress.getEnrollment().getOnlineCourse()
                : (progress.getLesson().getModule() == null || progress.getLesson().getModule().getOnlineCourseVersion() == null
                        ? null
                        : progress.getLesson().getModule().getOnlineCourseVersion().getOnlineCourse());
        String courseTitle = course == null || false
                ? null
                : course.getTitle();
        return LearnerLessonReviewFlagResponse.builder()
                .id(progress.getId())
                .courseId(course == null ? null : course.getId())
                .lessonId(progress.getLesson().getId())
                .lessonTitle(progress.getLesson().getTitle())
                .courseTitle(courseTitle)
                .createdAt(progress.getUpdatedAt() != null ? progress.getUpdatedAt() : progress.getCreatedAt())
                .build();
    }

    private record LearningContext(OnlineCourse course, OnlineLesson lesson, OnlineCourseEnrollment enrollment) {}
}
