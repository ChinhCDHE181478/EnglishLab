package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.ContentReviewRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomSyllabusItemResponse;
import fu.sap490.g23.backend.dto.response.classroom.PendingContentReviewResponse;

import java.util.List;

public interface ClassroomContentApprovalService {
    List<PendingContentReviewResponse> listPending();

    ClassroomMaterialResponse submitMaterialForReview(Long materialId, String submitterEmail);

    ClassroomSyllabusItemResponse submitSyllabusForReview(Long itemId, String submitterEmail);

    ClassroomMaterialResponse approveMaterial(Long materialId, String reviewerEmail, ContentReviewRequest request);

    ClassroomMaterialResponse rejectMaterial(Long materialId, String reviewerEmail, ContentReviewRequest request);

    ClassroomSyllabusItemResponse approveSyllabus(Long itemId, String reviewerEmail, ContentReviewRequest request);

    ClassroomSyllabusItemResponse rejectSyllabus(Long itemId, String reviewerEmail, ContentReviewRequest request);
}
