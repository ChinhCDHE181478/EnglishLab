package fu.sep490.g23.backend.controller.assessment;

import fu.sep490.g23.backend.dto.request.assessment.AssessmentRubricRequest;
import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.service.assessment.AssessmentRubricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content-manager/rubrics")
@RequiredArgsConstructor
public class AssessmentRubricController {

    private final AssessmentRubricService rubricService;

    @GetMapping
    public ResponseEntity<List<AssessmentRubricResponse>> list(
            @RequestParam(required = false, defaultValue = "false") Boolean includeInactive,
            @RequestParam(required = false) AssessmentSkill skill
    ) {
        return ResponseEntity.ok(rubricService.list(includeInactive, skill));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentRubricResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(rubricService.get(id));
    }

    @PostMapping
    public ResponseEntity<AssessmentRubricResponse> create(@Valid @RequestBody AssessmentRubricRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rubricService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssessmentRubricResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AssessmentRubricRequest request
    ) {
        return ResponseEntity.ok(rubricService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AssessmentRubricResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(rubricService.deactivate(id));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<AssessmentRubricResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(rubricService.reactivate(id));
    }
}
