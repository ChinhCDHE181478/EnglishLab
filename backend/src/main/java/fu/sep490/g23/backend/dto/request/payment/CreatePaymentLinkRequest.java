package fu.sep490.g23.backend.dto.request.payment;

import fu.sep490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
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

    private Long learningPathId;

    @Size(max = 40)
    private String couponCode;

    private Long displayedOriginalAmount;

    private Long displayedSystemDiscountAmount;

    private Long displayedLearningPathDiscountAmount;

    private Long displayedCouponDiscountAmount;

    private Long finalAmount;

    /** Chỉ áp dụng cho học phí lớp: DEPOSIT hoặc FULL. */
    private TuitionPaymentKind classroomPaymentKind;
}
