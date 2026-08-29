package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomPracticeAttemptHistoryRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitContentRefRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.classroom.CompletePracticeRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomPracticeAttemptResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomPracticeResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassroomPracticeAttemptHistory;
import fu.sep490.g23.backend.entity.course.enums.CourseUnitContentType;
import fu.sep490.g23.backend.entity.course.CourseUnitContentRef;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.ClassroomPracticeService;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomPracticeServiceImpl implements ClassroomPracticeService {
    private static final Set<ClassroomRegistrationStatus> HAS_LEARNING_ACCESS = ClassroomRegistrationSupport.HAS_LEARNING_ACCESS;

    private final ClassSectionRepository offeringRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassroomPracticeAttemptHistoryRepository attemptHistoryRepository;
    private final ClassroomAccessHelper accessHelper;
    private final CourseUnitContentRefRepository contentRefRepository;
    private final ExerciseBankItemRepository exerciseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();



    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }


    private record ScoreResult(Integer correctAnswers, Integer totalQuestions, Double scorePercent) {
    }

    private record PracticeRef(
            CourseUnitContentRef link,
            fu.sep490.g23.backend.entity.assessment.ExerciseBankItem exercise
    ) {
    }
}
