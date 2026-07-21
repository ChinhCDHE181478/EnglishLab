package fu.sap490.g23.backend.entity.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.enums.CourseVersionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "online_course_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_online_course_version_number",
                columnNames = {"online_course_id", "version_number"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class OnlineCourseVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "online_course_id", nullable = false)
    private OnlineCourse onlineCourse;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseVersionStatus status;

    @Column(name = "content_snapshot_json", columnDefinition = "text", nullable = false)
    @Builder.Default
    private String contentSnapshotJson = "{}";

    @Column(name = "assessment_ids_json", columnDefinition = "text")
    @Builder.Default
    private String assessmentIdsJson = "[]";

    @Column(name = "total_required_lessons", nullable = false)
    @Builder.Default
    private Integer totalRequiredLessons = 0;

    @Column(name = "total_required_assessments", nullable = false)
    @Builder.Default
    private Integer totalRequiredAssessments = 0;

    @Column(name = "change_note", length = 700)
    private String changeNote;

    @Column(name = "review_note", length = 700)
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
