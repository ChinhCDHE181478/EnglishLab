package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.PackageBundleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageBundleItemRepository extends JpaRepository<PackageBundleItem, Long> {

    List<PackageBundleItem> findByBundlePackageIdOrderByDisplayOrderAscIdAsc(Long bundlePackageId);

    long countByBundlePackageId(Long bundlePackageId);

    @Modifying(flushAutomatically = true)
    @Query("delete from PackageBundleItem item where item.bundlePackage.id = :bundlePackageId")
    void deleteByBundlePackageId(@Param("bundlePackageId") Long bundlePackageId);
}
