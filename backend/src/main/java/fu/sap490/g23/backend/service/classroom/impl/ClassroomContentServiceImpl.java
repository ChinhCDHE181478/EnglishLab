package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.*;


import fu.sap490.g23.backend.dto.request.classroom.CreateAnnouncementRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateMaterialRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateSyllabusItemRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomAnnouncementResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomSyllabusItemResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sap490.g23.backend.entity.classroom.ClassroomAnnouncement;
import fu.sap490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.ClassroomSyllabusItem;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomAnnouncementRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSyllabusItemRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomContentServiceImpl implements ClassroomContentService {

    private static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATIONS =
            ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS;

    private final ClassroomMaterialRepository materialRepository;
    private final ClassroomAnnouncementRepository announcementRepository;
    private final ClassroomSyllabusItemRepository syllabusItemRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final CenterMaterialLibraryItemRepository centerMaterialLibraryItemRepository;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomMaterialResponse> getMaterials(Long offeringId) {
        return materialRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offeringId).stream()
                .map(mapper::toMaterialResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomMaterialResponse> getLearnerMaterials(Long offeringId, String learnerEmail) {
        assertLearnerPortalAccess(offeringId, learnerEmail);
        return materialRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offeringId).stream()
                .filter(material -> material.getReviewStatus() == null
                        || material.getReviewStatus() == fu.sap490.g23.backend.entity.classroom.enums.ContentReviewStatus.APPROVED)
                .map(mapper::toMaterialResponse)
                .toList();
    }

    @Override
    public ClassroomMaterialResponse createMaterial(Long offeringId, CreateMaterialRequest request, String uploaderEmail) {
        User uploader = accessHelper.requireUser(uploaderEmail);
        assertContentAccess(uploader);

        ClassroomOffering offering = findOffering(offeringId);
        ClassroomSession session = resolveSession(offeringId, request.getSessionId());
        if (!StringUtils.hasText(request.getFileUrl())) {
            throw new IllegalArgumentException("Vui lòng cung cấp tệp hoặc liên kết tài liệu.");
        }

        ClassroomMaterial material = ClassroomMaterial.builder()
                .classroomOffering(offering)
                .session(session)
                .title(request.getTitle().trim())
                .fileUrl(request.getFileUrl().trim())
                .fileType(normalizeUpper(request.getFileType()))
                .description(normalize(request.getDescription()))
                .materialType(normalize(request.getMaterialType()))
                .provider(normalize(request.getProvider()))
                .visibility(normalizeDefault(request.getVisibility(), "LEARNERS_IN_CLASS"))
                .sourceType(normalizeDefaultUpper(request.getSourceType(), "CLASSROOM_UPLOAD"))
                .centerMaterialId(request.getCenterMaterialId())
                .uploadedBy(uploader)
                .reviewStatus(fu.sap490.g23.backend.entity.classroom.enums.ContentReviewStatus.DRAFT)
                .build();

        return mapper.toMaterialResponse(materialRepository.save(material));
    }

    @Override
    public ClassroomMaterialResponse attachCenterMaterial(Long offeringId, Long centerMaterialId, Long sessionId, String uploaderEmail) {
        User uploader = accessHelper.requireUser(uploaderEmail);
        assertContentAccess(uploader);

        ClassroomOffering offering = findOffering(offeringId);
        ClassroomSession session = resolveSession(offeringId, sessionId);
        CenterMaterialLibraryItem libraryItem = centerMaterialLibraryItemRepository.findById(centerMaterialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học liệu trung tâm."));

        if (!"PUBLISHED".equalsIgnoreCase(libraryItem.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể gắn học liệu trung tâm đang ở trạng thái đã xuất bản.");
        }
        if (!StringUtils.hasText(libraryItem.getFileUrl())) {
            throw new IllegalArgumentException("Học liệu trung tâm này chưa có tệp hoặc liên kết hợp lệ.");
        }

        boolean exists = session == null
                ? materialRepository.existsByClassroomOfferingIdAndCenterMaterialIdAndSessionIsNull(offeringId, centerMaterialId)
                : materialRepository.existsByClassroomOfferingIdAndCenterMaterialIdAndSessionId(
                        offeringId,
                        centerMaterialId,
                        session.getId()
                );
        if (exists) {
            throw new IllegalArgumentException("Học liệu này đã được gắn vào lớp ở vị trí hiện tại.");
        }

        ClassroomMaterial material = ClassroomMaterial.builder()
                .classroomOffering(offering)
                .session(session)
                .title(libraryItem.getTitle())
                .fileUrl(libraryItem.getFileUrl())
                .fileType(libraryItem.getFileType())
                .description(libraryItem.getDescription())
                .materialType(libraryItem.getMaterialType())
                .provider(libraryItem.getProvider())
                .visibility("LEARNERS_IN_CLASS")
                .sourceType("CENTER_LIBRARY")
                .centerMaterialId(libraryItem.getId())
                .uploadedBy(uploader)
                .build();
        return mapper.toMaterialResponse(materialRepository.save(material));
    }

    @Override
    public ClassroomMaterialResponse updateMaterial(Long materialId, CreateMaterialRequest request, String editorEmail) {
        User editor = accessHelper.requireUser(editorEmail);
        assertContentAccess(editor);

        ClassroomMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu."));
        Long offeringId = material.getClassroomOffering().getId();
        ClassroomSession session = resolveSession(offeringId, request.getSessionId());
        if (!StringUtils.hasText(request.getFileUrl())) {
            throw new IllegalArgumentException("Vui lòng cung cấp tệp hoặc liên kết tài liệu.");
        }

        material.setSession(session);
        material.setTitle(request.getTitle().trim());
        material.setFileUrl(request.getFileUrl().trim());
        material.setFileType(normalizeUpper(request.getFileType()));
        material.setDescription(normalize(request.getDescription()));
        material.setMaterialType(normalize(request.getMaterialType()));
        material.setProvider(normalize(request.getProvider()));
        material.setVisibility(normalizeDefault(request.getVisibility(), "LEARNERS_IN_CLASS"));
        material.setSourceType(normalizeDefaultUpper(request.getSourceType(), material.getSourceType()));
        material.setCenterMaterialId(request.getCenterMaterialId());
        material.setUploadedBy(editor);

        return mapper.toMaterialResponse(materialRepository.save(material));
    }

    @Override
    public void deleteMaterial(Long materialId) {
        materialRepository.delete(materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu.")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomAnnouncementResponse> getAnnouncements(Long offeringId) {
        return announcementRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offeringId).stream()
                .map(mapper::toAnnouncementResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomAnnouncementResponse> getLearnerAnnouncements(Long offeringId, String learnerEmail) {
        assertLearnerPortalAccess(offeringId, learnerEmail);
        return getAnnouncements(offeringId);
    }

    @Override
    public ClassroomAnnouncementResponse createAnnouncement(
            Long offeringId,
            CreateAnnouncementRequest request,
            String creatorEmail
    ) {
        User creator = accessHelper.requireUser(creatorEmail);
        assertContentAccess(creator);

        ClassroomOffering offering = findOffering(offeringId);
        ClassroomAnnouncement announcement = ClassroomAnnouncement.builder()
                .classroomOffering(offering)
                .title(request.getTitle().trim())
                .content(request.getContent())
                .createdBy(creator)
                .build();

        return mapper.toAnnouncementResponse(announcementRepository.save(announcement));
    }

    @Override
    public void deleteAnnouncement(Long announcementId) {
        announcementRepository.delete(announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo.")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomSyllabusItemResponse> getSyllabus(Long offeringId) {
        return syllabusItemRepository.findByClassroomOfferingIdOrderByDisplayOrderAsc(offeringId).stream()
                .map(mapper::toSyllabusItemResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomSyllabusItemResponse> getLearnerSyllabus(Long offeringId, String learnerEmail) {
        assertLearnerPortalAccess(offeringId, learnerEmail);
        return syllabusItemRepository.findByClassroomOfferingIdOrderByDisplayOrderAsc(offeringId).stream()
                .filter(item -> item.getReviewStatus() == null
                        || item.getReviewStatus() == fu.sap490.g23.backend.entity.classroom.enums.ContentReviewStatus.APPROVED)
                .map(mapper::toSyllabusItemResponse)
                .toList();
    }

    @Override
    public ClassroomSyllabusItemResponse createSyllabusItem(Long offeringId, CreateSyllabusItemRequest request) {
        ClassroomOffering offering = findOffering(offeringId);
        ClassroomSyllabusItem item = ClassroomSyllabusItem.builder()
                .classroomOffering(offering)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder())
                .sessionPlan(request.getSessionPlan())
                .homeworkNotes(request.getHomeworkNotes())
                .quizNotes(request.getQuizNotes())
                .teacherNotes(request.getTeacherNotes())
                .sessionNumber(request.getSessionNumber())
                .linkedSessionId(request.getLinkedSessionId())
                .status(request.getStatus() == null ? "DRAFT" : request.getStatus())
                .reviewStatus(fu.sap490.g23.backend.entity.classroom.enums.ContentReviewStatus.DRAFT)
                .build();
        return mapper.toSyllabusItemResponse(syllabusItemRepository.save(item));
    }

    @Override
    public ClassroomSyllabusItemResponse updateSyllabusItem(Long itemId, CreateSyllabusItemRequest request) {
        ClassroomSyllabusItem item = syllabusItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục đề cương."));
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) {
            item.setDisplayOrder(request.getDisplayOrder());
        }
        item.setSessionPlan(request.getSessionPlan());
        item.setHomeworkNotes(request.getHomeworkNotes());
        item.setQuizNotes(request.getQuizNotes());
        item.setTeacherNotes(request.getTeacherNotes());
        if (request.getSessionNumber() != null) {
            item.setSessionNumber(request.getSessionNumber());
        }
        if (request.getLinkedSessionId() != null) {
            item.setLinkedSessionId(request.getLinkedSessionId());
        }
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }
        return mapper.toSyllabusItemResponse(syllabusItemRepository.save(item));
    }

    @Override
    public void deleteSyllabusItem(Long itemId) {
        syllabusItemRepository.delete(syllabusItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục đề cương.")));
    }

    private ClassroomOffering findOffering(Long offeringId) {
        return offeringRepository.findById(offeringId)
                .filter(offering -> !offering.getLearningPackage().isDeleted())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
    }

    private void assertContentAccess(User user) {
        if (!accessHelper.canManageClassroom(user) && !accessHelper.canTeach(user)) {
            throw new RuntimeException("Bạn không có quyền truy cập nội dung này.");
        }
    }

    private void assertLearnerPortalAccess(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offeringId)
                .filter(enrollment -> ACTIVE_REGISTRATIONS.contains(enrollment.getRegistrationStatus()))
                .orElseThrow(() -> new RuntimeException("Bạn không có quyền truy cập lớp học này."));
    }

    private ClassroomSession resolveSession(Long offeringId, Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
        if (!session.getClassroomOffering().getId().equals(offeringId)) {
            throw new IllegalArgumentException("Buổi học được chọn không thuộc lớp hiện tại.");
        }
        return session;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String normalizeDefaultUpper(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }
}
