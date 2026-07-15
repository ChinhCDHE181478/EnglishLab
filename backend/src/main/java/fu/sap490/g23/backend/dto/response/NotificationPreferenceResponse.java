package fu.sap490.g23.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private boolean emailEnabled;
    private boolean inAppEnabled;
}
