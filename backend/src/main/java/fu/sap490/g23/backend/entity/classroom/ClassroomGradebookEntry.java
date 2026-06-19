package fu.sap490.g23.backend.entity.classroom;

import fu.sap490.g23.backend.entity.User;
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
@Table(
        name = "classroom_gradebook_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_gradebook_offering_student",
                columnNames = {"classroom_offering_id", "student_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class ClassroomGradebookEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_offering_id", nullable = false)
    private ClassroomOffering classroomOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "homework_score", precision = 6, scale = 2)
    private BigDecimal homeworkScore;

    @Column(name = "quiz_score", precision = 6, scale = 2)
    private BigDecimal quizScore;

    @Column(name = "attendance_percent", precision = 5, scale = 2)
    private BigDecimal attendancePercent;

    @Column(name = "participation_score", precision = 6, scale = 2)
    private BigDecimal participationScore;

    @Column(name = "final_result", precision = 6, scale = 2)
    private BigDecimal finalResult;

    @Column(name = "teacher_comment", columnDefinition = "text")
    private String teacherComment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GradebookEntryStatus status = GradebookEntryStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
