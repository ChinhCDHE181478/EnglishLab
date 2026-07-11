package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CreateAnnouncementRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateMaterialRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateSyllabusItemRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomAnnouncementResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomMaterialResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomSyllabusItemResponse;
import java.util.List;

public interface ClassroomContentService {

    List<ClassroomMaterialResponse> getMaterials(Long offeringId);

    List<ClassroomMaterialResponse> getLearnerMaterials(Long offeringId, String learnerEmail);

    ClassroomMaterialResponse createMaterial(Long offeringId, CreateMaterialRequest request, String uploaderEmail);

    ClassroomMaterialResponse attachCenterMaterial(Long offeringId, Long centerMaterialId, Long sessionId, String uploaderEmail);

    ClassroomMaterialResponse updateMaterial(Long materialId, CreateMaterialRequest request, String editorEmail);

    void deleteMaterial(Long materialId);

    List<ClassroomAnnouncementResponse> getAnnouncements(Long offeringId);

    List<ClassroomAnnouncementResponse> getLearnerAnnouncements(Long offeringId, String learnerEmail);

    ClassroomAnnouncementResponse createAnnouncement(Long offeringId, CreateAnnouncementRequest request, String creatorEmail);

    void deleteAnnouncement(Long announcementId);

    List<ClassroomSyllabusItemResponse> getSyllabus(Long offeringId);

    List<ClassroomSyllabusItemResponse> getLearnerSyllabus(Long offeringId, String learnerEmail);

    ClassroomSyllabusItemResponse createSyllabusItem(Long offeringId, CreateSyllabusItemRequest request);

    ClassroomSyllabusItemResponse updateSyllabusItem(Long itemId, CreateSyllabusItemRequest request);

    void deleteSyllabusItem(Long itemId);
}
