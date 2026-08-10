package fu.sap490.g23.backend.entity.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.enums.VocabularyMasteryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "saved_vocabularies",
        uniqueConstraints = @UniqueConstraint(name = "uk_saved_vocabulary_user_word", columnNames = {"user_id", "word"})
)
public class SavedVocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String word;

    @Column(length = 180)
    private String phonetic;

    @Column(name = "primary_definition", nullable = false, length = 1200)
    private String primaryDefinition;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VocabularyMasteryStatus status = VocabularyMasteryStatus.LEARNING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
