package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
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
        name = "classroom_practice_attempts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"classroom_offering_id", "student_id", "exercise_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class ClassroomPracticeAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_offering_id", nullable = false)
    private ClassroomOffering classroomOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseBankItem exercise;

    @Column(name = "response_text", columnDefinition = "text")
    private String responseText;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
