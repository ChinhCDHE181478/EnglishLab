package fu.sap490.g23.backend.entity.teacher;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.teacher.enums.TeacherFeedbackPace;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "teacher_course_feedback",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_teacher_feedback_enrollment_teacher",
                columnNames = {"enrollment_id", "teacher_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCourseFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private ClassroomEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_offering_id", nullable = false)
    private ClassroomOffering classroomOffering;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "clarity_score", nullable = false)
    private int clarityScore;

    @Column(name = "engagement_score", nullable = false)
    private int engagementScore;

    @Column(name = "learner_support_score", nullable = false)
    private int learnerSupportScore;

    @Column(name = "feedback_timeliness_score", nullable = false)
    private int feedbackTimelinessScore;

    @Column(name = "professionalism_score", nullable = false)
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
