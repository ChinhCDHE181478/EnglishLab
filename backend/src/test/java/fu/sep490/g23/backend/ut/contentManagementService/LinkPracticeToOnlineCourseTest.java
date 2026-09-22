package fu.sep490.g23.backend.ut.contentManagementService;

import fu.sep490.g23.backend.dto.request.assessment.ContentManagerCourseAssessmentRequest;
import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
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
 * UC-118: Link Practice to Online Course - test đầy đủ với request validation.
 * Service: OnlineCourseService.saveManagerCourseAssessments()
 * Request DTO: ContentManagerCourseAssessmentRequest
 */
@ExtendWith(MockitoExtension.class)
public class LinkPracticeToOnlineCourseTest {

    @Mock
    private OnlineCourseService onlineCourseService;

    // TC01: Tạo & cấu hình bài đánh giá mới thành công
    @Test
    void linkPracticeToOnlineCourse_UTC01_createAssessment() {
        CourseAssessmentResponse expected = CourseAssessmentResponse.builder()
                .id(201L)
                .title("Bài kiểm tra cuối Module 1")
                .build();

        when(onlineCourseService.saveManagerCourseAssessments(eq(101L), any(), eq("manager@test.com")))
                .thenReturn(List.of(expected));

        // Build complete request with all fields
        ContentManagerCourseAssessmentRequest req = ContentManagerCourseAssessmentRequest.builder()
                .id(null) // New assessment, no existing id
                .moduleId(1L)
                .lessonId(10L)
                .rubricId(100L)
                .title("Bài kiểm tra cuối Module 1")
                .description("Bài kiểm tra tổng hợp các kiến thức đã học trong Module 1")
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.MIXED)
                .aiEvaluationMode(AiEvaluationMode.RUBRIC_FEEDBACK)
                .instructions("Làm bài trong 60 phút. Không sử dụng tài liệu.")
                .passingScore(BigDecimal.valueOf(7.0))
                .maxScore(BigDecimal.valueOf(10.0))
                .timeLimitMinutes(60)
                .displayOrder(1)
                .active(true)
                .build();

        List<CourseAssessmentResponse> result = onlineCourseService.saveManagerCourseAssessments(
                101L, List.of(req), "manager@test.com");

        // Verify response
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Bài kiểm tra cuối Module 1");

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<Long> courseIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List<ContentManagerCourseAssessmentRequest>> requestsCaptor = 
                ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(onlineCourseService, times(1))
                .saveManagerCourseAssessments(courseIdCaptor.capture(), requestsCaptor.capture(), emailCaptor.capture());

        // Verify course ID
        assertThat(courseIdCaptor.getValue()).isEqualTo(101L);
        
        // Verify email
        assertThat(emailCaptor.getValue()).isEqualTo("manager@test.com");
        
        // Verify request list
        List<ContentManagerCourseAssessmentRequest> capturedRequests = requestsCaptor.getValue();
        assertThat(capturedRequests).hasSize(1);
        
