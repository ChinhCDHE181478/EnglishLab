package fu.sap490.g23.backend.dto.response.course;

import fu.sap490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OnlineCoursePreviewResponse {
    private OnlineCourseResponse course;

    @Builder.Default
    private List<ModuleResponse> modules = List.of();

    @Builder.Default
    private List<CourseAssessmentResponse> assessments = List.of();

    @Builder.Default
    private List<OnlineCoursePreviewWarningResponse> validationWarnings = List.of();

    @Builder.Default
    private boolean previewMode = true;
}
