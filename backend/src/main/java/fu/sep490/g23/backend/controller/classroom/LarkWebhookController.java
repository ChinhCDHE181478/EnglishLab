package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.service.classroom.LarkWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lark")
@RequiredArgsConstructor
public class LarkWebhookController {

    private final LarkWebhookService webhookService;

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> receiveEvent(@RequestBody Map<String, Object> payload) {
        Object challenge = payload.get("challenge");
        if (challenge != null) {
            webhookService.verifyChallenge(payload);
            return ResponseEntity.ok(Map.of("challenge", challenge));
        }
        webhookService.handle(payload);
        return ResponseEntity.ok(Map.of("code", 0));
    }
}
