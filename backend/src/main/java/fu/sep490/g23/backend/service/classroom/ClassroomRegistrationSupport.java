package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionPaymentKind;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;

public final class ClassroomRegistrationSupport {

    public static final int TUITION_DEPOSIT_PERCENT = 30;
    public static final int TUITION_BALANCE_DUE_DAYS_BEFORE_START = 7;
    public static final int LATE_APPROVAL_PAYMENT_HOURS = 24;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private ClassroomRegistrationSupport() {
    }

    /** Hồ sơ đã được duyệt giữ chỗ cho tới khi thanh toán đủ hoặc hết hạn. */
    public static final Set<ClassroomRegistrationStatus> OCCUPIES_CLASS_SLOT = EnumSet.of(
            ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT,
            ClassroomRegistrationStatus.DEPOSIT_PAID,
            ClassroomRegistrationStatus.PARTIALLY_PAID,
            ClassroomRegistrationStatus.FULLY_PAID,
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
            ClassroomRegistrationStatus.SUSPENDED,
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

    public static BigDecimal requiredDeposit(BigDecimal amountDue) {
        BigDecimal due = amountDue == null ? BigDecimal.ZERO : amountDue.max(BigDecimal.ZERO);
        return due.multiply(BigDecimal.valueOf(TUITION_DEPOSIT_PERCENT))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    public static BigDecimal remainingDeposit(BigDecimal amountDue, BigDecimal amountPaid) {
        BigDecimal paid = amountPaid == null ? BigDecimal.ZERO : amountPaid.max(BigDecimal.ZERO);
        return requiredDeposit(amountDue).subtract(paid).max(BigDecimal.ZERO);
    }

    public static LocalDateTime tuitionPaymentDeadline(ClassEnrollment enrollment) {
        if (enrollment == null || enrollment.getClassSection() == null
                || enrollment.getClassSection().getStartDate() == null) {
            return null;
        }
        LocalDateTime regularDeadline = enrollment.getClassSection().getStartDate()
                .minusDays(TUITION_BALANCE_DUE_DAYS_BEFORE_START)
                .atTime(LocalTime.MAX);
        LocalDateTime invitedAt = enrollment.getEnrolledAt();
        if (invitedAt == null || !invitedAt.isAfter(regularDeadline)) {
            return regularDeadline;
        }
        LocalDateTime lateDeadline = invitedAt.plusHours(LATE_APPROVAL_PAYMENT_HOURS);
        LocalDateTime classStartDayEnd = enrollment.getClassSection().getStartDate().atTime(LocalTime.MAX);
        return lateDeadline.isBefore(classStartDayEnd) ? lateDeadline : classStartDayEnd;
    }

    public static boolean requiresFullTuitionPayment(ClassEnrollment enrollment) {
        if (enrollment == null || enrollment.getClassSection() == null
                || enrollment.getClassSection().getStartDate() == null
                || enrollment.getEnrolledAt() == null) {
            return false;
        }
        LocalDateTime regularDeadline = enrollment.getClassSection().getStartDate()
                .minusDays(TUITION_BALANCE_DUE_DAYS_BEFORE_START)
                .atTime(LocalTime.MAX);
        return enrollment.getEnrolledAt().isAfter(regularDeadline);
    }

    public static boolean isTuitionPaymentOverdue(ClassEnrollment enrollment, LocalDateTime now) {
        LocalDateTime deadline = tuitionPaymentDeadline(enrollment);
        return deadline != null && now != null && now.isAfter(deadline)
                && enrollment.tuitionBalance().compareTo(BigDecimal.ZERO) > 0;
    }

    public static LocalDateTime currentBusinessTime() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }

    public static TuitionPaymentKind classifyTuitionPayment(
            BigDecimal amountDue,
            BigDecimal amountPaidBefore,
            BigDecimal paymentAmount
    ) {
        BigDecimal due = amountDue == null ? BigDecimal.ZERO : amountDue;
        BigDecimal paidBefore = amountPaidBefore == null ? BigDecimal.ZERO : amountPaidBefore;
        BigDecimal paidAfter = paidBefore.add(paymentAmount == null ? BigDecimal.ZERO : paymentAmount);
        if (due.compareTo(BigDecimal.ZERO) > 0 && paidAfter.compareTo(due) >= 0) {
            return TuitionPaymentKind.FULL;
        }
        BigDecimal deposit = requiredDeposit(due);
        if (deposit.compareTo(BigDecimal.ZERO) > 0
                && paidBefore.compareTo(deposit) < 0
                && paidAfter.compareTo(deposit) >= 0) {
            return TuitionPaymentKind.DEPOSIT;
        }
        return TuitionPaymentKind.PARTIAL;
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
            case SUSPENDED -> "Đang bảo lưu";
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
