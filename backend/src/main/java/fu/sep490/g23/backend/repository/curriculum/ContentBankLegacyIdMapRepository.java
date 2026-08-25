package fu.sep490.g23.backend.repository.curriculum;

import fu.sep490.g23.backend.entity.curriculum.ContentBankLegacyIdMap;
import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentBankLegacyIdMapRepository extends JpaRepository<ContentBankLegacyIdMap, ContentBankLegacyIdMap.Pk> {
    Optional<ContentBankLegacyIdMap> findByLegacyTypeAndLegacyId(ContentBankType legacyType, Long legacyId);

    Optional<ContentBankLegacyIdMap> findByContentBankItemId(Long contentBankItemId);

    Optional<ContentBankLegacyIdMap> findByLegacyTypeAndContentBankItemId(ContentBankType legacyType, Long contentBankItemId);
}
