package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomQuizStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "classroom_quizzes")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomQuiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    private ClassSection classSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ClassSchedule session;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "passing_score")
    private Integer passingScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ClassroomQuizStatus status = ClassroomQuizStatus.DRAFT;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "questions_jsonb", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<ClassroomQuizQuestion> questions = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
