package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;

import fu.sep490.g23.backend.entity.classroom.enums.*;

import fu.sep490.g23.backend.entity.User;
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
        name = "classroom_homework_submissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_homework_submission_homework_student",
                columnNames = {"homework_id", "student_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class ClassroomHomeworkSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", nullable = false)
    private ClassroomHomework homework;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "text_answer", columnDefinition = "text")
    private String textAnswer;

    @Column(name = "attachment_url", length = 700)
    private String attachmentUrl;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HomeworkSubmissionStatus status = HomeworkSubmissionStatus.DRAFT;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "teacher_feedback", columnDefinition = "text")
    private String teacherFeedback;

    @Column(name = "teacher_annotations_json", columnDefinition = "text")
    private String teacherAnnotationsJson;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by_id")
    private User gradedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
