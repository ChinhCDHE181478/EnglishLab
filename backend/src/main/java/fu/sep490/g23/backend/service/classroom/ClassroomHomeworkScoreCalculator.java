package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sap490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sap490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ClassroomHomeworkScoreCalculator {

    private static final BigDecimal GRADEBOOK_MAX_SCORE = BigDecimal.TEN;

    public BigDecimal calculateAverage(
            Collection<ClassroomHomework> homeworks,
            Collection<ClassroomHomeworkSubmission> submissions
    ) {
        Map<Long, ClassroomHomework> homeworkById = homeworks.stream()
                .collect(Collectors.toMap(ClassroomHomework::getId, Function.identity()));

        var normalizedScores = submissions.stream()
                .filter(submission -> submission.getStatus() == HomeworkSubmissionStatus.GRADED)
                .filter(submission -> submission.getScore() != null)
                .filter(submission -> homeworkById.containsKey(submission.getHomework().getId()))
                .map(submission -> normalizeScore(
                        submission.getScore(),
                        homeworkById.get(submission.getHomework().getId()).getMaxScore()
                ))
                .toList();

        if (normalizedScores.isEmpty()) {
            return null;
        }

        return normalizedScores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(normalizedScores.size()), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeScore(BigDecimal score, BigDecimal maxScore) {
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return score;
        }
        return score.multiply(GRADEBOOK_MAX_SCORE)
                .divide(maxScore, 4, RoundingMode.HALF_UP);
    }
}
