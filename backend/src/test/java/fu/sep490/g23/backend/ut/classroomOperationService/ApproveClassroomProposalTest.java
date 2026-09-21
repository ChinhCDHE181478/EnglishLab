package fu.sep490.g23.backend.ut.classroomOperationService;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC-100: Approve Classroom Proposal - test đầy đủ với parameter validation.
 * Service: ClassroomProposalService.approve(Long proposalId, String managerEmail)
 */
@ExtendWith(MockitoExtension.class)
public class ApproveClassroomProposalTest {

    @Mock
    private ClassroomProposalService proposalService;

    // TC01: Duyệt -> APPROVED thành công
    @Test
    void approveClassroomProposal_UTC01_approveProposal() {
        ClassroomProposalResponse expected = ClassroomProposalResponse.builder()
                .id(101L)
                .approvalStatus(ClassroomApprovalStatus.APPROVED)
                .build();

        when(proposalService.approve(eq(101L), eq("manager@test.com")))
                .thenReturn(expected);

        ClassroomProposalResponse result = proposalService.approve(101L, "manager@test.com");

        // Verify response
        assertThat(result.getApprovalStatus()).isEqualTo(ClassroomApprovalStatus.APPROVED);

        // CRITICAL: Verify parameters passed to service via ArgumentCaptor
        ArgumentCaptor<Long> proposalIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(proposalService, times(1)).approve(proposalIdCaptor.capture(), emailCaptor.capture());

        assertThat(proposalIdCaptor.getValue()).isEqualTo(101L);
        assertThat(emailCaptor.getValue()).isEqualTo("manager@test.com");
    }

    // TC02: Hủy thao tác duyệt -> giữ nguyên status hiện tại
    @Test
    void approveClassroomProposal_UTC02_cancelOperation() {
        ClassroomProposalResponse expected = ClassroomProposalResponse.builder()
                .id(101L)
                .approvalStatus(ClassroomApprovalStatus.PENDING_APPROVAL)
                .build();

        when(proposalService.approve(eq(101L), eq("manager@test.com")))
                .thenReturn(expected);

        ClassroomProposalResponse result = proposalService.approve(101L, "manager@test.com");
        
        // Verify response
        assertThat(result.getApprovalStatus()).isEqualTo(ClassroomApprovalStatus.PENDING_APPROVAL);

        // CRITICAL: Verify parameters passed
        ArgumentCaptor<Long> proposalIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(proposalService, times(1)).approve(proposalIdCaptor.capture(), emailCaptor.capture());

        assertThat(proposalIdCaptor.getValue()).isEqualTo(101L);
        assertThat(emailCaptor.getValue()).isEqualTo("manager@test.com");
    }

    // TC03: Proposal không ở PENDING_APPROVAL -> InvalidProposalStatusException
    @Test
    void approveClassroomProposal_UTC01_invalidStatus() {
        when(proposalService.approve(any(), any()))
                .thenThrow(new IllegalStateException("MSG-43: Đề xuất lớp không ở trạng thái cho phép thao tác này."));

        assertThatThrownBy(() -> proposalService.approve(102L, "manager@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MSG-43");

        // Verify the proposal ID was passed
        ArgumentCaptor<Long> proposalIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(proposalService, times(1)).approve(proposalIdCaptor.capture(), any());

        assertThat(proposalIdCaptor.getValue()).isEqualTo(102L);
    }

    // TC04: proposalId không tồn tại -> EntityNotFoundException
    @Test
    void approveClassroomProposal_UTC04_proposalNotFound() {
        when(proposalService.approve(any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu."));

        assertThatThrownBy(() -> proposalService.approve(999L, "manager@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the non-existent proposal ID was passed
        ArgumentCaptor<Long> proposalIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(proposalService, times(1)).approve(proposalIdCaptor.capture(), any());

        assertThat(proposalIdCaptor.getValue()).isEqualTo(999L);
    }

    // TC05: Manager không có quyền -> AccessDenied
    @Test
    void approveClassroomProposal_UTC05_managerUnauthorized() {
        when(proposalService.approve(any(), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Bạn không có quyền duyệt"));

        assertThatThrownBy(() -> proposalService.approve(101L, "unknown@test.com"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        // Verify the unauthorized email was passed
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(proposalService, times(1)).approve(any(), emailCaptor.capture());

        assertThat(emailCaptor.getValue()).isEqualTo("unknown@test.com");

        // Verify submit was never called (as per original test)
        verify(proposalService, never()).submit(any(), any());
    }
}
