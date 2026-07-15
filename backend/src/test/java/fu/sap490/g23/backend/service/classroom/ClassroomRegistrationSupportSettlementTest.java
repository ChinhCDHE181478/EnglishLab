package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.entity.classroom.enums.TuitionSettlementStatus;
import fu.sap490.g23.backend.entity.classroom.enums.TuitionSettlementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClassroomRegistrationSupportSettlementTest {

    @Test
    void computeSettlement_ReturnsNeedRefundWhenOverpaid() {
        assertEquals(
                TuitionSettlementType.NEED_REFUND,
                ClassroomRegistrationSupport.computeSettlement(bd("1000000"), bd("1500000"))
        );
    }

    @Test
    void computeSettlement_ReturnsNeedAdditionalWhenUnderpaid() {
        assertEquals(
                TuitionSettlementType.NEED_ADDITIONAL_PAYMENT,
                ClassroomRegistrationSupport.computeSettlement(bd("1000000"), bd("200000"))
        );
    }

    @Test
    void computeSettlement_ReturnsNoneWhenBalanced() {
        assertEquals(
                TuitionSettlementType.NONE,
                ClassroomRegistrationSupport.computeSettlement(bd("1000000"), bd("1000000"))
        );
    }

    @Test
    void applyComputedSettlement_SetsPendingNeedRefund() {
        ClassroomEnrollment enrollment = ClassroomEnrollment.builder()
                .tuitionAmountDue(bd("1000000"))
                .tuitionAmountPaid(bd("1500000"))
                .tuitionSettlementStatus(TuitionSettlementStatus.NONE)
                .build();

        ClassroomRegistrationSupport.applyComputedSettlement(enrollment);

        assertEquals(TuitionSettlementType.NEED_REFUND, enrollment.getTuitionSettlementType());
        assertEquals(TuitionSettlementStatus.PENDING, enrollment.getTuitionSettlementStatus());
        assertEquals("Cần xử lý hoàn tiền 500000 VND.", enrollment.getTuitionSettlementNote());
    }

    @Test
    void markNeedRefundForExit_IgnoresZeroPaid() {
        ClassroomEnrollment enrollment = ClassroomEnrollment.builder()
                .tuitionAmountPaid(BigDecimal.ZERO)
                .tuitionSettlementType(TuitionSettlementType.NONE)
                .build();

        ClassroomRegistrationSupport.markNeedRefundForExit(enrollment, "Cần xử lý hoàn tiền do hủy");

        assertEquals(TuitionSettlementType.NONE, enrollment.getTuitionSettlementType());
        assertNull(enrollment.getTuitionSettlementNote());
    }

    @Test
    void markNeedRefundForExit_FlagsPendingRefund() {
        ClassroomEnrollment enrollment = ClassroomEnrollment.builder()
                .registrationStatus(ClassroomRegistrationStatus.CANCELLED)
                .tuitionAmountPaid(bd("500000"))
                .build();

        ClassroomRegistrationSupport.markNeedRefundForExit(enrollment, "Cần xử lý hoàn tiền do học viên hủy đăng ký");

        assertEquals(TuitionSettlementType.NEED_REFUND, enrollment.getTuitionSettlementType());
        assertEquals(TuitionSettlementStatus.PENDING, enrollment.getTuitionSettlementStatus());
        assertEquals(
                "Cần xử lý hoàn tiền do học viên hủy đăng ký 500000 VND.",
                enrollment.getTuitionSettlementNote()
        );
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
