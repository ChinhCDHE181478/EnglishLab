package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Resolves canonical instructor-led course identifiers. */
@Component
@RequiredArgsConstructor
public class InstructorLedCourseIdResolver {

    private final InstructorLedCourseRepository instructorLedCourseRepository;

    public Optional<InstructorLedCourse> resolveById(Long instructorLedCourseId) {
        if (instructorLedCourseId == null) {
            return Optional.empty();
        }
        return instructorLedCourseRepository.findById(instructorLedCourseId);
    }

}
