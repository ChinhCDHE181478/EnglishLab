package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.dto.request.classroom.ContentReviewRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomSyllabusItemResponse;
import fu.sap490.g23.backend.dto.response.classroom.PendingContentReviewResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sap490.g23.backend.entity.classroom.ClassroomSyllabusItem;
import fu.sap490.g23.backend.entity.classroom.enums.ContentReviewStatus;
import fu.sap490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSyllabusItemRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.ClassroomContentApprovalService;
import fu.sap490.g23.backend.service.classroom.ClassroomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomContentApprovalServiceImpl implements ClassroomContentApprovalService {

    private final ClassroomMaterialRepository materialRepository;
    private final ClassroomSyllabusItemRepository syllabusItemRepository;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<PendingContentReviewResponse> listPending() {
        List<PendingContentReviewResponse> pending = new ArrayList<>();
        materialRepository.findAll().stream()
                .filter(material -> material.getReviewStatus() == ContentReviewStatus.PENDING_REVIEW)
                .forEach(material -> pending.add(PendingContentReviewResponse.builder()
                        .contentType("MATERIAL")
                        .id(material.getId())
                        .classroomOfferingId(material.getClassroomOffering().getId())
                        .classroomTitle(material.getClassroomOffering().getLearningPackage().getTitle())
                        .title(material.getTitle())
                        .reviewStatus(material.getReviewStatus())
                        .reviewNote(material.getReviewNote())
                        .submittedForReviewAt(material.getSubmittedForReviewAt())
                        .updatedAt(material.getUpdatedAt())
                        .build()));
        syllabusItemRepository.findAll().stream()
                .filter(item -> item.getReviewStatus() == ContentReviewStatus.PENDING_REVIEW)
                .forEach(item -> pending.add(PendingContentReviewResponse.builder()
                        .contentType("SYLLABUS")
                        .id(item.getId())
                        .classroomOfferingId(item.getClassroomOffering().getId())
                        .classroomTitle(item.getClassroomOffering().getLearningPackage().getTitle())
                        .title(item.getTitle())
                        .reviewStatus(item.getReviewStatus())
                        .reviewNote(item.getReviewNote())
                        .submittedForReviewAt(item.getSubmittedForReviewAt())
                        .updatedAt(item.getUpdatedAt())
                        .build()));
        pending.sort(Comparator.comparing(
                PendingContentReviewResponse::getSubmittedForReviewAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return pending;
    }

    @Override
    public ClassroomMaterialResponse submitMaterialForReview(Long materialId, String submitterEmail) {
        accessHelper.requireUser(submitterEmail);
        ClassroomMaterial material = findMaterial(materialId);
        if (material.getReviewStatus() != ContentReviewStatus.DRAFT
                && material.getReviewStatus() != ContentReviewStatus.REJECTED) {
            throw new IllegalArgumentException("Chỉ có thể gửi duyệt tài liệu ở trạng thái nháp hoặc bị từ chối.");
        }
        material.setReviewStatus(ContentReviewStatus.PENDING_REVIEW);
        material.setSubmittedForReviewAt(LocalDateTime.now());
        material.setReviewNote(null);
        return mapper.toMaterialResponse(materialRepository.save(material));
    }

    @Override
    public ClassroomSyllabusItemResponse submitSyllabusForReview(Long itemId, String submitterEmail) {
        accessHelper.requireUser(submitterEmail);
        ClassroomSyllabusItem item = findSyllabusItem(itemId);
        if (item.getReviewStatus() != ContentReviewStatus.DRAFT
                && item.getReviewStatus() != ContentReviewStatus.REJECTED) {
            throw new IllegalArgumentException("Chỉ có thể gửi duyệt mục giáo trình ở trạng thái nháp hoặc bị từ chối.");
        }
        item.setReviewStatus(ContentReviewStatus.PENDING_REVIEW);
        item.setSubmittedForReviewAt(LocalDateTime.now());
        item.setReviewNote(null);
        return mapper.toSyllabusItemResponse(syllabusItemRepository.save(item));
    }

    @Override
    public ClassroomMaterialResponse approveMaterial(Long materialId, String reviewerEmail, ContentReviewRequest request) {
        User reviewer = requireManager(reviewerEmail);
        ClassroomMaterial material = findMaterial(materialId);
        if (material.getReviewStatus() != ContentReviewStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Tài liệu không ở trạng thái chờ duyệt.");
        }
        material.setReviewStatus(ContentReviewStatus.APPROVED);
        material.setReviewedAt(LocalDateTime.now());
        material.setReviewedBy(reviewer);
        material.setReviewNote(trimNote(request));
        return mapper.toMaterialResponse(materialRepository.save(material));
    }

    @Override
    public ClassroomMaterialResponse rejectMaterial(Long materialId, String reviewerEmail, ContentReviewRequest request) {
        User reviewer = requireManager(reviewerEmail);
        ClassroomMaterial material = findMaterial(materialId);
        if (material.getReviewStatus() != ContentReviewStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Tài liệu không ở trạng thái chờ duyệt.");
        }
        if (!StringUtils.hasText(request == null ? null : request.getReviewNote())) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
        }
        material.setReviewStatus(ContentReviewStatus.REJECTED);
        material.setReviewedAt(LocalDateTime.now());
        material.setReviewedBy(reviewer);
        material.setReviewNote(request.getReviewNote().trim());
        return mapper.toMaterialResponse(materialRepository.save(material));
    }

    @Override
    public ClassroomSyllabusItemResponse approveSyllabus(Long itemId, String reviewerEmail, ContentReviewRequest request) {
        User reviewer = requireManager(reviewerEmail);
        ClassroomSyllabusItem item = findSyllabusItem(itemId);
        if (item.getReviewStatus() != ContentReviewStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Mục giáo trình không ở trạng thái chờ duyệt.");
        }
        item.setReviewStatus(ContentReviewStatus.APPROVED);
        item.setReviewedAt(LocalDateTime.now());
        item.setReviewedBy(reviewer);
        item.setReviewNote(trimNote(request));
        item.setStatus("PUBLISHED");
        return mapper.toSyllabusItemResponse(syllabusItemRepository.save(item));
    }

    @Override
    public ClassroomSyllabusItemResponse rejectSyllabus(Long itemId, String reviewerEmail, ContentReviewRequest request) {
        User reviewer = requireManager(reviewerEmail);
        ClassroomSyllabusItem item = findSyllabusItem(itemId);
        if (item.getReviewStatus() != ContentReviewStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Mục giáo trình không ở trạng thái chờ duyệt.");
        }
        if (!StringUtils.hasText(request == null ? null : request.getReviewNote())) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
        }
        item.setReviewStatus(ContentReviewStatus.REJECTED);
        item.setReviewedAt(LocalDateTime.now());
        item.setReviewedBy(reviewer);
        item.setReviewNote(request.getReviewNote().trim());
        return mapper.toSyllabusItemResponse(syllabusItemRepository.save(item));
    }

    private User requireManager(String email) {
        User user = accessHelper.requireUser(email);
        accessHelper.assertManager(user);
        return user;
    }

    private ClassroomMaterial findMaterial(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu."));
    }

    private ClassroomSyllabusItem findSyllabusItem(Long id) {
        return syllabusItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục giáo trình."));
    }

    private String trimNote(ContentReviewRequest request) {
        return request == null || !StringUtils.hasText(request.getReviewNote())
                ? null
                : request.getReviewNote().trim();
    }
}
