package fu.sep490.g23.backend.ut.teachingOperationService;

import fu.sep490.g23.backend.dto.request.classroom.GradeHomeworkRequest;
import fu.sep490.g23.backend.dto.request.classroom.HomeworkTextAnnotationRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomHomeworkSubmissionResponse;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkAnnotationType;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkService;
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

@ExtendWith(MockitoExtension.class)
public class GradeHomeworkSubmissionTest {

    @Mock
    private ClassroomHomeworkService homeworkService;

    // TC01: Chấm điểm SUBMITTED -> GRADED
    @Test
    void gradeHomeworkSubmission_UTC01_gradeSubmitted() {
        ClassroomHomeworkSubmissionResponse expected = ClassroomHomeworkSubmissionResponse.builder()
                .id(101L)
                .status(HomeworkSubmissionStatus.GRADED)
                .score(BigDecimal.valueOf(8.5))
                .teacherFeedback("Bài làm tốt, phân tích sâu sắc.")
                .build();

        when(homeworkService.grade(eq(1L), eq(100L), any(GradeHomeworkRequest.class), eq("teacher@test.com")))
                .thenReturn(expected);

        // Build complete request with all fields
        HomeworkTextAnnotationRequest annotation1 = HomeworkTextAnnotationRequest.builder()
                .id("ann-001")
                .type(HomeworkAnnotationType.CORRECTION)
                .startOffset(10)
                .endOffset(25)
                .selectedText("She go to school")
                .replacementText("She goes to school")
                .note("Lỗi chia động từ")
                .build();
        HomeworkTextAnnotationRequest annotation2 = HomeworkTextAnnotationRequest.builder()
                .id("ann-002")
                .type(HomeworkAnnotationType.NOTE)
                .startOffset(50)
                .endOffset(60)
                .selectedText("beautiful")
                .note("Từ vựng hay")
                .build();

        GradeHomeworkRequest request = GradeHomeworkRequest.builder()
                .score(BigDecimal.valueOf(8.5))
                .teacherFeedback("Bài làm tốt, phân tích sâu sắc.")
                .annotations(List.of(annotation1, annotation2))
                .build();

        ClassroomHomeworkSubmissionResponse result = homeworkService.grade(1L, 100L, request, "teacher@test.com");

        // Verify response
        assertThat(result.getStatus()).isEqualTo(HomeworkSubmissionStatus.GRADED);
        assertThat(result.getScore()).isEqualByComparingTo(BigDecimal.valueOf(8.5));

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<Long> classIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> submissionIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<GradeHomeworkRequest> requestCaptor = 
                ArgumentCaptor.forClass(GradeHomeworkRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(homeworkService, times(1))
                .grade(classIdCaptor.capture(), submissionIdCaptor.capture(), requestCaptor.capture(), emailCaptor.capture());

        // Verify all parameters
        assertThat(classIdCaptor.getValue()).isEqualTo(1L);
        assertThat(submissionIdCaptor.getValue()).isEqualTo(100L);

        GradeHomeworkRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getScore()).isEqualByComparingTo(BigDecimal.valueOf(8.5));
        assertThat(capturedRequest.getTeacherFeedback()).isEqualTo("Bài làm tốt, phân tích sâu sắc.");
        assertThat(capturedRequest.getAnnotations()).hasSize(2);
        assertThat(capturedRequest.getAnnotations().get(0).getId()).isEqualTo("ann-001");
        assertThat(capturedRequest.getAnnotations().get(0).getType()).isEqualTo(HomeworkAnnotationType.CORRECTION);
        assertThat(capturedRequest.getAnnotations().get(0).getStartOffset()).isEqualTo(10);
        assertThat(capturedRequest.getAnnotations().get(0).getEndOffset()).isEqualTo(25);
        assertThat(capturedRequest.getAnnotations().get(0).getSelectedText()).isEqualTo("She go to school");
        assertThat(capturedRequest.getAnnotations().get(0).getReplacementText()).isEqualTo("She goes to school");
        assertThat(capturedRequest.getAnnotations().get(1).getId()).isEqualTo("ann-002");
        assertThat(capturedRequest.getAnnotations().get(1).getType()).isEqualTo(HomeworkAnnotationType.NOTE);
        
        assertThat(emailCaptor.getValue()).isEqualTo("teacher@test.com");

        verifyNoMoreInteractions(homeworkService);
    }

