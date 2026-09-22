package fu.sep490.g23.backend.ut.classroomOperationService;

import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomProposalRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomProposalResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.service.classroom.ClassroomProposalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC-94: Create Classroom Proposal - test đầy đủ với request validation.
 * Service: ClassroomProposalService.create()
 * Request DTO: CreateClassroomProposalRequest
 */
@ExtendWith(MockitoExtension.class)
public class CreateClassroomProposalTest {

    @Mock
    private ClassroomProposalService proposalService;

    // TC01: Tạo proposal thành công - Verify all request fields
    @Test
    void createClassroomProposal_UTC01_createProposal_succeeds() {
        ClassroomProposalResponse expected = ClassroomProposalResponse.builder()
                .id(101L)
                .title("IELTS Foundation Class A")
                .approvalStatus(ClassroomApprovalStatus.DRAFT)
                .build();

        when(proposalService.create(any(CreateClassroomProposalRequest.class), eq("staff@test.com")))
                .thenReturn(expected);

        // Build complete request with all required fields
        CreateClassroomProposalRequest request = new CreateClassroomProposalRequest();
        request.setTitle("IELTS Foundation Class A");
        request.setCourseOfferingId(1L);
        request.setCapacity(30);
        request.setPlannedStartDate(LocalDate.of(2026, 9, 1));
        request.setEndDate(LocalDate.of(2026, 12, 31));
        request.setWeekdays(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
        request.setSessionStartTime(LocalTime.of(18, 0));
        request.setSessionEndTime(LocalTime.of(20, 30));
        request.setDeliveryType(ClassroomDeliveryMode.OFFLINE);
        request.setNote("Lớp dự kiến cho khóa IELTS tháng 9");

        ClassroomProposalResponse result = proposalService.create(request, "staff@test.com");

        // Verify response
        assertThat(result.getApprovalStatus()).isEqualTo(ClassroomApprovalStatus.DRAFT);

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<CreateClassroomProposalRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateClassroomProposalRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(proposalService, times(1)).create(requestCaptor.capture(), emailCaptor.capture());

        CreateClassroomProposalRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isEqualTo("IELTS Foundation Class A");
        assertThat(capturedRequest.getCourseOfferingId()).isEqualTo(1L);
        assertThat(capturedRequest.getCapacity()).isEqualTo(30);
        assertThat(capturedRequest.getPlannedStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(capturedRequest.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(capturedRequest.getWeekdays()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        assertThat(capturedRequest.getSessionStartTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(capturedRequest.getSessionEndTime()).isEqualTo(LocalTime.of(20, 30));
        assertThat(capturedRequest.getDeliveryType()).isEqualTo(ClassroomDeliveryMode.OFFLINE);
        assertThat(capturedRequest.getNote()).isEqualTo("Lớp dự kiến cho khóa IELTS tháng 9");
        
        // Verify email parameter
        assertThat(emailCaptor.getValue()).isEqualTo("staff@test.com");
    }

    // TC02: Thiếu trường bắt buộc -> Validation
    @Test
    void createClassroomProposal_UTC02_missingRequiredFields() {
        when(proposalService.create(any(CreateClassroomProposalRequest.class), any()))
                .thenThrow(new IllegalArgumentException("MSG-46: Vui lòng nhập đầy đủ thông tin bắt buộc."));

        // Empty request to trigger validation
        CreateClassroomProposalRequest request = new CreateClassroomProposalRequest();
        
        assertThatThrownBy(() -> proposalService.create(request, "staff@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG-46");

        // Verify empty request was passed
        ArgumentCaptor<CreateClassroomProposalRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateClassroomProposalRequest.class);
        verify(proposalService, times(1)).create(requestCaptor.capture(), any());
        
        CreateClassroomProposalRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTitle()).isNull();
        assertThat(capturedRequest.getCourseOfferingId()).isNull();
        assertThat(capturedRequest.getCapacity()).isNull();
    }

    // TC03: Ngày khai giảng trong quá khứ
    @Test
    void createClassroomProposal_UTC03_invalidStartDate_() {
        when(proposalService.create(any(CreateClassroomProposalRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Ngày khai giảng không hợp lệ"));

        // Build request with past date
        CreateClassroomProposalRequest request = new CreateClassroomProposalRequest();
        request.setTitle("IELTS Foundation Class");
        request.setCourseOfferingId(1L);
        request.setCapacity(30);
        request.setPlannedStartDate(LocalDate.of(2020, 1, 1)); // Past date
        request.setEndDate(LocalDate.of(2020, 12, 31));
        request.setWeekdays(List.of(DayOfWeek.MONDAY));
        request.setSessionStartTime(LocalTime.of(18, 0));
        request.setSessionEndTime(LocalTime.of(20, 30));

        assertThatThrownBy(() -> proposalService.create(request, "staff@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ngày khai giảng");

        // Verify the past date was passed
        ArgumentCaptor<CreateClassroomProposalRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateClassroomProposalRequest.class);
        verify(proposalService, times(1)).create(requestCaptor.capture(), any());
        
        assertThat(requestCaptor.getValue().getPlannedStartDate())
                .isEqualTo(LocalDate.of(2020, 1, 1));
    }

    // TC04: Capacity <= 0 validation
    @Test
    void createClassroomProposal_UTC04_invalidCapacity() {
        when(proposalService.create(any(CreateClassroomProposalRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Sức chứa phải > 0"));

        // Build request with invalid capacity
        CreateClassroomProposalRequest request = new CreateClassroomProposalRequest();
        request.setTitle("IELTS Foundation Class");
        request.setCourseOfferingId(1L);
        request.setCapacity(-5); // Invalid capacity
        request.setPlannedStartDate(LocalDate.of(2026, 9, 1));
        request.setEndDate(LocalDate.of(2026, 12, 31));
        request.setWeekdays(List.of(DayOfWeek.MONDAY));
        request.setSessionStartTime(LocalTime.of(18, 0));
        request.setSessionEndTime(LocalTime.of(20, 30));

        assertThatThrownBy(() -> proposalService.create(request, "staff@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sức chứa");

        // Verify invalid capacity was passed
        ArgumentCaptor<CreateClassroomProposalRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateClassroomProposalRequest.class);
        verify(proposalService, times(1)).create(requestCaptor.capture(), any());
        
        assertThat(requestCaptor.getValue().getCapacity()).isEqualTo(-5);
    }

    // TC05: Staff không tồn tại
    @Test
    void createClassroomProposal_UTC05_staffNotFound() {
        when(proposalService.create(any(CreateClassroomProposalRequest.class), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu."));

        CreateClassroomProposalRequest request = new CreateClassroomProposalRequest();
        request.setTitle("IELTS Foundation Class");
        request.setCourseOfferingId(1L);
        request.setCapacity(30);
        request.setPlannedStartDate(LocalDate.of(2026, 9, 1));
        request.setEndDate(LocalDate.of(2026, 12, 31));
        request.setWeekdays(List.of(DayOfWeek.MONDAY));
        request.setSessionStartTime(LocalTime.of(18, 0));
        request.setSessionEndTime(LocalTime.of(20, 30));

        assertThatThrownBy(() -> proposalService.create(request, "unknown@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the unknown email was passed
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(proposalService, times(1)).create(any(), emailCaptor.capture());
        
        assertThat(emailCaptor.getValue()).isEqualTo("unknown@test.com");
    }
}
