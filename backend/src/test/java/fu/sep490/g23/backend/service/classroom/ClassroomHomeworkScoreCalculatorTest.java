package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassroomHomeworkScoreCalculatorTest {

    private final ClassroomHomeworkScoreCalculator calculator = new ClassroomHomeworkScoreCalculator();

    @Test
    void calculateAverage_NormalizesDifferentMaximumScoresAndIgnoresUngradedHomework() {
        ClassroomHomework twentyPointHomework = homework(1L, "20");
        ClassroomHomework tenPointHomework = homework(2L, "10");
        ClassroomHomework ungradedHomework = homework(3L, "10");
        User student = User.builder().id(11L).build();

        List<ClassroomHomeworkSubmission> submissions = List.of(
                gradedSubmission(twentyPointHomework, student, "10"),
                gradedSubmission(tenPointHomework, student, "8"),
                ClassroomHomeworkSubmission.builder()
                        .homework(ungradedHomework)
                        .student(student)
                        .status(HomeworkSubmissionStatus.SUBMITTED)
                        .build()
        );

        BigDecimal average = calculator.calculateAverage(
                List.of(twentyPointHomework, tenPointHomework, ungradedHomework),
                submissions
        );

        assertThat(average).isEqualByComparingTo("6.5");
    }

    @Test
    void calculateAverage_ReturnsNullWhenNoHomeworkHasBeenGraded() {
        ClassroomHomework homework = homework(1L, "10");

        assertThat(calculator.calculateAverage(List.of(homework), List.of())).isNull();
    }

    private ClassroomHomework homework(Long id, String maxScore) {
        return ClassroomHomework.builder()
                .id(id)
                .title("Homework " + id)
                .maxScore(new BigDecimal(maxScore))
                .build();
    }

    private ClassroomHomeworkSubmission gradedSubmission(
            ClassroomHomework homework,
            User student,
            String score
    ) {
        return ClassroomHomeworkSubmission.builder()
                .homework(homework)
                .student(student)
                .score(new BigDecimal(score))
                .status(HomeworkSubmissionStatus.GRADED)
                .build();
    }
}
