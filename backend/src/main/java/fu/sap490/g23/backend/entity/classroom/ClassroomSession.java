package fu.sap490.g23.backend.entity.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.*;

import fu.sap490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "classroom_sessions")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_offering_id", nullable = false)
    private ClassroomOffering classroomOffering;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ClassroomSessionStatus status = ClassroomSessionStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    private ClassroomDeliveryMode deliveryMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ClassroomRoom room;

    @Column(name = "lark_meeting_url", length = 700)
    private String larkMeetingUrl;

    @Column(name = "lark_calendar_id", length = 255)
    private String larkCalendarId;

    @Column(name = "lark_event_id", length = 255)
    private String larkEventId;

    @Column(name = "lark_meeting_id", length = 255)
    private String larkMeetingId;

    @Column(name = "lark_meeting_no", length = 30)
    private String larkMeetingNo;

    @Column(name = "lark_empty_since")
    private LocalDateTime larkEmptySince;

    @Column(name = "lark_sync_status", length = 30)
    private String larkSyncStatus;

    @Column(name = "lark_sync_error", length = 1000)
    private String larkSyncError;

    @Column(name = "lark_synced_at")
    private LocalDateTime larkSyncedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "lark_meeting_status", length = 30)
    @Builder.Default
    private LarkMeetingStatus larkMeetingStatus = LarkMeetingStatus.NOT_CREATED;

    @Column(name = "recording_url", length = 700)
    private String recordingUrl;

    @Column(name = "recording_visible")
    @Builder.Default
    private Boolean recordingVisible = false;

    @Column(name = "session_content", columnDefinition = "text")
    private String sessionContent;

    @Column(length = 500)
    private String note;

    @Column(name = "locked", nullable = false)
    @Builder.Default
    private boolean locked = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LocalDateTime getStartDateTime() {
        return LocalDateTime.of(sessionDate, startTime);
    }

    public LocalDateTime getEndDateTime() {
        return LocalDateTime.of(sessionDate, endTime);
    }
}
