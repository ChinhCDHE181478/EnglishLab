package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;


import fu.sep490.g23.backend.dto.request.classroom.CreateAnnouncementRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateMaterialRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateSyllabusItemRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomAnnouncementResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSyllabusItemResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomAnnouncement;
import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.ClassroomSession;
import fu.sep490.g23.backend.entity.classroom.ClassroomSyllabusItem;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.repository.classroom.ClassroomAnnouncementRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomMaterialRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSyllabusItemRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.ClassroomContentService;
import fu.sep490.g23.backend.service.classroom.ClassroomMaterialSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
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
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomMapper mapper;
    private final ClassroomMaterialSyncService classroomMaterialSyncService;

    @Override
    public List<ClassroomMaterialResponse> getMaterials(Long offeringId) {
        ClassroomOffering offering = findOffering(offeringId);
        classroomMaterialSyncService.synchronizeMandatoryMaterials(offering, null);
        return materialRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offeringId).stream()
                .map(mapper::toMaterialResponse)
                .toList();
    }

    @Override
    public List<ClassroomMaterialResponse> getTeacherMaterials(Long offeringId, String teacherEmail) {
        User teacher = accessHelper.requireUser(teacherEmail);
        ClassroomOffering offering = findOffering(offeringId);
        assertOfferingContentAccess(teacher, offering);
        return getMaterials(offeringId);
    }

    @Override
    public List<ClassroomMaterialResponse> getLearnerMaterials(Long offeringId, String learnerEmail) {
        assertLearnerPortalAccess(offeringId, learnerEmail);
        ClassroomOffering offering = findOffering(offeringId);
        classroomMaterialSyncService.synchronizeMandatoryMaterials(offering, null);
        return materialRepository.findByClassroomOfferingIdOrderByCreatedAtDesc(offeringId).stream()
                .map(mapper::toMaterialResponse)
                .toList();
    }

    @Override
    public ClassroomMaterialResponse createMaterial(Long offeringId, CreateMaterialRequest request, String uploaderEmail) {
        User uploader = accessHelper.requireUser(uploaderEmail);
        ClassroomOffering offering = findOffering(offeringId);
        assertOfferingContentAccess(uploader, offering);
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
                .sourceType("CLASSROOM_UPLOAD")
                .centerMaterialId(null)
                .uploadedBy(uploader)
                .reviewStatus(fu.sep490.g23.backend.entity.classroom.enums.ContentReviewStatus.APPROVED)
                .build();

        return mapper.toMaterialResponse(materialRepository.save(material));
    }

    @Override
    public ClassroomMaterialResponse updateMaterial(Long materialId, CreateMaterialRequest request, String editorEmail) {
        User editor = accessHelper.requireUser(editorEmail);
        ClassroomMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu."));
        assertOfferingContentAccess(editor, material.getClassroomOffering());
        assertSupplementaryMaterial(material);
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
        material.setSourceType("CLASSROOM_UPLOAD");
        material.setCenterMaterialId(null);
        material.setUploadedBy(editor);

        return mapper.toMaterialResponse(materialRepository.save(material));
    }

    @Override
    public void deleteMaterial(Long materialId, String actorEmail) {
        User actor = accessHelper.requireUser(actorEmail);
        ClassroomMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu."));
        assertOfferingContentAccess(actor, material.getClassroomOffering());
        assertSupplementaryMaterial(material);
        materialRepository.delete(material);
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
        ClassroomOffering offering = findOffering(offeringId);
        assertOfferingContentAccess(creator, offering);
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
                .reviewStatus(fu.sep490.g23.backend.entity.classroom.enums.ContentReviewStatus.APPROVED)
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

    private void assertOfferingContentAccess(User user, ClassroomOffering offering) {
        if (accessHelper.canManageTrainingOperations(user)) {
            return;
        }
        accessHelper.assertTeacher(user);
        LocalDate today = LocalDate.now();
        boolean assigned = teacherAssignmentRepository
                .findAllByClassroomOfferingIdAndTeacherId(offering.getId(), user.getId())
                .stream()
                .anyMatch(assignment -> (assignment.getEffectiveFrom() == null || !assignment.getEffectiveFrom().isAfter(today))
                        && (assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(today)));
        if (!assigned) {
            throw new RuntimeException("Bạn không được phân công giảng dạy lớp học này.");
        }
    }

    private void assertSupplementaryMaterial(ClassroomMaterial material) {
        String sourceType = material.getSourceType();
        if ("CURRICULUM_LIBRARY".equalsIgnoreCase(sourceType)) {
            throw new IllegalArgumentException(
                    "Học liệu bắt buộc thuộc giáo trình. Hãy cập nhật unit trong giáo trình thay vì sửa hoặc xóa trực tiếp trong lớp."
            );
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

}
