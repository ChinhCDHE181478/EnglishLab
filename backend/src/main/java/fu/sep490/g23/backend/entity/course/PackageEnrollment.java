package fu.sep490.g23.backend.entity.course;

import fu.sep490.g23.backend.entity.course.enums.*;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "package_enrollments",
        uniqueConstraints = @UniqueConstraint(name = "uk_package_enrollment_user_package", columnNames = {"student_id", "package_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class PackageEnrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private LearningPackage learningPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_version_id")
    private OnlineCourseVersion courseVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "registered_at", nullable = false)
    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
