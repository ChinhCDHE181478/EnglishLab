package fu.sep490.g23.backend.repository.curriculum;

import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ContentBankItemRepository extends JpaRepository<ContentBankItem, Long>, JpaSpecificationExecutor<ContentBankItem> {
    List<ContentBankItem> findByBankTypeOrderByUpdatedAtDescIdDesc(ContentBankType bankType);

    List<ContentBankItem> findByBankTypeAndActiveTrueOrderByUpdatedAtDescIdDesc(ContentBankType bankType);

    Optional<ContentBankItem> findByIdAndBankType(Long id, ContentBankType bankType);

    Optional<ContentBankItem> findByCodeAndBankType(String code, ContentBankType bankType);

    Optional<ContentBankItem> findByCodeIgnoreCaseAndBankType(String code, ContentBankType bankType);

    List<ContentBankItem> findByBankTypeAndStatusAndActiveTrue(ContentBankType bankType, String status);
}
