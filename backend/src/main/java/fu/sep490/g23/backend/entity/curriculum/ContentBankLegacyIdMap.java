package fu.sep490.g23.backend.entity.curriculum;

import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "content_bank_legacy_id_map")
@IdClass(ContentBankLegacyIdMap.Pk.class)
public class ContentBankLegacyIdMap {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "legacy_type", nullable = false, length = 30)
    private ContentBankType legacyType;

    @Id
    @Column(name = "legacy_id", nullable = false)
    private Long legacyId;

    @Column(name = "content_bank_item_id", nullable = false)
    private Long contentBankItemId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private ContentBankType legacyType;
        private Long legacyId;
    }
}
