package fu.sep490.g23.backend.entity.classroom;
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
    @Column(name = "delivery_mode_override", length = 20)
    private ClassroomDeliveryMode deliveryModeOverride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "recording_url", length = 700)
    private String recordingUrl;

    @Column(name = "recording_visible")
    @Builder.Default
    private Boolean recordingVisible = false;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "recording_status",
            nullable = false,
            length = 30,
            columnDefinition = "VARCHAR(30) DEFAULT 'NOT_AVAILABLE'"
    )
    @Builder.Default
    private RecordingSyncStatus recordingStatus = RecordingSyncStatus.NOT_AVAILABLE;

    @Column(name = "recording_synced_at")
    private LocalDateTime recordingSyncedAt;

    @Column(name = "recording_last_attempt_at")
    private LocalDateTime recordingLastAttemptAt;

    @Column(name = "recording_sync_error", length = 1000)
    private String recordingSyncError;

    @Column(name = "recording_sync_attempts", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer recordingSyncAttempts = 0;

    @Column(name = "session_content", columnDefinition = "text")
    private String sessionContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_lesson_id")
    private CourseLesson courseLesson;

    @Column(length = 500)
    private String note;

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

    public User getEffectiveTeacher() {
        return teacher != null ? teacher : classSection.getPrimaryTeacher();
    }

    public Room getEffectiveRoom() {
        return room != null ? room : classSection.getRegularRoom();
    }

    public ClassroomDeliveryMode getEffectiveDeliveryMode() {
        return deliveryModeOverride != null ? deliveryModeOverride : classSection.getDeliveryMode();
    }

    public boolean isImmutable() {
        return status == ClassroomSessionStatus.COMPLETED || status == ClassroomSessionStatus.CANCELLED;
    }
}
