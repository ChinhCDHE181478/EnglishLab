package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CenterMaterialLibraryUpsertRequest;
import fu.sap490.g23.backend.dto.response.classroom.CenterMaterialLibraryItemResponse;
import java.util.List;

public interface CenterMaterialLibraryService {

    List<CenterMaterialLibraryItemResponse> listForContentManager();

    List<CenterMaterialLibraryItemResponse> listPublishedForTeacher();

    CenterMaterialLibraryItemResponse create(CenterMaterialLibraryUpsertRequest request, String actorEmail);

    CenterMaterialLibraryItemResponse update(Long materialId, CenterMaterialLibraryUpsertRequest request, String actorEmail);

    void delete(Long materialId, String actorEmail);
}
