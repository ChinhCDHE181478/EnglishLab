package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomGradebookEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Compatibility facade over the canonical gradebook columns on class_enrollments. */
@Repository
@RequiredArgsConstructor
public class ClassroomGradebookEntryRepository {
    private final ClassEnrollmentRepository enrollmentRepository;

    public List<ClassroomGradebookEntry> findAll() {
        return enrollmentRepository.findAll().stream().map(this::toEntry).toList();
    }

    public List<ClassroomGradebookEntry> findByClassSectionId(Long classSectionId) {
        return enrollmentRepository.findAllByClassSectionId(classSectionId).stream().map(this::toEntry).toList();
    }

    public Optional<ClassroomGradebookEntry> findByClassSectionIdAndStudentId(Long classSectionId, Long studentId) {
        return enrollmentRepository.findByStudentIdAndClassSectionId(studentId, classSectionId).map(this::toEntry);
    }

    public ClassroomGradebookEntry save(ClassroomGradebookEntry entry) {
        ClassEnrollment enrollment = resolveEnrollment(entry);
        enrollment.setHomeworkScore(entry.getHomeworkScore());
        enrollment.setAttendancePercent(entry.getAttendancePercent());
        enrollment.setFinalResult(entry.getFinalResult());
        enrollment.setTeacherComment(entry.getTeacherComment());
        enrollment.setGradebookStatus(entry.getStatus());
        enrollment.setGradebookUpdatedBy(entry.getUpdatedBy());
        enrollment.setGradebookUpdatedAt(LocalDateTime.now());
        return toEntry(enrollmentRepository.save(enrollment));
    }

    public List<ClassroomGradebookEntry> saveAll(Collection<ClassroomGradebookEntry> entries) {
        return entries.stream().map(this::save).toList();
    }

    private ClassEnrollment resolveEnrollment(ClassroomGradebookEntry entry) {
        if (entry.getId() != null) {
            return enrollmentRepository.findById(entry.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký lớp để cập nhật điểm."));
        }
        if (entry.getClassSection() == null || entry.getStudent() == null) {
            throw new IllegalArgumentException("Bảng điểm phải thuộc một học viên đã đăng ký lớp.");
        }
        return enrollmentRepository.findByStudentIdAndClassSectionId(
                        entry.getStudent().getId(), entry.getClassSection().getId())
                .orElseThrow(() -> new IllegalArgumentException("Học viên chưa đăng ký lớp này."));
    }

    private ClassroomGradebookEntry toEntry(ClassEnrollment enrollment) {
        return ClassroomGradebookEntry.builder()
                .id(enrollment.getId())
                .classSection(enrollment.getClassSection())
                .student(enrollment.getStudent())
                .homeworkScore(enrollment.getHomeworkScore())
                .attendancePercent(enrollment.getAttendancePercent())
                .finalResult(enrollment.getFinalResult())
                .teacherComment(enrollment.getTeacherComment())
                .status(enrollment.getGradebookStatus())
                .updatedBy(enrollment.getGradebookUpdatedBy())
                .createdAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getGradebookUpdatedAt())
                .build();
    }
}
