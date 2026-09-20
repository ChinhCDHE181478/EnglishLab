package fu.sep490.g23.backend.service.payment;

public record CheckoutPriceSnapshot(
        Long originalAmount,
        Long systemDiscountAmount,
        Long learningPathDiscountAmount,
        Long couponDiscountAmount,
        Long finalAmount
) {
    public boolean isComplete() {
        return originalAmount != null
                && systemDiscountAmount != null
                && learningPathDiscountAmount != null
                && couponDiscountAmount != null
                && finalAmount != null;
    }
}
