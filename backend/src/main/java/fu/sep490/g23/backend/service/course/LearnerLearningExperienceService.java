package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.LearnerLessonNoteRequest;
import fu.sap490.g23.backend.dto.response.course.LearnerLessonNoteResponse;
import fu.sap490.g23.backend.dto.response.course.LearnerLessonReviewFlagResponse;

import java.util.List;

public interface LearnerLearningExperienceService {
    List<LearnerLessonNoteResponse> getNotes(String email);
    LearnerLessonNoteResponse createNote(Long courseId, Long lessonId, LearnerLessonNoteRequest request, String email);
    LearnerLessonNoteResponse updateNote(Long noteId, LearnerLessonNoteRequest request, String email);
    void deleteNote(Long noteId, String email);
    List<LearnerLessonReviewFlagResponse> getReviewFlags(String email);
    LearnerLessonReviewFlagResponse addReviewFlag(Long courseId, Long lessonId, String email);
    void removeReviewFlag(Long courseId, Long lessonId, String email);
}
