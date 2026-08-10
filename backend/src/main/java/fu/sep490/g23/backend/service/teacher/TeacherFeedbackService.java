package fu.sap490.g23.backend.service.teacher;

import fu.sap490.g23.backend.dto.request.teacher.UpsertTeacherCourseFeedbackRequest;
import fu.sap490.g23.backend.dto.response.teacher.LearnerTeacherFeedbackResponse;
import fu.sap490.g23.backend.dto.response.teacher.ManagerTeacherFeedbackDetailResponse;
import fu.sap490.g23.backend.dto.response.teacher.TeacherFeedbackAggregateResponse;

import java.util.List;

public interface TeacherFeedbackService {
    List<LearnerTeacherFeedbackResponse> getLearnerForms(Long classroomId, String learnerEmail);

    LearnerTeacherFeedbackResponse saveLearnerFeedback(
            Long classroomId,
            Long teacherId,
            String learnerEmail,
            UpsertTeacherCourseFeedbackRequest request
    );

    List<TeacherFeedbackAggregateResponse> getManagerDashboard();

    ManagerTeacherFeedbackDetailResponse getManagerDetail(Long teacherId);

    TeacherFeedbackAggregateResponse getTeacherSummary(String teacherEmail);
}
