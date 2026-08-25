package fu.sep490.g23.backend.entity.course;

import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "assessment_component_records")
@SQLRestriction("record_type = 'course_lesson_flashcard_refs'")
public class CourseLessonFlashcardRef extends DomainRecord {
    @Override
    protected String domainRecordType() {
        return "course_lesson_flashcard_refs";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private OnlineLesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_set_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private FlashcardSet flashcardSet;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
