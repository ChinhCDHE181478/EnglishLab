package fu.sep490.g23.backend.entity.course;

import fu.sep490.g23.backend.entity.course.enums.InstructorLedCourseLegacyKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "instructor_led_course_id_map")
@IdClass(InstructorLedCourseIdMap.Pk.class)
public class InstructorLedCourseIdMap {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "legacy_kind", nullable = false, length = 30)
    private InstructorLedCourseLegacyKind legacyKind;

    @Id
    @Column(name = "legacy_id", nullable = false)
    private Long legacyId;

    @Column(name = "instructor_led_course_id", nullable = false)
    private Long instructorLedCourseId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private InstructorLedCourseLegacyKind legacyKind;
        private Long legacyId;
    }
}
