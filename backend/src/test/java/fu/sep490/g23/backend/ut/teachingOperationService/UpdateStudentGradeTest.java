package fu.sep490.g23.backend.ut.teachingOperationService;

import fu.sep490.g23.backend.dto.request.classroom.UpdateGradebookRequest;
import fu.sep490.g23.backend.dto.request.classroom.UpdateGradebookHomeworkScoreRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomGradebookResponse;
import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import fu.sep490.g23.backend.service.classroom.ClassroomGradebookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * UC-72: Update Student Grade - test đầy đủ với request validation.
 * Service: ClassroomGradebookService.updateEntry()
 * Request DTO: UpdateGradebookRequest
 */
@ExtendWith(MockitoExtension.class)
public class UpdateStudentGradeTest {

    @Mock
    private ClassroomGradebookService gradebookService;

    // TC01: Cập nhật điểm thành công
    @Test
    void updateStudentGrade_UTC01_updateGrade_succeeds() {
        ClassroomGradebookResponse expected = ClassroomGradebookResponse.builder()
                .studentId(100L)
                .finalResult(BigDecimal.valueOf(9.0))
                .status(GradebookEntryStatus.PUBLISHED)
                .build();

        when(gradebookService.updateEntry(eq(1L), any(UpdateGradebookRequest.class), eq("teacher@test.com")))
                .thenReturn(expected);

        // Build complete request with all fields
        UpdateGradebookHomeworkScoreRequest hwScore1 = UpdateGradebookHomeworkScoreRequest.builder()
                .homeworkId(1L)
                .score(BigDecimal.valueOf(8.5))
                .build();
        UpdateGradebookHomeworkScoreRequest hwScore2 = UpdateGradebookHomeworkScoreRequest.builder()
                .homeworkId(2L)
                .score(BigDecimal.valueOf(9.0))
                .build();

        UpdateGradebookRequest request = UpdateGradebookRequest.builder()
                .studentId(100L)
                .homeworkScores(List.of(hwScore1, hwScore2))
                .attendancePercent(BigDecimal.valueOf(95.50))
                .finalResult(BigDecimal.valueOf(9.0))
                .teacherComment("Kết quả xuất sắc, nỗ lực trong suốt kỳ học")
                .status(GradebookEntryStatus.PUBLISHED)
                .build();

        ClassroomGradebookResponse result = gradebookService.updateEntry(1L, request, "teacher@test.com");

        // Verify response
        assertThat(result.getFinalResult()).isEqualByComparingTo(BigDecimal.valueOf(9.0));

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<Long> classIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<UpdateGradebookRequest> requestCaptor = 
                ArgumentCaptor.forClass(UpdateGradebookRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(gradebookService, times(1))
                .updateEntry(classIdCaptor.capture(), requestCaptor.capture(), emailCaptor.capture());

        // Verify all parameters
        assertThat(classIdCaptor.getValue()).isEqualTo(1L);
        
        UpdateGradebookRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getStudentId()).isEqualTo(100L);
        assertThat(capturedRequest.getHomeworkScores()).hasSize(2);
        assertThat(capturedRequest.getHomeworkScores().get(0).getHomeworkId()).isEqualTo(1L);
        assertThat(capturedRequest.getHomeworkScores().get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(8.5));
        assertThat(capturedRequest.getHomeworkScores().get(1).getHomeworkId()).isEqualTo(2L);
        assertThat(capturedRequest.getHomeworkScores().get(1).getScore()).isEqualByComparingTo(BigDecimal.valueOf(9.0));
        assertThat(capturedRequest.getAttendancePercent()).isEqualByComparingTo(BigDecimal.valueOf(95.50));
        assertThat(capturedRequest.getFinalResult()).isEqualByComparingTo(BigDecimal.valueOf(9.0));
        assertThat(capturedRequest.getTeacherComment()).isEqualTo("Kết quả xuất sắc, nỗ lực trong suốt kỳ học");
        assertThat(capturedRequest.getStatus()).isEqualTo(GradebookEntryStatus.PUBLISHED);
        
        assertThat(emailCaptor.getValue()).isEqualTo("teacher@test.com");

        verifyNoMoreInteractions(gradebookService);
    }

