package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
import fu.sap490.g23.backend.entity.classroom.enums.TuitionSettlementStatus;
import fu.sap490.g23.backend.entity.classroom.enums.TuitionSettlementType;

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

    /** Hồ sơ đăng ký cần Training Manager xử lý (không gồm đã xếp lớp / từ chối / hủy). */
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

    @SuppressWarnings("deprecation")
    public static void syncLegacyStatus(fu.sap490.g23.backend.entity.classroom.ClassroomEnrollment enrollment) {
        ClassroomRegistrationStatus registrationStatus = enrollment.getRegistrationStatus();
        if (registrationStatus == null) {
            return;
        }
        ClassroomEnrollmentStatus legacy = switch (registrationStatus) {
            case ASSIGNED -> ClassroomEnrollmentStatus.ENROLLED;
            case WAITLIST,
                 PENDING_CONFIRMATION,
                 PENDING_TUITION_PAYMENT,
                 DEPOSIT_PAID,
                 PARTIALLY_PAID,
                 FULLY_PAID -> ClassroomEnrollmentStatus.WAITING;
            case REJECTED, CANCELLED -> ClassroomEnrollmentStatus.CANCELLED;
        };
        enrollment.setStatus(legacy);
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

    public static TuitionSettlementType computeSettlement(java.math.BigDecimal amountDue, java.math.BigDecimal amountPaid) {
        java.math.BigDecimal due = amountDue == null ? java.math.BigDecimal.ZERO : amountDue;
        java.math.BigDecimal paid = amountPaid == null ? java.math.BigDecimal.ZERO : amountPaid;
        int compare = paid.compareTo(due);
        if (compare < 0) {
            return TuitionSettlementType.NEED_ADDITIONAL_PAYMENT;
        }
        if (compare > 0) {
            return TuitionSettlementType.NEED_REFUND;
        }
        return TuitionSettlementType.NONE;
    }

    public static String buildSettlementNote(TuitionSettlementType type, BigDecimal amountDue, BigDecimal amountPaid) {
        if (type == null || type == TuitionSettlementType.NONE) {
            return null;
        }
        BigDecimal due = amountDue == null ? BigDecimal.ZERO : amountDue;
        BigDecimal paid = amountPaid == null ? BigDecimal.ZERO : amountPaid;
        BigDecimal diff = paid.subtract(due).abs();
        return switch (type) {
            case NEED_ADDITIONAL_PAYMENT -> "Cần thanh toán thêm " + diff.toPlainString() + " VND.";
            case HAS_BALANCE -> "Có số dư " + diff.toPlainString() + " VND.";
            case NEED_REFUND -> "Cần xử lý hoàn tiền " + diff.toPlainString() + " VND.";
            case NONE -> null;
        };
    }

    /** Gán type/note/status PENDING khi còn lệch học phí; xóa settlement khi cân bằng. */
    public static void applyComputedSettlement(ClassroomEnrollment enrollment) {
        if (enrollment == null) {
            return;
        }
        TuitionSettlementType type = computeSettlement(enrollment.getTuitionAmountDue(), enrollment.getTuitionAmountPaid());
        enrollment.setTuitionSettlementType(type);
        enrollment.setTuitionSettlementNote(buildSettlementNote(
                type,
                enrollment.getTuitionAmountDue(),
                enrollment.getTuitionAmountPaid()
        ));
        if (type == TuitionSettlementType.NONE) {
            clearOpenSettlement(enrollment);
        } else if (enrollment.getTuitionSettlementStatus() != TuitionSettlementStatus.REJECTED) {
            enrollment.setTuitionSettlementStatus(TuitionSettlementStatus.PENDING);
            enrollment.setTuitionSettlementResolvedAt(null);
            enrollment.setTuitionSettlementResolvedBy(null);
            enrollment.setTuitionSettlementResolutionNote(null);
        }
    }

    public static void markNeedRefundForExit(ClassroomEnrollment enrollment, String reasonPrefix) {
        if (enrollment == null) {
            return;
        }
        BigDecimal paid = enrollment.getTuitionAmountPaid() == null ? BigDecimal.ZERO : enrollment.getTuitionAmountPaid();
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        enrollment.setTuitionSettlementType(TuitionSettlementType.NEED_REFUND);
        enrollment.setTuitionSettlementStatus(TuitionSettlementStatus.PENDING);
        enrollment.setTuitionSettlementNote(
                (reasonPrefix == null || reasonPrefix.isBlank() ? "Cần xử lý hoàn tiền" : reasonPrefix)
                        + " " + paid.toPlainString() + " VND."
        );
        enrollment.setTuitionSettlementResolvedAt(null);
        enrollment.setTuitionSettlementResolvedBy(null);
        enrollment.setTuitionSettlementResolutionNote(null);
    }

    public static void clearOpenSettlement(ClassroomEnrollment enrollment) {
        if (enrollment == null) {
            return;
        }
        enrollment.setTuitionSettlementType(TuitionSettlementType.NONE);
        enrollment.setTuitionSettlementNote(null);
        enrollment.setTuitionSettlementStatus(TuitionSettlementStatus.NONE);
        enrollment.setTuitionSettlementResolvedAt(null);
        enrollment.setTuitionSettlementResolvedBy(null);
        enrollment.setTuitionSettlementResolutionNote(null);
    }

    public static void clearOpenSettlementAsResolved(ClassroomEnrollment enrollment, String note) {
        if (enrollment == null) {
            return;
        }
        enrollment.setTuitionSettlementType(TuitionSettlementType.NONE);
        enrollment.setTuitionSettlementNote(null);
        enrollment.setTuitionSettlementStatus(TuitionSettlementStatus.RESOLVED);
        enrollment.setTuitionSettlementResolutionNote(note);
    }

    public static String tuitionSettlementStatusLabel(TuitionSettlementStatus status) {
        if (status == null || status == TuitionSettlementStatus.NONE) {
            return null;
        }
        return switch (status) {
            case PENDING -> "Chờ xử lý";
            case RESOLVED -> "Đã xử lý";
            case REJECTED -> "Từ chối hoàn";
            case NONE -> null;
        };
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

    public static String tuitionSettlementLabel(TuitionSettlementType type) {
        if (type == null || type == TuitionSettlementType.NONE) {
            return null;
        }
        return switch (type) {
            case NEED_ADDITIONAL_PAYMENT -> "Cần thanh toán thêm";
            case HAS_BALANCE -> "Có số dư";
            case NEED_REFUND -> "Cần xử lý hoàn tiền";
            case NONE -> null;
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
