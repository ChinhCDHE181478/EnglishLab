package fu.sep490.g23.backend.entity.teacher;

import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.teacher.enums.TeacherFeedbackPace;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_auxiliary_records")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("record_type = 'teacher_course_feedback'")
public class TeacherCourseFeedback extends DomainRecord {
    @Override
    protected String domainRecordType() {
        return "teacher_course_feedback";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ClassEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_section_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ClassSection classSection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User teacher;

    @Column(name = "clarity_score", nullable = false)
    private int clarityScore;

    @Column(name = "engagement_score", nullable = false)
    private int engagementScore;

    @Column(name = "feedback_learner_support_score", nullable = false)
    private int learnerSupportScore;

    @Column(name = "feedback_timeliness_score", nullable = false)
    private int feedbackTimelinessScore;

    @Column(name = "feedback_professionalism_score", nullable = false)
    private int professionalismScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "pace", nullable = false, length = 20)
    private TeacherFeedbackPace pace;

    @Column(name = "would_recommend", nullable = false)
    private boolean wouldRecommend;

    @Column(nullable = false, length = 1500)
    private String strengths;

    @Column(name = "improvement_suggestions", nullable = false, length = 1500)
    private String improvementSuggestions;

    @Column(name = "additional_comment", length = 1500)
    private String additionalComment;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
