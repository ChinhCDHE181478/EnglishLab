package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.HomeworkAnnotationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeworkTextAnnotationRequest {

    @NotBlank(message = "Mã ghi chú không được để trống")
    @Size(max = 64, message = "Mã ghi chú không được vượt quá 64 ký tự")
    private String id;

    @NotNull(message = "Loại ghi chú không được để trống")
    private HomeworkAnnotationType type;

    @NotNull(message = "Vị trí bắt đầu không được để trống")
    @Min(value = 0, message = "Vị trí bắt đầu không hợp lệ")
    private Integer startOffset;

    @NotNull(message = "Vị trí kết thúc không được để trống")
    @Min(value = 1, message = "Vị trí kết thúc không hợp lệ")
    private Integer endOffset;

    @NotBlank(message = "Đoạn được chọn không được để trống")
    @Size(max = 2000, message = "Đoạn được chọn không được vượt quá 2.000 ký tự")
    private String selectedText;

    @Size(max = 2000, message = "Nội dung sửa không được vượt quá 2.000 ký tự")
    private String replacementText;

    @Size(max = 2000, message = "Ghi chú không được vượt quá 2.000 ký tự")
    private String note;
}
