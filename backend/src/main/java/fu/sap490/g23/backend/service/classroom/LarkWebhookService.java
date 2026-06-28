package fu.sap490.g23.backend.service.classroom;

import java.util.Map;

public interface LarkWebhookService {

    void handle(Map<String, Object> payload);
}
