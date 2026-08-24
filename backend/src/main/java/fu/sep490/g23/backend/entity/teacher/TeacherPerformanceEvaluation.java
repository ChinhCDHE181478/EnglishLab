package fu.sep490.g23.backend.entity.teacher;

import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.teacher.enums.TeacherEvaluationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_auxiliary_records")
@SQLRestriction("record_type = 'teacher_performance_evaluations'")
public class TeacherPerformanceEvaluation extends DomainRecord {
    @Override
    protected String domainRecordType() {
        return "teacher_performance_evaluations";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User evaluator;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "lesson_delivery_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal lessonDeliveryScore;

    @Column(name = "evaluation_learner_support_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal learnerSupportScore;

    @Column(name = "grading_timeliness_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal gradingTimelinessScore;

    @Column(name = "evaluation_professionalism_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal professionalismScore;

    @Column(name = "overall_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal overallScore;

    @Column(length = 1500)
    private String strengths;

    @Column(name = "improvement_areas", length = 1500)
    private String improvementAreas;

    @Column(name = "action_plan", length = 1500)
    private String actionPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false, length = 20)
    @Builder.Default
    private TeacherEvaluationStatus status = TeacherEvaluationStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
