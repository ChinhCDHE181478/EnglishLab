package fu.sep490.g23.backend.service.assessment.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fu.sep490.g23.backend.dto.request.assessment.UpsertExerciseBankItemRequest;
import fu.sep490.g23.backend.dto.response.assessment.ExerciseBankItemResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.service.assessment.ExerciseBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExerciseBankServiceImpl implements ExerciseBankService {

    private final ExerciseBankItemRepository repository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseBankItemResponse> list(String skill, boolean includeInactive) {
        List<ExerciseBankItem> items;
        if (StringUtils.hasText(skill)) {
            items = includeInactive
                    ? repository.findAllByOrderByUpdatedAtDesc().stream()
                    .filter(item -> skill.equalsIgnoreCase(item.getSkill()))
                    .toList()
                    : repository.findBySkillAndActiveTrueOrderByUpdatedAtDesc(skill.toUpperCase());
        } else {
            items = includeInactive
                    ? repository.findAllByOrderByUpdatedAtDesc()
                    : repository.findByActiveTrueOrderByUpdatedAtDesc();
        }
        return items.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExerciseBankItemResponse get(Long id) {
        return toResponse(findItem(id));
    }

    @Override
    public ExerciseBankItemResponse create(UpsertExerciseBankItemRequest request, String creatorEmail) {
        validateSystemPractice(request);
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
        ExerciseBankItem item = ExerciseBankItem.builder()
                .title(request.getTitle().trim())
                .skill(request.getSkill().trim().toUpperCase())
                .level(trimOrNull(request.getLevel()))
                .exerciseType(StringUtils.hasText(request.getExerciseType())
                        ? request.getExerciseType().trim().toUpperCase()
                        : "HOMEWORK")
                .prompt(request.getPrompt().trim())
                .answerKey(trimOrNull(request.getAnswerKey()))
                .explanation(trimOrNull(request.getExplanation()))
                .tags(trimOrNull(request.getTags()))
                .active(request.getActive() == null || request.getActive())
                .createdBy(creator)
                .build();
        return toResponse(repository.save(item));
    }

    @Override
    public ExerciseBankItemResponse update(Long id, UpsertExerciseBankItemRequest request) {
        validateSystemPractice(request);
        ExerciseBankItem item = findItem(id);
        item.setTitle(request.getTitle().trim());
        item.setSkill(request.getSkill().trim().toUpperCase());
        item.setLevel(trimOrNull(request.getLevel()));
        if (StringUtils.hasText(request.getExerciseType())) {
            item.setExerciseType(request.getExerciseType().trim().toUpperCase());
        }
        item.setPrompt(request.getPrompt().trim());
        item.setAnswerKey(trimOrNull(request.getAnswerKey()));
        item.setExplanation(trimOrNull(request.getExplanation()));
        item.setTags(trimOrNull(request.getTags()));
        if (request.getActive() != null) {
            item.setActive(request.getActive());
        }
        return toResponse(repository.save(item));
    }

    @Override
    public void deactivate(Long id) {
        ExerciseBankItem item = findItem(id);
        item.setActive(false);
        repository.save(item);
    }

    private ExerciseBankItem findItem(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập trong ngân hàng."));
    }

    private void validateSystemPractice(UpsertExerciseBankItemRequest request) {
        String exerciseType = StringUtils.hasText(request.getExerciseType())
                ? request.getExerciseType().trim().toUpperCase()
                : "HOMEWORK";
        String skill = StringUtils.hasText(request.getSkill())
                ? request.getSkill().trim().toUpperCase()
                : "";
        if (!"PRACTICE".equals(exerciseType)
                || !("LISTENING".equals(skill) || "READING".equals(skill))) {
            return;
        }

        JsonNode config;
        try {
            config = objectMapper.readTree(request.getPrompt());
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Bài luyện tập Listening/Reading phải được biên soạn bằng trình làm bài trên hệ thống.");
        }
        JsonNode parts = config == null ? null : config.path("parts");
        if (config == null || !config.isObject() || !parts.isArray() || parts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bài luyện tập Listening/Reading phải được biên soạn bằng trình làm bài trên hệ thống.");
        }

        JsonNode answerKey;
        try {
            answerKey = objectMapper.readTree(request.getAnswerKey());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Bài luyện tập phải có đáp án chấm tự động.");
        }
        if (answerKey == null || !answerKey.isObject() || answerKey.isEmpty()) {
            throw new IllegalArgumentException("Bài luyện tập phải có đáp án chấm tự động.");
        }
    }

    private ExerciseBankItemResponse toResponse(ExerciseBankItem item) {
        return ExerciseBankItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .skill(item.getSkill())
                .level(item.getLevel())
                .exerciseType(item.getExerciseType())
                .prompt(item.getPrompt())
                .answerKey(item.getAnswerKey())
                .explanation(item.getExplanation())
                .tags(item.getTags())
                .active(item.isActive())
                .createdByName(item.getCreatedBy() == null ? null : item.getCreatedBy().getFullName())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
