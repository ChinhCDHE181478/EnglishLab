package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;

import fu.sep490.g23.backend.entity.classroom.enums.*;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.CourseLesson;
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
@Table(name = "class_schedules")
@EntityListeners(AuditingEntityListener.class)
public class ClassSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    private ClassSection classSection;

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
    private Room room;

    @Column(name = "meeting_url", length = 700)
    private String meetingUrl;

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

    @Column(name = "lark_reserve_id", length = 255)
    private String larkReserveId;

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

    @Enumerated(EnumType.STRING)
    @Column(
            name = "recording_sync_status",
            nullable = false,
            length = 30,
            columnDefinition = "VARCHAR(30) DEFAULT 'NOT_AVAILABLE'"
    )
    @Builder.Default
    private RecordingSyncStatus recordingSyncStatus = RecordingSyncStatus.NOT_AVAILABLE;

    @Column(name = "recording_provider", length = 30)
    private String recordingProvider;

    @Column(name = "recording_duration_ms")
    private Long recordingDurationMs;

    @Column(name = "recording_synced_at")
    private LocalDateTime recordingSyncedAt;

    @Column(name = "recording_last_attempt_at")
    private LocalDateTime recordingLastAttemptAt;

    @Column(name = "recording_sync_error", length = 1000)
    private String recordingSyncError;

    @Column(name = "recording_sync_attempts", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer recordingSyncAttempts = 0;

    @Column(name = "recording_published_at")
    private LocalDateTime recordingPublishedAt;

    @Column(name = "recording_expires_at")
    private LocalDateTime recordingExpiresAt;

    @Column(name = "session_content", columnDefinition = "text")
    private String sessionContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_lesson_id")
    private CourseLesson courseLesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 30)
    @Builder.Default
    private ClassScheduleType scheduleType = ClassScheduleType.OTHER;

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
