package fu.sep490.g23.backend.entity.enums;

import java.util.List;

/** Reserved role codes used by fixed business and authorization rules. */
public final class RoleCodes {
    public static final String LEARNER = "LEARNER";
    public static final String TEACHER = "TEACHER";
    public static final String MANAGER = "MANAGER";
    public static final String CONTENT_MANAGER = "CONTENT_MANAGER";
    public static final String STAFF = "STAFF";
    public static final String ADMIN = "ADMIN";

    public static final List<String> DISPLAY_PRIORITY = List.of(
            ADMIN, MANAGER, STAFF, CONTENT_MANAGER, TEACHER, LEARNER);

    private RoleCodes() {
    }
}
