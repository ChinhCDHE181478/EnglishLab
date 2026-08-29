package fu.sep490.g23.backend.service.assessment.impl;
import fu.sep490.g23.backend.dto.response.assessment.RubricCriterionResponse;
import fu.sep490.g23.backend.service.assessment.IeltsBandScale;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.dto.response.assessment.AiAssessmentSubmissionResponse;
import fu.sep490.g23.backend.service.assessment.AssessmentPassingThresholdResolver;
import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.service.assessment.AiAssessmentService;
import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sep490.g23.backend.service.assessment.AssessmentAudioStorageService;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fu.sep490.g23.backend.dto.request.assessment.AssessmentSubmissionRequest;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.AssessmentSubmission;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.RubricCriterion;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.course.CourseProgressService;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class AiAssessmentServiceImpl implements AiAssessmentService {
    private static final Pattern VOCABULARY_HEADING = Pattern.compile("(?m)^###\\s+\\d+\\.\\s+(.+)$");
    private static final BigDecimal VOCABULARY_OFF_TOPIC_CAP_RATIO = BigDecimal.valueOf(0.35);
    private static final String UI_CONFIG_MARKER = "\n\n[ENGLISHLAB_UI_CONFIG]\n";
    private static final Pattern SPEAKING_DURATION_PATTERN = Pattern.compile("Recording duration seconds:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEAKING_WORD_COUNT_PATTERN = Pattern.compile("Transcript word count:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private final CourseAssessmentRepository courseAssessmentRepository;
    private final AssessmentSubmissionRepository submissionRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AiEvaluationClient aiEvaluationClient;
    private final AssessmentAudioStorageService assessmentAudioStorageService;
    private final CourseProgressService courseProgressService;
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    private final OnlineCourseVersionService onlineCourseVersionService;
    private final AssessmentPassingThresholdResolver passingThresholdResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();





    private ArrayNode listToArrayNode(List<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        if (values == null) {
            return array;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .forEach(array::add);
        return array;
    }


    private Set<String> parseAnswerSet(String studentAnswer) {
        Set<String> selected = new LinkedHashSet<>();
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return selected;
        }
        Arrays.stream(studentAnswer.split("[,\\s]+"))
                .map(this::normalizeLetter)
                .filter(value -> !value.isBlank())
                .forEach(selected::add);
        return selected;
    }



    private String normalizeLetter(String value) {
        return String.valueOf(value == null ? "" : value).trim().toUpperCase(Locale.ROOT);
    }

    private String fallbackText(String value) {
        return hasText(value) ? value.trim() : "chưa trả lời";
    }

    private String prettyPartLabel(String partKey) {
        if (partKey == null || partKey.isBlank()) {
            return "Phần thi";
        }
        String normalized = partKey.replace("part_", "").trim();
        return "Part " + normalized;
    }

    private static final class ObjectiveEvaluationSummary {
        private int correctCount;
        private int totalCount;
        private final Map<String, ObjectivePartSummary> parts = new LinkedHashMap<>();
    }

    private static final class ObjectivePartSummary {
        private final String partKey;
        private final String partLabel;
        private int correctCount;
        private int totalCount;
        private final List<String> strengths = new ArrayList<>();
        private final List<String> weaknesses = new ArrayList<>();

        private ObjectivePartSummary(String partKey, String partLabel) {
            this.partKey = partKey;
            this.partLabel = partLabel;
        }
    }

    private java.util.Optional<AssessmentAudioStorageService.StoredAssessmentAudio> resolveSpeakingAudio(CourseAssessment assessment, AssessmentSubmissionRequest request) {
        if (assessment.getSkill() != AssessmentSkill.SPEAKING || request == null || !hasText(request.getSubmittedAudioUrl())) {
            return java.util.Optional.empty();
        }
        return assessmentAudioStorageService.loadStoredAudioFromUrl(request.getSubmittedAudioUrl());
    }




    private int extractInt(String source, Pattern pattern) {
        if (source == null || source.isBlank()) {
            return 0;
        }
        var matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception ignored) {
            return 0;
        }
    }


    private void appendUniqueText(ObjectNode root, String fieldName, String value) {
        ArrayNode array = ensureArray(root, fieldName);
        boolean exists = false;
        for (JsonNode item : array) {
            if (value.equalsIgnoreCase(item.asText())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            array.add(value);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean containsWholePhrase(String text, String phrase) {
        String normalizedText = " " + text.toLowerCase(Locale.ROOT) + " ";
        String normalizedPhrase = " " + phrase.toLowerCase(Locale.ROOT) + " ";
        return normalizedText.contains(normalizedPhrase);
    }

    private boolean hasSubmissionContent(AssessmentSubmissionRequest request) {
        return request != null && (
                hasText(request.getSubmittedText())
                        || hasText(request.getObjectiveAnswersJson())
                        || hasText(request.getSubmittedAudioUrl())
        );
    }



    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(Object value) {
        return value == null ? "Not provided" : value.toString();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }




    private String cleanMarkdown(String value) {
        return value == null ? "" : value.replace("**", "").replaceAll("^['\"]|['\"]$", "").trim();
    }

}
