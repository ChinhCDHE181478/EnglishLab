package fu.sep490.g23.backend.entity.assessment.enums;

/** Lifecycle of one placement attempt from start through scoring to course eligibility. */
public enum PlacementEvaluationStatus {
    NOT_STARTED,
    IN_PROGRESS,
    SUBMITTED,                 // Saved, scoring may still be incomplete (e.g. AI down).
    AUTO_GRADING,
    MANUAL_REVIEW_REQUIRED,    // IELTS: staff must confirm Writing/Speaking.
    UNDER_REVIEW,              // Staff has opened the attempt.
    ELIGIBLE,                  // Can be used for course placement / full recommendations.
    NOT_ELIGIBLE,              // Skill-only, cancelled, or fraud — not for placement.
    EXPIRED
}
