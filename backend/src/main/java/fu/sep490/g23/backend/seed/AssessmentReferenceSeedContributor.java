package fu.sep490.g23.backend.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.curriculum.AssessmentBankItemRequest;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import fu.sep490.g23.backend.service.curriculum.CurriculumProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AssessmentReferenceSeedContributor implements SeedDataContributor {

    private static final List<String> MOCK_INDEXES = List.of(
            "sheet-data/iot-mocks-index.json",
            "sheet-data/toeic-mocks-index.json"
    );

    private final AssessmentBankItemRepository assessmentRepository;
    private final CurriculumProgramService curriculumProgramService;
    private final PlacementTestDefinitionService placementTestDefinitionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public int order() {
        return 200;
    }

    @Override
    public String name() {
        return "Đề thi placement test và mock test";
    }

    @Override
    public Set<SeedMode> supportedModes() {
        return Set.of(
                SeedMode.TEST,
                SeedMode.REVIEW,
                SeedMode.SHEET,
                SeedMode.ASSESSMENT_REFERENCE
        );
    }

    @Override
    public void seed(Set<SeedMode> activeModes) {
        placementTestDefinitionService.getDefinition();
        MOCK_INDEXES.forEach(this::seedMockIndex);
    }

    private void seedMockIndex(String resourcePath) {
        try {
            JsonNode index = readJson(resourcePath);
            if (!index.isArray()) {
                throw new IllegalStateException("Danh mục đề thi không phải một mảng: " + resourcePath);
            }
            for (JsonNode item : index) {
                upsertMock(item);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc dữ liệu đề thi: " + resourcePath, exception);
        }
    }

    private void upsertMock(JsonNode item) throws IOException {
        String title = requiredText(item, "title");
        AssessmentSkill skill = AssessmentSkill.valueOf(requiredText(item, "skill"));
        int minutes = item.path("minutes").asInt();
        String resourcePath = requiredText(item, "resource");
        String configJson = readText(resourcePath);

        AssessmentBankItem existing = assessmentRepository.findByTypeOrderByUpdatedAtDescIdDesc(AssessmentType.MOCK_TEST)
                .stream()
                .filter(candidate -> title.equalsIgnoreCase(candidate.getTitle()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.setSkill(skill);
            existing.setUiConfigJson(configJson);
            existing.setTimeLimitMinutes(minutes);
            existing.setObjectiveAnswerKey("{}");
            existing.setStatus("PUBLISHED");
            existing.setActive(true);
            assessmentRepository.save(existing);
            return;
        }

        AssessmentBankItemRequest request = new AssessmentBankItemRequest();
        request.setTitle(title);
        request.setDescription("Đề thi thử được quản lý trong kho đề EnglishLab.");
        request.setType(AssessmentType.MOCK_TEST);
        request.setSkill(skill);
        request.setAiEvaluationMode(AiEvaluationMode.NONE);
        request.setInstructions("Làm bài theo thời gian và hướng dẫn của từng phần thi.");
        request.setUiConfigJson(configJson);
        request.setObjectiveAnswerKey("{}");
        request.setPassingScore(BigDecimal.valueOf(6));
        request.setMaxScore(BigDecimal.valueOf(9));
        request.setTimeLimitMinutes(minutes);
        request.setStatus("PUBLISHED");
        request.setDisplayOrder(20);
        curriculumProgramService.createAssessmentBankItem(request);
    }

    private JsonNode readJson(String resourcePath) throws IOException {
        return objectMapper.readTree(new ClassPathResource(resourcePath).getInputStream());
    }

    private String readText(String resourcePath) throws IOException {
        return new String(
                new ClassPathResource(resourcePath).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Thiếu trường " + field + " trong danh mục đề thi.");
        }
        return value;
    }
}
