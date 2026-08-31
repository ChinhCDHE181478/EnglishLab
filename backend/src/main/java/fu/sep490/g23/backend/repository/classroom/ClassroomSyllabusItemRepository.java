package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomSyllabusItem;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ClassroomSyllabusItemRepository {
    private final ClassSectionRepository sectionRepository;
    private final CourseUnitRepository unitRepository;

    public List<ClassroomSyllabusItem> findByClassSectionIdOrderByDisplayOrderAsc(Long sectionId) {
        var section = sectionRepository.findById(sectionId).orElse(null);
        if (section == null || section.getInstructorLedCourse() == null) return List.of();
        return unitRepository.findByInstructorLedCourseIdOrderBySequenceNumberAscIdAsc(
                section.getInstructorLedCourse().getId()).stream().map(unit -> toItem(unit, section)).toList();
    }

    public java.util.Optional<ClassroomSyllabusItem> findById(Long id) {
        return unitRepository.findById(id).map(unit -> {
            var section = sectionRepository.findAll().stream()
                    .filter(item -> item.getInstructorLedCourse() != null
                            && item.getInstructorLedCourse().getId().equals(unit.getInstructorLedCourse().getId()))
                    .findFirst().orElse(null);
            return toItem(unit, section);
        });
    }

    public ClassroomSyllabusItem save(ClassroomSyllabusItem item) {
        CourseUnit unit = item.getId() == null ? new CourseUnit() : unitRepository.findById(item.getId()).orElseThrow();
        unit.setInstructorLedCourse(item.getClassSection().getInstructorLedCourse());
        unit.setTitle(item.getTitle());
        unit.setDescription(item.getDescription());
        unit.setLearningObjectives(item.getSessionPlan());
        unit.setSequenceNumber(item.getDisplayOrder());
        return toItem(unitRepository.save(unit), item.getClassSection());
    }

    public void delete(ClassroomSyllabusItem item) {
        unitRepository.deleteById(item.getId());
    }

    private ClassroomSyllabusItem toItem(CourseUnit unit, fu.sep490.g23.backend.entity.classroom.ClassSection section) {
        return ClassroomSyllabusItem.builder().id(unit.getId()).classSection(section).title(unit.getTitle())
                .description(unit.getDescription()).displayOrder(unit.getSequenceNumber())
                .sessionPlan(unit.getLearningObjectives()).status("PUBLISHED")
                .createdAt(unit.getCreatedAt()).updatedAt(unit.getUpdatedAt()).build();
    }
}
