package fu.sep490.g23.backend.service.assessment.impl;

import fu.sep490.g23.backend.dto.request.assessment.AssessmentRubricRequest;
import fu.sep490.g23.backend.dto.request.assessment.RubricCriterionRequest;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssessmentRubricServiceImplTest {

    @Mock
    private AssessmentRubricRepository rubricRepository;

    @InjectMocks
    private AssessmentRubricServiceImpl service;

    @Test
    void createRejectsRubricForObjectiveSkill() {
        AssessmentRubricRequest request = validRequest(AssessmentSkill.LISTENING);

        assertThatThrownBy(() -> service.create(request))
                .hasMessage("Bộ tiêu chí chỉ áp dụng cho kỹ năng Writing hoặc Speaking.");
        verify(rubricRepository, never()).save(any(AssessmentRubric.class));
    }

    private AssessmentRubricRequest validRequest(AssessmentSkill skill) {
        RubricCriterionRequest criterion = new RubricCriterionRequest();
        criterion.setName("Task Response");
        criterion.setWeight(100);

        AssessmentRubricRequest request = new AssessmentRubricRequest();
        request.setName("IELTS rubric");
        request.setSkill(skill);
        request.setCriteria(List.of(criterion));
        return request;
    }
}