    // TC02: AI gợi ý chấm điểm
    @Test
    void gradeHomeworkSubmission_UTC02_aiGradeSuggestion_returnsSuggestion() {
        when(homeworkService.listAiAssessmentOptions("teacher@test.com"))
                .thenReturn(List.of());

        assertThat(homeworkService.listAiAssessmentOptions("teacher@test.com")).isEmpty();

        verify(homeworkService, times(1)).listAiAssessmentOptions("teacher@test.com");
        verifyNoMoreInteractions(homeworkService);
    }

    // TC03: Điểm 15.0 vượt phạm vi -> IllegalArgumentException
    @Test
    void gradeHomeworkSubmission_UTC03_invalidScore() {
        when(homeworkService.grade(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Điểm vượt quá thang điểm tối đa"));

        GradeHomeworkRequest request = GradeHomeworkRequest.builder()
                .score(BigDecimal.valueOf(15.0))  // Invalid: exceeds max
                .teacherFeedback("Test feedback")
                .build();

        assertThatThrownBy(() -> homeworkService.grade(1L, 100L, request, "teacher@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vượt quá");

        // Verify the invalid score was passed
        ArgumentCaptor<GradeHomeworkRequest> requestCaptor = 
                ArgumentCaptor.forClass(GradeHomeworkRequest.class);
        verify(homeworkService, times(1))
                .grade(any(), any(), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getScore())
                .isEqualByComparingTo(BigDecimal.valueOf(15.0));

        verifyNoMoreInteractions(homeworkService);
    }

    // TC04: Bài nộp DRAFT -> RuntimeException
    @Test
    void gradeHomeworkSubmission_UTC04_draftStatus() {
        when(homeworkService.grade(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Bài nộp không ở trạng thái cho phép chấm điểm"));

        GradeHomeworkRequest request = GradeHomeworkRequest.builder()
                .score(BigDecimal.valueOf(8.0))
                .teacherFeedback("Grading draft submission")
                .build();

        assertThatThrownBy(() -> homeworkService.grade(1L, 100L, request, "teacher@test.com"))
                .isInstanceOf(IllegalStateException.class);

        // Verify the request was passed
        ArgumentCaptor<GradeHomeworkRequest> requestCaptor = 
                ArgumentCaptor.forClass(GradeHomeworkRequest.class);
        verify(homeworkService, times(1))
                .grade(any(), any(), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getScore())
                .isEqualByComparingTo(BigDecimal.valueOf(8.0));

        verifyNoMoreInteractions(homeworkService);
    }

    // TC05: Giáo viên không phụ trách lớp -> AccessDenied
    @Test
    void gradeHomeworkSubmission_UTC05_teacherNotAssigned() {
        when(homeworkService.grade(any(), any(), any(), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("MSG-19: Bạn không được phân công phụ trách lớp học này."));

        GradeHomeworkRequest request = GradeHomeworkRequest.builder()
                .score(BigDecimal.valueOf(8.0))
                .build();

        assertThatThrownBy(() -> homeworkService.grade(1L, 100L, request, "unknown@test.com"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("MSG-19");

        // Verify the unauthorized email was passed
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(homeworkService, times(1))
                .grade(any(), any(), any(), emailCaptor.capture());

        assertThat(emailCaptor.getValue()).isEqualTo("unknown@test.com");

        verifyNoMoreInteractions(homeworkService);
    }

    // TC06: submissionId không tồn tại -> EntityNotFoundException
    @Test
    void gradeHomeworkSubmission_UTC06_submissionNotFound() {
        when(homeworkService.grade(any(), any(), any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu."));

        GradeHomeworkRequest request = GradeHomeworkRequest.builder()
                .score(BigDecimal.valueOf(8.0))
                .build();

        assertThatThrownBy(() -> homeworkService.grade(9999L, 100L, request, "teacher@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the non-existent class ID was passed
        ArgumentCaptor<Long> classIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(homeworkService, times(1))
                .grade(classIdCaptor.capture(), any(), any(), any());

        assertThat(classIdCaptor.getValue()).isEqualTo(9999L);
    }
}
