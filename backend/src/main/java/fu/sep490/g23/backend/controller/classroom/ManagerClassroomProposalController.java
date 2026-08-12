package fu.sep490.g23.backend.controller.classroom;

import fu.sep490.g23.backend.dto.request.classroom.RejectClassroomProposalRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomProposalResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import fu.sep490.g23.backend.service.classroom.ClassroomProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manager/classroom-proposals")
@RequiredArgsConstructor
public class ManagerClassroomProposalController {
    private final ClassroomProposalService classroomProposalService;

    @GetMapping
    public ResponseEntity<List<ClassroomProposalResponse>> list(
            @RequestParam(required = false) ClassroomApprovalStatus status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomProposalService.listForManager(status, authentication.getName()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ClassroomProposalResponse>> listPending(Authentication authentication) {
        return ResponseEntity.ok(classroomProposalService.listForManager(
                ClassroomApprovalStatus.PENDING_APPROVAL,
                authentication.getName()
        ));
    }

    @PatchMapping("/{proposalId}/approve")
    public ResponseEntity<ClassroomProposalResponse> approve(
            @PathVariable Long proposalId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomProposalService.approve(proposalId, authentication.getName()));
    }

    @PatchMapping("/{proposalId}/reject")
    public ResponseEntity<ClassroomProposalResponse> reject(
            @PathVariable Long proposalId,
            @Valid @RequestBody RejectClassroomProposalRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomProposalService.reject(
                proposalId,
                request,
                authentication.getName()
        ));
    }
}
