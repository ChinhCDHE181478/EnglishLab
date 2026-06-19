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

    ClassroomMaterialResponse createMaterial(Long offeringId, CreateMaterialRequest request, String uploaderEmail);

    void deleteMaterial(Long materialId);

    List<ClassroomAnnouncementResponse> getAnnouncements(Long offeringId);

    ClassroomAnnouncementResponse createAnnouncement(Long offeringId, CreateAnnouncementRequest request, String creatorEmail);

    void deleteAnnouncement(Long announcementId);

    List<ClassroomSyllabusItemResponse> getSyllabus(Long offeringId);

    ClassroomSyllabusItemResponse createSyllabusItem(Long offeringId, CreateSyllabusItemRequest request);

    ClassroomSyllabusItemResponse updateSyllabusItem(Long itemId, CreateSyllabusItemRequest request);

    void deleteSyllabusItem(Long itemId);
}
