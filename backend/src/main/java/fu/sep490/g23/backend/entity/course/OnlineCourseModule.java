package fu.sep490.g23.backend.entity.course;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "online_course_modules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_oce_module_version_seq",
                columnNames = {"online_course_version_id", "sequence_number"}
        )
)
public class OnlineCourseModule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_course_version_id", nullable = false)
    private OnlineCourseVersion onlineCourseVersion;

    /** Legacy root-course FK kept through Slice 2 reconciliation; prefer version ownership. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_course_id")
    private OnlineCourse onlineCourse;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "sequence_number", nullable = false)
    @Builder.Default
    private Integer sequenceNumber = 0;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC, id ASC")
    @Builder.Default
    private List<OnlineLesson> lessons = new ArrayList<>();

    public void addLesson(OnlineLesson lesson) {
        lessons.add(lesson);
        lesson.setModule(this);
    }

    /** Compatibility alias used by older call sites during Slice 2. */
    public Integer getDisplayOrder() {
        return sequenceNumber;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.sequenceNumber = displayOrder == null ? 0 : displayOrder;
    }
}
