package fu.sep490.g23.backend.service.assessment.impl;

import fu.sep490.g23.backend.dto.request.assessment.AssessmentRubricRequest;
import fu.sep490.g23.backend.dto.request.assessment.RubricCriterionRequest;
import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.dto.response.assessment.RubricCriterionResponse;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.RubricCriterion;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sep490.g23.backend.service.assessment.AssessmentRubricService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentRubricServiceImpl implements AssessmentRubricService {

    private final AssessmentRubricRepository rubricRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentRubricResponse> list(Boolean includeInactive, AssessmentSkill skill) {
        boolean showInactive = Boolean.TRUE.equals(includeInactive);
        return rubricRepository.findAll().stream()
                .filter(rubric -> showInactive || rubric.isActive())
                .filter(rubric -> skill == null || rubric.getSkill() == skill)
                .sorted(Comparator
                        .comparing((AssessmentRubric rubric) -> rubric.getSkill() == null ? "" : rubric.getSkill().name())
                        .thenComparing(AssessmentRubric::getId))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssessmentRubricResponse> page(
            Boolean includeInactive,
            Boolean active,
            AssessmentSkill skill,
            String keyword,
            Pageable pageable
    ) {
        Specification<AssessmentRubric> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        if (!Boolean.TRUE.equals(includeInactive)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.isTrue(root.get("active")));
        } else if (active != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("active"), active));
        }
        if (skill != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("skill"), skill));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("examType")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("taskType")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
            ));
        }
        return rubricRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> stats(AssessmentSkill skill) {
        List<AssessmentRubric> rubrics = rubricRepository.findAll().stream()
                .filter(rubric -> skill == null || rubric.getSkill() == skill)
                .toList();
        return Map.of(
                "total", (long) rubrics.size(),
                "active", rubrics.stream().filter(AssessmentRubric::isActive).count(),
                "inactive", rubrics.stream().filter(rubric -> !rubric.isActive()).count(),
                "criteria", rubrics.stream().mapToLong(rubric -> rubric.getCriteria().size()).sum()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentRubricResponse get(Long id) {
        return toResponse(findRubric(id));
    }

    @Override
    public AssessmentRubricResponse create(AssessmentRubricRequest request) {
        validateRequest(request);
        AssessmentRubric rubric = AssessmentRubric.builder().build();
        applyRequest(rubric, request);
        return toResponse(rubricRepository.save(rubric));
    }

    @Override
    public AssessmentRubricResponse update(Long id, AssessmentRubricRequest request) {
        validateRequest(request);
        AssessmentRubric rubric = findRubric(id);
        applyRequest(rubric, request);
        return toResponse(rubricRepository.save(rubric));
    }

    @Override
    public AssessmentRubricResponse deactivate(Long id) {
        AssessmentRubric rubric = findRubric(id);
        rubric.setActive(false);
        return toResponse(rubricRepository.save(rubric));
    }

    @Override
    public AssessmentRubricResponse reactivate(Long id) {
        AssessmentRubric rubric = findRubric(id);
        rubric.setActive(true);
        return toResponse(rubricRepository.save(rubric));
    }

    private AssessmentRubric findRubric(Long id) {
        return rubricRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rubric."));
    }

    private void validateRequest(AssessmentRubricRequest request) {
        if (request.getCriteria() == null || request.getCriteria().isEmpty()) {
            throw new IllegalArgumentException("Rubric cần có ít nhất một tiêu chí chấm điểm.");
        }
        int totalWeight = request.getCriteria().stream()
                .map(RubricCriterionRequest::getWeight)
                .mapToInt(weight -> weight == null ? 0 : weight)
                .sum();
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("Tổng trọng số tiêu chí phải lớn hơn 0.");
        }
        boolean hasBlankCriterion = request.getCriteria().stream()
                .anyMatch(criterion -> criterion.getName() == null || criterion.getName().trim().isEmpty());
        if (hasBlankCriterion) {
            throw new IllegalArgumentException("Tên tiêu chí không được để trống.");
        }
    }

    private void applyRequest(AssessmentRubric rubric, AssessmentRubricRequest request) {
        rubric.setName(request.getName().trim());
        rubric.setExamType(trimToNull(request.getExamType()));
        rubric.setSkill(request.getSkill());
        rubric.setTaskType(trimToNull(request.getTaskType()));
        rubric.setScoringScale(trimToNull(request.getScoringScale()));
        rubric.setDescription(trimToNull(request.getDescription()));
        rubric.setActive(request.getActive() == null || request.getActive());
        rubric.getCriteria().clear();
        List<RubricCriterionRequest> criteria = request.getCriteria();
        for (int index = 0; index < criteria.size(); index++) {
            RubricCriterionRequest item = criteria.get(index);
            RubricCriterion criterion = RubricCriterion.builder()
                    .name(item.getName().trim())
                    .weight(item.getWeight() == null ? 0 : item.getWeight())
                    .description(trimToNull(item.getDescription()))
                    .bandDescriptors(trimToNull(item.getBandDescriptors()))
                    .displayOrder(item.getDisplayOrder() == null ? index + 1 : item.getDisplayOrder())
                    .build();
            rubric.addCriterion(criterion);
        }
    }

    private AssessmentRubricResponse toResponse(AssessmentRubric rubric) {
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
                                .description(criterion.getDescription())
                                .bandDescriptors(criterion.getBandDescriptors())
                                .displayOrder(criterion.getDisplayOrder())
                                .build())
                        .toList())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
