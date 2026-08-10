package fu.sap490.g23.backend.service.classroom.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.dto.request.classroom.CompletePracticeRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomPracticeAttemptResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomPracticeResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.entity.curriculum.CurriculumExerciseRef;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.ClassroomPracticeService;
import fu.sap490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomPracticeServiceImpl implements ClassroomPracticeService {
    private static final Set<ClassroomRegistrationStatus> HAS_LEARNING_ACCESS = ClassroomRegistrationSupport.HAS_LEARNING_ACCESS;

    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassroomPracticeAttemptRepository attemptRepository;
    private final ClassroomPracticeAttemptHistoryRepository attemptHistoryRepository;
    private final ClassroomAccessHelper accessHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomPracticeResponse> listForLearner(Long offeringId, String learnerEmail) {
        User learner = requireLearnerAccess(offeringId, learnerEmail);
        ClassroomOffering offering = requireOffering(offeringId);
        Map<Long, ClassroomPracticeAttempt> attempts = attemptRepository
                .findByClassroomOfferingIdAndStudentId(offeringId, learner.getId()).stream()
                .collect(Collectors.toMap(attempt -> attempt.getExercise().getId(), Function.identity()));
        return practiceRefs(offering).stream()
                .map(ref -> toResponse(offering, ref, attempts.get(ref.getExercise().getId()), learner.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomPracticeResponse> listAllForLearner(String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        return enrollmentRepository.findByStudentIdAndRegistrationStatusIn(learner.getId(), HAS_LEARNING_ACCESS).stream()
                .map(ClassroomEnrollment::getClassroomOffering)
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(offering -> {
                    Map<Long, ClassroomPracticeAttempt> attempts = attemptRepository
                            .findByClassroomOfferingIdAndStudentId(offering.getId(), learner.getId()).stream()
                            .collect(Collectors.toMap(attempt -> attempt.getExercise().getId(), Function.identity()));
                    return practiceRefs(offering).stream()
                            .map(ref -> toResponse(offering, ref, attempts.get(ref.getExercise().getId()), learner.getId()));
                })
                .sorted(Comparator
                        .comparing(ClassroomPracticeResponse::getClassroomTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(item -> Optional.ofNullable(item.getUnitDisplayOrder()).orElse(0))
                        .thenComparing(ClassroomPracticeResponse::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    @Override
    public ClassroomPracticeResponse complete(
            Long offeringId,
            Long exerciseId,
            CompletePracticeRequest request,
            String learnerEmail
    ) {
        submitAttempt(offeringId, exerciseId, request, learnerEmail);
        User learner = requireLearnerAccess(offeringId, learnerEmail);
        ClassroomOffering offering = requireOffering(offeringId);
        CurriculumExerciseRef ref = requirePracticeRef(offering, exerciseId);
        ClassroomPracticeAttempt attempt = attemptRepository
                .findByClassroomOfferingIdAndStudentIdAndExerciseId(offeringId, learner.getId(), exerciseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lượt luyện tập vừa hoàn thành."));
        return toResponse(offering, ref, attempt, learner.getId());
    }

    @Override
    public ClassroomPracticeAttemptResponse submitAttempt(
            Long offeringId,
            Long exerciseId,
            CompletePracticeRequest request,
            String learnerEmail
    ) {
        User learner = requireLearnerAccess(offeringId, learnerEmail);
        ClassroomOffering offering = requireOffering(offeringId);
        CurriculumExerciseRef ref = requirePracticeRef(offering, exerciseId);
        validateSubmission(request);

        LocalDateTime completedAt = LocalDateTime.now();
        Optional<ClassroomPracticeAttempt> previousSummary = attemptRepository
                .findByClassroomOfferingIdAndStudentIdAndExerciseId(offeringId, learner.getId(), exerciseId);
        long historyCount = attemptHistoryRepository
                .countByClassroomOfferingIdAndStudentIdAndExerciseId(offeringId, learner.getId(), exerciseId);
        if (historyCount == 0 && previousSummary.isPresent()) {
            ClassroomPracticeAttempt legacy = previousSummary.get();
            attemptHistoryRepository.save(ClassroomPracticeAttemptHistory.builder()
                    .classroomOffering(offering)
                    .student(learner)
                    .exercise(ref.getExercise())
                    .attemptNumber(1)
                    .responseText(legacy.getResponseText())
                    .completedAt(legacy.getCompletedAt())
                    .build());
            historyCount = 1;
        }

        ClassroomPracticeAttempt summary = previousSummary
                .orElseGet(() -> ClassroomPracticeAttempt.builder()
                        .classroomOffering(offering)
                        .student(learner)
                        .exercise(ref.getExercise())
                        .build());
        summary.setResponseText(request.getResponseText());
        summary.setCompletedAt(completedAt);
        attemptRepository.save(summary);

        ScoreResult score = score(request.getAnswersJson(), ref.getExercise().getAnswerKey());
        int attemptNumber = Math.toIntExact(historyCount + 1);
        ClassroomPracticeAttemptHistory history = ClassroomPracticeAttemptHistory.builder()
                .classroomOffering(offering)
                .student(learner)
                .exercise(ref.getExercise())
                .attemptNumber(attemptNumber)
                .responseText(request.getResponseText())
                .answersJson(request.getAnswersJson())
                .correctAnswers(score.correctAnswers())
                .totalQuestions(score.totalQuestions())
                .scorePercent(score.scorePercent())
                .durationSeconds(request.getDurationSeconds())
                .startedAt(request.getStartedAt() == null
                        ? null
                        : LocalDateTime.ofInstant(request.getStartedAt(), java.time.ZoneId.systemDefault()))
                .completedAt(completedAt)
                .build();
        return toAttemptResponse(attemptHistoryRepository.save(history));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomPracticeAttemptResponse> listAttempts(Long offeringId, Long exerciseId, String learnerEmail) {
        User learner = requireLearnerAccess(offeringId, learnerEmail);
        ClassroomOffering offering = requireOffering(offeringId);
        requirePracticeRef(offering, exerciseId);
        List<ClassroomPracticeAttemptResponse> history = attemptHistoryRepository
                .findByClassroomOfferingIdAndStudentIdAndExerciseIdOrderByCompletedAtDesc(
                        offeringId, learner.getId(), exerciseId
                ).stream()
                .map(this::toAttemptResponse)
                .toList();
        if (!history.isEmpty()) return history;

        return attemptRepository.findByClassroomOfferingIdAndStudentIdAndExerciseId(
                        offeringId, learner.getId(), exerciseId
                )
                .map(summary -> List.of(toLegacyAttemptResponse(summary)))
                .orElseGet(List::of);
    }

    private User requireLearnerAccess(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        boolean hasAccess = enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                learner.getId(), offeringId, HAS_LEARNING_ACCESS);
        if (!hasAccess) throw new RuntimeException("Bạn không thuộc lớp học này.");
        return learner;
    }

    private ClassroomOffering requireOffering(Long offeringId) {
        return offeringRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
    }

    private List<CurriculumExerciseRef> practiceRefs(ClassroomOffering offering) {
        if (offering.getCurriculumProgram() == null) return List.of();
        return offering.getCurriculumProgram().getUnits().stream()
                .sorted(Comparator.comparing(unit -> Optional.ofNullable(unit.getDisplayOrder()).orElse(0)))
                .flatMap(unit -> unit.getExerciseRefs().stream()
                        .filter(ref -> ref.getExercise() != null && ref.getExercise().isActive())
                        .sorted(Comparator.comparing(ref -> Optional.ofNullable(ref.getDisplayOrder()).orElse(0))))
                .toList();
    }

    private CurriculumExerciseRef requirePracticeRef(ClassroomOffering offering, Long exerciseId) {
        return practiceRefs(offering).stream()
                .filter(candidate -> candidate.getExercise().getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Bài luyện tập không thuộc giáo trình của lớp học này."));
    }

    private ClassroomPracticeResponse toResponse(
            ClassroomOffering offering,
            CurriculumExerciseRef ref,
            ClassroomPracticeAttempt attempt,
            Long learnerId
    ) {
        List<ClassroomPracticeAttemptHistory> history = attemptHistoryRepository
                .findByClassroomOfferingIdAndStudentIdAndExerciseIdOrderByCompletedAtDesc(
                        offering.getId(), learnerId, ref.getExercise().getId()
                );
        ClassroomPracticeAttemptHistory latest = history.isEmpty() ? null : history.getFirst();
        return ClassroomPracticeResponse.builder()
                .classroomOfferingId(offering.getId())
                .classroomTitle(resolveClassroomTitle(offering))
                .unitId(ref.getUnit().getId())
                .unitDisplayOrder(ref.getUnit().getDisplayOrder())
                .unitTitle(ref.getUnit().getTitle())
                .exerciseId(ref.getExercise().getId())
                .title(ref.getExercise().getTitle())
                .skill(ref.getExercise().getSkill())
                .exerciseType(ref.getExercise().getExerciseType())
                .instruction(ref.getExercise().getPrompt())
                .note(ref.getNote())
                .completed(attempt != null)
                .responseText(attempt == null ? null : attempt.getResponseText())
                .completedAt(attempt == null ? null : attempt.getCompletedAt())
                .attemptCount(history.isEmpty() && attempt != null ? 1 : history.size())
                .lastScorePercent(latest == null ? null : latest.getScorePercent())
                .build();
    }

    private ClassroomPracticeAttemptResponse toAttemptResponse(ClassroomPracticeAttemptHistory attempt) {
        return ClassroomPracticeAttemptResponse.builder()
                .id(attempt.getId())
                .classroomOfferingId(attempt.getClassroomOffering().getId())
                .exerciseId(attempt.getExercise().getId())
                .exerciseTitle(attempt.getExercise().getTitle())
                .attemptNumber(attempt.getAttemptNumber())
                .responseText(attempt.getResponseText())
                .answersJson(attempt.getAnswersJson())
                .correctAnswers(attempt.getCorrectAnswers())
                .totalQuestions(attempt.getTotalQuestions())
                .scorePercent(attempt.getScorePercent())
                .durationSeconds(attempt.getDurationSeconds())
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .explanation(attempt.getExercise().getExplanation())
                .build();
    }

    private ClassroomPracticeAttemptResponse toLegacyAttemptResponse(ClassroomPracticeAttempt attempt) {
        return ClassroomPracticeAttemptResponse.builder()
                .id(-attempt.getId())
                .classroomOfferingId(attempt.getClassroomOffering().getId())
                .exerciseId(attempt.getExercise().getId())
                .exerciseTitle(attempt.getExercise().getTitle())
                .attemptNumber(1)
                .responseText(attempt.getResponseText())
                .completedAt(attempt.getCompletedAt())
                .explanation(attempt.getExercise().getExplanation())
                .build();
    }

    private void validateSubmission(CompletePracticeRequest request) {
        if (request == null || (isBlank(request.getResponseText()) && isBlank(request.getAnswersJson()))) {
            throw new IllegalArgumentException("Bạn cần hoàn thành bài làm trước khi nộp lượt luyện tập.");
        }
    }

    private ScoreResult score(String answersJson, String answerKeyJson) {
        if (isBlank(answersJson) || isBlank(answerKeyJson)) return new ScoreResult(null, null, null);
        try {
            JsonNode answers = objectMapper.readTree(answersJson);
            JsonNode answerKey = objectMapper.readTree(answerKeyJson);
            if (!answers.isObject() || !answerKey.isObject() || answerKey.isEmpty()) {
                return new ScoreResult(null, null, null);
            }
            int total = answerKey.size();
            int correct = 0;
            Iterator<String> fields = answerKey.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (answerKey.path(field).asText().trim().equalsIgnoreCase(answers.path(field).asText().trim())) {
                    correct += 1;
                }
            }
            return new ScoreResult(correct, total, total == 0 ? null : correct * 100.0D / total);
        } catch (Exception ignored) {
            return new ScoreResult(null, null, null);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveClassroomTitle(ClassroomOffering offering) {
        if (offering.getLearningPackage() != null && offering.getLearningPackage().getTitle() != null) {
            return offering.getLearningPackage().getTitle();
        }
        if (offering.getTrainingProgram() != null && offering.getTrainingProgram().getTitle() != null) {
            return offering.getTrainingProgram().getTitle();
        }
        return "Lớp học #" + offering.getId();
    }

    private record ScoreResult(Integer correctAnswers, Integer totalQuestions, Double scorePercent) {
    }
}
