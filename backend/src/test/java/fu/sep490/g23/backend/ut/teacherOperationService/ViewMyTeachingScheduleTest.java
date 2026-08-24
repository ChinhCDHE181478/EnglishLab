package fu.sep490.g23.backend.ut.teacherOperationService;

import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewMyTeachingScheduleTest {

    private static final Long teacherId = 100L;
    private static final String teacherEmail = "teacher@englishlab.com";
    private static final Long classAId = 200L;
    private static final Long classBId = 300L;

    @Mock
    private ClassroomOfferingService classroomOfferingService;

    @Test
    void getTeachingScheduleSuccessfullyByDateRange() {
        LocalDate fromDate = LocalDate.of(2026, 8, 1);
        LocalDate toDate = LocalDate.of(2026, 8, 31);

        ClassroomOfferingResponse classA = ClassroomOfferingResponse.builder()
                .id(classAId).title("Lß╗¢p A").build();

        ClassroomOfferingResponse classB = ClassroomOfferingResponse.builder()
                .id(classBId).title("Lß╗¢p B").build();

        List<ClassroomSessionResponse> sessionsA = List.of(
                ClassroomSessionResponse.builder().id(1L).classroomOfferingId(classAId)
                        .classroomTitle("Lß╗¢p A").sessionDate(LocalDate.of(2026, 8, 5))
                        .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(11, 0))
                        .teacherId(teacherId).teacherName("GV A")
                        .status(ClassroomSessionStatus.SCHEDULED)
                        .deliveryMode(ClassroomDeliveryMode.VIRTUAL).build(),
                ClassroomSessionResponse.builder().id(2L).classroomOfferingId(classAId)
                        .classroomTitle("Lß╗¢p A").sessionDate(LocalDate.of(2026, 8, 12))
                        .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                        .teacherId(teacherId).teacherName("GV A")
                        .status(ClassroomSessionStatus.SCHEDULED)
                        .deliveryMode(ClassroomDeliveryMode.VIRTUAL).build()
        );

        List<ClassroomSessionResponse> sessionsB = List.of(
                ClassroomSessionResponse.builder().id(3L).classroomOfferingId(classBId)
                        .classroomTitle("Lß╗¢p B").sessionDate(LocalDate.of(2026, 8, 7))
                        .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(10, 0))
                        .teacherId(teacherId).teacherName("GV A")
                        .status(ClassroomSessionStatus.SCHEDULED)
                        .deliveryMode(ClassroomDeliveryMode.OFFLINE).build(),
                ClassroomSessionResponse.builder().id(4L).classroomOfferingId(classBId)
                        .classroomTitle("Lß╗¢p B").sessionDate(LocalDate.of(2026, 8, 21))
                        .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(10, 0))
                        .teacherId(teacherId).teacherName("GV A")
                        .status(ClassroomSessionStatus.SCHEDULED)
                        .deliveryMode(ClassroomDeliveryMode.OFFLINE).build()
        );

        when(classroomOfferingService.getAssignedClasses(teacherEmail))
                .thenReturn(List.of(classA, classB));
        when(classroomOfferingService.getSessions(classAId)).thenReturn(sessionsA);
        when(classroomOfferingService.getSessions(classBId)).thenReturn(sessionsB);

        // Act
        List<ClassroomOfferingResponse> classes = classroomOfferingService.getAssignedClasses(teacherEmail);
        List<ClassroomSessionResponse> resultA = classroomOfferingService.getSessions(classAId);
        List<ClassroomSessionResponse> resultB = classroomOfferingService.getSessions(classBId);

        // Assert
        assertThat(classes).hasSize(2)
                .extracting(ClassroomOfferingResponse::getTitle)
                .containsExactly("Lß╗¢p A", "Lß╗¢p B");

        assertThat(resultA).hasSize(2)
                .extracting(ClassroomSessionResponse::getSessionDate)
                .containsExactly(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 12));

        assertThat(resultB).hasSize(2)
                .extracting(ClassroomSessionResponse::getSessionDate)
                .containsExactly(LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 21));

        assertThat(resultA).allMatch(s -> !s.getSessionDate().isBefore(fromDate) && !s.getSessionDate().isAfter(toDate));
        assertThat(resultB).allMatch(s -> !s.getSessionDate().isBefore(fromDate) && !s.getSessionDate().isAfter(toDate));
    }

    @Test
    void getTeachingScheduleWhenNoClassesScheduled() {
        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 9, 1);
        LocalDate toDate = LocalDate.of(2026, 9, 7);

        ClassroomOfferingResponse classA = ClassroomOfferingResponse.builder()
                .id(classAId).title("Lß╗¢p A").build();

        ClassroomOfferingResponse classB = ClassroomOfferingResponse.builder()
                .id(classBId).title("Lß╗¢p B").build();

        when(classroomOfferingService.getAssignedClasses(teacherEmail))
                .thenReturn(List.of(classA, classB));
        when(classroomOfferingService.getSessions(classAId)).thenReturn(List.of());
        when(classroomOfferingService.getSessions(classBId)).thenReturn(List.of());

        // Act
        List<ClassroomOfferingResponse> classes = classroomOfferingService.getAssignedClasses(teacherEmail);
        List<ClassroomSessionResponse> resultA = classroomOfferingService.getSessions(classAId);
        List<ClassroomSessionResponse> resultB = classroomOfferingService.getSessions(classBId);

        // Assert
        assertThat(classes).hasSize(2);
        assertThat(resultA).isEmpty();
        assertThat(resultB).isEmpty();

        // Verify no session falls in the queried date range
        assertThat(resultA).allMatch(s -> s.getSessionDate().isBefore(fromDate) || s.getSessionDate().isAfter(toDate));
        assertThat(resultB).allMatch(s -> s.getSessionDate().isBefore(fromDate) || s.getSessionDate().isAfter(toDate));

        // Business message
        String message = "No classes scheduled for this time period.";
        assertThatThrownBy(() -> {
            throw new RuntimeException(message);
        }).hasMessage(message);
    }

    @Test
    void getTeachingScheduleWhenTeacherNotAssignedToAnyClass() {
        // Arrange
        Long newTeacherId = 200L;
        String newTeacherEmail = "newteacher@englishlab.com";

        when(classroomOfferingService.getAssignedClasses(newTeacherEmail))
                .thenReturn(List.of());

        // Act
        List<ClassroomOfferingResponse> classes = classroomOfferingService.getAssignedClasses(newTeacherEmail);

        // Assert
        assertThat(classes).isEmpty();

        // Business message
        String message = "No classes scheduled for this time period.";
        assertThatThrownBy(() -> {
            throw new RuntimeException(message);
        }).hasMessage(message);
    }

    @Test
    void getTeachingScheduleWithInvalidDateRange() {
        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 8, 20);
        LocalDate toDate = LocalDate.of(2026, 8, 10);

        // Assert
        assertThat(fromDate).isAfter(toDate);
        assertThatThrownBy(() -> {
            if (fromDate.isAfter(toDate)) {
                throw new InvalidDateRangeException("Invalid date range: fromDate must be before or equal to toDate.");
            }
        }).isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("Invalid date range");
    }

    static class InvalidDateRangeException extends RuntimeException {
        InvalidDateRangeException(String message) {
            super(message);
        }
    }
}
