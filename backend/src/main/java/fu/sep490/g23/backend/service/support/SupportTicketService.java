package fu.sap490.g23.backend.service.support;

import fu.sap490.g23.backend.dto.request.support.CreateSupportTicketRequest;
import fu.sap490.g23.backend.dto.request.support.LearnerSupportTicketStatusRequest;
import fu.sap490.g23.backend.dto.request.support.SupportTicketReplyRequest;
import fu.sap490.g23.backend.dto.request.support.UpdateSupportTicketRequest;
import fu.sap490.g23.backend.dto.response.support.SupportTicketResponse;
import fu.sap490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sap490.g23.backend.entity.support.enums.SupportTicketStatus;

import java.util.List;

public interface SupportTicketService {
    SupportTicketResponse create(String userEmail, CreateSupportTicketRequest request);
    List<SupportTicketResponse> listMine(String userEmail);
    SupportTicketResponse getMine(Long ticketId, String userEmail);
    SupportTicketResponse replyAsLearner(Long ticketId, String userEmail, SupportTicketReplyRequest request);
    SupportTicketResponse updateMyStatus(Long ticketId, String userEmail, LearnerSupportTicketStatusRequest request);
    List<SupportTicketResponse> listQueue(String staffEmail, SupportTicketStatus status, SupportTicketPriority priority);
    SupportTicketResponse getForStaff(Long ticketId, String staffEmail);
    SupportTicketResponse claim(Long ticketId, String staffEmail);
    SupportTicketResponse replyAsStaff(Long ticketId, String staffEmail, SupportTicketReplyRequest request);
    SupportTicketResponse updateAsStaff(Long ticketId, String staffEmail, UpdateSupportTicketRequest request);
}
