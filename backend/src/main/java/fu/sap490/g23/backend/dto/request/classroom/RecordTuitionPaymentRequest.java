package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.classroom.TuitionPaymentKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RecordTuitionPaymentRequest {
    @NotNull(message = "Số tiền không hợp lệ.")
    @DecimalMin(value = "0", message = "Số tiền phải lớn hơn hoặc bằng 0.")
    private BigDecimal amount;

    @NotNull(message = "Loại thanh toán không hợp lệ.")
    private TuitionPaymentKind paymentKind;

    private String note;

    /** Nếu true, tự động xếp lớp khi đã thanh toán đủ */
    private boolean assignIfFullyPaid = true;
}
