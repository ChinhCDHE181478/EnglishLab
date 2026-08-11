package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.CompletePracticeRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomPracticeAttemptResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.ClassroomPracticeAttemptHistory;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.curriculum.CurriculumExerciseRef;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.entity.curriculum.CurriculumUnit;
import fu.sep490.g23.backend.repository.classroom.*;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomPracticeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomPracticeServiceImplTest {
    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private ClassroomEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomPracticeAttemptRepository attemptRepository;
    @Mock private ClassroomPracticeAttemptHistoryRepository attemptHistoryRepository;
    @Mock private ClassroomAccessHelper accessHelper;
    @InjectMocks private ClassroomPracticeServiceImpl service;

    @Test
    void submitAttemptCreatesNewHistoryEntryAndScoresObjectiveAnswers() {
        User learner = User.builder().id(7L).email("learner@example.com").build();
        ExerciseBankItem exercise = ExerciseBankItem.builder()
                .id(3L).title("Practice").skill("READING").prompt("{}")
                .answerKey("{\"1\":\"B\",\"2\":\"A\"}").active(true).build();
        CurriculumUnit unit = CurriculumUnit.builder().id(4L).displayOrder(1).title("Unit 1").build();
        unit.setExerciseRefs(List.of(CurriculumExerciseRef.builder().unit(unit).exercise(exercise).build()));
        CurriculumProgram program = CurriculumProgram.builder().id(5L).title("Program").units(List.of(unit)).build();
        ClassroomOffering offering = ClassroomOffering.builder().id(6L)
                .learningPackage(LearningPackage.builder().title("Lớp TOEIC").build())
                .curriculumProgram(program).build();
        CompletePracticeRequest request = new CompletePracticeRequest();
        request.setAnswersJson("{\"1\":\"B\",\"2\":\"C\"}");
        request.setStartedAt(Instant.parse("2026-08-06T10:15:30Z"));

        when(accessHelper.requireUser("learner@example.com")).thenReturn(learner);
        when(enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(any(), any(), any()))
                .thenReturn(true);
        when(offeringRepository.findById(6L)).thenReturn(Optional.of(offering));
        when(attemptRepository.findByClassroomOfferingIdAndStudentIdAndExerciseId(6L, 7L, 3L))
                .thenReturn(Optional.empty());
        when(attemptHistoryRepository.countByClassroomOfferingIdAndStudentIdAndExerciseId(6L, 7L, 3L))
                .thenReturn(1L);
        when(attemptHistoryRepository.save(any(ClassroomPracticeAttemptHistory.class)))
                .thenAnswer(invocation -> {
                    ClassroomPracticeAttemptHistory saved = invocation.getArgument(0);
                    saved.setId(20L);
                    return saved;
                });

        ClassroomPracticeAttemptResponse result = service.submitAttempt(6L, 3L, request, "learner@example.com");

        assertThat(result.getAttemptNumber()).isEqualTo(2);
        assertThat(result.getCorrectAnswers()).isEqualTo(1);
        assertThat(result.getTotalQuestions()).isEqualTo(2);
        assertThat(result.getScorePercent()).isEqualTo(50.0D);
        assertThat(result.getStartedAt()).isEqualTo(
                LocalDateTime.ofInstant(request.getStartedAt(), ZoneId.systemDefault()));
    }
}
