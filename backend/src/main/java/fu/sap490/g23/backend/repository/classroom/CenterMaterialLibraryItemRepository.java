package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CenterMaterialLibraryItemRepository extends JpaRepository<CenterMaterialLibraryItem, Long> {

    List<CenterMaterialLibraryItem> findAllByOrderByUpdatedAtDescIdDesc();

    Optional<CenterMaterialLibraryItem> findFirstByFileUrlEndingWith(String suffix);

    boolean existsByFileUrlEndingWith(String suffix);

}
