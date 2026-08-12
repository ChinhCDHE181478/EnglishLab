package fu.sep490.g23.backend.service.classroom;

import java.util.Map;

public interface LarkWebhookService {

    void verifyChallenge(Map<String, Object> payload);

    void handle(Map<String, Object> payload);
}
