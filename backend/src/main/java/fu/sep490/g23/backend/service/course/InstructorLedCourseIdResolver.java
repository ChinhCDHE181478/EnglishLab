package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.InstructorLedCourseIdMap;
import fu.sep490.g23.backend.entity.course.enums.InstructorLedCourseLegacyKind;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseIdMapRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves legacy TrainingProgram / CurriculumProgram IDs to {@code instructor_led_courses}
 * via {@code instructor_led_course_id_map}. Prefer ILC IDs for new traffic.
 */
@Component
@RequiredArgsConstructor
public class InstructorLedCourseIdResolver {

    private final InstructorLedCourseIdMapRepository idMapRepository;
    private final InstructorLedCourseRepository instructorLedCourseRepository;

    public Optional<Long> resolve(InstructorLedCourseLegacyKind legacyKind, Long legacyId) {
        if (legacyKind == null || legacyId == null) {
            return Optional.empty();
        }
        return idMapRepository.findByLegacyKindAndLegacyId(legacyKind, legacyId)
                .map(InstructorLedCourseIdMap::getInstructorLedCourseId);
    }

    public Optional<Long> reverseResolve(InstructorLedCourseLegacyKind legacyKind, Long instructorLedCourseId) {
        if (legacyKind == null || instructorLedCourseId == null) {
            return Optional.empty();
        }
        return idMapRepository.findByLegacyKindAndInstructorLedCourseId(legacyKind, instructorLedCourseId)
                .map(InstructorLedCourseIdMap::getLegacyId);
    }

    public Optional<InstructorLedCourse> resolveCourse(InstructorLedCourseLegacyKind kind, Long id) {
        if (kind == null || id == null) {
            return Optional.empty();
        }
        Optional<InstructorLedCourse> byIlcId = instructorLedCourseRepository.findById(id);
        if (byIlcId.isPresent() && kind == InstructorLedCourseLegacyKind.TRAINING_PROGRAM) {
            // TP.id == ILC.id after backfill; still accept direct ILC id.
            return byIlcId;
        }
        if (byIlcId.isPresent() && kind == InstructorLedCourseLegacyKind.CURRICULUM_PROGRAM) {
            // CP ids are independent — only trust the map for CP.
            return resolve(kind, id).flatMap(instructorLedCourseRepository::findById);
        }
        return resolve(kind, id).flatMap(instructorLedCourseRepository::findById);
    }

    public Optional<InstructorLedCourse> resolveFromTrainingProgramId(Long trainingProgramId) {
        if (trainingProgramId == null) {
            return Optional.empty();
        }
        return instructorLedCourseRepository.findById(trainingProgramId)
                .or(() -> resolve(InstructorLedCourseLegacyKind.TRAINING_PROGRAM, trainingProgramId)
                        .flatMap(instructorLedCourseRepository::findById));
    }

    public Optional<InstructorLedCourse> resolveFromCurriculumProgramId(Long curriculumProgramId) {
        return resolve(InstructorLedCourseLegacyKind.CURRICULUM_PROGRAM, curriculumProgramId)
                .flatMap(instructorLedCourseRepository::findById);
    }

    public InstructorLedCourse requireFromTrainingProgramId(Long trainingProgramId) {
        return resolveFromTrainingProgramId(trainingProgramId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học giảng viên (ILC)."));
    }
}
