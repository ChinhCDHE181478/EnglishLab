package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLarkLinkRequest {

    @NotBlank(message = "Link Lark không được để trống")
    @Size(max = 700)
    private String larkMeetingUrl;
}
