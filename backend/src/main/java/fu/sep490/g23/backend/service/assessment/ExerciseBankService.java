package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.UpsertExerciseBankItemRequest;
import fu.sep490.g23.backend.dto.response.assessment.ExerciseBankItemResponse;

import java.util.List;

public interface ExerciseBankService {
    List<ExerciseBankItemResponse> list(String skill, boolean includeInactive);

    ExerciseBankItemResponse get(Long id);

    ExerciseBankItemResponse create(UpsertExerciseBankItemRequest request, String creatorEmail);

    ExerciseBankItemResponse update(Long id, UpsertExerciseBankItemRequest request);

    void deactivate(Long id);
}
