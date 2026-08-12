package fu.sep490.g23.backend.service.payment.impl;


import fu.sep490.g23.backend.dto.request.payment.DiscountCodeRequest;
import fu.sep490.g23.backend.dto.response.payment.DiscountCodeResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.payment.DiscountCode;
import fu.sep490.g23.backend.entity.payment.enums.DiscountType;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.payment.DiscountCodeRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.service.payment.DiscountCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class DiscountCodeServiceImpl implements DiscountCodeService {

    private final DiscountCodeRepository discountCodeRepository;
    private final UserRepository userRepository;
    private final PaymentOrderRepository paymentOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<DiscountCodeResponse> getDiscountCodes(String keyword, boolean includeInactive, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Page<DiscountCode> sourcePage = includeInactive
                ? discountCodeRepository.findAll(pageable)
                : discountCodeRepository.findByActiveTrue(pageable);
        Page<DiscountCodeResponse> page = sourcePage.map(this::toResponse);
        if (normalizedKeyword.isBlank()) {
            return page;
        }
        List<DiscountCodeResponse> filtered = page.getContent().stream()
                .filter(response -> response.getCode().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || response.getName().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .toList();
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    @Override
    public DiscountCodeResponse createDiscountCode(DiscountCodeRequest request, String creatorEmail) {
        String normalizedCode = normalizeCode(request.getCode());
        if (discountCodeRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new RuntimeException("Mã giảm giá này đã tồn tại.");
        }

        User creator = userRepository.findByEmail(creatorEmail).orElse(null);
        DiscountCode discountCode = DiscountCode.builder()
                .code(normalizedCode)
                .name(request.getName().trim())
                .type(request.getType())
                .value(normalizeValue(request.getType(), request.getValue()))
                .usageLimit(request.getUsageLimit())
                .active(request.getActive() == null || request.getActive())
                .startsAt(request.getStartsAt())
                .expiresAt(request.getExpiresAt())
                .createdBy(creator)
                .build();

        validateDateRange(discountCode);
        return toResponse(discountCodeRepository.save(discountCode));
    }

    @Override
    public DiscountCodeResponse updateDiscountCode(Long id, DiscountCodeRequest request) {
        DiscountCode discountCode = discountCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá."));
        String normalizedCode = normalizeCode(request.getCode());
        discountCodeRepository.findByCodeIgnoreCase(normalizedCode)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new RuntimeException("Mã giảm giá này đã tồn tại.");
                });

        discountCode.setCode(normalizedCode);
        discountCode.setName(request.getName().trim());
        discountCode.setType(request.getType());
        discountCode.setValue(normalizeValue(request.getType(), request.getValue()));
        discountCode.setUsageLimit(Math.max(request.getUsageLimit(), discountCode.getUsedCount()));
        discountCode.setActive(request.getActive() == null || request.getActive());
        discountCode.setStartsAt(request.getStartsAt());
        discountCode.setExpiresAt(request.getExpiresAt());
        validateDateRange(discountCode);
        return toResponse(discountCode);
    }

    @Override
    public void deleteDiscountCode(Long id) {
        DiscountCode discountCode = discountCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá."));
        int usedCount = safeCount(discountCode.getUsedCount());
        int reservedCount = safeCount(discountCode.getReservedCount());
        long paymentReferences = paymentOrderRepository.countByDiscountCode_Id(id);

        if (usedCount == 0 && reservedCount == 0 && paymentReferences == 0) {
            discountCodeRepository.delete(discountCode);
            return;
        }

        discountCode.setActive(false);
        discountCodeRepository.save(discountCode);
    }

    private DiscountCodeResponse toResponse(DiscountCode discountCode) {
        int usedCount = safeCount(discountCode.getUsedCount());
        int reservedCount = safeCount(discountCode.getReservedCount());
        int usageLimit = safeCount(discountCode.getUsageLimit());
        return DiscountCodeResponse.builder()
                .id(discountCode.getId())
                .code(discountCode.getCode())
                .name(discountCode.getName())
                .type(discountCode.getType())
                .value(discountCode.getValue())
                .usageLimit(discountCode.getUsageLimit())
                .usedCount(usedCount)
                .reservedCount(reservedCount)
                .remainingUses(Math.max(0, usageLimit - usedCount - reservedCount))
                .active(discountCode.isActive())
                .startsAt(discountCode.getStartsAt())
                .expiresAt(discountCode.getExpiresAt())
                .createdAt(discountCode.getCreatedAt())
                .updatedAt(discountCode.getUpdatedAt())
                .build();
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeValue(DiscountType type, BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        if (type == DiscountType.PERCENTAGE && safeValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("Phần trăm giảm giá không được vượt quá 100%.");
        }
        return safeValue;
    }

    private void validateDateRange(DiscountCode discountCode) {
        if (discountCode.getStartsAt() != null
                && discountCode.getExpiresAt() != null
                && !discountCode.getStartsAt().isBefore(discountCode.getExpiresAt())) {
            throw new RuntimeException("Thời gian bắt đầu phải trước thời gian hết hạn.");
        }
    }

    private int safeCount(Integer value) {
        return Math.max(0, value == null ? 0 : value);
    }
}
