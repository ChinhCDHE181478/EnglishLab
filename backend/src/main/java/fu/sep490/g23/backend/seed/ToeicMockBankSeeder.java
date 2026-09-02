package fu.sep490.g23.backend.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.curriculum.AssessmentBankItemRequest;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.service.curriculum.InstructorLedCourseManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Component
@Order(46)
@RequiredArgsConstructor
@Slf4j
public class ToeicMockBankSeeder implements CommandLineRunner {

    private final AssessmentBankItemRepository assessmentBankItemRepository;
    private final InstructorLedCourseManagementService instructorLedCourseManagementService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) {
        try {
            JsonNode index = objectMapper.readTree(
                    new ClassPathResource("sheet-data/toeic-mocks-index.json").getInputStream());
            for (JsonNode item : index) {
                publishMock(
                        item.path("title").asText(),
                        AssessmentSkill.valueOf(item.path("skill").asText()),
                        item.path("minutes").asInt(),
                        item.path("resource").asText()
                );
            }
            log.info("[TOEIC mock] Da cap nhat {} de Listening/Reading.", index.size());
        } catch (Exception ex) {
            log.warn("[TOEIC mock] Khong xuat ban het de TOEIC: {}", ex.getMessage());
        }
    }

    private void publishMock(String title, AssessmentSkill skill, int minutes, String resource) throws Exception {
        String json = new String(new ClassPathResource(resource).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        var existing = assessmentBankItemRepository
                .findByTypeAndStatusOrderByUpdatedAtDescIdDesc(AssessmentType.MOCK_TEST, "PUBLISHED")
                .stream()
                .filter(item -> title.equalsIgnoreCase(item.getTitle()))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().setUiConfigJson(json);
            existing.get().setTimeLimitMinutes(minutes);
            existing.get().setObjectiveAnswerKey("{}");
            assessmentBankItemRepository.save(existing.get());
            return;
        }
        AssessmentBankItemRequest request = new AssessmentBankItemRequest();
        request.setTitle(title);
        request.setDescription("Đề thi thử TOEIC Listening & Reading.");
        request.setType(AssessmentType.MOCK_TEST);
        request.setSkill(skill);
        request.setAiEvaluationMode(AiEvaluationMode.NONE);
        request.setInstructions("Làm bài theo đúng giao diện thi thử EnglishLab.");
        request.setUiConfigJson(json);
        request.setObjectiveAnswerKey("{}");
        request.setPassingScore(BigDecimal.valueOf(6.0));
        request.setMaxScore(BigDecimal.valueOf(9.0));
        request.setTimeLimitMinutes(minutes);
        request.setStatus("PUBLISHED");
        instructorLedCourseManagementService.createAssessmentBankItem(request);
    }
}
