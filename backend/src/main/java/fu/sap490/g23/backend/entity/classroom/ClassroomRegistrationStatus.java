package fu.sap490.g23.backend.entity.classroom;

/**
 * Trạng thái đăng ký lớp và học phí (không phải luồng mua khóa self-paced).
 */
public enum ClassroomRegistrationStatus {
    PENDING_CONFIRMATION,
    PENDING_TUITION_PAYMENT,
    DEPOSIT_PAID,
    PARTIALLY_PAID,
    FULLY_PAID,
    ASSIGNED,
    WAITLIST,
    REJECTED,
    CANCELLED
}
