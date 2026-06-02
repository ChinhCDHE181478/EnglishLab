package fu.sap490.g23.backend.entity.course;

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
