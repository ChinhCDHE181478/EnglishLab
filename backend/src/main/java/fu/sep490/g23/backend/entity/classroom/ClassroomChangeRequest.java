package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;

import fu.sep490.g23.backend.entity.classroom.enums.*;

import fu.sep490.g23.backend.entity.enums.RoleEnum;
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
@Table(name = "classroom_change_requests")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomChangeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 40)
    private ClassroomChangeRequestType requestType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "requester_role", nullable = false, length = 30)
    private RoleEnum requesterRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_offering_id", nullable = false)
    private ClassroomOffering classroomOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_session_id")
    private ClassroomSession targetSession;

    @Column(name = "old_values_json", columnDefinition = "text")
    private String oldValuesJson;

    @Column(name = "new_values_json", columnDefinition = "text")
    private String newValuesJson;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ClassroomChangeRequestStatus status = ClassroomChangeRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
