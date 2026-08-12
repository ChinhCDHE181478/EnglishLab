package fu.sep490.g23.backend.controller.support;

import fu.sep490.g23.backend.dto.request.support.SupportTicketReplyRequest;
import fu.sep490.g23.backend.dto.request.support.UpdateSupportTicketRequest;
import fu.sep490.g23.backend.dto.response.support.SupportTicketResponse;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketStatus;
import fu.sep490.g23.backend.service.support.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/staff/support-tickets", "/api/manager/support-tickets"})
@RequiredArgsConstructor
public class ManagerSupportTicketController {

    private final SupportTicketService supportTicketService;

    @GetMapping
    public ResponseEntity<List<SupportTicketResponse>> listQueue(
            Authentication authentication,
            @RequestParam(required = false) SupportTicketStatus status,
            @RequestParam(required = false) SupportTicketPriority priority
    ) {
        return ResponseEntity.ok(supportTicketService.listQueue(
                authentication.getName(),
                status,
                priority
        ));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<SupportTicketResponse> get(
            @PathVariable Long ticketId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(supportTicketService.getForStaff(ticketId, authentication.getName()));
    }

    @PostMapping("/{ticketId}/claim")
    public ResponseEntity<SupportTicketResponse> claim(
            @PathVariable Long ticketId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(supportTicketService.claim(ticketId, authentication.getName()));
    }

    @PostMapping("/{ticketId}/replies")
    public ResponseEntity<SupportTicketResponse> reply(
            @PathVariable Long ticketId,
            Authentication authentication,
            @Valid @RequestBody SupportTicketReplyRequest request
    ) {
        return ResponseEntity.ok(supportTicketService.replyAsStaff(
                ticketId,
                authentication.getName(),
                request
        ));
    }

    @PatchMapping("/{ticketId}")
    public ResponseEntity<SupportTicketResponse> update(
            @PathVariable Long ticketId,
            Authentication authentication,
            @RequestBody UpdateSupportTicketRequest request
    ) {
        return ResponseEntity.ok(supportTicketService.updateAsStaff(
                ticketId,
                authentication.getName(),
                request
        ));
    }
}
