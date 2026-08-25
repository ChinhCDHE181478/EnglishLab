package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;

import fu.sep490.g23.backend.entity.classroom.enums.*;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.entity.curriculum.CurriculumUnit;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "classroom_homework")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomHomework {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_offering_id", nullable = false)
    private ClassroomOffering classroomOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ClassroomSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_unit_id")
    private CurriculumUnit curriculumUnit;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(columnDefinition = "text")
    private String instruction;

    @Column
    private LocalDateTime deadline;

    @Column(name = "max_score", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal maxScore = BigDecimal.valueOf(10);

    @Column(name = "allow_resubmission", nullable = false)
    @Builder.Default
    private boolean allowResubmission = false;

    @Column(name = "attachment_url", length = 700)
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    @Builder.Default
    private HomeworkActivityType activityType = HomeworkActivityType.TEXT_RESPONSE;

    @Column(name = "activity_config_json", columnDefinition = "text")
    private String activityConfigJson;

    @Column(name = "ai_review_enabled", nullable = false)
    @Builder.Default
    private boolean aiReviewEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HomeworkStatus status = HomeworkStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_mode", nullable = false, length = 20)
    @Builder.Default
    private HomeworkGradingMode gradingMode = HomeworkGradingMode.TEACHER;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill", length = 30)
    private AssessmentSkill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_content_bank_item_id")
    private AssessmentRubric rubric;

    @Column(name = "rubric_id")
    private Long legacyRubricId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_content_bank_item_id")
    private AssessmentBankItem assessmentBankItem;

    @Column(name = "assessment_bank_item_id")
    private Long legacyAssessmentBankItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_content_bank_item_id", insertable = false, updatable = false)
    private ContentBankItem assessmentContentBankItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_content_bank_item_id", insertable = false, updatable = false)
    private ContentBankItem rubricContentBankItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
