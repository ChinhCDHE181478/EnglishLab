package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.request.course.LearnerLessonNoteRequest;
import fu.sep490.g23.backend.dto.response.course.LearnerLessonNoteResponse;
import fu.sep490.g23.backend.dto.response.course.LearnerLessonReviewFlagResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearnerLessonNote;
import fu.sep490.g23.backend.entity.course.LearnerLessonReviewFlag;
import fu.sep490.g23.backend.entity.course.Lesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.LearnerLessonNoteRepository;
import fu.sep490.g23.backend.repository.course.LearnerLessonReviewFlagRepository;
import fu.sep490.g23.backend.repository.course.LessonRepository;
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
    private final UserRepository userRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final LessonRepository lessonRepository;
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
        return reviewFlagRepository.findByUserOrderByCreatedAtDesc(user).stream().map(this::toFlagResponse).toList();
    }

    @Override
    public LearnerLessonReviewFlagResponse addReviewFlag(Long courseId, Long lessonId, String email) {
        User user = findUser(email);
        LearningContext context = findLearningContext(courseId, lessonId, user);
        LearnerLessonReviewFlag flag = reviewFlagRepository.findByUserAndLesson(user, context.lesson())
                .orElseGet(() -> reviewFlagRepository.save(LearnerLessonReviewFlag.builder()
                        .user(user)
                        .course(context.course())
                        .lesson(context.lesson())
                        .build()));
        return toFlagResponse(flag);
    }

    @Override
    public void removeReviewFlag(Long courseId, Long lessonId, String email) {
        User user = findUser(email);
        LearningContext context = findLearningContext(courseId, lessonId, user);
        reviewFlagRepository.findByUserAndLesson(user, context.lesson()).ifPresent(reviewFlagRepository::delete);
    }

    private LearningContext findLearningContext(Long courseId, Long lessonId, User user) {
        OnlineCourse course = onlineCourseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        Lesson lesson = lessonRepository.findByIdAndModuleOnlineCourseId(lessonId, courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học trong khóa học này."));
        courseEnrollmentAccessPolicy.requireLearningAccess(user, course);
        return new LearningContext(course, lesson);
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

    private LearnerLessonReviewFlagResponse toFlagResponse(LearnerLessonReviewFlag flag) {
        return LearnerLessonReviewFlagResponse.builder()
                .id(flag.getId())
                .courseId(flag.getCourse().getId())
                .lessonId(flag.getLesson().getId())
                .lessonTitle(flag.getLesson().getTitle())
                .courseTitle(flag.getCourse().getLearningPackage().getTitle())
                .createdAt(flag.getCreatedAt())
                .build();
    }

    private record LearningContext(OnlineCourse course, Lesson lesson) {}
}
