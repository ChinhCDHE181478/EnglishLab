package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.classroom.enums.*;

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
@SQLRestriction("record_type = 'lark_meeting_participants'")
@EntityListeners(AuditingEntityListener.class)
public class LarkMeetingParticipant extends DomainRecord {

    @Override
    protected String domainRecordType() {
        return "lark_meeting_participants";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_schedule_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ClassSchedule classSchedule;

    @Column(name = "participant_key", nullable = false, length = 255)
    private String participantKey;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
