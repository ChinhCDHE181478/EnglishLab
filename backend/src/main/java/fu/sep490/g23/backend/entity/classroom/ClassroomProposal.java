package fu.sap490.g23.backend.entity.classroom;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "classroom_proposals")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposal_code", nullable = false, unique = true, length = 40)
    private String proposalCode;

    @Column(nullable = false, length = 180)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private TrainingProgram courseOffering;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 20)
    private ClassroomDeliveryMode deliveryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement_level", length = 30)
    private PlacementLevel placementLevel;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "planned_start_date", nullable = false)
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date", nullable = false)
    private LocalDate plannedEndDate;

    @Column(name = "schedule_weekdays", nullable = false, length = 100)
    private String scheduleWeekdays;

    @Column(name = "session_start_time", nullable = false)
    private LocalTime sessionStartTime;

    @Column(name = "session_end_time", nullable = false)
    private LocalTime sessionEndTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_teacher_id")
    private User primaryTeacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ClassroomRoom room;

    @Column(name = "offline_address", length = 500)
    private String offlineAddress;

    @Column(name = "virtual_meeting_url", length = 700)
    private String virtualMeetingUrl;

    @Column(name = "staff_note", length = 700)
    private String staffNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    @Builder.Default
    private ClassroomApprovalStatus approvalStatus = ClassroomApprovalStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id")
    private User submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 700)
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_classroom_id")
    private ClassroomOffering approvedClassroom;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClassroomProposalMember> members = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public void addMember(ClassroomProposalMember member) {
        members.add(member);
        member.setProposal(this);
    }
}
