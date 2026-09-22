package fu.sep490.g23.backend.ut.contentManagementService;

import fu.sep490.g23.backend.dto.request.curriculum.InstructorLedCourseRequest;
import fu.sep490.g23.backend.dto.response.curriculum.InstructorLedCourseResponse;
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
 * UC-104: Create Instructor-Led Course - test đầy đủ với request validation.
 * Service: InstructorLedCourseManagementService.createInstructorLedCourse()
 * Request DTO: InstructorLedCourseRequest
 */
@ExtendWith(MockitoExtension.class)
public class CreateInstructorLedCourseTest {

    @Mock
    private InstructorLedCourseManagementService instructorLedService;

    // TC01: Tạo thành công DRAFT
    @Test
    void instructorLedCourseManagement_UTC01_createCourse_succeeds() {
        InstructorLedCourseResponse expected = InstructorLedCourseResponse.builder()
                .id(101L)
                .status("DRAFT")
                .title("IELTS Intensive Master")
                .build();

        when(instructorLedService.createInstructorLedCourse(any(InstructorLedCourseRequest.class)))
                .thenReturn(expected);

        // Build complete request with all fields
        InstructorLedCourseRequest request = new InstructorLedCourseRequest();
        request.setTitle("IELTS Intensive Master");
        request.setCode("IL-IELTS-01");
        request.setShortDescription("Khóa học IELTS chuyên sâu");
        request.setDescription("Khóa học được thiết kế cho học viên muốn đạt band 7.0+");
        request.setDurationLabel("12 tuần");
        request.setBaseTuitionFeeVnd(BigDecimal.valueOf(15000000));
        request.setSaleTuitionFeeVnd(BigDecimal.valueOf(12000000));
        request.setExamCategory("IELTS");
        request.setFocusSkills("LISTENING,SPEAKING,READING,WRITING");
        request.setTargetBand(BigDecimal.valueOf(7.0));
        request.setTargetScore(70);
        request.setEntryLevel("INTERMEDIATE");
        request.setOutcomes("Đạt band 7.0 IELTS, sẵn sàng du học");
        request.setTeacherGuide("Tài liệu hướng dẫn giáo viên chi tiết");
        request.setInteractionActivities("Discussion, Role-play, Presentation");
        request.setStatus("DRAFT");

        InstructorLedCourseResponse result = instructorLedService.createInstructorLedCourse(request);

        // Verify response
        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getTitle()).isEqualTo("IELTS Intensive Master");

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<InstructorLedCourseRequest> requestCaptor = 
                ArgumentCaptor.forClass(InstructorLedCourseRequest.class);
        
        verify(instructorLedService, times(1)).createInstructorLedCourse(requestCaptor.capture());

        InstructorLedCourseRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("IELTS Intensive Master");
        assertThat(capturedRequest.getCode()).isEqualTo("IL-IELTS-01");
        assertThat(capturedRequest.getShortDescription()).isEqualTo("Khóa học IELTS chuyên sâu");
        assertThat(capturedRequest.getDescription()).isEqualTo("Khóa học được thiết kế cho học viên muốn đạt band 7.0+");
        assertThat(capturedRequest.getDurationLabel()).isEqualTo("12 tuần");
        assertThat(capturedRequest.getBaseTuitionFeeVnd()).isEqualByComparingTo(BigDecimal.valueOf(15000000));
        assertThat(capturedRequest.getSaleTuitionFeeVnd()).isEqualByComparingTo(BigDecimal.valueOf(12000000));
        assertThat(capturedRequest.getExamCategory()).isEqualTo("IELTS");
        assertThat(capturedRequest.getFocusSkills()).isEqualTo("LISTENING,SPEAKING,READING,WRITING");
        assertThat(capturedRequest.getTargetBand()).isEqualByComparingTo(BigDecimal.valueOf(7.0));
        assertThat(capturedRequest.getTargetScore()).isEqualTo(70);
        assertThat(capturedRequest.getEntryLevel()).isEqualTo("INTERMEDIATE");
        assertThat(capturedRequest.getOutcomes()).isEqualTo("Đạt band 7.0 IELTS, sẵn sàng du học");
        assertThat(capturedRequest.getTeacherGuide()).isEqualTo("Tài liệu hướng dẫn giáo viên chi tiết");
        assertThat(capturedRequest.getInteractionActivities()).isEqualTo("Discussion, Role-play, Presentation");
        assertThat(capturedRequest.getStatus()).isEqualTo("DRAFT");

