package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;

import fu.sep490.g23.backend.entity.classroom.enums.*;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "classroom_offerings")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "package_id", nullable = false, unique = true)
    private LearningPackage learningPackage;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    private ClassroomDeliveryMode deliveryMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_program_id")
    private TrainingProgram trainingProgram;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_program_id")
    private CurriculumProgram curriculumProgram;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_led_course_id")
    private InstructorLedCourse instructorLedCourse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ClassroomOfferingStatus status = ClassroomOfferingStatus.DRAFT;

    @Column(name = "entry_level", length = 120)
    private String entryLevel;

    @Column(name = "target_outcome", length = 700)
    private String targetOutcome;

    @Column(name = "max_capacity", nullable = false)
    @Builder.Default
    private Integer maxCapacity = 30;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_teacher_id")
    private User primaryTeacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "virtual_meeting_owner_id")
    private User virtualMeetingOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_room_id")
    private ClassroomRoom defaultRoom;

    @Column(name = "offline_address", length = 500)
    private String offlineAddress;

    @Column(name = "location_note", length = 500)
    private String locationNote;

    @Column(name = "default_lark_meeting_url", length = 700)
    private String defaultLarkMeetingUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "lark_meeting_status", length = 30)
    @Builder.Default
    private LarkMeetingStatus larkMeetingStatus = LarkMeetingStatus.NOT_CREATED;

    @Column(name = "recording_url", length = 700)
    private String recordingUrl;

    @Column(name = "recording_visible", nullable = false)
    @Builder.Default
    private boolean recordingVisible = false;

    @Column(name = "syllabus_summary", columnDefinition = "text")
    private String syllabusSummary;

    @Column(name = "program_outcomes", columnDefinition = "text")
    private String programOutcomes;

    @Column(name = "teacher_guide", columnDefinition = "text")
    private String teacherGuide;

    @Column(name = "interaction_activities", columnDefinition = "text")
    private String interactionActivities;

    @OneToMany(mappedBy = "classroomOffering", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClassroomSession> sessions = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addSession(ClassroomSession session) {
        sessions.add(session);
        session.setClassroomOffering(this);
    }
}
