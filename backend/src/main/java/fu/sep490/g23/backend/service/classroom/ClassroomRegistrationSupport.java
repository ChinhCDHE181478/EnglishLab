package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionPaymentKind;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

public final class ClassroomRegistrationSupport {

    private ClassroomRegistrationSupport() {
    }

    /** Học viên đã được xếp lớp — chiếm sĩ số và có quyền học. */
    public static final Set<ClassroomRegistrationStatus> OCCUPIES_CLASS_SLOT = EnumSet.of(
            ClassroomRegistrationStatus.ASSIGNED
    );

    /** Đăng ký còn hiệu lực (chưa hủy). */
    public static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATIONS = EnumSet.of(
            ClassroomRegistrationStatus.PENDING_CONFIRMATION,
            ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT,
            ClassroomRegistrationStatus.DEPOSIT_PAID,
            ClassroomRegistrationStatus.PARTIALLY_PAID,
            ClassroomRegistrationStatus.FULLY_PAID,
            ClassroomRegistrationStatus.ASSIGNED,
            ClassroomRegistrationStatus.WAITLIST
    );

    public static final Set<ClassroomRegistrationStatus> INACTIVE_REGISTRATIONS = EnumSet.of(
            ClassroomRegistrationStatus.REJECTED,
            ClassroomRegistrationStatus.CANCELLED
    );

    public static final Set<ClassroomRegistrationStatus> HAS_LEARNING_ACCESS = EnumSet.of(
            ClassroomRegistrationStatus.ASSIGNED
    );

    /** Hồ sơ đăng ký cần Nhân viên đào tạo xử lý (không gồm đã xếp lớp / từ chối / hủy). */
    public static final Set<ClassroomRegistrationStatus> NEEDS_ACTION_STATUSES = EnumSet.of(
            ClassroomRegistrationStatus.PENDING_CONFIRMATION,
            ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT,
            ClassroomRegistrationStatus.DEPOSIT_PAID,
            ClassroomRegistrationStatus.PARTIALLY_PAID,
            ClassroomRegistrationStatus.FULLY_PAID,
            ClassroomRegistrationStatus.WAITLIST
    );

    public static Set<ClassroomRegistrationStatus> allRegistrationStatuses() {
        return EnumSet.allOf(ClassroomRegistrationStatus.class);
    }

    public static Set<ClassroomRegistrationStatus> filterStatuses(ClassroomRegistrationStatus status) {
        if (status == null) {
            return allRegistrationStatuses();
        }
        return EnumSet.of(status);
    }

    public static Set<ClassroomRegistrationStatus> resolveRegistrationFilter(
            ClassroomRegistrationStatus status,
            Boolean needsAction
    ) {
        if (Boolean.TRUE.equals(needsAction)) {
            return NEEDS_ACTION_STATUSES;
        }
        return filterStatuses(status);
    }

    public static ClassroomRegistrationStatus resolveRegistrationStatusAfterPayment(
            java.math.BigDecimal amountDue,
            java.math.BigDecimal amountPaid,
            java.math.BigDecimal depositPaid,
            TuitionPaymentKind paymentKind
    ) {
        java.math.BigDecimal due = amountDue == null ? java.math.BigDecimal.ZERO : amountDue;
        java.math.BigDecimal paid = amountPaid == null ? java.math.BigDecimal.ZERO : amountPaid;

        if (due.compareTo(java.math.BigDecimal.ZERO) > 0 && paid.compareTo(due) >= 0) {
            return ClassroomRegistrationStatus.FULLY_PAID;
        }
        if (paymentKind == TuitionPaymentKind.DEPOSIT
                || (depositPaid != null && depositPaid.compareTo(java.math.BigDecimal.ZERO) > 0)) {
            return ClassroomRegistrationStatus.DEPOSIT_PAID;
        }
        if (paid.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return ClassroomRegistrationStatus.PARTIALLY_PAID;
        }
        return ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT;
    }

    public static void applyComputedSettlement(ClassEnrollment enrollment) {
    }

    public static void markNeedRefundForExit(ClassEnrollment enrollment, String reasonPrefix) {
    }

    public static void clearOpenSettlement(ClassEnrollment enrollment) {
    }

    public static void clearOpenSettlementAsResolved(ClassEnrollment enrollment, String note) {
    }

    public static String tuitionSettlementStatusLabel(Object status) {
        return null;
    }

    public static String tuitionSettlementLabel(Object type) {
        return null;
    }

    public static String registrationStatusLabel(ClassroomRegistrationStatus status) {
        if (status == null) {
            return "Đang cập nhật";
        }
        return switch (status) {
            case PENDING_CONFIRMATION -> "Chờ xác nhận";
            case PENDING_TUITION_PAYMENT -> "Chờ thanh toán học phí";
            case DEPOSIT_PAID -> "Đã đặt cọc";
            case PARTIALLY_PAID -> "Thanh toán một phần";
            case FULLY_PAID -> "Đã thanh toán đủ";
            case ASSIGNED -> "Đã được xếp lớp";
            case WAITLIST -> "Chờ xếp lớp";
            case REJECTED -> "Từ chối";
            case CANCELLED -> "Đã hủy";
        };
    }

    public static String tuitionPaymentKindLabel(TuitionPaymentKind kind) {
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case DEPOSIT -> "Đặt cọc";
            case PARTIAL -> "Thanh toán một phần";
            case FULL -> "Thanh toán đủ";
            case MANUAL_CONFIRMATION -> "Xác nhận thủ công";
            case REFUND -> "Hoàn tiền học phí";
        };
    }
}
