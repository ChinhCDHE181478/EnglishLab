package fu.sep490.g23.backend.entity.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
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
import jakarta.persistence.OneToOne;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "online_courses")
@EntityListeners(AuditingEntityListener.class)
public class OnlineCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Legacy commercial twin row. Kept through Slice 1 for classroom Package FKs and dual-write.
     * Prefer commercial fields on this entity for online catalog/enrollment flows.
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "package_id", nullable = false, unique = true)
    private LearningPackage learningPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CourseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseLevel level;

    @Column(name = "recommended_current_band_min")
    private Double recommendedCurrentBandMin;

    @Column(name = "target_band")
    private Double targetBand;

    @Column(name = "learning_path_code", length = 80)
    private String learningPathCode;

    @Column(name = "learning_path_name", length = 180)
    private String learningPathName;

    @Column(name = "learning_path_order")
    private Integer learningPathOrder;

    @Column(name = "target_outcome", length = 700)
    private String targetOutcome;

    @Column(name = "recommended_next_course_slug", length = 220)
    private String recommendedNextCourseSlug;

    @Column(name = "total_lessons", nullable = false)
    @Builder.Default
    private Integer totalLessons = 0;

    @Column(name = "total_hours", nullable = false)
    @Builder.Default
    private Integer totalHours = 0;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "target_score", length = 80)
    private String targetScore;

    @Column(name = "duration_label", length = 80)
    private String duration;

    @Column(name = "study_mode", length = 120)
    private String studyMode;

    @Column(precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "thumbnail_url", length = 700)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PackageStatus status = PackageStatus.DRAFT;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @Column(name = "submitted_for_review_at")
    private LocalDateTime submittedForReviewAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "onlineCourse", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<CourseModule> modules = new ArrayList<>();

    public void addModule(CourseModule module) {
        modules.add(module);
        module.setOnlineCourse(this);
    }

    public boolean isPublished() {
        return PackageStatus.PUBLISHED.equals(status) && !deleted;
    }

    /** Dual-write commercial fields onto legacy packages row until Package drop. */
    public void syncCommercialToLegacyPackage() {
        if (learningPackage == null) {
            return;
        }
        learningPackage.setTitle(title);
        learningPackage.setSlug(slug);
        learningPackage.setShortDescription(shortDescription);
        learningPackage.setDescription(description);
        learningPackage.setTargetScore(targetScore);
        learningPackage.setDuration(duration);
        learningPackage.setStudyMode(studyMode);
        learningPackage.setPrice(price != null ? price : BigDecimal.ZERO);
        learningPackage.setSalePrice(salePrice);
        learningPackage.setThumbnailUrl(thumbnailUrl);
        learningPackage.setStatus(status != null ? status : PackageStatus.DRAFT);
        learningPackage.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        learningPackage.setFeatured(featured);
        learningPackage.setDeleted(deleted);
        learningPackage.setCreatedBy(createdBy);
        learningPackage.setReviewNote(reviewNote);
        learningPackage.setSubmittedForReviewAt(submittedForReviewAt);
        learningPackage.setReviewedAt(reviewedAt);
        learningPackage.setReviewedBy(reviewedBy);
    }
}
