package fu.sap490.g23.backend.dto.request.payment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreatePaymentLinkRequest {

    private List<@NotNull(message = "Mã khóa học không hợp lệ.") Long> courseIds;

    private List<@NotNull(message = "Mã lớp học không hợp lệ.") Long> classroomOfferingIds;

    @Size(max = 40)
    private String couponCode;
}
