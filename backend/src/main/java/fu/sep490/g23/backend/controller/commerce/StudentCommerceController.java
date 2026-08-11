package fu.sep490.g23.backend.controller.commerce;

import fu.sep490.g23.backend.dto.response.commerce.CommerceCourseItemResponse;
import fu.sep490.g23.backend.service.commerce.StudentCommerceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/commerce")
@RequiredArgsConstructor
public class StudentCommerceController {

    private final StudentCommerceService commerceService;

    @GetMapping("/cart")
    public ResponseEntity<List<CommerceCourseItemResponse>> getCart(Authentication authentication) {
        return ResponseEntity.ok(commerceService.getCart(authentication.getName()));
    }

    @PostMapping("/cart/{courseId}")
    public ResponseEntity<CommerceCourseItemResponse> addToCart(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(commerceService.addToCart(courseId, authentication.getName()));
    }

    @DeleteMapping("/cart/{courseId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long courseId, Authentication authentication) {
        commerceService.removeFromCart(courseId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cart")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        commerceService.clearCart(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cart/sync")
    public ResponseEntity<List<CommerceCourseItemResponse>> syncCart(
            @RequestBody Map<String, List<Long>> body,
            Authentication authentication
    ) {
        return ResponseEntity.ok(commerceService.syncCart(body.get("courseIds"), authentication.getName()));
    }

    @GetMapping("/wishlist")
    public ResponseEntity<List<CommerceCourseItemResponse>> getWishlist(Authentication authentication) {
        return ResponseEntity.ok(commerceService.getWishlist(authentication.getName()));
    }

    @PostMapping("/wishlist/{courseId}")
    public ResponseEntity<CommerceCourseItemResponse> addToWishlist(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(commerceService.addToWishlist(courseId, authentication.getName()));
    }

    @DeleteMapping("/wishlist/{courseId}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long courseId, Authentication authentication) {
        commerceService.removeFromWishlist(courseId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/wishlist/{courseId}/move-to-cart")
    public ResponseEntity<CommerceCourseItemResponse> moveToCart(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(commerceService.moveWishlistToCart(courseId, authentication.getName()));
    }
}
