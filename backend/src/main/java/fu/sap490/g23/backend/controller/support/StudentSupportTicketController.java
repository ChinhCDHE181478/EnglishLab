package fu.sap490.g23.backend.controller.support;

import fu.sap490.g23.backend.dto.request.support.CreateSupportTicketRequest;
import fu.sap490.g23.backend.dto.request.support.LearnerSupportTicketStatusRequest;
import fu.sap490.g23.backend.dto.request.support.SupportTicketReplyRequest;
import fu.sap490.g23.backend.dto.response.support.SupportTicketResponse;
import fu.sap490.g23.backend.service.support.SupportTicketService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/support-tickets")
@RequiredArgsConstructor
public class StudentSupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    public ResponseEntity<SupportTicketResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateSupportTicketRequest request
    ) {
        return ResponseEntity.ok(supportTicketService.create(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<SupportTicketResponse>> listMine(Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.listMine(authentication.getName()));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<SupportTicketResponse> getMine(
            @PathVariable Long ticketId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(supportTicketService.getMine(ticketId, authentication.getName()));
    }

    @PostMapping("/{ticketId}/replies")
    public ResponseEntity<SupportTicketResponse> reply(
            @PathVariable Long ticketId,
            Authentication authentication,
            @Valid @RequestBody SupportTicketReplyRequest request
    ) {
        return ResponseEntity.ok(supportTicketService.replyAsLearner(
                ticketId,
                authentication.getName(),
                request
        ));
    }

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<SupportTicketResponse> updateStatus(
            @PathVariable Long ticketId,
            Authentication authentication,
            @Valid @RequestBody LearnerSupportTicketStatusRequest request
    ) {
        return ResponseEntity.ok(supportTicketService.updateMyStatus(
                ticketId,
                authentication.getName(),
                request
        ));
    }
}
