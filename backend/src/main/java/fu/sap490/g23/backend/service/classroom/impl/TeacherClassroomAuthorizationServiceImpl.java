package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.ClassroomTeacherAssignment;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.classroom.ClassroomHomeworkRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.TeacherClassroomAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherClassroomAuthorizationServiceImpl implements TeacherClassroomAuthorizationService {

    private final ClassroomAccessHelper accessHelper;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomHomeworkRepository homeworkRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final ClassroomTeacherAssignmentRepository assignmentRepository;

    @Override
    public void assertClassroomAccess(Long classroomId, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTeacher(actor);
        ClassroomOffering offering = offeringRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        assertAssigned(actor, offering);
    }

    @Override
    public void assertSessionAccess(Long sessionId, String actorEmail) {
        ClassroomOffering offering = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."))
                .getClassroomOffering();
        assertActorAccess(actorEmail, offering);
    }

    @Override
    public void assertHomeworkAccess(Long homeworkId, String actorEmail) {
        ClassroomOffering offering = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập."))
                .getClassroomOffering();
        assertActorAccess(actorEmail, offering);
    }

    @Override
    public void assertMaterialAccess(Long materialId, String actorEmail) {
        ClassroomOffering offering = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu."))
                .getClassroomOffering();
        assertActorAccess(actorEmail, offering);
    }

    private void assertActorAccess(String actorEmail, ClassroomOffering offering) {
        User actor = accessHelper.requireUser(actorEmail);
        accessHelper.assertTeacher(actor);
        assertAssigned(actor, offering);
    }

    private void assertAssigned(User actor, ClassroomOffering offering) {
        if (actor.hasRole(RoleEnum.ADMIN) || actor.hasRole(RoleEnum.MANAGER)) {
            return;
        }
        LocalDate today = LocalDate.now();
        boolean assigned = assignmentRepository
                .findByClassroomOfferingIdAndTeacherId(offering.getId(), actor.getId())
                .filter(assignment -> isActive(assignment, today))
                .isPresent();
        if (!assigned) {
            throw new RuntimeException("Bạn không được phân công phụ trách lớp học này.");
        }
    }

    private boolean isActive(ClassroomTeacherAssignment assignment, LocalDate today) {
        return (assignment.getEffectiveFrom() == null || !assignment.getEffectiveFrom().isAfter(today))
                && (assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(today));
    }
}
