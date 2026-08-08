package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.response.course.DictionaryEntryResponse;
import fu.sap490.g23.backend.service.course.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
public class DictionaryLookupController {

    private final DictionaryService dictionaryService;

    @GetMapping("/lookup")
    public ResponseEntity<DictionaryEntryResponse> lookup(@RequestParam String word) {
        return ResponseEntity.ok(dictionaryService.lookup(word));
    }
}
