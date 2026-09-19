package fu.sep490.g23.backend.seed.master;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Shared markers / preserved identities for MASTER operational dataset.
 * <p>
 * User-visible emails/codes/slugs must NOT contain DEMO/TEST/SAMPLE.
 * Cleanup ownership is driven by {@code ownership} manifest inside master-dataset.json
 * plus legacy patterns below — never by deleting all {@code @englishlab.vn} accounts.
 */
public final class MasterDemoMarkers {

    /** Technical marker stored in import JSON / internal refs only (not shown as UI title). */
    public static final String MARKER = "MASTER_OPS";

    public static final String EMAIL_DOMAIN = "englishlab.vn";

    /** Generated account local-part prefixes (exact ownership; not entire domain). */
    public static final String PREFIX_TEACHER = "gv.";
    public static final String PREFIX_LEARNER = "hs.";
    public static final String PREFIX_STAFF_GEN = "nv.";
    public static final String PREFIX_MANAGER_GEN = "ql.";
    public static final String PREFIX_CONTENT_GEN = "cm.";
    public static final String PREFIX_ADMIN_GEN = "ad.";

    public static final String CLASS_CODE_PREFIX = "ilc-";
    public static final String COURSE_SLUG_PREFIX = "ilc-";
    public static final String ROOM_NAME_PREFIX = "Phòng ";
    public static final String ROOM_CODE_PREFIX = "ILC_R";
    public static final String ILC_CODE_PREFIX = "ILC_";
    public static final String DISCOUNT_CODE_PREFIX = "ILC_";
    public static final String FLASHCARD_CODE_PREFIX = "ilc-fc-";
    public static final String EXERCISE_CODE_PREFIX = "ilc-ex-";
    public static final String MASTER_REF_PREFIX = "master-ops://";

    public static final String PRESERVED_LEARNER_EMAIL = "0386852628z@gmail.com";
    public static final String PRESERVED_TEACHER_EMAIL = "alien1062004@gmail.com";

    public static final Set<String> PRESERVED_STAFF_EMAILS = Set.of(
            "staff@englishlab.vn",
            "classroom.manager@englishlab.vn",
            "content.manager@englishlab.vn",
            "classroom.admin@englishlab.vn"
    );

    public static final String PROTECTED_E2_SLUG = "e2-ielts-practice-tests";
    public static final String PROTECTED_VOCAB_SLUG = "ielts-master-vocabulary-band-7-plus";

    /** Legacy rows from prior MASTER/sheet seeds — eligible for cleanup. */
    public static final Pattern LEGACY_DEMO_EMAIL = Pattern.compile("^demo\\..+@englishlab\\.local$", Pattern.CASE_INSENSITIVE);
    public static final Pattern LEGACY_SHEET_TEACHER = Pattern.compile("^gv\\.sheet\\.\\d+@englishlab\\.vn$", Pattern.CASE_INSENSITIVE);
    public static final Pattern LEGACY_SHEET_LEARNER = Pattern.compile("^hs\\.(sheet|consult)\\.\\d+@englishlab\\.vn$", Pattern.CASE_INSENSITIVE);
    public static final String LEGACY_CLASS_DEMO_PREFIX = "demo-class-";
    public static final String LEGACY_CLASS_SHEET_PREFIX = "center-sheet-class-";
    public static final String LEGACY_COURSE_SLUG_PREFIX = "demo-";
    public static final String LEGACY_CENTER_SHEET_SLUG_PREFIX = "center-sheet-";
    public static final String LEGACY_ILC_DEMO_PREFIX = "DEMO_";
    public static final String LEGACY_ROOM_CODE_PREFIX = "demo-room-";
    public static final String LEGACY_ROOM_NAME_PREFIX = "Phòng học ";
    public static final String LEGACY_ROOM_P_PREFIX = "Phòng P";
    public static final String LEGACY_FLASHCARD_PREFIX = "demo-fc-";
    public static final String LEGACY_EXERCISE_PREFIX = "demo-ex-";
    public static final String LEGACY_MASTER_REF_PREFIX = "master-demo://";
    public static final String LEGACY_DISCOUNT_PREFIX = "DEMO_";

    private static final Pattern FORBIDDEN_UI = Pattern.compile(
            "(?i)(?<!mock )(?<!placement )(?<!module )\\b(demo|sample)\\b|(?<![a-z])test(?![a-z])"
    );

    private MasterDemoMarkers() {
    }

