package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.InstructorLedCourseIdMap;
import fu.sep490.g23.backend.entity.course.enums.InstructorLedCourseLegacyKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstructorLedCourseIdMapRepository
        extends JpaRepository<InstructorLedCourseIdMap, InstructorLedCourseIdMap.Pk> {

    Optional<InstructorLedCourseIdMap> findByLegacyKindAndLegacyId(
            InstructorLedCourseLegacyKind legacyKind,
            Long legacyId
    );

    Optional<InstructorLedCourseIdMap> findByLegacyKindAndInstructorLedCourseId(
            InstructorLedCourseLegacyKind legacyKind,
            Long instructorLedCourseId
    );
}
