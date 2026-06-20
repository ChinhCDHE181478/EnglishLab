package fu.sap490.g23.backend.dto.response.payment;

import fu.sap490.g23.backend.entity.payment.enums.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DiscountCodeResponse {
    private Long id;
    private String code;
    private String name;
    private DiscountType type;
    private BigDecimal value;
    private Integer usageLimit;
    private Integer usedCount;
    private Integer reservedCount;
    private Integer remainingUses;
    private boolean active;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
