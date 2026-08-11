package fu.sep490.g23.backend.service.commerce;

import fu.sep490.g23.backend.dto.response.commerce.CommerceCourseItemResponse;

import java.util.List;

public interface StudentCommerceService {
    List<CommerceCourseItemResponse> getCart(String studentEmail);
    CommerceCourseItemResponse addToCart(Long courseId, String studentEmail);
    void removeFromCart(Long courseId, String studentEmail);
    void clearCart(String studentEmail);
    List<CommerceCourseItemResponse> getWishlist(String studentEmail);
    CommerceCourseItemResponse addToWishlist(Long courseId, String studentEmail);
    void removeFromWishlist(Long courseId, String studentEmail);
    CommerceCourseItemResponse moveWishlistToCart(Long courseId, String studentEmail);
    List<CommerceCourseItemResponse> syncCart(List<Long> courseIds, String studentEmail);
}