    public static boolean isPreservedEmail(String email) {
        String value = normalize(email);
        return PRESERVED_LEARNER_EMAIL.equalsIgnoreCase(value)
                || PRESERVED_TEACHER_EMAIL.equalsIgnoreCase(value)
                || PRESERVED_STAFF_EMAILS.contains(value);
    }

    public static boolean isProtectedCourseSlug(String slug) {
        return PROTECTED_E2_SLUG.equalsIgnoreCase(slug)
                || PROTECTED_VOCAB_SLUG.equalsIgnoreCase(slug);
    }

    public static boolean isGeneratedMasterEmail(String email) {
        String value = normalize(email);
        if (!value.endsWith("@" + EMAIL_DOMAIN) || isPreservedEmail(value)) {
            return false;
        }
        String local = value.substring(0, value.indexOf('@'));
        return local.startsWith(PREFIX_TEACHER)
                || local.startsWith(PREFIX_LEARNER)
                || local.startsWith(PREFIX_STAFF_GEN)
                || local.startsWith(PREFIX_MANAGER_GEN)
                || local.startsWith(PREFIX_CONTENT_GEN)
                || local.startsWith(PREFIX_ADMIN_GEN);
    }

    /** Seed acceptance: generated MASTER emails only (never whole domain). */
    public static boolean isMasterEmail(String email) {
        return isGeneratedMasterEmail(email);
    }

    /** Cleanup ownership: generated OR legacy markers — never all @englishlab.vn. */
    public static boolean isCleanupOwnedEmail(String email) {
        return isGeneratedMasterEmail(email) || isLegacyCleanupEmail(email);
    }

    public static boolean isLegacyCleanupEmail(String email) {
        String value = normalize(email);
        if (isPreservedEmail(value)) {
            return false;
        }
        return LEGACY_DEMO_EMAIL.matcher(value).matches()
                || LEGACY_SHEET_TEACHER.matcher(value).matches()
                || LEGACY_SHEET_LEARNER.matcher(value).matches()
                || value.startsWith("review.") && value.endsWith("@" + EMAIL_DOMAIN);
    }

    public static boolean isOwnedClassCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return code.startsWith(CLASS_CODE_PREFIX)
                || code.startsWith(LEGACY_CLASS_DEMO_PREFIX)
                || code.startsWith(LEGACY_CLASS_SHEET_PREFIX);
    }

    public static boolean isOwnedCourseSlug(String slug) {
        if (slug == null || slug.isBlank() || isProtectedCourseSlug(slug)) {
            return false;
        }
        return slug.startsWith(COURSE_SLUG_PREFIX)
                || slug.startsWith(LEGACY_COURSE_SLUG_PREFIX)
                || slug.startsWith(LEGACY_CENTER_SHEET_SLUG_PREFIX);
    }

    public static boolean isOwnedIlcCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return code.startsWith(ILC_CODE_PREFIX) || code.startsWith(LEGACY_ILC_DEMO_PREFIX);
    }

    public static boolean isOwnedRoom(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (name.startsWith(LEGACY_ROOM_NAME_PREFIX) || name.startsWith(LEGACY_ROOM_P_PREFIX)) {
            return true;
        }
        if ("Studio Online A".equals(name) || "Studio Online B".equals(name)) {
            return true;
        }
        // Exact MASTER physical rooms only — never wipe arbitrary "Phòng …" campus rooms
        return name.matches("^Phòng (101|102|103|201|202|203|301|302)$");
    }

    public static boolean isOwnedDiscountCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return code.startsWith(DISCOUNT_CODE_PREFIX) || code.startsWith(LEGACY_DISCOUNT_PREFIX);
    }

    public static boolean isOwnedFlashcardCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return code.startsWith(FLASHCARD_CODE_PREFIX) || code.startsWith(LEGACY_FLASHCARD_PREFIX);
    }

    public static boolean isOwnedExerciseCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return code.startsWith(EXERCISE_CODE_PREFIX) || code.startsWith(LEGACY_EXERCISE_PREFIX);
    }

    public static boolean isOwnedMasterRef(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.startsWith(MASTER_REF_PREFIX) || value.startsWith(LEGACY_MASTER_REF_PREFIX);
    }

    public static boolean looksForbiddenUiText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("mock test") || lower.contains("placement test") || lower.contains("module test")) {
            // still flag explicit DEMO token
            return lower.contains("demo") || lower.contains("sample");
        }
        return FORBIDDEN_UI.matcher(text).find() || lower.contains("demo");
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
