package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnrollmentDemandReportResponse {
    private Long courseOfferingId;
    private String courseOfferingCode;
    private String courseOfferingTitle;
    private ClassroomDeliveryMode deliveryMode;
    private Integer classCapacity;
    private Long totalRegistrations;
    private Long awaitingContact;
    private Long invitationsSent;
    private Long testsScheduled;
    private Long qualifiedForClass;
    private Long assigned;
    private Long rejected;
    /** Số lớp SẮP KHAI GIẢNG (UPCOMING) — không gồm đang học / đã kết thúc. */
    private Integer existingOpenClassCount;
    /** Chỗ trống còn lại trên các lớp sắp khai giảng. */
    private Long openSeatRemaining;
    private Integer suggestedClassCount;
}
