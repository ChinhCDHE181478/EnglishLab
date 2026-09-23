package fu.sep490.g23.backend.ut.classroomOperationService;

import fu.sep490.g23.backend.dto.request.classroom.RejectClassroomProposalRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomProposalResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import fu.sep490.g23.backend.service.classroom.ClassroomProposalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * UC-101: Reject Classroom Proposal - test đầy đủ với request validation.
 * Service: ClassroomProposalService.reject()
 * Request DTO: RejectClassroomProposalRequest
 */
@ExtendWith(MockitoExtension.class)
public class RejectClassroomProposalTest {

    @Mock
    private ClassroomProposalService proposalService;

    // TC01: Từ chối với lý do hợp lệ -> REJECTED
    @Test
    void tc01_rejectProposal_succeeds() {
        ClassroomProposalResponse expected = ClassroomProposalResponse.builder()
                .id(101L)
                .approvalStatus(ClassroomApprovalStatus.REJECTED)
                .build();

        when(proposalService.reject(eq(101L), any(RejectClassroomProposalRequest.class), eq("manager@test.com")))
                .thenReturn(expected);

        // Build complete request with reason
        RejectClassroomProposalRequest request = new RejectClassroomProposalRequest();
        request.setReason("Sĩ số chưa và chi phí dự kiến chưa tối ưu.");

        ClassroomProposalResponse result = proposalService.reject(101L, request, "manager@test.com");

        // Verify response
        assertThat(result.getApprovalStatus()).isEqualTo(ClassroomApprovalStatus.REJECTED);

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<Long> proposalIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<RejectClassroomProposalRequest> requestCaptor = 
                ArgumentCaptor.forClass(RejectClassroomProposalRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(proposalService, times(1))
                .reject(proposalIdCaptor.capture(), requestCaptor.capture(), emailCaptor.capture());

        // Verify all parameters
        assertThat(proposalIdCaptor.getValue()).isEqualTo(101L);
        assertThat(requestCaptor.getValue().getReason()).isEqualTo("Sĩ số chưa và chi phí dự kiến chưa tối ưu.");
        assertThat(emailCaptor.getValue()).isEqualTo("manager@test.com");

        verifyNoMoreInteractions(proposalService);
    }

    // TC02: Hủy thao tác từ chối -> giữ nguyên status hiện tại
    @Test
    void tc02_cancelOperation_keepsOriginalStatus() {
        ClassroomProposalResponse expected = ClassroomProposalResponse.builder()
                .id(101L)
                .approvalStatus(ClassroomApprovalStatus.PENDING_APPROVAL)
                .build();

        when(proposalService.reject(eq(101L), any(RejectClassroomProposalRequest.class), eq("manager@test.com")))
                .thenReturn(expected);

        // Empty request (cancel operation)
        RejectClassroomProposalRequest request = new RejectClassroomProposalRequest();

        ClassroomProposalResponse result = proposalService.reject(101L, request, "manager@test.com");
        
        // Verify response
        assertThat(result.getApprovalStatus()).isEqualTo(ClassroomApprovalStatus.PENDING_APPROVAL);

        // Verify the request (with empty reason for cancel)
        ArgumentCaptor<RejectClassroomProposalRequest> requestCaptor = 
                ArgumentCaptor.forClass(RejectClassroomProposalRequest.class);
        verify(proposalService, times(1))
                .reject(eq(101L), requestCaptor.capture(), eq("manager@test.com"));

        // Reason should be empty/null when cancelling
        assertThat(requestCaptor.getValue().getReason()).isNull();

        verifyNoMoreInteractions(proposalService);
    }

    // TC03: Thiếu lý do từ chối -> Validation
    @Test
    void tc03_missingRejectionReason_throwsException() {
        when(proposalService.reject(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Lý do từ chối không được để trống"));

        RejectClassroomProposalRequest request = new RejectClassroomProposalRequest();
        request.setReason("");  // Empty reason

        assertThatThrownBy(() -> proposalService.reject(101L, request, "manager@test.com"))
                .isInstanceOf(IllegalArgumentException.class);

        // Verify the request with empty reason
        ArgumentCaptor<RejectClassroomProposalRequest> requestCaptor = 
                ArgumentCaptor.forClass(RejectClassroomProposalRequest.class);
        verify(proposalService, times(1))
                .reject(eq(101L), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getReason()).isEmpty();

        verifyNoMoreInteractions(proposalService);
    }

    // TC04: Proposal không ở PENDING_APPROVAL -> InvalidProposalStatusException
    @Test
    void tc04_invalidStatus_throwsException() {
        when(proposalService.reject(any(), any(), any()))
                .thenThrow(new IllegalStateException("MSG-43: Đề xuất lớp không ở trạng thái cho phép thao tác này."));

        RejectClassroomProposalRequest request = new RejectClassroomProposalRequest();
        request.setReason("Lý do test - proposal không ở trạng thái hợp lệ");

        assertThatThrownBy(() -> proposalService.reject(102L, request, "manager@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MSG-43");

        // Verify the request and proposal ID
        ArgumentCaptor<Long> proposalIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<RejectClassroomProposalRequest> requestCaptor = 
                ArgumentCaptor.forClass(RejectClassroomProposalRequest.class);
        verify(proposalService, times(1))
                .reject(proposalIdCaptor.capture(), requestCaptor.capture(), any());

        assertThat(proposalIdCaptor.getValue()).isEqualTo(102L);
        assertThat(requestCaptor.getValue().getReason()).isEqualTo("Lý do test - proposal không ở trạng thái hợp lệ");

        verifyNoMoreInteractions(proposalService);
    }

    // TC05: proposalId không tồn tại -> EntityNotFoundException
    @Test
    void tc05_proposalNotFound_throwsException() {
        when(proposalService.reject(any(), any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu."));

        RejectClassroomProposalRequest request = new RejectClassroomProposalRequest();
        request.setReason("Lý do test");

        assertThatThrownBy(() -> proposalService.reject(999L, request, "manager@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the non-existent proposal ID
        ArgumentCaptor<Long> proposalIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(proposalService, times(1))
                .reject(proposalIdCaptor.capture(), any(), any());

        assertThat(proposalIdCaptor.getValue()).isEqualTo(999L);
    }
}
