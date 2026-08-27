package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.TeacherClassroomAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherClassroomAuthorizationServiceImpl implements TeacherClassroomAuthorizationService {

    private final ClassroomAccessHelper accessHelper;
    private final ClassSectionRepository offeringRepository;
    private final ClassScheduleRepository sessionRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final ClassroomTeacherAssignmentRepository assignmentRepository;

    @Override
    public void assertClassroomAccess(Long classroomId, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTeacher(actor);
        ClassSection offering = offeringRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        assertAssigned(actor, offering);
    }

    @Override
    public void assertSessionAccess(Long sessionId, String actorEmail) {
        ClassSection offering = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."))
                .getClassSection();
        assertActorAccess(actorEmail, offering);
    }

    @Override
    public void assertHomeworkAccess(Long homeworkId, String actorEmail) {
        ClassSection offering = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập."))
                .getClassSection();
        assertActorAccess(actorEmail, offering);
    }

    @Override
    public void assertMaterialAccess(Long materialId, String actorEmail) {
        ClassSection offering = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu."))
                .getClassSection();
        assertActorAccess(actorEmail, offering);
    }

    private void assertActorAccess(String actorEmail, ClassSection offering) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTeacher(actor);
        assertAssigned(actor, offering);
    }

    private void assertAssigned(User actor, ClassSection offering) {
        if (actor.hasRole(RoleCodes.ADMIN) || actor.hasRole(RoleCodes.MANAGER)) {
            return;
        }
        LocalDate today = LocalDate.now();
        boolean assigned = assignmentRepository
                .findAllByClassSectionIdAndTeacherId(offering.getId(), actor.getId())
                .stream()
                .anyMatch(assignment -> isActive(assignment, today));
        if (!assigned) {
            throw new RuntimeException("Bạn không được phân công phụ trách lớp học này.");
        }
    }

    private boolean isActive(ClassroomTeacherAssignment assignment, LocalDate today) {
        return (assignment.getEffectiveFrom() == null || !assignment.getEffectiveFrom().isAfter(today))
                && (assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(today));
    }
}
