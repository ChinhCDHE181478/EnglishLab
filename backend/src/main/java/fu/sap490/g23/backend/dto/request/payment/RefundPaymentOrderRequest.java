package fu.sap490.g23.backend.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundPaymentOrderRequest {

    @NotBlank(message = "Vui lòng nhập lý do hoàn tiền.")
    @Size(max = 500)
    private String reason;
}
