package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CenterMaterialLibraryItemRepository extends JpaRepository<CenterMaterialLibraryItem, Long> {

    List<CenterMaterialLibraryItem> findAllByOrderByUpdatedAtDescIdDesc();

    List<CenterMaterialLibraryItem> findByStatusIgnoreCaseOrderByUpdatedAtDescIdDesc(String status);
}
