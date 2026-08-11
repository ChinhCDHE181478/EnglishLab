package fu.sep490.g23.backend.dto.request.payment;

import fu.sep490.g23.backend.entity.payment.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class DiscountCodeRequest {

    @NotBlank(message = "Mã giảm giá không được để trống.")
    @Size(max = 40)
    private String code;

    @NotBlank(message = "Tên mã giảm giá không được để trống.")
    @Size(max = 180)
    private String name;

    @NotNull(message = "Loại giảm giá không hợp lệ.")
    private DiscountType type;

    @NotNull(message = "Giá trị giảm giá không được để trống.")
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0.")
    private BigDecimal value;

    @NotNull(message = "Giới hạn sử dụng không được để trống.")
    @Min(value = 1, message = "Giới hạn sử dụng phải lớn hơn 0.")
    private Integer usageLimit;

    private Boolean active;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
}
