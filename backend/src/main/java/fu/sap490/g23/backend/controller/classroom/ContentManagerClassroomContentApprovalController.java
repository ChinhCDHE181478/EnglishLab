package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.ContentReviewRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomSyllabusItemResponse;
import fu.sap490.g23.backend.dto.response.classroom.PendingContentReviewResponse;
import fu.sap490.g23.backend.service.classroom.ClassroomContentApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content-manager/classroom-content-approvals")
@RequiredArgsConstructor
public class ContentManagerClassroomContentApprovalController {

    private final ClassroomContentApprovalService approvalService;

    @GetMapping("/pending")
    public ResponseEntity<List<PendingContentReviewResponse>> listPending() {
        return ResponseEntity.ok(approvalService.listPending());
    }

    @PostMapping("/materials/{materialId}/approve")
    public ResponseEntity<ClassroomMaterialResponse> approveMaterial(
            @PathVariable Long materialId,
            @Valid @RequestBody ContentReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(approvalService.approveMaterial(materialId, authentication.getName(), request));
    }

    @PostMapping("/materials/{materialId}/reject")
    public ResponseEntity<ClassroomMaterialResponse> rejectMaterial(
            @PathVariable Long materialId,
            @Valid @RequestBody ContentReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(approvalService.rejectMaterial(materialId, authentication.getName(), request));
    }

    @PostMapping("/syllabus/{itemId}/approve")
    public ResponseEntity<ClassroomSyllabusItemResponse> approveSyllabus(
            @PathVariable Long itemId,
            @Valid @RequestBody ContentReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(approvalService.approveSyllabus(itemId, authentication.getName(), request));
    }

    @PostMapping("/syllabus/{itemId}/reject")
    public ResponseEntity<ClassroomSyllabusItemResponse> rejectSyllabus(
            @PathVariable Long itemId,
            @Valid @RequestBody ContentReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(approvalService.rejectSyllabus(itemId, authentication.getName(), request));
    }
}
