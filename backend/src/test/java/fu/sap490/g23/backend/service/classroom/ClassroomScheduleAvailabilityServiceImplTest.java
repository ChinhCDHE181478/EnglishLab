package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.service.classroom.impl.ClassroomScheduleAvailabilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomScheduleAvailabilityServiceImplTest {

    @Mock private ClassroomRoomRepository roomRepository;
    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private ClassroomSessionRepository sessionRepository;
    @Mock private UserRepository userRepository;

    private ClassroomScheduleAvailabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassroomScheduleAvailabilityServiceImpl(
                roomRepository,
                offeringRepository,
                sessionRepository,
                userRepository
        );
    }

    @Test
    void replacementListExcludesTeacherBusyInAnyUpcomingSession() {
        User freeTeacher = teacher(11L, "Giáo viên rảnh");
        User busyTeacher = teacher(12L, "Giáo viên bận");
        ClassroomSession first = session(101L, LocalDate.now().plusDays(2), 18);
        ClassroomSession second = session(102L, LocalDate.now().plusDays(4), 18);

        when(offeringRepository.findById(any())).thenReturn(Optional.of(ClassroomOffering.builder().id(7L).build()));
        when(sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(7L))
                .thenReturn(List.of(first, second));
        when(userRepository.findDistinctByRoles_CodeIn(List.of(RoleEnum.TEACHER)))
                .thenReturn(List.of(freeTeacher, busyTeacher));
        when(sessionRepository.findTeacherConflicts(
                eq(freeTeacher.getId()), any(), any(), any(), anyCollection(), any()
        )).thenReturn(List.of());
        when(sessionRepository.findTeacherConflicts(
                eq(busyTeacher.getId()), eq(first.getSessionDate()), any(), any(), anyCollection(), eq(first.getId())
        )).thenReturn(List.of());
        when(sessionRepository.findTeacherConflicts(
                eq(busyTeacher.getId()), eq(second.getSessionDate()), any(), any(), anyCollection(), eq(second.getId())
        )).thenReturn(List.of(session(999L, second.getSessionDate(), 18)));

        assertThat(service.listAvailableReplacementTeachers(7L))
                .extracting("id")
                .containsExactly(freeTeacher.getId());
    }

    private User teacher(Long id, String name) {
        return User.builder().id(id).fullName(name).email(id + "@example.com").build();
    }

    private ClassroomSession session(Long id, LocalDate date, int hour) {
        return ClassroomSession.builder()
                .id(id)
                .sessionDate(date)
                .startTime(LocalTime.of(hour, 0))
                .endTime(LocalTime.of(hour + 2, 0))
                .status(ClassroomSessionStatus.SCHEDULED)
                .build();
    }
}
