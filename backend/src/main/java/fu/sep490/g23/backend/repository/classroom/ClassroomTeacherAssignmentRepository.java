package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class ClassroomTeacherAssignmentRepository {
    private final ClassSectionRepository sectionRepository;
    private final ClassScheduleRepository scheduleRepository;

    public long count() {
        return sectionRepository.countByPrimaryTeacherIsNotNull() + scheduleRepository.countByTeacherIsNotNull();
    }

    public List<ClassroomTeacherAssignment> findByClassSectionId(Long sectionId) {
        List<ClassroomTeacherAssignment> result = new java.util.ArrayList<>();
        sectionRepository.findById(sectionId).filter(section -> section.getPrimaryTeacher() != null)
                .map(this::primaryAssignment).ifPresent(result::add);
        scheduleRepository.findByClassSectionIdAndTeacherIsNotNull(sectionId).stream()
                .map(this::scheduleAssignment).forEach(result::add);
        return result;
    }

    public List<ClassroomTeacherAssignment> findByTeacherId(Long teacherId) {
        List<ClassroomTeacherAssignment> result = new java.util.ArrayList<>();
        sectionRepository.findByPrimaryTeacherId(teacherId).stream().map(this::primaryAssignment).forEach(result::add);
        scheduleRepository.findByTeacherId(teacherId).stream().map(this::scheduleAssignment).forEach(result::add);
        return result;
    }

    public List<ClassroomTeacherAssignment> findAllByClassSectionIdAndTeacherId(Long sectionId, Long teacherId) {
        return findByClassSectionId(sectionId).stream()
                .filter(item -> item.getTeacher() != null && item.getTeacher().getId().equals(teacherId)).toList();
    }

    public Optional<ClassroomTeacherAssignment> findByClassScheduleId(Long scheduleId) {
        return scheduleRepository.findById(scheduleId).filter(schedule -> schedule.getTeacher() != null)
                .map(this::scheduleAssignment);
    }

    public ClassroomTeacherAssignment save(ClassroomTeacherAssignment assignment) {
        LocalDateTime now = LocalDateTime.now();
        if (assignment.getClassSchedule() != null) {
            assignment.getClassSchedule().setTeacher(assignment.getTeacher());
            scheduleRepository.save(assignment.getClassSchedule());
            assignment.setId(assignment.getClassSchedule().getId());
        } else {
            assignment.getClassSection().setPrimaryTeacher(assignment.getTeacher());
            sectionRepository.save(assignment.getClassSection());
            assignment.setId(assignment.getClassSection().getId());
        }
        if (assignment.getCreatedAt() == null) assignment.setCreatedAt(now);
        assignment.setUpdatedAt(now);
        return assignment;
    }

    public void delete(ClassroomTeacherAssignment assignment) {
        if (assignment.getClassSchedule() != null) {
            assignment.getClassSchedule().setTeacher(null);
            scheduleRepository.save(assignment.getClassSchedule());
        } else if (assignment.getClassSection() != null
                && assignment.getClassSection().getPrimaryTeacher() != null
                && assignment.getClassSection().getPrimaryTeacher().getId().equals(assignment.getTeacher().getId())) {
            assignment.getClassSection().setPrimaryTeacher(null);
            sectionRepository.save(assignment.getClassSection());
        }
    }

    private ClassroomTeacherAssignment primaryAssignment(fu.sep490.g23.backend.entity.classroom.ClassSection section) {
        return ClassroomTeacherAssignment.builder().id(section.getId()).classSection(section)
                .teacher(section.getPrimaryTeacher()).role(fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole.PRIMARY)
                .createdAt(section.getCreatedAt()).updatedAt(section.getUpdatedAt()).build();
    }

    private ClassroomTeacherAssignment scheduleAssignment(fu.sep490.g23.backend.entity.classroom.ClassSchedule schedule) {
        return ClassroomTeacherAssignment.builder().id(schedule.getId()).classSection(schedule.getClassSection())
                .classSchedule(schedule).teacher(schedule.getTeacher())
                .role(fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole.SUBSTITUTE)
                .effectiveFrom(schedule.getSessionDate()).effectiveTo(schedule.getSessionDate())
                .createdAt(schedule.getCreatedAt()).updatedAt(schedule.getUpdatedAt()).build();
    }
}
