package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CenterMaterialLibraryItemRepository extends JpaRepository<CenterMaterialLibraryItem, Long>, JpaSpecificationExecutor<CenterMaterialLibraryItem> {

    List<CenterMaterialLibraryItem> findAllByOrderByUpdatedAtDescIdDesc();

    Optional<CenterMaterialLibraryItem> findFirstByFileUrlEndingWith(String suffix);

    long countByStatus(String status);

    long countByExamCategory(String examCategory);

    @Query("select distinct item.provider from CenterMaterialLibraryItem item where item.provider is not null and item.provider <> '' order by item.provider")
    List<String> findDistinctProviders();

    boolean existsByFileUrlEndingWith(String suffix);

}
