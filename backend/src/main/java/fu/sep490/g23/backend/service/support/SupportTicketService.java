package fu.sep490.g23.backend.service.support;

import fu.sep490.g23.backend.dto.request.support.CreateSupportTicketRequest;
import fu.sep490.g23.backend.dto.request.support.LearnerSupportTicketStatusRequest;
import fu.sep490.g23.backend.dto.request.support.SupportTicketReplyRequest;
import fu.sep490.g23.backend.dto.request.support.UpdateSupportTicketRequest;
import fu.sep490.g23.backend.dto.response.support.SupportTicketResponse;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketStatus;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupportTicketService {
    SupportTicketResponse create(String userEmail, CreateSupportTicketRequest request);
    List<SupportTicketResponse> listMine(String userEmail);
    SupportTicketResponse getMine(Long ticketId, String userEmail);
    SupportTicketResponse replyAsLearner(Long ticketId, String userEmail, SupportTicketReplyRequest request);
    SupportTicketResponse updateMyStatus(Long ticketId, String userEmail, LearnerSupportTicketStatusRequest request);
    List<SupportTicketResponse> listQueue(String staffEmail, SupportTicketStatus status, SupportTicketPriority priority);
    Page<SupportTicketResponse> pageQueue(String staffEmail, SupportTicketStatus status, SupportTicketPriority priority, String keyword, Pageable pageable);
    SupportTicketResponse getForStaff(Long ticketId, String staffEmail);
    SupportTicketResponse claim(Long ticketId, String staffEmail);
    SupportTicketResponse replyAsStaff(Long ticketId, String staffEmail, SupportTicketReplyRequest request);
    SupportTicketResponse updateAsStaff(Long ticketId, String staffEmail, UpdateSupportTicketRequest request);
}
