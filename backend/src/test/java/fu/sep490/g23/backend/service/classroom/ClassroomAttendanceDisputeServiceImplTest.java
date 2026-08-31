package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceDisputeRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomAttendanceDisputeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomAttendanceDisputeServiceImplTest {

    @Mock
    private ClassroomAttendanceDisputeRepository disputeRepository;

    @Mock
    private ClassroomAttendanceRepository attendanceRepository;

    @Mock
    private ClassSectionRepository offeringRepository;

    @Mock
    private ClassroomTeacherAssignmentRepository teacherAssignmentRepository;

    @Mock
    private ClassroomAccessHelper accessHelper;

    @InjectMocks
    private ClassroomAttendanceDisputeServiceImpl service;

    @Test
    void rejectsTeacherWhoIsNotAssignedToClassroom() {
        User teacher = mock(User.class);
        when(teacher.getId()).thenReturn(17L);
        when(accessHelper.requireUser("teacher@englishlab.vn")).thenReturn(teacher);
        when(offeringRepository.findById(7L)).thenReturn(Optional.empty());
        when(teacherAssignmentRepository.findAllByClassSectionIdAndTeacherId(7L, 17L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.listForClass(7L, "teacher@englishlab.vn"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Bạn không được phân công phụ trách lớp học này.");

        verifyNoInteractions(disputeRepository);
    }
}