        ContentManagerCourseAssessmentRequest captured = capturedRequests.get(0);
        assertThat(captured.getModuleId()).isEqualTo(1L);
        assertThat(captured.getLessonId()).isEqualTo(10L);
        assertThat(captured.getRubricId()).isEqualTo(100L);
        assertThat(captured.getTitle()).isEqualTo("Bài kiểm tra cuối Module 1");
        assertThat(captured.getDescription()).isEqualTo("Bài kiểm tra tổng hợp các kiến thức đã học trong Module 1");
        assertThat(captured.getType()).isEqualTo(AssessmentType.MODULE_TEST);
        assertThat(captured.getSkill()).isEqualTo(AssessmentSkill.MIXED);
        assertThat(captured.getAiEvaluationMode()).isEqualTo(AiEvaluationMode.RUBRIC_FEEDBACK);
        assertThat(captured.getInstructions()).isEqualTo("Làm bài trong 60 phút. Không sử dụng tài liệu.");
        assertThat(captured.getPassingScore()).isEqualByComparingTo(BigDecimal.valueOf(7.0));
        assertThat(captured.getMaxScore()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
        assertThat(captured.getTimeLimitMinutes()).isEqualTo(60);
        assertThat(captured.getDisplayOrder()).isEqualTo(1);
        assertThat(captured.getActive()).isTrue();

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC02: Nhập từ Assessment Bank / Rubric thành công
    @Test
    void linkPracticeToOnlineCourse_UTC02_linkFromBank() {
        CourseAssessmentResponse expected = CourseAssessmentResponse.builder()
                .id(202L)
                .title("IELTS Writing Task 1 Practice")
                .build();

        when(onlineCourseService.saveManagerCourseAssessments(eq(101L), any(), eq("manager@test.com")))
                .thenReturn(List.of(expected));

        // Build request with linked bank item
        ContentManagerCourseAssessmentRequest req = ContentManagerCourseAssessmentRequest.builder()
                .moduleId(2L)
                .assessmentBankItemId(500L) // Link from existing bank item
                .rubricId(200L)
                .title("IELTS Writing Task 1 Practice")
                .type(AssessmentType.WRITING_TASK)
                .skill(AssessmentSkill.WRITING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .passingScore(BigDecimal.valueOf(6.0))
                .maxScore(BigDecimal.valueOf(9.0))
                .displayOrder(1)
                .active(true)
                .build();

        List<CourseAssessmentResponse> result = onlineCourseService.saveManagerCourseAssessments(
                101L, List.of(req), "manager@test.com");

        assertThat(result).hasSize(1);

        // CRITICAL: Verify request data
        ArgumentCaptor<List<ContentManagerCourseAssessmentRequest>> requestsCaptor = 
                ArgumentCaptor.forClass(List.class);
        verify(onlineCourseService, times(1))
                .saveManagerCourseAssessments(eq(101L), requestsCaptor.capture(), eq("manager@test.com"));

        List<ContentManagerCourseAssessmentRequest> capturedRequests = requestsCaptor.getValue();
        ContentManagerCourseAssessmentRequest captured = capturedRequests.get(0);
        assertThat(captured.getModuleId()).isEqualTo(2L);
        assertThat(captured.getAssessmentBankItemId()).isEqualTo(500L);
        assertThat(captured.getRubricId()).isEqualTo(200L);
        assertThat(captured.getTitle()).isEqualTo("IELTS Writing Task 1 Practice");
        assertThat(captured.getType()).isEqualTo(AssessmentType.WRITING_TASK);
        assertThat(captured.getSkill()).isEqualTo(AssessmentSkill.WRITING);
        assertThat(captured.getAiEvaluationMode()).isEqualTo(AiEvaluationMode.ESTIMATED_BAND);

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC03: passingScore > maxScore -> IllegalArgumentException
    @Test
    void linkPracticeToOnlineCourse_UTC03_passingExceedsMax() {
        when(onlineCourseService.saveManagerCourseAssessments(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("passingScore lớn hơn maxScore"));

        // Build request with invalid scores
        ContentManagerCourseAssessmentRequest req = ContentManagerCourseAssessmentRequest.builder()
                .moduleId(1L)
                .title("Test")
                .type(AssessmentType.QUIZ)
                .skill(AssessmentSkill.READING)
                .aiEvaluationMode(AiEvaluationMode.RUBRIC_FEEDBACK)
                .passingScore(BigDecimal.valueOf(10.0))  // Invalid: > maxScore
                .maxScore(BigDecimal.valueOf(8.0))
                .build();

        assertThatThrownBy(() -> onlineCourseService.saveManagerCourseAssessments(
                101L, List.of(req), "manager@test.com"))
                .isInstanceOf(IllegalArgumentException.class);

        // Verify the invalid request was passed
        ArgumentCaptor<List<ContentManagerCourseAssessmentRequest>> requestsCaptor = 
                ArgumentCaptor.forClass(List.class);
        verify(onlineCourseService, times(1))
                .saveManagerCourseAssessments(eq(101L), requestsCaptor.capture(), any());

        List<ContentManagerCourseAssessmentRequest> capturedRequests = requestsCaptor.getValue();
        assertThat(capturedRequests.get(0).getPassingScore())
                .isEqualByComparingTo(BigDecimal.valueOf(10.0));
        assertThat(capturedRequests.get(0).getMaxScore())
                .isEqualByComparingTo(BigDecimal.valueOf(8.0));

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC05: Khóa học đã published, không có draft -> BusinessRule
    @Test
    void linkPracticeToOnlineCourse_UTC04_publishedCourseNoDraft() {
        when(onlineCourseService.saveManagerCourseAssessments(any(), any(), any()))
                .thenThrow(new IllegalStateException("Khóa học đã xuất bản, cần tạo bản nháp"));

        ContentManagerCourseAssessmentRequest req = ContentManagerCourseAssessmentRequest.builder()
                .moduleId(1L)
                .title("Test")
                .type(AssessmentType.QUIZ)
                .skill(AssessmentSkill.READING)
                .aiEvaluationMode(AiEvaluationMode.RUBRIC_FEEDBACK)
                .build();

        assertThatThrownBy(() -> onlineCourseService.saveManagerCourseAssessments(
                999L, List.of(req), "manager@test.com"))
                .isInstanceOf(IllegalStateException.class);

        // Verify the course ID that caused the error
        ArgumentCaptor<Long> courseIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(onlineCourseService, times(1))
                .saveManagerCourseAssessments(courseIdCaptor.capture(), any(), any());
        
        assertThat(courseIdCaptor.getValue()).isEqualTo(999L);

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC05: Thiếu trường bắt buộc -> Validation
    @Test
    void linkPracticeToOnlineCourse_UTC05_missingFields() {
        when(onlineCourseService.saveManagerCourseAssessments(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("MSG-46: Vui lòng nhập đầy đủ thông tin bắt buộc."));

        // Build request missing required fields
        ContentManagerCourseAssessmentRequest req = ContentManagerCourseAssessmentRequest.builder()
                .moduleId(1L)
                .title("Test Assessment")
                // Missing: type, skill, aiEvaluationMode (required fields)
                .build();

        assertThatThrownBy(() -> onlineCourseService.saveManagerCourseAssessments(
                101L, List.of(req), "manager@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG-46");

        // Verify the incomplete request was passed
        ArgumentCaptor<List<ContentManagerCourseAssessmentRequest>> requestsCaptor = 
                ArgumentCaptor.forClass(List.class);
        verify(onlineCourseService, times(1))
                .saveManagerCourseAssessments(eq(101L), requestsCaptor.capture(), any());

        List<ContentManagerCourseAssessmentRequest> capturedRequests = requestsCaptor.getValue();
        ContentManagerCourseAssessmentRequest captured = capturedRequests.get(0);
        assertThat(captured.getModuleId()).isEqualTo(1L);
        assertThat(captured.getTitle()).isEqualTo("Test Assessment");
        assertThat(captured.getType()).isNull();
        assertThat(captured.getSkill()).isNull();
        assertThat(captured.getAiEvaluationMode()).isNull();

        verifyNoMoreInteractions(onlineCourseService);
    }

    // TC06: courseDraftId không tồn tại -> EntityNotFoundException
    @Test
    void linkPracticeToOnlineCourse_UTC06_draftNotFound() {
        when(onlineCourseService.saveManagerCourseAssessments(any(), any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu."));

        ContentManagerCourseAssessmentRequest req = ContentManagerCourseAssessmentRequest.builder()
                .moduleId(1L)
                .title("Test")
                .type(AssessmentType.QUIZ)
                .skill(AssessmentSkill.READING)
                .aiEvaluationMode(AiEvaluationMode.RUBRIC_FEEDBACK)
                .build();

        assertThatThrownBy(() -> onlineCourseService.saveManagerCourseAssessments(
                9999L, List.of(req), "manager@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the non-existent course ID was passed
        ArgumentCaptor<Long> courseIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(onlineCourseService, times(1))
                .saveManagerCourseAssessments(courseIdCaptor.capture(), any(), any());
        
        assertThat(courseIdCaptor.getValue()).isEqualTo(9999L);
    }
}
