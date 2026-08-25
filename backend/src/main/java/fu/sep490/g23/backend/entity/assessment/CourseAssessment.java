package fu.sep490.g23.backend.entity.assessment;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;

import fu.sep490.g23.backend.entity.assessment.enums.*;

import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_assessments")
public class CourseAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_course_id", nullable = false)
    private OnlineCourse onlineCourse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private OnlineCourseModule module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id")
    private AssessmentRubric rubric;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_bank_item_id")
    private AssessmentBankItem assessmentBankItem;

    /**
     * Stable identity used to carry learner submissions across published course versions.
     * Different immutable assessment rows may share this key when one is a new version
     * of the same logical assessment.
     */
    @Column(name = "progress_key", length = 80)
    @Builder.Default
    private String progressKey = UUID.randomUUID().toString();

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 700)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssessmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssessmentSkill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_evaluation_mode", nullable = false, length = 40)
    @Builder.Default
    private AiEvaluationMode aiEvaluationMode = AiEvaluationMode.EXPLAIN_ONLY;

    @Column(name = "instructions", columnDefinition = "text")
    private String instructions;

    @Column(name = "objective_answer_key", columnDefinition = "text")
    private String objectiveAnswerKey;

    @Column(name = "ui_config_json", columnDefinition = "text")
    private String uiConfigJson;

    @Column(name = "passing_score", precision = 4, scale = 1)
    private BigDecimal passingScore;

    @Column(name = "max_score", precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal maxScore = BigDecimal.TEN;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
