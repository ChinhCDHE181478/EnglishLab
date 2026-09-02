package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.request.course.LearnerLessonNoteRequest;
import fu.sep490.g23.backend.dto.response.course.LearnerLessonNoteResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearnerLessonNote;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.LearnerLessonNoteRepository;
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
        courseEnrollmentAccessPolicy.requireLearningAccess(user, courseOf(note));
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

    private LearningContext findLearningContext(Long courseId, Long lessonId, User user) {
        OnlineCourse course = onlineCourseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        OnlineLesson lesson = lessonRepository.findByIdAndModuleOnlineCourseVersionOnlineCourseId(lessonId, courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học trong khóa học này."));
        courseEnrollmentAccessPolicy.requireLearningAccess(user, course);
        return new LearningContext(lesson);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LearnerLessonNoteResponse toNoteResponse(LearnerLessonNote note) {
        OnlineCourse course = courseOf(note);
        return LearnerLessonNoteResponse.builder()
                .id(note.getId())
                .courseId(course.getId())
                .lessonId(note.getLesson().getId())
                .lessonTitle(note.getLesson().getTitle())
                .courseTitle(course.getTitle())
                .content(note.getContent())
                .selectedText(note.getSelectedText())
                .transcriptStartSeconds(note.getTranscriptStartSeconds())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }

    private OnlineCourse courseOf(LearnerLessonNote note) {
        return note.getLesson().getModule().getOnlineCourseVersion().getOnlineCourse();
    }

    private record LearningContext(OnlineLesson lesson) {}
}
