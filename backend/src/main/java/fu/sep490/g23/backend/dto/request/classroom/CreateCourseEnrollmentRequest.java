package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCourseEnrollmentRequest {
    /** Khóa học học viên quan tâm; lớp cụ thể chỉ được chọn sau khi test và tư vấn. */
    @NotNull(message = "Vui lòng chọn khóa học bạn quan tâm")
    private Long courseOfferingId;

    /** Giữ để đọc dữ liệu cũ; form mới không đăng ký trực tiếp vào từng lớp. */
    private Long classroomId;

    private Long placementAttemptId;

    @NotBlank
    @Size(max = 100)
    private String contactName;

    @NotBlank
    @Email
    @Size(max = 150)
    private String contactEmail;

    @NotBlank
    @Size(max = 30)
    private String contactPhone;

    @Size(max = 500)
    private String facebookUrl;


    @NotBlank
    @Size(max = 80)
    private String consultationTrack;

    @Size(max = 500)
    private String studyWorkGoal;

    @Size(max = 500)
    private String preferredSchedule;


    @Size(max = 700)
    private String note;
}
