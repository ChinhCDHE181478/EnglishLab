package fu.sep490.g23.backend.entity.course;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "online_courses")
public class OnlineCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "recommended_current_band_max")
    private Double recommendedCurrentBandMax;

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

    @OneToMany(mappedBy = "onlineCourse", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<CourseModule> modules = new ArrayList<>();

    public void addModule(CourseModule module) {
        modules.add(module);
        module.setOnlineCourse(this);
    }
}
