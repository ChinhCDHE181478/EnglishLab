package fu.sep490.g23.backend.dto.request.curriculum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseLessonRequest {

    @NotNull(message = "Thứ tự bài học không được để trống.")
    @Min(value = 1, message = "Thứ tự bài học phải bắt đầu từ 1.")
    private Integer sessionNumber;

    @Min(value = 0, message = "Thứ tự hiển thị không được âm.")
    private Integer displayOrder;

    @Min(value = 1, message = "Số buổi dự kiến phải bắt đầu từ 1.")
    private Integer plannedSessionCount = 1;

    @NotBlank(message = "Tiêu đề bài học không được để trống.")
    @Size(max = 220, message = "Tiêu đề bài học không được vượt quá 220 ký tự.")
    private String title;

    @Size(max = 4000, message = "Mô tả bài học không được vượt quá 4.000 ký tự.")
    private String description;

    @Size(max = 4000, message = "Mục tiêu học tập không được vượt quá 4.000 ký tự.")
    private String learningObjectives;
}
