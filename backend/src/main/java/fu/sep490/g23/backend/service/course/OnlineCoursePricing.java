package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.entity.course.OnlineCourse;

import java.math.BigDecimal;

public final class OnlineCoursePricing {

    private OnlineCoursePricing() {
    }

    public static BigDecimal originalPrice(OnlineCourse course) {
        return course == null || course.getPrice() == null ? BigDecimal.ZERO : course.getPrice();
    }

    public static BigDecimal effectivePrice(OnlineCourse course) {
        BigDecimal originalPrice = originalPrice(course);
        BigDecimal salePrice = course == null ? null : course.getSalePrice();
        if (salePrice == null
                || salePrice.compareTo(BigDecimal.ZERO) < 0
                || salePrice.compareTo(originalPrice) >= 0) {
            return originalPrice;
        }
        return salePrice;
    }

    public static boolean isFree(OnlineCourse course) {
        return effectivePrice(course).compareTo(BigDecimal.ZERO) <= 0;
    }
}
