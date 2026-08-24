package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.CenterMaterialLibraryUpsertRequest;
import fu.sep490.g23.backend.dto.response.classroom.CenterMaterialLibraryItemResponse;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CenterMaterialLibraryService {

    List<CenterMaterialLibraryItemResponse> listForContentManager();

    Page<CenterMaterialLibraryItemResponse> pageForContentManager(String keyword, String examCategory, String materialType, String skill, String status, String provider, Pageable pageable);

    Map<String, Long> getStats();

    List<String> listProviders();

    CenterMaterialLibraryItemResponse create(CenterMaterialLibraryUpsertRequest request, String actorEmail);

    CenterMaterialLibraryItemResponse update(Long materialId, CenterMaterialLibraryUpsertRequest request, String actorEmail);

    void delete(Long materialId, String actorEmail);
}
