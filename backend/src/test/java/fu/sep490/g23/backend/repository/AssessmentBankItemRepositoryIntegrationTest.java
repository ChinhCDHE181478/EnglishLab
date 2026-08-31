package fu.sep490.g23.backend.repository;

import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AssessmentBankItemRepositoryIntegrationTest {

    @Autowired
    private AssessmentBankItemRepository assessmentBankItemRepository;

    @Test
    void searchPageFiltersFieldsStoredInCanonicalPayload() {
        String marker = UUID.randomUUID().toString();
        AssessmentBankItem item = AssessmentBankItem.builder()
                .title("TOEIC repository regression " + marker)
                .description("Kiểm tra truy vấn kho đề")
                .skill(AssessmentSkill.READING)
                .status("PUBLISHED")
                .type(AssessmentType.MODULE_TEST)
                .instructions("Find marker " + marker)
                .uiConfigJson("{\"examCategory\":\"TOEIC\"}")
                .build();
        assessmentBankItemRepository.saveAndFlush(item);

        Page<AssessmentBankItem> result = assessmentBankItemRepository.searchPage(
                "READING",
                "MODULE_TEST",
                "PUBLISHED",
                "%" + marker.toLowerCase() + "%",
                "TOEIC",
                PageRequest.of(0, 8)
        );

        assertThat(result.getContent())
                .extracting(AssessmentBankItem::getId)
                .contains(item.getId());
    }
}
