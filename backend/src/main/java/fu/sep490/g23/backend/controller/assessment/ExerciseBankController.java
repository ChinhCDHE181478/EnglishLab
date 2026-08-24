package fu.sep490.g23.backend.controller.assessment;

import fu.sep490.g23.backend.dto.request.assessment.UpsertExerciseBankItemRequest;
import fu.sep490.g23.backend.dto.response.assessment.ExerciseBankItemResponse;
import fu.sep490.g23.backend.service.assessment.ExerciseBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content-manager/exercise-bank")
@RequiredArgsConstructor
public class ExerciseBankController {

    private final ExerciseBankService exerciseBankService;

    @GetMapping
    public ResponseEntity<List<ExerciseBankItemResponse>> list(
            @RequestParam(required = false) String skill,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(exerciseBankService.list(skill, includeInactive));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<ExerciseBankItemResponse>> page(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String exerciseType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 8, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(exerciseBankService.page(skill, exerciseType, active, keyword, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats(
            @RequestParam(required = false) String skill
    ) {
        return ResponseEntity.ok(exerciseBankService.stats(skill));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExerciseBankItemResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(exerciseBankService.get(id));
    }

    @PostMapping
    public ResponseEntity<ExerciseBankItemResponse> create(
            @Valid @RequestBody UpsertExerciseBankItemRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(exerciseBankService.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExerciseBankItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertExerciseBankItemRequest request
    ) {
        return ResponseEntity.ok(exerciseBankService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        exerciseBankService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
