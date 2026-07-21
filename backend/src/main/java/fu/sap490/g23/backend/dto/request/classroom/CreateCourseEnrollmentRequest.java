package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCourseEnrollmentRequest {
    /** Giữ để tương thích dữ liệu cũ; form tư vấn chung không bắt buộc chọn chương trình. */
    private Long courseOfferingId;

    /** Giữ để tương thích liên kết cũ từ trang chi tiết lớp. */
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

    @Size(max = 120)
    private String desiredClassCode;

    @NotBlank
    @Size(max = 80)
    private String consultationTrack;

    @Size(max = 500)
    private String studyWorkGoal;

    @Size(max = 500)
    private String preferredSchedule;

    @Size(max = 255)
    private String campusPreference;

    @Size(max = 700)
    private String note;
}
