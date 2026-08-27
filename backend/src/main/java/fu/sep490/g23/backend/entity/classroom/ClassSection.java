package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.GoogleMeetStatus;

import fu.sep490.g23.backend.entity.classroom.enums.*;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "class_sections")
@EntityListeners(AuditingEntityListener.class)
public class ClassSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    private ClassroomDeliveryMode deliveryMode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_led_course_id", nullable = false)
    private InstructorLedCourse instructorLedCourse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ClassroomOfferingStatus status = ClassroomOfferingStatus.DRAFT;

    @Column(name = "entry_level", length = 120)
    private String entryLevel;

    @Column(name = "target_outcome", length = 700)
    private String targetOutcome;

    @Column(nullable = false, length = 120)
    private String code;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "tuition_fee_vnd", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal tuitionFeeVnd = java.math.BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer capacity = 30;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_teacher_id")
    private User primaryTeacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "google_meet_owner_id")
    private User googleMeetOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "offline_address", length = 500)
    private String offlineAddress;

    @Column(name = "location_note", length = 500)
    private String locationNote;

    @Column(name = "google_meet_space_name", length = 255)
    private String googleMeetSpaceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "google_meet_status", nullable = false, length = 30)
    @Builder.Default
    private GoogleMeetStatus googleMeetStatus = GoogleMeetStatus.NOT_CREATED;

    @Column(name = "google_meet_url", length = 700)
    private String googleMeetUrl;

    @Column(name = "google_meet_sync_error", length = 1000)
    private String googleMeetSyncError;

    @Column(name = "syllabus_summary", columnDefinition = "text")
    private String syllabusSummary;

    @Column(name = "program_outcomes", columnDefinition = "text")
    private String programOutcomes;

    @Column(name = "teacher_guide", columnDefinition = "text")
    private String teacherGuide;

    @Column(name = "interaction_activities", columnDefinition = "text")
    private String interactionActivities;

    @OneToMany(mappedBy = "classSection", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClassSchedule> schedules = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addSchedule(ClassSchedule schedule) {
        schedules.add(schedule);
        schedule.setClassSection(this);
    }

    public String getTitle() {
        return name;
    }

    public String getSlug() {
        return instructorLedCourse == null ? code : instructorLedCourse.getSlug();
    }

    public BigDecimal getPrice() {
        return tuitionFeeVnd;
    }

    public BigDecimal getSalePrice() {
        return instructorLedCourse == null ? null : instructorLedCourse.getSaleTuitionFeeVnd();
    }

    public String getShortDescription() {
        return instructorLedCourse == null ? null : instructorLedCourse.getShortDescription();
    }

    public String getDescription() {
        return instructorLedCourse == null ? null : instructorLedCourse.getDescription();
    }

    public String getThumbnailUrl() {
        return instructorLedCourse == null ? null : instructorLedCourse.getThumbnailUrl();
    }

    public String getStudyMode() {
        return deliveryMode == null ? null : deliveryMode.name();
    }

    public boolean isDeleted() {
        return false;
    }
}
