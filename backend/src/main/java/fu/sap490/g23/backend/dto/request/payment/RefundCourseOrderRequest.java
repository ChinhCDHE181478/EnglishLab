package fu.sap490.g23.backend.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundCourseOrderRequest {

    @NotBlank(message = "Vui long nhap ly do hoan tien.")
    @Size(max = 500, message = "Ly do hoan tien toi da 500 ky tu.")
    private String reason;
}
