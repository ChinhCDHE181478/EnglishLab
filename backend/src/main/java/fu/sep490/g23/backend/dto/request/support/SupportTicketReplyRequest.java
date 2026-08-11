package fu.sep490.g23.backend.dto.request.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportTicketReplyRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống.")
    @Size(max = 5000, message = "Nội dung phản hồi không được vượt quá 5000 ký tự.")
    private String message;
}
