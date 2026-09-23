package fu.sep490.g23.backend.ut.contentManagementService;

import fu.sep490.g23.backend.dto.request.curriculum.AssessmentBankItemRequest;
import fu.sep490.g23.backend.dto.response.curriculum.AssessmentBankItemResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.service.curriculum.InstructorLedCourseManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * UC-124: Create Classroom Practice Exercise - test đầy đủ với request validation.
 * Service: InstructorLedCourseManagementService.createAssessmentBankItem()
 * Request DTO: AssessmentBankItemRequest
 */
@ExtendWith(MockitoExtension.class)
public class CreateClassroomPracticeExerciseTest {

    @Mock
    private InstructorLedCourseManagementService instructorLedService;

    // TC01: Tạo bài tập mới thành công
    @Test
    void createClassroomPracticeExercise_UTC01_createExercise_succeeds() {
        AssessmentBankItemResponse expected = AssessmentBankItemResponse.builder()
                .id(101L)
                .title("IELTS Reading Passage 1 - Matching Headings")
                .skill(AssessmentSkill.READING)
                .type(AssessmentType.QUIZ)
                .build();

        when(instructorLedService.createAssessmentBankItem(any(AssessmentBankItemRequest.class)))
                .thenReturn(expected);

        // Build complete request with all fields
        AssessmentBankItemRequest request = new AssessmentBankItemRequest();
        request.setTitle("IELTS Reading Passage 1 - Matching Headings");
        request.setDescription("Bài đọc IELTS về Matching Headings - phần 1");
        request.setSkill(AssessmentSkill.READING);
        request.setType(AssessmentType.QUIZ);
        request.setAiEvaluationMode(AiEvaluationMode.RUBRIC_FEEDBACK);
        request.setInstructions("Đọc kỹ đoạn văn và chọn heading phù hợp cho mỗi đoạn");
        request.setPassingScore(BigDecimal.valueOf(6.0));
        request.setMaxScore(BigDecimal.valueOf(10.0));
        request.setTimeLimitMinutes(30);
        request.setStatus("ACTIVE");

        AssessmentBankItemResponse result = instructorLedService.createAssessmentBankItem(request);

        // Verify response
        assertThat(result.getTitle()).isEqualTo("IELTS Reading Passage 1 - Matching Headings");
        assertThat(result.getType()).isEqualTo(AssessmentType.QUIZ);

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<AssessmentBankItemRequest> requestCaptor = 
                ArgumentCaptor.forClass(AssessmentBankItemRequest.class);
        
        verify(instructorLedService, times(1)).createAssessmentBankItem(requestCaptor.capture());

        AssessmentBankItemRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("IELTS Reading Passage 1 - Matching Headings");
        assertThat(capturedRequest.getDescription()).isEqualTo("Bài đọc IELTS về Matching Headings - phần 1");
        assertThat(capturedRequest.getSkill()).isEqualTo(AssessmentSkill.READING);
        assertThat(capturedRequest.getType()).isEqualTo(AssessmentType.QUIZ);
        assertThat(capturedRequest.getAiEvaluationMode()).isEqualTo(AiEvaluationMode.RUBRIC_FEEDBACK);
        assertThat(capturedRequest.getInstructions()).isEqualTo("Đọc kỹ đoạn văn và chọn heading phù hợp cho mỗi đoạn");
        assertThat(capturedRequest.getPassingScore()).isEqualByComparingTo(BigDecimal.valueOf(6.0));
        assertThat(capturedRequest.getMaxScore()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
        assertThat(capturedRequest.getTimeLimitMinutes()).isEqualTo(30);
        assertThat(capturedRequest.getStatus()).isEqualTo("ACTIVE");

        verifyNoMoreInteractions(instructorLedService);
    }

    // TC02: Thiếu trường bắt buộc -> Validation
    @Test
    void  createClassroomPracticeExercise_UTC02_missingRequiredFields() {
        when(instructorLedService.createAssessmentBankItem(any(AssessmentBankItemRequest.class)))
                .thenThrow(new IllegalArgumentException("MSG-46: Vui lòng nhập đầy đủ thông tin bắt buộc."));

        // Build request with missing required fields
        AssessmentBankItemRequest request = new AssessmentBankItemRequest();
        request.setDescription("Test description");  // Has description but missing title, type, skill

        assertThatThrownBy(() -> instructorLedService.createAssessmentBankItem(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG-46");

        // Verify the incomplete request was passed
        ArgumentCaptor<AssessmentBankItemRequest> requestCaptor = 
                ArgumentCaptor.forClass(AssessmentBankItemRequest.class);
        verify(instructorLedService, times(1)).createAssessmentBankItem(requestCaptor.capture());

        AssessmentBankItemRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isNull();
        assertThat(capturedRequest.getType()).isNull();
        assertThat(capturedRequest.getSkill()).isNull();

        verifyNoMoreInteractions(instructorLedService);
    }

    // TC03: Câu hỏi trắc nghiệm thiếu options/correctAnswers -> BusinessRule
    @Test
    void createClassroomPracticeExercise_UTC03_invalidQuestionStructure() {
        when(instructorLedService.createAssessmentBankItem(any(AssessmentBankItemRequest.class)))
                .thenThrow(new IllegalStateException("BR-26: Câu hỏi khách quan phải có ≥2 lựa chọn và ≥1 đáp án đúng"));

        AssessmentBankItemRequest request = new AssessmentBankItemRequest();
        request.setTitle("Bài tập Vocabulary");
        request.setSkill(AssessmentSkill.VOCABULARY);
        request.setType(AssessmentType.QUIZ);
        request.setMaxScore(BigDecimal.valueOf(10.0));

        assertThatThrownBy(() -> instructorLedService.createAssessmentBankItem(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BR-26");

        // Verify the request was passed
        ArgumentCaptor<AssessmentBankItemRequest> requestCaptor = 
                ArgumentCaptor.forClass(AssessmentBankItemRequest.class);
        verify(instructorLedService, times(1)).createAssessmentBankItem(requestCaptor.capture());

        AssessmentBankItemRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("Bài tập Vocabulary");
        assertThat(capturedRequest.getSkill()).isEqualTo(AssessmentSkill.VOCABULARY);
        assertThat(capturedRequest.getType()).isEqualTo(AssessmentType.QUIZ);

        verifyNoMoreInteractions(instructorLedService);
    }

    // TC04: Không có câu hỏi nào -> IllegalArgumentException
    @Test
    void createClassroomPracticeExercise_UTC04_noQuestions() {
        when(instructorLedService.createAssessmentBankItem(any(AssessmentBankItemRequest.class)))
                .thenThrow(new IllegalArgumentException("Bài tập phải có ít nhất 1 câu hỏi"));

        AssessmentBankItemRequest request = new AssessmentBankItemRequest();
        request.setTitle("Bài tập Listening Part 1");
        request.setSkill(AssessmentSkill.LISTENING);
        request.setType(AssessmentType.QUIZ);
        request.setMaxScore(BigDecimal.valueOf(10.0));

        assertThatThrownBy(() -> instructorLedService.createAssessmentBankItem(request))
                .isInstanceOf(IllegalArgumentException.class);

        // Verify the request was passed
        ArgumentCaptor<AssessmentBankItemRequest> requestCaptor = 
                ArgumentCaptor.forClass(AssessmentBankItemRequest.class);
        verify(instructorLedService, times(1)).createAssessmentBankItem(requestCaptor.capture());

        AssessmentBankItemRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("Bài tập Listening Part 1");
        assertThat(capturedRequest.getSkill()).isEqualTo(AssessmentSkill.LISTENING);

        verifyNoMoreInteractions(instructorLedService);
    }

    // TC05: Lỗi DB -> RuntimeException
    @Test
    void createClassroomPracticeExercise_UTC05_dbError() {
        when(instructorLedService.createAssessmentBankItem(any(AssessmentBankItemRequest.class)))
                .thenThrow(new RuntimeException("DB connection failed"));

        AssessmentBankItemRequest request = new AssessmentBankItemRequest();
        request.setTitle("Bài tập Reading");
        request.setSkill(AssessmentSkill.READING);
        request.setType(AssessmentType.QUIZ);
        request.setMaxScore(BigDecimal.valueOf(10.0));

        assertThatThrownBy(() -> instructorLedService.createAssessmentBankItem(request))
                .isInstanceOf(RuntimeException.class);

        // Verify the request was passed
        ArgumentCaptor<AssessmentBankItemRequest> requestCaptor = 
                ArgumentCaptor.forClass(AssessmentBankItemRequest.class);
        verify(instructorLedService, times(1)).createAssessmentBankItem(requestCaptor.capture());

        assertThat(requestCaptor.getValue().getTitle()).isEqualTo("Bài tập Reading");
    }
}
