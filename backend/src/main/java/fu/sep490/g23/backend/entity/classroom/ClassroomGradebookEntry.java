package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;

import fu.sep490.g23.backend.entity.classroom.enums.*;

import fu.sep490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
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
@Table(name = "classroom_operation_records")
@SQLRestriction("record_type = 'classroom_gradebook_entries'")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomGradebookEntry extends DomainRecord {
    @Override
    protected String domainRecordType() {
        return "classroom_gradebook_entries";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ClassSection classSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
    @Column(name = "gradebook_status", nullable = false, length = 20)
    @Builder.Default
    private GradebookEntryStatus status = GradebookEntryStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User updatedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
