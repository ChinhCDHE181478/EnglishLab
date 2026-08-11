package fu.sep490.g23.backend.controller.classroom;

import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomProposalRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomProposalResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import fu.sep490.g23.backend.service.classroom.ClassroomProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff/classroom-proposals")
@RequiredArgsConstructor
public class StaffClassroomProposalController {
    private final ClassroomProposalService classroomProposalService;

    @GetMapping
    public ResponseEntity<List<ClassroomProposalResponse>> list(
            @RequestParam(required = false) ClassroomApprovalStatus status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomProposalService.listForStaff(status, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<ClassroomProposalResponse> create(
            @Valid @RequestBody CreateClassroomProposalRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomProposalService.create(request, authentication.getName()));
    }

    @PostMapping("/validate-schedule")
    public ResponseEntity<ConflictCheckResultResponse> validateSchedule(
            @Valid @RequestBody CreateClassroomProposalRequest request,
            @RequestParam(required = false) Long excludeProposalId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomProposalService.validateSchedule(
                request,
                excludeProposalId,
                authentication.getName()
        ));
    }

    @PutMapping("/{proposalId}")
    public ResponseEntity<ClassroomProposalResponse> update(
            @PathVariable Long proposalId,
            @Valid @RequestBody CreateClassroomProposalRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomProposalService.update(
                proposalId,
                request,
                authentication.getName()
        ));
    }

    @PatchMapping("/{proposalId}/submit")
    public ResponseEntity<ClassroomProposalResponse> submit(
            @PathVariable Long proposalId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomProposalService.submit(proposalId, authentication.getName()));
    }
}
