package fu.sap490.g23.backend.controller.assessment;

import fu.sap490.g23.backend.dto.request.assessment.UpsertExerciseBankItemRequest;
import fu.sap490.g23.backend.dto.response.assessment.ExerciseBankItemResponse;
import fu.sap490.g23.backend.service.assessment.ExerciseBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
