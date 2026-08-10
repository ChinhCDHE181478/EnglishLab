package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CenterMaterialLibraryUpsertRequest {

    @NotBlank(message = "Tên học liệu không được để trống.")
    @Size(max = 220, message = "Tên học liệu không được vượt quá 220 ký tự.")
    private String title;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự.")
    private String description;

    @NotBlank(message = "Cần cung cấp tệp hoặc liên kết học liệu.")
    @Size(max = 700, message = "Liên kết học liệu không được vượt quá 700 ký tự.")
    private String fileUrl;

    @Size(max = 80, message = "Định dạng tệp không được vượt quá 80 ký tự.")
    private String fileType;

    @Size(max = 80, message = "Loại học liệu không được vượt quá 80 ký tự.")
    private String materialType;

    @Size(max = 120, message = "Nguồn cung cấp không được vượt quá 120 ký tự.")
    private String provider;

    @Size(max = 40, message = "Nhóm kỳ thi không được vượt quá 40 ký tự.")
    private String examCategory;

    @Min(value = 0, message = "Điểm TOEIC tối thiểu phải từ 0 trở lên.")
    @Max(value = 990, message = "Điểm TOEIC tối thiểu không được vượt quá 990.")
    private Integer toeicScoreMin;

    @Min(value = 0, message = "Điểm TOEIC tối thiểu phải từ 0 trở lên.")
    @Max(value = 990, message = "Điểm TOEIC tối đa không được vượt quá 990.")
    private Integer toeicScoreMax;

    @DecimalMin(value = "0.0", message = "Band IELTS tối thiểu phải từ 0 trở lên.")
    @DecimalMax(value = "9.0", message = "Band IELTS tối thiểu không được vượt quá 9.")
    private BigDecimal ieltsBandMin;

    @DecimalMin(value = "0.0", message = "Band IELTS tối thiểu phải từ 0 trở lên.")
    @DecimalMax(value = "9.0", message = "Band IELTS tối đa không được vượt quá 9.")
    private BigDecimal ieltsBandMax;

    @Size(max = 80, message = "Kỹ năng không được vượt quá 80 ký tự.")
    private String skill;

    @Size(max = 500, message = "Nhãn gợi ý không được vượt quá 500 ký tự.")
    private String tags;

    @Size(max = 40, message = "Trạng thái không được vượt quá 40 ký tự.")
    private String status;
}
