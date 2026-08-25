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
import fu.sep490.g23.backend.repository.course.LearnerLessonReviewFlagRepository;
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
    private final LearnerLessonReviewFlagRepository reviewFlagRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineLessonRepository lessonRepository;
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;

    @Override
    @Transactional(readOnly = true)
    public List<LearnerLessonNoteResponse> getNotes(String email) {
        User user = findUser(email);
        return noteRepository.findByUserOrderByUpdatedAtDesc(user).stream().map(this::toNoteResponse).toList();
    }

    @Override
    public LearnerLessonNoteResponse createNote(Long courseId, Long lessonId, LearnerLessonNoteRequest request, String email) {
        User user = findUser(email);
        LearningContext context = findLearningContext(courseId, lessonId, user);
        LearnerLessonNote note = LearnerLessonNote.builder()
                .user(user)
                .course(context.course())
                .lesson(context.lesson())
                .content(request.getContent().trim())
                .selectedText(cleanNullable(request.getSelectedText()))
                .transcriptStartSeconds(request.getTranscriptStartSeconds())
                .build();
        return toNoteResponse(noteRepository.save(note));
    }

    @Override
    public LearnerLessonNoteResponse updateNote(Long noteId, LearnerLessonNoteRequest request, String email) {
        User user = findUser(email);
        LearnerLessonNote note = noteRepository.findByIdAndUser(noteId, user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ghi chú của bạn."));
        courseEnrollmentAccessPolicy.requireLearningAccess(user, note.getCourse());
        note.setContent(request.getContent().trim());
        note.setSelectedText(cleanNullable(request.getSelectedText()));
        note.setTranscriptStartSeconds(request.getTranscriptStartSeconds());
        return toNoteResponse(note);
    }

    @Override
    public void deleteNote(Long noteId, String email) {
        User user = findUser(email);
        LearnerLessonNote note = noteRepository.findByIdAndUser(noteId, user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ghi chú của bạn."));
        noteRepository.delete(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearnerLessonReviewFlagResponse> getReviewFlags(String email) {
        User user = findUser(email);
        return lessonProgressRepository.findByStudentAndNeedsReviewTrueOrderByUpdatedAtDesc(user).stream()
                .map(this::toFlagResponse)
                .toList();
    }

    @Override
    public LearnerLessonReviewFlagResponse addReviewFlag(Long courseId, Long lessonId, String email) {
        User user = findUser(email);
        LearningContext context = findLearningContext(courseId, lessonId, user);
        LessonProgress progress = ensureLessonProgress(context.enrollment(), context.lesson(), user);
        progress.setNeedsReview(true);
        return toFlagResponse(lessonProgressRepository.save(progress));
    }

    @Override
    public void removeReviewFlag(Long courseId, Long lessonId, String email) {
        User user = findUser(email);
        LearningContext context = findLearningContext(courseId, lessonId, user);
        lessonProgressRepository.findByEnrollmentAndLesson(context.enrollment(), context.lesson())
                .ifPresent(progress -> {
                    progress.setNeedsReview(false);
                    lessonProgressRepository.save(progress);
                });
        reviewFlagRepository.findByUserAndLesson(user, context.lesson()).ifPresent(reviewFlagRepository::delete);
    }

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
        OnlineLesson lesson = lessonRepository.findByIdAndModuleOnlineCourseId(lessonId, courseId)
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
                .courseTitle(note.getCourse().getLearningPackage().getTitle())
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
                : (progress.getLesson().getModule() == null ? null : progress.getLesson().getModule().getOnlineCourse());
        String courseTitle = course == null || course.getLearningPackage() == null
                ? null
                : course.getLearningPackage().getTitle();
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
