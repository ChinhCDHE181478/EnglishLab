package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.ClassroomSession;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomLifecycleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomLifecycleServiceImplTest {

    @Mock private ClassroomSessionRepository sessionRepository;
    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private VirtualAttendanceService virtualAttendanceService;

    private ClassroomLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassroomLifecycleServiceImpl(
                sessionRepository,
                offeringRepository,
                virtualAttendanceService
        );
    }

    @Test
    void overdueOpenSessionAndItsClassAreCompleted() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 10, 0);
        ClassroomOffering offering = offering(7L, ClassroomOfferingStatus.ACTIVE, LocalDate.of(2026, 7, 31));
        ClassroomSession session = session(
                70L,
                LocalDate.of(2026, 7, 10),
                ClassroomSessionStatus.OPEN,
                ClassroomDeliveryMode.VIRTUAL
        );
        session.setClassroomOffering(offering);

        when(sessionRepository.findSessionsEndedBefore(
                anyCollection(),
                eq(LocalDate.of(2026, 8, 11)),
                eq(LocalTime.of(9, 30))
        )).thenReturn(List.of(session));
        when(offeringRepository.findByStatusIn(anyCollection())).thenReturn(List.of(offering));
        when(sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId()))
                .thenReturn(List.of(session));

        service.reconcileStatuses(now);

        assertThat(session.getStatus()).isEqualTo(ClassroomSessionStatus.COMPLETED);
        assertThat(session.isLocked()).isTrue();
        assertThat(session.getLarkMeetingStatus()).isEqualTo(LarkMeetingStatus.ENDED);
        assertThat(offering.getStatus()).isEqualTo(ClassroomOfferingStatus.COMPLETED);
        verify(virtualAttendanceService).finalizeVirtualAttendance(session);
        verify(sessionRepository).saveAll(List.of(session));
        verify(offeringRepository).saveAll(List.of(offering));
    }

    @Test
    void upcomingClassBecomesActiveWhenStartDateArrives() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 10, 0);
        ClassroomOffering offering = offering(8L, ClassroomOfferingStatus.UPCOMING, LocalDate.of(2026, 9, 30));
        offering.setStartDate(LocalDate.of(2026, 8, 11));
        ClassroomSession futureSession = session(
                80L,
                LocalDate.of(2026, 8, 12),
                ClassroomSessionStatus.SCHEDULED,
                ClassroomDeliveryMode.OFFLINE
        );

        when(sessionRepository.findSessionsEndedBefore(anyCollection(), eq(now.toLocalDate()), eq(LocalTime.of(9, 30))))
                .thenReturn(List.of());
        when(offeringRepository.findByStatusIn(anyCollection())).thenReturn(List.of(offering));
        when(sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId()))
                .thenReturn(List.of(futureSession));

        service.reconcileStatuses(now);

        assertThat(offering.getStatus()).isEqualTo(ClassroomOfferingStatus.ACTIVE);
    }

    @Test
    void futureMakeupSessionKeepsClassActiveAfterPlannedEndDate() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 10, 0);
        ClassroomOffering offering = offering(9L, ClassroomOfferingStatus.ACTIVE, LocalDate.of(2026, 8, 10));
        ClassroomSession completed = session(
                90L,
                LocalDate.of(2026, 8, 9),
                ClassroomSessionStatus.COMPLETED,
                ClassroomDeliveryMode.OFFLINE
        );
        ClassroomSession makeup = session(
                91L,
                LocalDate.of(2026, 8, 12),
                ClassroomSessionStatus.MAKEUP,
                ClassroomDeliveryMode.OFFLINE
        );

        when(sessionRepository.findSessionsEndedBefore(anyCollection(), eq(now.toLocalDate()), eq(LocalTime.of(9, 30))))
                .thenReturn(List.of());
        when(offeringRepository.findByStatusIn(anyCollection())).thenReturn(List.of(offering));
        when(sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId()))
                .thenReturn(List.of(completed, makeup));

        service.reconcileStatuses(now);

        assertThat(offering.getStatus()).isEqualTo(ClassroomOfferingStatus.ACTIVE);
    }

    @Test
    void classRemainsActiveThroughoutItsEndDate() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 22, 0);
        ClassroomOffering offering = offering(10L, ClassroomOfferingStatus.ACTIVE, now.toLocalDate());
        ClassroomSession completed = session(
                100L,
                now.toLocalDate(),
                ClassroomSessionStatus.COMPLETED,
                ClassroomDeliveryMode.OFFLINE
        );

        when(sessionRepository.findSessionsEndedBefore(anyCollection(), eq(now.toLocalDate()), eq(LocalTime.of(21, 30))))
                .thenReturn(List.of());
        when(offeringRepository.findByStatusIn(anyCollection())).thenReturn(List.of(offering));
        when(sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId()))
                .thenReturn(List.of(completed));

        service.reconcileStatuses(now);

        assertThat(offering.getStatus()).isEqualTo(ClassroomOfferingStatus.ACTIVE);
    }

    @Test
    void onlyCurrentlyRunningStatusesAreAutoCompleted() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 22, 0);

        when(sessionRepository.findSessionsEndedBefore(anyCollection(), eq(now.toLocalDate()), eq(LocalTime.of(21, 30))))
                .thenReturn(List.of());
        when(offeringRepository.findByStatusIn(anyCollection())).thenReturn(List.of());

        service.reconcileStatuses(now);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<ClassroomSessionStatus>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(sessionRepository).findSessionsEndedBefore(
                statusesCaptor.capture(),
                eq(now.toLocalDate()),
                eq(LocalTime.of(21, 30))
        );
        assertThat(statusesCaptor.getValue())
                .containsExactlyInAnyOrder(ClassroomSessionStatus.OPEN, ClassroomSessionStatus.IN_PROGRESS);
    }

    private ClassroomOffering offering(Long id, ClassroomOfferingStatus status, LocalDate endDate) {
        return ClassroomOffering.builder()
                .id(id)
                .status(status)
                .startDate(endDate.minusMonths(1))
                .endDate(endDate)
                .build();
    }

    private ClassroomSession session(
            Long id,
            LocalDate date,
            ClassroomSessionStatus status,
            ClassroomDeliveryMode deliveryMode
    ) {
        return ClassroomSession.builder()
                .id(id)
                .sessionDate(date)
                .startTime(LocalTime.of(19, 30))
                .endTime(LocalTime.of(21, 0))
                .status(status)
                .deliveryMode(deliveryMode)
                .larkMeetingStatus(LarkMeetingStatus.OPEN)
                .build();
    }
}
