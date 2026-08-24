package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.UpsertExerciseBankItemRequest;
import fu.sep490.g23.backend.dto.response.assessment.ExerciseBankItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ExerciseBankService {
    List<ExerciseBankItemResponse> list(String skill, boolean includeInactive);

    Page<ExerciseBankItemResponse> page(
            String skill,
            String exerciseType,
            Boolean active,
            String keyword,
            Pageable pageable
    );

    Map<String, Long> stats(String skill);

    ExerciseBankItemResponse get(Long id);

    ExerciseBankItemResponse create(UpsertExerciseBankItemRequest request, String creatorEmail);

    ExerciseBankItemResponse update(Long id, UpsertExerciseBankItemRequest request);

    void deactivate(Long id);
}
