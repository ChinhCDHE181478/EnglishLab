package fu.sap490.g23.backend.entity.course;

import fu.sap490.g23.backend.entity.curriculum.FlashcardSet;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "course_lesson_flashcard_refs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"lesson_id", "flashcard_set_id"})
)
public class CourseLessonFlashcardRef {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_set_id", nullable = false)
    private FlashcardSet flashcardSet;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
