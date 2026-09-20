package fu.sep490.g23.backend.exception;

import lombok.Getter;

@Getter
public class CheckoutChangedException extends RuntimeException {

    private final String reason;

    public CheckoutChangedException(String reason) {
        super("Giá khóa học đã được cập nhật. Vui lòng kiểm tra lại thông tin thanh toán trước khi tiếp tục.");
        this.reason = reason;
    }
}
