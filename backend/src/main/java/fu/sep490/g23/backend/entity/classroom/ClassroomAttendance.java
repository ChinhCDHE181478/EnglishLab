package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomAttendanceStatus;

import fu.sep490.g23.backend.entity.classroom.enums.*;

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
@Table(
        name = "classroom_attendance_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_session_student",
                columnNames = {"session_id", "student_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class ClassroomAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSchedule session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ClassroomAttendanceStatus status = ClassroomAttendanceStatus.ABSENT;

    @Column(length = 500)
    private String note;


    @Column(name = "teacher_confirmed", nullable = false)
    @Builder.Default
    private boolean teacherConfirmed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by_id")
    private User markedBy;

    @Column(name = "dispute_reason", columnDefinition = "text")
    private String disputeReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_status", length = 20)
    private AttendanceDisputeStatus disputeStatus;

    @Column(name = "dispute_review_note", columnDefinition = "text")
    private String disputeReviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_reviewed_by_id")
    private User disputeReviewedBy;

    @Column(name = "dispute_reviewed_at")
    private LocalDateTime disputeReviewedAt;


    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