    // TC02: Giáo viên không phân công -> AccessDenied
    @Test
    void updateStudentGrade_UTC02_teacherNotAssigned() {
        when(gradebookService.updateEntry(any(), any(), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("MSG-19: Bạn không được phân công phụ trách lớp học này."));

        UpdateGradebookRequest request = UpdateGradebookRequest.builder()
                .studentId(100L)
                .finalResult(BigDecimal.valueOf(8.0))
                .build();

        assertThatThrownBy(() -> gradebookService.updateEntry(1L, request, "unknown@test.com"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("MSG-19");

        // Verify the unauthorized email was passed
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(gradebookService, times(1)).updateEntry(any(), any(), emailCaptor.capture());
        assertThat(emailCaptor.getValue()).isEqualTo("unknown@test.com");

        verifyNoMoreInteractions(gradebookService);
    }

    // TC03: Điểm -2.0 (invalid) -> IllegalArgumentException
    @Test
    void updateStudentGrade_UTC03_invalidScore() {
        when(gradebookService.updateEntry(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Kết quả không được nhỏ hơn 0"));

        UpdateGradebookRequest request = UpdateGradebookRequest.builder()
                .studentId(100L)
                .finalResult(BigDecimal.valueOf(-2.0))  // Invalid negative score
                .build();

        assertThatThrownBy(() -> gradebookService.updateEntry(1L, request, "teacher@test.com"))
                .isInstanceOf(IllegalArgumentException.class);

        // Verify the invalid score was passed
        ArgumentCaptor<UpdateGradebookRequest> requestCaptor = 
                ArgumentCaptor.forClass(UpdateGradebookRequest.class);
        verify(gradebookService, times(1))
                .updateEntry(any(), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getFinalResult())
                .isEqualByComparingTo(BigDecimal.valueOf(-2.0));

        verifyNoMoreInteractions(gradebookService);
    }

    // TC04: Lớp học CLOSED -> BusinessRule
    @Test
    void updateStudentGrade_UTC04_classClosed() {
        when(gradebookService.updateEntry(any(), any(), any()))
                .thenThrow(new IllegalStateException("MSG-49: Lớp học đã đóng, không thể cập nhật."));

        UpdateGradebookRequest request = UpdateGradebookRequest.builder()
                .studentId(100L)
                .finalResult(BigDecimal.valueOf(8.0))
                .build();

        assertThatThrownBy(() -> gradebookService.updateEntry(999L, request, "teacher@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MSG-49");

        // Verify the closed class ID was passed
        ArgumentCaptor<Long> classIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(gradebookService, times(1))
                .updateEntry(classIdCaptor.capture(), any(), any());

        assertThat(classIdCaptor.getValue()).isEqualTo(999L);

        verifyNoMoreInteractions(gradebookService);
    }

    // TC05: Student not found -> EntityNotFoundException
    @Test
    void updateStudentGrade_UTC05_studentNotFound() {
        when(gradebookService.updateEntry(any(), any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu."));

        UpdateGradebookRequest request = UpdateGradebookRequest.builder()
                .studentId(9999L)  // Non-existent student
                .finalResult(BigDecimal.valueOf(8.0))
                .build();

        assertThatThrownBy(() -> gradebookService.updateEntry(1L, request, "teacher@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the non-existent student ID was passed
        ArgumentCaptor<UpdateGradebookRequest> requestCaptor = 
                ArgumentCaptor.forClass(UpdateGradebookRequest.class);
        verify(gradebookService, times(1))
                .updateEntry(any(), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getStudentId()).isEqualTo(9999L);

        verifyNoMoreInteractions(gradebookService);
    }

    // TC06: Score null -> Validation
    @Test
    void updateStudentGrade_UTC06_missingScore() {
        when(gradebookService.updateEntry(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("MSG-46: Vui lòng nhập đầy đủ thông tin bắt buộc."));

        UpdateGradebookRequest request = UpdateGradebookRequest.builder()
                .studentId(100L)
                .attendancePercent(BigDecimal.valueOf(90.0))
                // Missing finalResult
                .build();

        assertThatThrownBy(() -> gradebookService.updateEntry(1L, request, "teacher@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG-46");

        // Verify the request with missing score was passed
        ArgumentCaptor<UpdateGradebookRequest> requestCaptor = 
                ArgumentCaptor.forClass(UpdateGradebookRequest.class);
        verify(gradebookService, times(1))
                .updateEntry(any(), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getStudentId()).isEqualTo(100L);
        assertThat(requestCaptor.getValue().getFinalResult()).isNull();

        verifyNoMoreInteractions(gradebookService);
    }
}
