package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomHomeworkResponse {
    private Long id;
    private Long classSectionId;
    private Long sessionId;
    private Long curriculumUnitId;
    private String curriculumUnitTitle;
    private String title;
    private String instruction;
    private LocalDateTime deadline;
    private BigDecimal maxScore;
    private boolean allowResubmission;
    private String attachmentUrl;
    private HomeworkActivityType activityType;
    private String activityConfigJson;
    private String objectiveAnswerKey;
    private boolean aiReviewEnabled;
    private HomeworkStatus status;
    private HomeworkGradingMode gradingMode;
    private AssessmentSkill skill;
    private Long rubricId;
    private String rubricName;
    private Long assessmentBankItemId;
    private String assessmentBankItemTitle;
    private String assessmentType;
    private AssessmentRubricResponse rubric;
    private boolean overdue;
    private ClassroomHomeworkSubmissionResponse mySubmission;
    private Integer submissionCount;
    private Integer gradedCount;
    private Integer pendingGradingCount;
}
