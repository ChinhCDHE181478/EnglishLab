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
public class CreateAnnouncementRequest {

    @NotBlank(message = "Tiêu đề thông báo không được để trống")
    @Size(max = 220)
    private String title;

    @NotBlank(message = "Nội dung thông báo không được để trống")
    private String content;
}
