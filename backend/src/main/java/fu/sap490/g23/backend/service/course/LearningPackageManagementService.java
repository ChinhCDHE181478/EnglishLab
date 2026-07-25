package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.LearningPackageRequest;
import fu.sap490.g23.backend.dto.request.course.UpdatePackageBundleItemsRequest;
import fu.sap490.g23.backend.dto.response.course.LearningPackageResponse;
import fu.sap490.g23.backend.dto.response.course.LearningPackageSummaryResponse;
import fu.sap490.g23.backend.dto.response.course.PackageTypeResponse;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageTypeCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LearningPackageManagementService {

    List<PackageTypeResponse> getPackageTypes();

    Page<LearningPackageResponse> listPackages(
            String keyword,
            PackageTypeCode packageTypeCode,
            PackageStatus status,
            Pageable pageable
    );

    List<LearningPackageSummaryResponse> listBundleCandidates();

    LearningPackageResponse getPackage(Long id);

    LearningPackageResponse createBundle(LearningPackageRequest request, String creatorEmail);

    LearningPackageResponse updateBundle(Long id, LearningPackageRequest request);

    LearningPackageResponse replaceBundleItems(Long id, UpdatePackageBundleItemsRequest request);

    LearningPackageResponse publishBundle(Long id, String actorEmail);

    LearningPackageResponse archiveBundle(Long id);

    void deleteBundle(Long id);
}
