package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestSource;
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

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "course_enrollment_requests")
@EntityListeners(AuditingEntityListener.class)
public class EnrollmentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    private User learner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id")
    private TrainingProgram courseOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_classroom_id")
    private ClassroomOffering requestedClassroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_attempt_id")
    private PlacementTestAttempt placementAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_classroom_id")
    private ClassroomOffering assignedClassroom;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "desired_class_code", length = 120)
    private String desiredClassCode;

    @Column(name = "consultation_track", length = 80)
    private String consultationTrack;

    @Column(name = "study_work_goal", length = 500)
    private String studyWorkGoal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private EnrollmentRequestStatus status = EnrollmentRequestStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_source", nullable = false, length = 20)
    @Builder.Default
    private EnrollmentRequestSource requestSource = EnrollmentRequestSource.ONLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmed_level", length = 30)
    private PlacementLevel confirmedLevel;

    @Column(name = "preferred_schedule", length = 500)
    private String preferredSchedule;

    @Column(name = "campus_preference", length = 255)
    private String campusPreference;

    @Column(name = "learner_note", length = 700)
    private String learnerNote;

    @Column(name = "staff_note", length = 700)
    private String staffNote;

    @Column(name = "rejection_reason", length = 700)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "invitation_sent_at")
    private LocalDateTime invitationSentAt;

    @Column(name = "test_appointment_at")
    private LocalDateTime testAppointmentAt;

    @Column(name = "test_location", length = 300)
    private String testLocation;

    @Column(name = "test_completed_at")
    private LocalDateTime testCompletedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
