package fu.sep490.g23.backend.service.assessment.impl;

import fu.sep490.g23.backend.dto.request.assessment.ReviewPlacementAttemptRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.security.TrainingRolePolicy;
import fu.sep490.g23.backend.service.assessment.PlacementEligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Decides if a scored attempt can be used for placement.
 * TOEIC can be eligible immediately; IELTS needs staff review of Writing/Speaking;
 * skill-only diagnostics are never eligible for course placement.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PlacementEligibilityServiceImpl implements PlacementEligibilityService {

    private final PlacementTestAttemptRepository attemptRepository;
    private final UserRepository userRepository;

    /** Called by getRecommendations to decide if ranking should run. Nested: evaluate(). */
    @Override
    @Transactional(readOnly = true)
    public PlacementEligibilityResult evaluateEligibility(Long learnerId, Long placementAttemptId) {
        PlacementTestAttempt attempt = requireAttempt(placementAttemptId);
        if (!attempt.getStudent().getId().equals(learnerId)) {
            throw new IllegalArgumentException("Kết quả placement test không thuộc học viên này.");
        }
        return evaluate(attempt); // Shared checklist used by recommendations and staff review.
    }

    /** Staff inbox: IELTS attempts waiting for (or currently in) Writing/Speaking review. */
    @Override
    @Transactional(readOnly = true)
    public List<PlacementTestAttemptResponse> listManualReviewQueue(String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên đào tạo."));
        if (!TrainingRolePolicy.canPerformStaffAction(staff)) {
            throw new IllegalArgumentException("Bạn không có quyền xem hàng đợi placement test.");
        }
        return attemptRepository.findByEvaluationStatusInOrderBySubmittedAtAsc(List.of(
                        PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED,
                        PlacementEvaluationStatus.UNDER_REVIEW
                )).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Staff accept the attempt: store recommendedLevel and flip status to ELIGIBLE. */
    @Override
    public PlacementEligibilityResult confirmManualReview(
            Long placementAttemptId,
            ReviewPlacementAttemptRequest request,
            String reviewerEmail
    ) {
        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên đánh giá."));
        if (!TrainingRolePolicy.canPerformStaffAction(reviewer)) {
            throw new IllegalArgumentException("Bạn không có quyền xác nhận placement test.");
        }

        PlacementTestAttempt attempt = requireAttempt(placementAttemptId);
        PlacementEligibilityResult current = evaluate(attempt);
        if (current.getStatus() == PlacementEvaluationStatus.EXPIRED
                || current.getStatus() == PlacementEvaluationStatus.NOT_ELIGIBLE) {
            throw new IllegalArgumentException("Placement test đã hết hiệu lực hoặc không hợp lệ.");
        }
        if (attempt.getSubmittedAt() == null) {
            throw new IllegalArgumentException("Placement test chưa được nộp.");
        }

        attempt.setEvaluationStatus(PlacementEvaluationStatus.ELIGIBLE);
        attempt.setRecommendedLevel(request.getRecommendedLevel());
        attempt.setReviewer(reviewer);
        attempt.setReviewedAt(LocalDateTime.now());
        attempt.setReviewNote(request.getNote().trim());
        attemptRepository.save(attempt);
        return evaluate(attempt); // Re-run checks so the response reflects the new ELIGIBLE state.
    }

    /** Walk blockers (skill-only, cancelled, expired, fraud, missing scores, pending review). */
    private PlacementEligibilityResult evaluate(PlacementTestAttempt attempt) {
        List<String> missing = new ArrayList<>();
        PlacementEvaluationStatus status = attempt.getEvaluationStatus() == null
                ? PlacementEvaluationStatus.SUBMITTED
                : attempt.getEvaluationStatus();
        LocalDateTime now = LocalDateTime.now();

        if (isSkillAssessment(attempt)) {
            status = PlacementEvaluationStatus.NOT_ELIGIBLE;
            missing.add("FULL_PLACEMENT_REQUIRED"); // Diagnostic tests cannot place a student into a course.
        } else if (attempt.getCancelledAt() != null) {
            status = PlacementEvaluationStatus.NOT_ELIGIBLE;
            missing.add("ATTEMPT_CANCELLED");
        } else if (attempt.getExpiresAt() != null && attempt.getExpiresAt().isBefore(now)) {
            status = PlacementEvaluationStatus.EXPIRED;
            missing.add("RESULT_EXPIRED");
        } else if (attempt.getSubmittedAt() == null) {
            status = PlacementEvaluationStatus.NOT_STARTED;
            missing.add("TEST_SUBMISSION");
        }

        boolean terminal = status == PlacementEvaluationStatus.EXPIRED
                || status == PlacementEvaluationStatus.NOT_ELIGIBLE;
        boolean toeic = isToeic(attempt);
        if (attempt.getListeningScore() == null) missing.add("LISTENING_SCORE");
        if (attempt.getReadingScore() == null) missing.add("READING_SCORE");
        // IELTS Writing/Speaking still need a human confirm unless already ELIGIBLE.
        if (!terminal && !toeic && status != PlacementEvaluationStatus.ELIGIBLE) {
            missing.add("WRITING_REVIEW");
            missing.add("SPEAKING_REVIEW");
            if (status != PlacementEvaluationStatus.UNDER_REVIEW) {
                status = PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED;
            }
        }

        PlacementLevel recommendedLevel = attempt.getRecommendedLevel();
        if (recommendedLevel == null && toeic && attempt.getOverallScore() != null) {
            recommendedLevel = toeicLevel(attempt.getOverallScore()); // Derive level from TOEIC total if staff never set it.
        }
        if (recommendedLevel == null) missing.add("RECOMMENDED_LEVEL");
        if (attempt.getOverallScore() == null) missing.add("OVERALL_SCORE");

        boolean eligible = status == PlacementEvaluationStatus.ELIGIBLE && missing.isEmpty();
        return PlacementEligibilityResult.builder()
                .attemptId(attempt.getId())
                .eligible(eligible)
                .status(eligible ? PlacementEvaluationStatus.ELIGIBLE : status)
                .missingRequirements(missing.stream().distinct().toList())
                .recommendedLevel(recommendedLevel)
                .expiresAt(attempt.getExpiresAt())
                .reviewerId(attempt.getReviewer() == null ? null : attempt.getReviewer().getId())
                .reviewedAt(attempt.getReviewedAt())
                .reviewNote(attempt.getReviewNote())
                .build();
    }

    private PlacementTestAttempt requireAttempt(Long id) {
        return attemptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả placement test."));
    }

    /** Detect TOEIC from stored AI JSON (set at submit time). */
    private boolean isToeic(PlacementTestAttempt attempt) {
        return String.valueOf(attempt.getAiFeedbackJson()).contains("\"examType\":\"TOEIC\"");
    }

    /** Skill-only diagnostics cannot unlock course placement. */
    private boolean isSkillAssessment(PlacementTestAttempt attempt) {
        return String.valueOf(attempt.getAiFeedbackJson()).contains("\"examType\":\"SKILL\"");
    }

    /** Same TOEIC cut-offs as scoring: <450 beginner, <700 intermediate, else advanced. */
    private PlacementLevel toeicLevel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(450)) < 0) return PlacementLevel.BEGINNER;
        if (score.compareTo(BigDecimal.valueOf(700)) < 0) return PlacementLevel.INTERMEDIATE;
        return PlacementLevel.ADVANCED;
    }

    private PlacementTestAttemptResponse toResponse(PlacementTestAttempt attempt) {
        return PlacementTestAttemptResponse.builder()
                .id(attempt.getId())
                .learnerId(attempt.getStudent().getId())
                .learnerName(attempt.getStudent().getFullName())
                .learnerEmail(attempt.getStudent().getEmail())
                .testCode(attempt.getTestCode())
                .examType(isToeic(attempt) ? "TOEIC" : isSkillAssessment(attempt) ? "SKILL" : "IELTS")
                .listeningScore(attempt.getListeningScore())
                .readingScore(attempt.getReadingScore())
                .writingScore(attempt.getWritingScore())
                .speakingScore(attempt.getSpeakingScore())
                .overallScore(attempt.getOverallScore())
                .correctListening(attempt.getCorrectListening())
                .correctReading(attempt.getCorrectReading())
                .aiFeedbackJson(attempt.getAiFeedbackJson())
                .status(attempt.getStatus())
                .evaluationStatus(attempt.getEvaluationStatus())
                .recommendedLevel(attempt.getRecommendedLevel())
                .expiresAt(attempt.getExpiresAt())
                .reviewerId(attempt.getReviewer() == null ? null : attempt.getReviewer().getId())
                .reviewerName(attempt.getReviewer() == null ? null : attempt.getReviewer().getFullName())
                .reviewedAt(attempt.getReviewedAt())
                .reviewNote(attempt.getReviewNote())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }
}
