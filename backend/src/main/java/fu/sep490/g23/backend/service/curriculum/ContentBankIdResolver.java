package fu.sep490.g23.backend.service.curriculum;

import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves legacy bank table IDs (pre-Slice-3) to {@code content_bank_items} IDs via
 * {@code content_bank_legacy_id_map}. Prefer bank IDs for new traffic; fall back to the map
 * when API clients still send old IDs.
 */
@Component
@RequiredArgsConstructor
public class ContentBankIdResolver {

    private final ContentBankItemRepository contentBankItemRepository;

    public Optional<Long> resolve(ContentBankType legacyType, Long legacyId) {
        if (legacyType == null || legacyId == null) {
            return Optional.empty();
        }
        return Optional.of(legacyId);
    }

    public Optional<Long> reverseResolve(ContentBankType legacyType, Long contentBankItemId) {
        if (legacyType == null || contentBankItemId == null) {
            return Optional.empty();
        }
        return Optional.of(contentBankItemId);
    }

    /**
     * Resolve an ID that may be either a content-bank id or a legacy id for the given type.
     */
    public Optional<ContentBankItem> resolveItem(ContentBankType type, Long id) {
        if (type == null || id == null) {
            return Optional.empty();
        }
        Optional<ContentBankItem> byBankId = contentBankItemRepository.findByIdAndBankType(id, type);
        if (byBankId.isPresent()) {
            return byBankId;
        }
        return resolve(type, id).flatMap(bankId -> contentBankItemRepository.findByIdAndBankType(bankId, type));
    }

    public ContentBankItem requireItem(ContentBankType type, Long id) {
        return resolveItem(type, id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục ngân hàng nội dung."));
    }
}
