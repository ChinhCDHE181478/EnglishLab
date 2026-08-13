package fu.sep490.g23.backend.dto.response.course;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.math.BigDecimal;

@Data
@Builder
public class LearnerLearningPathResponse {
    private Double currentBand;
    private String examType;
    private BigDecimal currentScore;
    private String targetExam;
    private String targetScore;
    private List<PathOverview> paths;

    @Data
    @Builder
    public static class PathOverview {
        private Long id;
        private String code;
        private String name;
        private String examCategory;
        private BigDecimal targetBand;
        private Integer targetScore;
        private Integer totalCourses;
        private Integer completedCourses;
        private Integer waivedCourses;
        private Long currentStepCourseId;
        private Long nextCourseId;
        private Long recommendedStartCourseId;
        private Integer recommendedStartOrder;
        private String recommendationReason;
        private List<LearnerLearningPathCourseResponse> courses;
    }
}