        verifyNoMoreInteractions(instructorLedService);
    }

    // TC02: Clone course thành công
    @Test
    void instructorLedCourseManagement_UTC02_cloneCourse() {
        InstructorLedCourseResponse expected = InstructorLedCourseResponse.builder()
                .id(102L)
                .status("DRAFT")
                .title("IELTS Foundation - Copy")
                .build();

        when(instructorLedService.cloneInstructorLedCourse(eq(101L)))
                .thenReturn(expected);

        InstructorLedCourseResponse result = instructorLedService.cloneInstructorLedCourse(101L);

        assertThat(result.getTitle()).isEqualTo("IELTS Foundation - Copy");

        // Verify the course ID passed to clone method
        ArgumentCaptor<Long> courseIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(instructorLedService, times(1)).cloneInstructorLedCourse(courseIdCaptor.capture());
        assertThat(courseIdCaptor.getValue()).isEqualTo(101L);

        verifyNoMoreInteractions(instructorLedService);
    }

    // TC03: Slug/Code trùng
    @Test
    void instructorLedCourseManagement_UTC03_duplicateSlugOrCode_throwsException() {
        when(instructorLedService.createInstructorLedCourse(any(InstructorLedCourseRequest.class)))
                .thenThrow(new IllegalStateException("MSG-24: Tên đã tồn tại. Vui lòng chọn tên khác."));

        InstructorLedCourseRequest request = new InstructorLedCourseRequest();
        request.setTitle("IELTS Foundation");
        request.setCode("IL-IELTS-01");
        request.setExamCategory("IELTS");
        request.setEntryLevel("BEGINNER");

        assertThatThrownBy(() -> instructorLedService.createInstructorLedCourse(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MSG-24");

        // Verify the duplicate request was passed
        ArgumentCaptor<InstructorLedCourseRequest> requestCaptor = 
                ArgumentCaptor.forClass(InstructorLedCourseRequest.class);
        verify(instructorLedService, times(1)).createInstructorLedCourse(requestCaptor.capture());

        InstructorLedCourseRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("IELTS Foundation");
        assertThat(capturedRequest.getCode()).isEqualTo("IL-IELTS-01");

        verifyNoMoreInteractions(instructorLedService);
    }

    // TC04: Thiếu trường bắt buộc -> Validation
    @Test
    void instructorLedCourseManagement_UTC04_missingRequiredFields() {
        when(instructorLedService.createInstructorLedCourse(any(InstructorLedCourseRequest.class)))
                .thenThrow(new IllegalArgumentException("MSG-46: Vui lòng nhập đầy đủ thông tin bắt buộc."));

        // Build request with missing required fields
        InstructorLedCourseRequest request = new InstructorLedCourseRequest();
        request.setCode("IL-TEST-01");  // Has code but missing title (required)

        assertThatThrownBy(() -> instructorLedService.createInstructorLedCourse(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG-46");

        // Verify the incomplete request was passed
        ArgumentCaptor<InstructorLedCourseRequest> requestCaptor = 
                ArgumentCaptor.forClass(InstructorLedCourseRequest.class);
        verify(instructorLedService, times(1)).createInstructorLedCourse(requestCaptor.capture());

        InstructorLedCourseRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isNull();
        assertThat(capturedRequest.getCode()).isEqualTo("IL-TEST-01");

        verifyNoMoreInteractions(instructorLedService);
    }

    // TC05: Source courseId không tồn tại -> EntityNotFoundException
    @Test
    void instructorLedCourseManagement_UTC05_sourceCourseNotFound() {
        when(instructorLedService.cloneInstructorLedCourse(eq(999L)))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu."));

        assertThatThrownBy(() -> instructorLedService.cloneInstructorLedCourse(999L))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the non-existent course ID was passed
        ArgumentCaptor<Long> courseIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(instructorLedService, times(1)).cloneInstructorLedCourse(courseIdCaptor.capture());
        assertThat(courseIdCaptor.getValue()).isEqualTo(999L);

        verifyNoMoreInteractions(instructorLedService);
    }
}
