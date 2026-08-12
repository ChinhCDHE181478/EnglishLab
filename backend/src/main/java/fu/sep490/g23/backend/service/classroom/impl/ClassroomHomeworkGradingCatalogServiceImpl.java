package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkGradingCatalogService;

import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.dto.response.assessment.RubricCriterionResponse;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.RubricCriterion;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomHomeworkGradingCatalogServiceImpl implements ClassroomHomeworkGradingCatalogService {

    private static final List<AssessmentSkill> HOMEWORK_SKILLS = List.of(
            AssessmentSkill.SPEAKING,
            AssessmentSkill.LISTENING,
            AssessmentSkill.WRITING,
            AssessmentSkill.READING,
            AssessmentSkill.VOCABULARY
    );

    private final AssessmentRubricRepository rubricRepository;

    public List<AssessmentSkill> listHomeworkSkills() {
        return HOMEWORK_SKILLS;
    }

    public List<AssessmentRubricResponse> listRubricsBySkill(AssessmentSkill skill) {
        if (skill == null) {
            return List.of();
        }
        return rubricRepository.findBySkillAndActiveTrueOrderByIdAsc(skill).stream()
                .map(this::toRubricResponse)
                .toList();
    }

    public List<AssessmentRubricResponse> listAllHomeworkRubrics() {
        return HOMEWORK_SKILLS.stream()
                .flatMap(skill -> rubricRepository.findBySkillAndActiveTrueOrderByIdAsc(skill).stream())
                .sorted(Comparator
                        .comparing((AssessmentRubric rubric) -> skillOrder(rubric.getSkill()))
                        .thenComparing(AssessmentRubric::getId))
                .map(this::toRubricResponse)
                .toList();
    }

    public AssessmentRubric requireActiveRubric(Long rubricId) {
        AssessmentRubric rubric = rubricRepository.findById(rubricId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ tiêu chí chấm."));
        if (!rubric.isActive()) {
            throw new RuntimeException("Bộ tiêu chí chấm không còn hoạt động.");
        }
        return rubric;
    }

    private int skillOrder(AssessmentSkill skill) {
        if (skill == null) {
            return 99;
        }
        int index = HOMEWORK_SKILLS.indexOf(skill);
        return index >= 0 ? index : 99;
    }

    public AssessmentRubricResponse mapRubric(AssessmentRubric rubric) {
        if (rubric == null) {
            return null;
        }
        return toRubricResponse(rubric);
    }

    private AssessmentRubricResponse toRubricResponse(AssessmentRubric rubric) {
        return AssessmentRubricResponse.builder()
                .id(rubric.getId())
                .name(rubric.getName())
                .examType(rubric.getExamType())
                .skill(rubric.getSkill())
                .taskType(rubric.getTaskType())
                .scoringScale(rubric.getScoringScale())
                .description(rubric.getDescription())
                .active(rubric.isActive())
                .criteria(rubric.getCriteria().stream()
                        .sorted(Comparator.comparing(RubricCriterion::getDisplayOrder).thenComparing(RubricCriterion::getId))
                        .map(criterion -> RubricCriterionResponse.builder()
                                .id(criterion.getId())
                                .name(criterion.getName())
                                .weight(criterion.getWeight())
                                .displayOrder(criterion.getDisplayOrder())
                                .description(criterion.getDescription())
                                .bandDescriptors(criterion.getBandDescriptors())
                                .build())
                        .toList())
                .build();
    }
}
