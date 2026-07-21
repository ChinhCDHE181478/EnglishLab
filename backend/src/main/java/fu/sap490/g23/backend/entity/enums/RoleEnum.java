package fu.sap490.g23.backend.entity.enums;

public enum RoleEnum {
    LEARNER,
    TEACHER,
    MANAGER,
    CONTENT_MANAGER,
    STAFF,
    /** Vai trò tương thích; nghiệp vụ vận hành mới dùng {@link #STAFF}. */
    TRAINING_MANAGER,
    ADMIN
}
