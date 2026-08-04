package fu.sap490.g23.backend.service.notification;

import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.service.notification.impl.TeacherFeedbackReminderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherFeedbackReminderServiceImplTest {

    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private TeacherFeedbackReminderDispatcher reminderDispatcher;

    private TeacherFeedbackReminderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TeacherFeedbackReminderServiceImpl(offeringRepository, reminderDispatcher);
        ReflectionTestUtils.setField(service, "opensDaysBeforeEnd", 7);
        ReflectionTestUtils.setField(service, "closesDaysAfterEnd", 14);
        ReflectionTestUtils.setField(service, "closingReminderDays", 2);
    }

    @Test
    void dispatchContinuesWithNextClassroomWhenOneTransactionFails() {
        ClassroomOffering first = ClassroomOffering.builder().id(1L).build();
        ClassroomOffering second = ClassroomOffering.builder().id(2L).build();
        when(offeringRepository.findByEndDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(first, second));
        doThrow(new RuntimeException("database failure"))
                .when(reminderDispatcher)
                .dispatchForClassroom(eq(1L), any(LocalDate.class), eq(7), eq(14), eq(2));

        service.dispatchTeacherFeedbackReminders();

        verify(reminderDispatcher)
                .dispatchForClassroom(eq(2L), any(LocalDate.class), eq(7), eq(14), eq(2));
    }
}
