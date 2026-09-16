package fu.sep490.g23.backend.seed.master;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class MasterDemoSemanticValidator {

    /** Banned in fullName only — digits catch fake "Học viên 01" names. */
    private static final Pattern BANNED_NAME = Pattern.compile(
            "(?i)(quản lý|nhân viên|content manager|classroom admin|staff englishlab|giáo viên\\s*\\d|học viên\\s*\\d|user demo|test student|admin user|teacher demo|\\d{2,}|lorem ipsum)"
    );

    /** Placeholder junk in discussion/content — do NOT ban digits (Task 2, 6.5, 25 phút…). */
    private static final Pattern BANNED_CONTENT = Pattern.compile(
            "(?i)(\\btest discussion\\b|\\bdemo comment\\b|\\bhello\\b|\\babc\\b|lorem ipsum|\\btest report\\b|\\bgood teacher\\b|^nice$|^ok$|^test$)"
    );

    public void validateDatasetOrThrow(JsonNode root) {
        List<String> issues = new ArrayList<>();
        for (JsonNode account : root.path("accounts")) {
            if (account.path("preserved").asBoolean(false)) {
                continue;
            }
            String email = account.path("email").asText();
            String fullName = account.path("fullName").asText();
            if (!isValidVietnameseName(fullName)) {
                issues.add("invalid_name:" + email + "=" + fullName);
            }
        }
        validateOptionalGapDomains(root.path("world"), issues);
        if (!issues.isEmpty()) {
            throw new IllegalStateException("MASTER semantic validation failed: " + String.join("; ", issues.subList(0, Math.min(20, issues.size()))));
        }
    }

    public void assertValidVietnameseName(String email, String fullName) {
        if (!isValidVietnameseName(fullName)) {
            throw new IllegalStateException("Invalid Vietnamese demo fullName for " + email + ": " + fullName);
        }
    }

    public boolean isValidVietnameseName(String fullName) {
        String value = fullName == null ? "" : fullName.trim();
        if (value.split("\\s+").length < 2) {
            return false;
        }
        return !BANNED_NAME.matcher(value).find();
    }

    private void validateOptionalGapDomains(JsonNode world, List<String> issues) {
        if (world == null || world.isMissingNode()) {
            return;
        }
        validateDiscussionArray(world.path("discussions"), issues);
        validateArrayField(world.path("discussionReplies"), "parentNaturalKey", issues, "discussion_reply");
        validateArrayField(world.path("discussionReactions"), "postNaturalKey", issues, "discussion_reaction");
        validateArrayField(world.path("discussionReports"), "postNaturalKey", issues, "discussion_report");
        validateArrayField(world.path("teacherFeedback"), "classCode", issues, "teacher_feedback");
        validateArrayField(world.path("announcements"), "classCode", issues, "announcement");
        validateArrayField(world.path("broadcasts"), "title", issues, "broadcast");
        validateArrayField(world.path("ticketMessages"), "ticketNaturalKey", issues, "ticket_message");
        validateArrayField(world.path("changeRequests"), "classCode", issues, "change_request");
        validateAttendanceDisputes(world.path("attendanceDisputes"), issues);
        validateArrayField(world.path("courseListItems"), "courseSlug", issues, "course_list_item");
        validateTuitionPayments(world.path("tuitionPayments"), issues);
        validateTuitionProofs(world.path("tuitionProofs"), issues);
        validateArrayField(world.path("teacherCredentials"), "teacherEmail", issues, "teacher_credential");
        validateArrayField(world.path("practiceAttempts"), "classCode", issues, "practice_attempt");
        validateFlashcardSets(world.path("flashcardSets"), issues);
        validateArrayField(world.path("centerLibrary"), "title", issues, "center_library");
        validateArrayField(world.path("lessonNotes"), "courseSlug", issues, "lesson_note");
        validateOnlineEnrollmentReviews(world.path("onlineEnrollments"), issues);
    }

    private void validateDiscussionArray(JsonNode discussions, List<String> issues) {
        if (discussions == null || !discussions.isArray()) {
            return;
        }
        Set<String> keys = new HashSet<>();
        for (JsonNode node : discussions) {
            String naturalKey = node.path("naturalKey").asText("");
            if (naturalKey.isBlank()) {
                issues.add("discussion_missing_naturalKey");
            } else if (!keys.add(naturalKey)) {
                issues.add("duplicate_discussion:" + naturalKey);
            }
            if (node.path("courseSlug").asText("").isBlank()) {
                issues.add("discussion_missing_course:" + naturalKey);
            }
            if (node.path("authorEmail").asText("").isBlank()) {
                issues.add("discussion_missing_author:" + naturalKey);
            }
            if ("THREAD".equalsIgnoreCase(node.path("postType").asText("THREAD"))
                    && node.path("title").asText("").isBlank()) {
                issues.add("discussion_missing_title:" + naturalKey);
            }
            String content = node.path("content").asText("") + " " + node.path("title").asText("");
            if (BANNED_CONTENT.matcher(content).find()) {
                issues.add("discussion_banned_content:" + naturalKey);
            }
        }
    }

    private void validateAttendanceDisputes(JsonNode disputes, List<String> issues) {
        if (disputes == null || !disputes.isArray()) {
            return;
        }
        for (JsonNode node : disputes) {
            String attendanceKey = node.path("attendanceNaturalKey").asText("");
            boolean hasExpanded = !node.path("classCode").asText("").isBlank()
                    && !node.path("learnerEmail").asText("").isBlank()
                    && !node.path("sessionDate").asText("").isBlank()
                    && !node.path("startTime").asText("").isBlank();
            if (!hasExpanded) {
                // Format: classCode|sessionDate|startTime|learnerEmail
                String[] parts = attendanceKey.split("\\|", -1);
                if (parts.length < 4
                        || parts[0].isBlank()
                        || parts[1].isBlank()
                        || parts[2].isBlank()
                        || parts[3].isBlank()) {
                    issues.add("attendance_dispute_missing_keys:" + attendanceKey);
                }
            }
            if (node.path("disputeReason").asText("").isBlank()) {
                issues.add("attendance_dispute_missing_reason");
            }
        }
    }

    private void validateTuitionPayments(JsonNode payments, List<String> issues) {
        if (payments == null || !payments.isArray()) {
            return;
        }
        for (JsonNode node : payments) {
            if (node.path("amount").asLong(0) < 0) {
                issues.add("tuition_payment_negative:" + node.path("naturalKey").asText());
            }
            if (node.path("classCode").asText("").isBlank() || node.path("learnerEmail").asText("").isBlank()) {
                issues.add("tuition_payment_missing_enrollment:" + node.path("naturalKey").asText());
            }
        }
    }

    private void validateTuitionProofs(JsonNode proofs, List<String> issues) {
        if (proofs == null || !proofs.isArray()) {
            return;
        }
        for (JsonNode node : proofs) {
            boolean hasEnrollment = !node.path("classCode").asText("").isBlank()
                    && !node.path("learnerEmail").asText("").isBlank();
            boolean hasPaymentKey = !node.path("tuitionPaymentNaturalKey").asText("").isBlank();
            boolean hasFileHint = node.path("fileUrl").asText("").contains("tuition-proof/");
            if (!hasEnrollment && !hasPaymentKey && !hasFileHint) {
                issues.add("tuition_proof_missing_link:" + node.path("naturalKey").asText());
            }
            if (node.path("fileUrl").asText("").isBlank()) {
                issues.add("tuition_proof_missing_fileUrl:" + node.path("naturalKey").asText());
            }
        }
    }

    private void validateFlashcardSets(JsonNode sets, List<String> issues) {
        if (sets == null || !sets.isArray()) {
            return;
        }
        for (JsonNode node : sets) {
            String code = node.path("code").asText("");
            if (!code.isBlank() && !code.startsWith("demo-fc-")) {
                issues.add("flashcard_code_prefix:" + code);
            }
        }
    }

    private void validateOnlineEnrollmentReviews(JsonNode enrollments, List<String> issues) {
        if (enrollments == null || !enrollments.isArray()) {
            return;
        }
        for (JsonNode node : enrollments) {
            if (!node.has("reviewRating")) {
                continue;
            }
            int rating = node.path("reviewRating").asInt(0);
            if (rating < 1 || rating > 5) {
                issues.add("invalid_review_rating:" + node.path("learnerEmail").asText());
            }
        }
    }

    private void validateArrayField(JsonNode array, String requiredField, List<String> issues, String label) {
        if (array == null || !array.isArray() || array.isEmpty()) {
            return;
        }
        for (JsonNode node : array) {
            if (node.path(requiredField).asText("").isBlank()) {
                issues.add(label + "_missing_" + requiredField);
            }
        }
    }
}
