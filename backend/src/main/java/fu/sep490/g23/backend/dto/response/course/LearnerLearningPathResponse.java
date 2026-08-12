package fu.sep490.g23.backend.dto.response.course;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LearnerLearningPathResponse {
    private Double currentBand;
    private String targetExam;
    private String targetScore;
    private List<PathOverview> paths;

    @Data
    @Builder
    public static class PathOverview {
        private String code;
        private String name;
        private Integer totalCourses;
        private Integer completedCourses;
        private Long currentStepCourseId;
        private Long nextCourseId;
        private List<LearnerLearningPathCourseResponse> courses;
    }
}
