package fu.sep490.g23.backend.entity.course;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Canonical course unit ({@code course_units}). IDs preserved from {@code curriculum_units}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_units")
@EntityListeners(AuditingEntityListener.class)
public class CourseUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_led_course_id", nullable = false)
    private InstructorLedCourse instructorLedCourse;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 700)
    private String description;

    @Column(name = "learning_objectives", columnDefinition = "text")
    private String learningObjectives;

    @Column(name = "sequence_number", nullable = false)
    @Builder.Default
    private Integer sequenceNumber = 0;

    @OneToMany(mappedBy = "courseUnit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC, id ASC")
    @Builder.Default
    private List<CourseLesson> lessons = new ArrayList<>();

    @OneToMany(mappedBy = "courseUnit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC, id ASC")
    @Builder.Default
    private List<CourseUnitContentRef> contentRefs = new ArrayList<>();

    public void addLesson(CourseLesson lesson) {
        lessons.add(lesson);
        lesson.setCourseUnit(this);
    }

    public void addContentRef(CourseUnitContentRef contentRef) {
        contentRefs.add(contentRef);
        contentRef.setCourseUnit(this);
    }

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
