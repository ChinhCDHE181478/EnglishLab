package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.dto.request.classroom.CompletePracticeRequest;
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
    private final ClassroomAccessHelper accessHelper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomPracticeResponse> listForLearner(Long offeringId, String learnerEmail) {
        User learner = requireLearnerAccess(offeringId, learnerEmail);
        ClassroomOffering offering = requireOffering(offeringId);
        Map<Long, ClassroomPracticeAttempt> attempts = attemptRepository
                .findByClassroomOfferingIdAndStudentId(offeringId, learner.getId()).stream()
                .collect(Collectors.toMap(attempt -> attempt.getExercise().getId(), Function.identity()));
        return practiceRefs(offering).stream()
                .map(ref -> toResponse(ref, attempts.get(ref.getExercise().getId())))
                .toList();
    }

    @Override
    public ClassroomPracticeResponse complete(
            Long offeringId,
            Long exerciseId,
            CompletePracticeRequest request,
            String learnerEmail
    ) {
        User learner = requireLearnerAccess(offeringId, learnerEmail);
        ClassroomOffering offering = requireOffering(offeringId);
        CurriculumExerciseRef ref = practiceRefs(offering).stream()
                .filter(candidate -> candidate.getExercise().getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Bài luyện tập không thuộc giáo trình của lớp học này."));
        ClassroomPracticeAttempt attempt = attemptRepository
                .findByClassroomOfferingIdAndStudentIdAndExerciseId(offeringId, learner.getId(), exerciseId)
                .orElseGet(() -> ClassroomPracticeAttempt.builder()
                        .classroomOffering(offering)
                        .student(learner)
                        .exercise(ref.getExercise())
                        .build());
        attempt.setResponseText(request == null ? null : request.getResponseText());
        attempt.setCompletedAt(LocalDateTime.now());
        return toResponse(ref, attemptRepository.save(attempt));
    }

    private User requireLearnerAccess(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        boolean hasAccess = enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                learner.getId(), offeringId, HAS_LEARNING_ACCESS);
        if (!hasAccess) {
            throw new RuntimeException("Bạn không thuộc lớp học này.");
        }
        return learner;
    }

    private ClassroomOffering requireOffering(Long offeringId) {
        return offeringRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
    }

    private List<CurriculumExerciseRef> practiceRefs(ClassroomOffering offering) {
        if (offering.getCurriculumProgram() == null) {
            return List.of();
        }
        return offering.getCurriculumProgram().getUnits().stream()
                .sorted(Comparator.comparing(unit -> Optional.ofNullable(unit.getDisplayOrder()).orElse(0)))
                .flatMap(unit -> unit.getExerciseRefs().stream()
                        .filter(ref -> ref.getExercise() != null && ref.getExercise().isActive())
                        .sorted(Comparator.comparing(ref -> Optional.ofNullable(ref.getDisplayOrder()).orElse(0))))
                .toList();
    }

    private ClassroomPracticeResponse toResponse(CurriculumExerciseRef ref, ClassroomPracticeAttempt attempt) {
        return ClassroomPracticeResponse.builder()
                .unitId(ref.getUnit().getId())
                .unitDisplayOrder(ref.getUnit().getDisplayOrder())
                .unitTitle(ref.getUnit().getTitle())
                .exerciseId(ref.getExercise().getId())
                .title(ref.getExercise().getTitle())
                .skill(ref.getExercise().getSkill())
                .instruction(ref.getExercise().getPrompt())
                .note(ref.getNote())
                .completed(attempt != null)
                .responseText(attempt == null ? null : attempt.getResponseText())
                .completedAt(attempt == null ? null : attempt.getCompletedAt())
                .build();
    }
}
