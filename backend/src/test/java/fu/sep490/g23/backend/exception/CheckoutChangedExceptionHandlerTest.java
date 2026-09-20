package fu.sep490.g23.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutChangedExceptionHandlerTest {

    @Test
    void returnsConflictWithStablePublicCodeAndMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleCheckoutChangedException(new CheckoutChangedException("PRICE_CHANGED"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CHECKOUT_CHANGED");
        assertThat(response.getBody().getMessage()).isEqualTo(
                "Giá khóa học đã được cập nhật. Vui lòng kiểm tra lại thông tin thanh toán trước khi tiếp tục."
        );
    }
}
