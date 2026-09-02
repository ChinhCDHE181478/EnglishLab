package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.GoogleMeetStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;

import fu.sep490.g23.backend.dto.response.curriculum.InstructorLedCourseResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class ClassroomOfferingResponse {
    private Long id;
    private String code;
    private String title;
    private String slug;
    private String shortDescription;
    private String description;
    private ClassroomDeliveryMode deliveryMode;
    private String deliveryModeLabel;
    private ClassroomOfferingStatus classroomStatus;
    private Long instructorLedCourseId;
    private String instructorLedCourseTitle;
    private String instructorLedCourseCode;
    private String instructorLedCourseExamType;
    private String instructorLedCourseStatus;
    // Materials of class
    private InstructorLedCourseResponse instructorLedCourse;
    private String entryLevel;
    private String targetOutcome;
    private Integer capacity;
    private Integer enrolledCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long primaryTeacherId;
    private String primaryTeacherName;
    private Long roomId;
    private String roomName;
    private String offlineAddress;
    private Long googleMeetOwnerId;
    private String googleMeetUrl;
    private GoogleMeetStatus googleMeetStatus;
    private String googleMeetSyncError;
    private String teacherGuide;
    private BigDecimal price;
    private BigDecimal tuitionFeeVnd;
    private BigDecimal salePrice;
    private String targetScore;
    private String duration;
    private ClassroomSessionResponse nextSession;
    private Integer progressPercent;
    private Long enrollmentId;
    private boolean enrolled;
    private boolean registered;
    private boolean hasClassAccess;
    private ClassroomRegistrationStatus registrationStatus;
    private String registrationStatusLabel;
    private BigDecimal tuitionAmountDue;
    private BigDecimal tuitionAmountPaid;
    private BigDecimal tuitionRemaining;
    private Integer waitlistCount;
    private Integer waitlistPosition;
    /** Lịch học tóm tắt, ví dụ "T2, T4 · 18:00–20:00". */
    private String scheduleSummary;
    /** Các thứ trong tuần có buổi học (1 = Thứ 2 ... 7 = Chủ nhật). */
    private List<Integer> scheduleDaysOfWeek;
    /** Giờ bắt đầu phổ biến nhất của các buổi học. */
    private LocalTime typicalStartTime;
    /** Giờ kết thúc phổ biến nhất của các buổi học. */
    private LocalTime typicalEndTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ClassroomSessionResponse> sessions;
    private List<ClassroomEnrollmentResponse> enrollments;
    private List<ClassroomTeacherSummaryResponse> teachers;
}
