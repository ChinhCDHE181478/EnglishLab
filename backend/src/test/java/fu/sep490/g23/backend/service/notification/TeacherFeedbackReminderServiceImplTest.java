package fu.sep490.g23.backend.service.notification;

import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.service.notification.impl.TeacherFeedbackReminderServiceImpl;
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

    @Mock private ClassSectionRepository offeringRepository;
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
        ClassSection first = ClassSection.builder().id(1L).build();
        ClassSection second = ClassSection.builder().id(2L).build();
        when(offeringRepository.findByPlannedEndDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(first, second));
        doThrow(new RuntimeException("database failure"))
                .when(reminderDispatcher)
                .dispatchForClassroom(eq(1L), any(LocalDate.class), eq(7), eq(14), eq(2));

        service.dispatchTeacherFeedbackReminders();

        verify(reminderDispatcher)
                .dispatchForClassroom(eq(2L), any(LocalDate.class), eq(7), eq(14), eq(2));
    }
}
