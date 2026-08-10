package fu.sap490.g23.backend.service.payment;

import fu.sap490.g23.backend.dto.request.payment.DiscountCodeRequest;
import fu.sap490.g23.backend.dto.response.payment.DiscountCodeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DiscountCodeService {
    Page<DiscountCodeResponse> getDiscountCodes(String keyword, boolean includeInactive, Pageable pageable);
    DiscountCodeResponse createDiscountCode(DiscountCodeRequest request, String creatorEmail);
    DiscountCodeResponse updateDiscountCode(Long id, DiscountCodeRequest request);
    void deleteDiscountCode(Long id);
}
