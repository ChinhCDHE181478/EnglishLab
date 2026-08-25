package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.DomainRecord;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.enums.AttendanceDisputeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
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
@Table(name = "classroom_operation_records")
@SQLRestriction("record_type = 'classroom_attendance_disputes'")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomAttendanceDispute extends DomainRecord {

    @Override
    protected String domainRecordType() {
        return "classroom_attendance_disputes";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private ClassroomAttendance attendance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "dispute_reason", nullable = false, columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_status", nullable = false, length = 20)
    @Builder.Default
    private AttendanceDisputeStatus status = AttendanceDisputeStatus.PENDING;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
