package fu.sep490.g23.backend.seed.master;

/**
 * Shared markers / preserved identities for MASTER demo data.
 */
public final class MasterDemoMarkers {

    public static final String MARKER = "MASTER_DEMO";
    public static final String EMAIL_DOMAIN = "englishlab.local";
    public static final String EMAIL_PREFIX = "demo.";
    public static final String CLASS_CODE_PREFIX = "demo-class-";
    public static final String ROOM_CODE_PREFIX = "demo-room-";
    public static final String COURSE_SLUG_PREFIX = "demo-";
    public static final String ILC_CODE_PREFIX = "DEMO_";

    public static final String PRESERVED_LEARNER_EMAIL = "0386852628z@gmail.com";
    public static final String PRESERVED_TEACHER_EMAIL = "alien1062004@gmail.com";

    public static final String PROTECTED_E2_SLUG = "e2-ielts-practice-tests";
    public static final String PROTECTED_VOCAB_SLUG = "ielts-master-vocabulary-band-7-plus";

    private MasterDemoMarkers() {
    }

    public static boolean isMasterEmail(String email) {
        String value = email == null ? "" : email.trim().toLowerCase();
        return value.startsWith(EMAIL_PREFIX) && value.endsWith("@" + EMAIL_DOMAIN);
    }

    public static boolean isPreservedEmail(String email) {
        String value = email == null ? "" : email.trim().toLowerCase();
        return PRESERVED_LEARNER_EMAIL.equalsIgnoreCase(value)
                || PRESERVED_TEACHER_EMAIL.equalsIgnoreCase(value);
    }

    public static boolean isProtectedCourseSlug(String slug) {
        return PROTECTED_E2_SLUG.equalsIgnoreCase(slug)
                || PROTECTED_VOCAB_SLUG.equalsIgnoreCase(slug);
    }
}
