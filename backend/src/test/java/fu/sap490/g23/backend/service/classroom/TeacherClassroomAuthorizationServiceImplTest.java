package fu.sap490.g23.backend.service.classroom;

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
import fu.sap490.g23.backend.service.classroom.impl.TeacherClassroomAuthorizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherClassroomAuthorizationServiceImplTest {

    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private ClassroomSessionRepository sessionRepository;
    @Mock private ClassroomHomeworkRepository homeworkRepository;
    @Mock private ClassroomMaterialRepository materialRepository;
    @Mock private ClassroomTeacherAssignmentRepository assignmentRepository;

    private TeacherClassroomAuthorizationServiceImpl service;
    private ClassroomOffering offering;
    private User teacher;

    @BeforeEach
    void setUp() {
        service = new TeacherClassroomAuthorizationServiceImpl(
                accessHelper,
                offeringRepository,
                sessionRepository,
                homeworkRepository,
                materialRepository,
                assignmentRepository
        );
        offering = ClassroomOffering.builder().id(21L).build();
        teacher = user(7L, "teacher@example.com", RoleEnum.TEACHER);
    }

    @Test
    void deniesTeacherWhoIsNotAssignedToRequestedClassroom() {
        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(offeringRepository.findById(offering.getId())).thenReturn(Optional.of(offering));
        when(assignmentRepository.findAllByClassroomOfferingIdAndTeacherId(offering.getId(), teacher.getId()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.assertClassroomAccess(offering.getId(), teacher.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Bạn không được phân công phụ trách lớp học này.");
    }

    @Test
    void allowsTeacherWithActiveAssignment() {
        ClassroomTeacherAssignment assignment = ClassroomTeacherAssignment.builder()
                .classroomOffering(offering)
                .teacher(teacher)
                .effectiveFrom(LocalDate.now().minusDays(1))
                .effectiveTo(LocalDate.now().plusDays(1))
                .build();
        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(offeringRepository.findById(offering.getId())).thenReturn(Optional.of(offering));
        when(assignmentRepository.findAllByClassroomOfferingIdAndTeacherId(offering.getId(), teacher.getId()))
                .thenReturn(List.of(assignment));

        assertThatCode(() -> service.assertClassroomAccess(offering.getId(), teacher.getEmail()))
                .doesNotThrowAnyException();
    }

    @Test
    void deniesExpiredTeacherAssignment() {
        ClassroomTeacherAssignment assignment = ClassroomTeacherAssignment.builder()
                .classroomOffering(offering)
                .teacher(teacher)
                .effectiveTo(LocalDate.now().minusDays(1))
                .build();
        when(accessHelper.requireUser(teacher.getEmail())).thenReturn(teacher);
        when(offeringRepository.findById(offering.getId())).thenReturn(Optional.of(offering));
        when(assignmentRepository.findAllByClassroomOfferingIdAndTeacherId(offering.getId(), teacher.getId()))
                .thenReturn(List.of(assignment));

        assertThatThrownBy(() -> service.assertClassroomAccess(offering.getId(), teacher.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Bạn không được phân công phụ trách lớp học này.");
    }

    @Test
    void allowsManagerToOperateAcrossClassrooms() {
        User manager = user(9L, "manager@example.com", RoleEnum.MANAGER);
        when(accessHelper.requireUser(manager.getEmail())).thenReturn(manager);
        when(offeringRepository.findById(offering.getId())).thenReturn(Optional.of(offering));

        assertThatCode(() -> service.assertClassroomAccess(offering.getId(), manager.getEmail()))
                .doesNotThrowAnyException();
    }

    private User user(Long id, String email, RoleEnum role) {
        User user = User.builder().id(id).email(email).fullName(email).build();
        user.setRole(role);
        return user;
    }
}
