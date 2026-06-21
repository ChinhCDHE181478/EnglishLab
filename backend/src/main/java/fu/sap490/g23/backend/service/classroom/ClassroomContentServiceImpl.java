package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CreateAnnouncementRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateMaterialRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateSyllabusItemRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomAnnouncementResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomSyllabusItemResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.*;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomContentServiceImpl implements ClassroomContentService {

    private static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATIONS = ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS;

    private final ClassroomMaterialRepository materialRepository;
    private final ClassroomAnnouncementRepository announcementRepository;
    private final ClassroomSyllabusItemRepository syllabusItemRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
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
        return getMaterials(offeringId);
    }

    @Override
    public ClassroomMaterialResponse createMaterial(Long offeringId, CreateMaterialRequest request, String uploaderEmail) {
        User uploader = accessHelper.requireUser(uploaderEmail);
        assertContentAccess(uploader);

        ClassroomOffering offering = findOffering(offeringId);
        ClassroomSession session = null;
        if (request.getSessionId() != null) {
            session = sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));
        }

        ClassroomMaterial material = ClassroomMaterial.builder()
                .classroomOffering(offering)
                .session(session)
                .title(request.getTitle().trim())
                .fileUrl(request.getFileUrl())
                .fileType(request.getFileType())
                .visibility(request.getVisibility() == null ? "LEARNERS_IN_CLASS" : request.getVisibility())
                .uploadedBy(uploader)
                .build();

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
    public ClassroomAnnouncementResponse createAnnouncement(Long offeringId, CreateAnnouncementRequest request, String creatorEmail) {
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
        return getSyllabus(offeringId);
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
                .status(request.getStatus() == null ? "DRAFT" : request.getStatus())
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
}
