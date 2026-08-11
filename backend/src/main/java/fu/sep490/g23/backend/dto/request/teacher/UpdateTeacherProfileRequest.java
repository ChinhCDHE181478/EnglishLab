package fu.sep490.g23.backend.dto.request.teacher;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTeacherProfileRequest {

    @Size(max = 180, message = "Tiêu đề hồ sơ không được vượt quá 180 ký tự.")
    private String headline;

    @Size(max = 5000, message = "Giới thiệu không được vượt quá 5.000 ký tự.")
    private String biography;

    @Size(max = 700, message = "Chuyên môn không được vượt quá 700 ký tự.")
    private String specializations;

    @Size(max = 300, message = "Ngôn ngữ giảng dạy không được vượt quá 300 ký tự.")
    private String teachingLanguages;

    @Min(value = 0, message = "Số năm kinh nghiệm không được âm.")
    @Max(value = 60, message = "Số năm kinh nghiệm không hợp lệ.")
    private Integer yearsOfExperience;

    @Size(max = 250, message = "Học vị cao nhất không được vượt quá 250 ký tự.")
    private String highestQualification;

    private boolean publicProfile;
}
