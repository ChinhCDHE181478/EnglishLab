package fu.sep490.g23.backend.dto.response.course;

import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PackageEnrollmentAdminResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long packageId;
    private String packageTitle;
    private String packageSlug;
    private EnrollmentStatus status;
    private Integer progressPercent;
    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;
}
