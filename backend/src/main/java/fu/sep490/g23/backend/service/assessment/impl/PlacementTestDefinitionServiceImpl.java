package fu.sep490.g23.backend.service.assessment.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.assessment.PlacementTestDefinitionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestDefinitionResponse;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestMonitoringResponse;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.PlacementTestDefinition;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestDefinitionRepository;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlacementTestDefinitionServiceImpl implements PlacementTestDefinitionService {
    public static final String TEST_CODE = PlacementTestDefinitionService.TEST_CODE;
    private static final String DEFAULT_TOEIC_RESOURCE = "placement-test/current-toeic.json";

    private final PlacementTestDefinitionRepository definitionRepository;
    private final PlacementTestAttemptRepository attemptRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public PlacementTestDefinition getDefinition() {
        return definitionRepository.findByTestCode(TEST_CODE).orElseGet(this::createDefaultDefinition);
    }

    @Override
    @Transactional
    public PlacementTestDefinitionResponse getManagementDefinition() {
        return toResponse(getDefinition());
    }

    @Override
    @Transactional
    public PlacementTestDefinitionResponse updateDefinition(PlacementTestDefinitionRequest request) {
        validateConfig(request.getListeningConfigJson(), "Nghe");
        validateConfig(request.getReadingConfigJson(), "Đọc");
        validateConfig(request.getWritingConfigJson(), "Viết");
        validateConfig(request.getSpeakingConfigJson(), "Nói");
        if (request.getToeicConfigJson() != null && !request.getToeicConfigJson().isBlank()) {
            validateConfig(request.getToeicConfigJson(), "TOEIC");
        }

        PlacementTestDefinition definition = getDefinition();
        definition.setTitle(request.getTitle().trim());
        definition.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        definition.setExamType(normalizeExamType(request.getExamType()));
        definition.setMaxAttempts(request.getMaxAttempts());
        definition.setActive(request.isActive());
        definition.setListeningConfigJson(request.getListeningConfigJson());
        definition.setReadingConfigJson(request.getReadingConfigJson());
        definition.setWritingConfigJson(request.getWritingConfigJson());
        definition.setSpeakingConfigJson(request.getSpeakingConfigJson());
        definition.setToeicConfigJson(request.getToeicConfigJson() == null || request.getToeicConfigJson().isBlank()
                ? defaultToeicConfig()
                : request.getToeicConfigJson());
        definition.setUpdatedAt(LocalDateTime.now());
        return toResponse(definitionRepository.save(definition));
    }

    @Override
    @Transactional(readOnly = true)
    public PlacementTestMonitoringResponse getMonitoring() {
        List<PlacementTestAttempt> attempts = attemptRepository.findByTestCodeOrderBySubmittedAtDesc(TEST_CODE);
        return PlacementTestMonitoringResponse.builder()
                .totalAttempts(attempts.size())
                .uniqueParticipants(attemptRepository.countDistinctStudentsByTestCode(TEST_CODE))
                .completedAttempts(attempts.stream().filter(attempt -> "COMPLETED".equals(attempt.getStatus())).count())
                .averageOverallBand(average(attempts, PlacementTestAttempt::getOverallScore))
                .averageListeningBand(average(attempts, PlacementTestAttempt::getListeningScore))
                .averageReadingBand(average(attempts, PlacementTestAttempt::getReadingScore))
                .averageWritingBand(average(attempts, PlacementTestAttempt::getWritingScore))
                .averageSpeakingBand(average(attempts, PlacementTestAttempt::getSpeakingScore))
                .bandDistribution(List.of(
                        distribution("Dưới 4.0", attempts, score -> score != null && score.compareTo(BigDecimal.valueOf(4)) < 0),
                        distribution("4.0 - 4.5", attempts, score -> score != null && score.compareTo(BigDecimal.valueOf(4)) >= 0 && score.compareTo(BigDecimal.valueOf(5)) < 0),
                        distribution("5.0 - 5.5", attempts, score -> score != null && score.compareTo(BigDecimal.valueOf(5)) >= 0 && score.compareTo(BigDecimal.valueOf(6)) < 0),
                        distribution("6.0 - 6.5", attempts, score -> score != null && score.compareTo(BigDecimal.valueOf(6)) >= 0 && score.compareTo(BigDecimal.valueOf(7)) < 0),
                        distribution("Từ 7.0", attempts, score -> score != null && score.compareTo(BigDecimal.valueOf(7)) >= 0)
                ))
                .recentAttempts(attemptRepository.findTop20ByTestCodeOrderBySubmittedAtDesc(TEST_CODE).stream()
                        .map(this::toRecentAttempt)
                        .toList())
                .build();
    }

    @Override
    public JsonNode getConfig(PlacementTestDefinition definition, String skill) {
        String config = switch (skill) {
            case "listening" -> definition.getListeningConfigJson();
            case "reading" -> definition.getReadingConfigJson();
            case "writing" -> definition.getWritingConfigJson();
            case "speaking" -> definition.getSpeakingConfigJson();
            case "toeic" -> normalizedToeicConfig(definition.getToeicConfigJson());
            default -> throw new IllegalArgumentException("Kỹ năng đánh giá đầu vào không hợp lệ.");
        };
        try {
            return objectMapper.readTree(config);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cấu hình bài đánh giá đầu vào không hợp lệ.", exception);
        }
    }

    private PlacementTestDefinition createDefaultDefinition() {
        PlacementTestDefinition definition = PlacementTestDefinition.builder()
                .testCode(TEST_CODE)
                .title("Bài đánh giá đầu vào IELTS")
                .description("Một phiên đánh giá gồm Nghe, Đọc, Viết và Nói để gợi ý điểm bắt đầu phù hợp.")
                .examType("IELTS")
                .maxAttempts(3)
                .active(true)
                .listeningConfigJson(loadResource("placement-test/current-listening.json"))
                .readingConfigJson(loadResource("assessment-data/ielts_mock_2025_january_reading_test_1.json"))
                .writingConfigJson(loadResource("assessment-data/ielts_mock_2025_january_writing_test_1.json"))
                .speakingConfigJson(loadResource("placement-test/current-speaking.json"))
                .toeicConfigJson(defaultToeicConfig())
                .updatedAt(LocalDateTime.now())
                .build();
        return definitionRepository.save(definition);
    }

    private String normalizeExamType(String value) {
        if (value == null || value.isBlank()) {
            return "IELTS";
        }
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        return "TOEIC".equals(normalized) ? "TOEIC" : "IELTS";
    }

    private void validateConfig(String json, String skill) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("Cấu hình phần " + skill + " phải là một đối tượng hợp lệ.");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cấu hình phần " + skill + " không hợp lệ.");
        }
    }

    private String loadResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tải dữ liệu khởi tạo cho bài đánh giá đầu vào.", exception);
        }
    }

    private PlacementTestDefinitionResponse toResponse(PlacementTestDefinition definition) {
        return PlacementTestDefinitionResponse.builder()
                .testCode(definition.getTestCode())
                .title(definition.getTitle())
                .description(definition.getDescription())
                .examType(definition.getExamType())
                .maxAttempts(definition.getMaxAttempts())
                .active(definition.isActive())
                .listeningConfigJson(definition.getListeningConfigJson())
                .readingConfigJson(definition.getReadingConfigJson())
                .writingConfigJson(definition.getWritingConfigJson())
                .speakingConfigJson(definition.getSpeakingConfigJson())
                .toeicConfigJson(normalizedToeicConfig(definition.getToeicConfigJson()))
                .updatedAt(definition.getUpdatedAt())
                .build();
    }

    private String normalizedToeicConfig(String config) {
        if (config == null || config.isBlank()) {
            return defaultToeicConfig();
        }
        try {
            JsonNode root = objectMapper.readTree(config);
            if (isEmptyToeicConfig(root) || isIncompleteToeicConfig(root)) {
                return defaultToeicConfig();
            }
            return config;
        } catch (JsonProcessingException exception) {
            return defaultToeicConfig();
        }
    }

    private boolean isEmptyToeicConfig(JsonNode root) {
        if (root == null || !root.isObject()) {
            return true;
        }
        return countToeicQuestions(root.path("listening")) + countToeicQuestions(root.path("reading")) == 0;
    }

    private boolean isIncompleteToeicConfig(JsonNode root) {
        if (root == null || !root.isObject()) {
            return false;
        }
        if (!"toeic_full_test".equals(root.path("type").asText())) {
            return false;
        }
        List<Integer> numbers = new ArrayList<>();
        collectToeicQuestionNumbers(root.path("listening"), numbers);
        collectToeicQuestionNumbers(root.path("reading"), numbers);
        if (numbers.size() != 200) {
            return true;
        }
        for (int expected = 1; expected <= 200; expected++) {
            if (!numbers.contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private int countToeicQuestions(JsonNode section) {
        if (section == null || !section.has("parts")) {
            return 0;
        }
        int total = 0;
        for (JsonNode part : section.withArray("parts")) {
            total += part.withArray("questions").size();
            for (JsonNode group : part.withArray("questionGroups")) {
                total += group.withArray("questions").size();
            }
        }
        return total;
    }

    private void collectToeicQuestionNumbers(JsonNode section, List<Integer> numbers) {
        if (section == null || !section.has("parts")) {
            return;
        }
        for (JsonNode part : section.withArray("parts")) {
            for (JsonNode question : part.withArray("questions")) {
                JsonNode number = question.path("number");
                if (number.canConvertToInt()) {
                    numbers.add(number.asInt());
                }
            }
            for (JsonNode group : part.withArray("questionGroups")) {
                for (JsonNode number : group.withArray("questionNumbers")) {
                    if (number.canConvertToInt()) {
                        numbers.add(number.asInt());
                    }
                }
                for (JsonNode question : group.withArray("questions")) {
                    JsonNode number = question.path("number");
                    if (number.canConvertToInt()) {
                        numbers.add(number.asInt());
                    }
                }
            }
        }
    }

    private String defaultToeicConfig() {
        return loadResource(DEFAULT_TOEIC_RESOURCE);
    }

    private BigDecimal average(List<PlacementTestAttempt> attempts, java.util.function.Function<PlacementTestAttempt, BigDecimal> extractor) {
        List<BigDecimal> scores = attempts.stream().map(extractor).filter(java.util.Objects::nonNull).toList();
        if (scores.isEmpty()) {
            return null;
        }
        return scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 1, java.math.RoundingMode.HALF_UP);
    }

    private PlacementTestMonitoringResponse.BandDistributionItem distribution(
            String label,
            List<PlacementTestAttempt> attempts,
            java.util.function.Predicate<BigDecimal> matcher
    ) {
        return PlacementTestMonitoringResponse.BandDistributionItem.builder()
                .label(label)
                .count(attempts.stream().map(PlacementTestAttempt::getOverallScore).filter(matcher).count())
                .build();
    }

    private PlacementTestMonitoringResponse.RecentAttempt toRecentAttempt(PlacementTestAttempt attempt) {
        return PlacementTestMonitoringResponse.RecentAttempt.builder()
                .id(attempt.getId())
                .learnerName(attempt.getStudent().getFullName())
                .learnerEmail(attempt.getStudent().getEmail())
                .overallBand(attempt.getOverallScore())
                .listeningBand(attempt.getListeningScore())
                .readingBand(attempt.getReadingScore())
                .writingBand(attempt.getWritingScore())
                .speakingBand(attempt.getSpeakingScore())
                .status(attempt.getStatus())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }
}
