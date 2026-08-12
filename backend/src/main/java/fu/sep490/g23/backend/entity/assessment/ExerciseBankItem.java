package fu.sep490.g23.backend.entity.assessment;

import fu.sep490.g23.backend.entity.User;
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
@Table(name = "exercise_bank_items")
@EntityListeners(AuditingEntityListener.class)
public class ExerciseBankItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(nullable = false, length = 30)
    private String skill;

    @Column(length = 60)
    private String level;

    @Column(name = "exercise_type", nullable = false, length = 30)
    @Builder.Default
    private String exerciseType = "HOMEWORK";

    @Column(columnDefinition = "text", nullable = false)
    private String prompt;

    @Column(name = "answer_key", columnDefinition = "text")
    private String answerKey;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(length = 500)
    private String tags;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

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
