package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.DomainRecord;
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
@SQLRestriction("record_type = 'classroom_quiz_questions'")
public class ClassroomQuizQuestion extends DomainRecord {
    @Override
    protected String domainRecordType() {
        return "classroom_quiz_questions";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ClassroomQuiz quiz;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(name = "options_json", columnDefinition = "text", nullable = false)
    private String optionsJson;

    @Column(name = "correct_answer", nullable = false, length = 500)
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "text")
    private String explanation;
}
