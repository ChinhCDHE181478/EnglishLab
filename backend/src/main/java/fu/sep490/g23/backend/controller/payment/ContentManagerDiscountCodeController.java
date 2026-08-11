package fu.sep490.g23.backend.controller.payment;

import fu.sep490.g23.backend.dto.request.payment.DiscountCodeRequest;
import fu.sep490.g23.backend.dto.response.payment.DiscountCodeResponse;
import fu.sep490.g23.backend.service.payment.DiscountCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content-manager/discount-codes")
@RequiredArgsConstructor
public class ContentManagerDiscountCodeController {

    private final DiscountCodeService discountCodeService;

    @GetMapping
    public ResponseEntity<Page<DiscountCodeResponse>> getDiscountCodes(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(discountCodeService.getDiscountCodes(keyword, includeInactive, pageable));
    }

    @PostMapping
    public ResponseEntity<DiscountCodeResponse> createDiscountCode(
            @Valid @RequestBody DiscountCodeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discountCodeService.createDiscountCode(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscountCodeResponse> updateDiscountCode(
            @PathVariable Long id,
            @Valid @RequestBody DiscountCodeRequest request
    ) {
        return ResponseEntity.ok(discountCodeService.updateDiscountCode(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiscountCode(@PathVariable Long id) {
        discountCodeService.deleteDiscountCode(id);
        return ResponseEntity.noContent().build();
    }
}
