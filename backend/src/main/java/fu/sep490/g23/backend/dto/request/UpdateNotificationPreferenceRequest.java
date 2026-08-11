package fu.sep490.g23.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateNotificationPreferenceRequest {

    @NotNull(message = "Vui lòng chọn tùy chọn thông báo email.")
    private Boolean emailEnabled;

    @NotNull(message = "Vui lòng chọn tùy chọn thông báo trong ứng dụng.")
    private Boolean inAppEnabled;

    private Boolean classReminderEnabled;

    private Boolean studyAlertEnabled;
}
