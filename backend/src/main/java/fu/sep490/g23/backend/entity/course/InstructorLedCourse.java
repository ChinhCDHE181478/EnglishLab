package fu.sep490.g23.backend.entity.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Canonical instructor-led course ({@code instructor_led_courses}).
 * Merges legacy {@code training_programs} + {@code curriculum_programs}.
 * IDs match {@code training_programs.id}. No {@code delivery_mode} (belongs on class section later).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "instructor_led_courses")
@EntityListeners(AuditingEntityListener.class)
public class InstructorLedCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String code;

    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "exam_type", nullable = false, length = 30)
    @Builder.Default
    private String examType = "IELTS";

    @Column(name = "program_track", length = 60)
    private String programTrack;

    @Column(length = 120)
    private String level;

    @Column(name = "entry_level", length = 120)
    private String entryLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_placement_level", length = 30)
    private PlacementLevel entryPlacementLevel;

    @Column(name = "focus_skills", length = 240)
    private String focusSkills;

    @Column(name = "target_band", precision = 3, scale = 1)
    private BigDecimal targetBand;

    @Column(name = "target_score")
    private Integer targetScore;

    @Column(name = "learning_outcomes", columnDefinition = "text")
    private String learningOutcomes;

    @Column(name = "teacher_guide", columnDefinition = "text")
    private String teacherGuide;

    @Column(name = "base_tuition_fee_vnd", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal baseTuitionFeeVnd = BigDecimal.ZERO;

    @Column(name = "sale_tuition_fee_vnd", precision = 12, scale = 2)
    private BigDecimal saleTuitionFeeVnd;

    @Column(name = "duration_label", length = 80)
    private String durationLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 30)
    @Builder.Default
    private PackageStatus publicationStatus = PackageStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @OneToMany(mappedBy = "instructorLedCourse", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC, id ASC")
    @Builder.Default
    private List<CourseUnit> units = new ArrayList<>();

    public void addUnit(CourseUnit unit) {
        units.add(unit);
        unit.setInstructorLedCourse(this);
    }

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
