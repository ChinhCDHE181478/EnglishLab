package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.LearnerLessonNoteRequest;
import fu.sep490.g23.backend.dto.response.course.LearnerLessonNoteResponse;

import java.util.List;

public interface LearnerLearningExperienceService {
    List<LearnerLessonNoteResponse> getNotes(String email);
    LearnerLessonNoteResponse createNote(Long courseId, Long lessonId, LearnerLessonNoteRequest request, String email);
    LearnerLessonNoteResponse updateNote(Long noteId, LearnerLessonNoteRequest request, String email);
    void deleteNote(Long noteId, String email);
}
