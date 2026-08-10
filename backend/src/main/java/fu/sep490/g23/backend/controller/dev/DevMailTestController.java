package fu.sap490.g23.backend.controller.dev;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.EnrollmentRequest;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.service.mail.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@Profile("dev")
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevMailTestController {

    private final AuthMailService authMailService;
    private final ClassroomHomeworkMailService classroomHomeworkMailService;
    private final EnrollmentRequestMailService enrollmentRequestMailService;
    private final LearningReminderMailService learningReminderMailService;
    private final CourseEnrollmentMailService courseEnrollmentMailService;

    @GetMapping("/send-all-test-emails")
    public ResponseEntity<Map<String, Object>> sendAllTestEmails(
            @RequestParam(defaultValue = "0386852628z@gmail.com") String email
    ) {
        log.info("Starting batch send of all email templates to {}", email);

        User testUser = User.builder()
                .id(999L)
                .email(email)
                .fullName("Phạm Minh Đức")
                .build();

        User teacherUser = User.builder()
                .id(888L)
                .email("teacher@englishlab.vn")
                .fullName("ThS. Nguyễn Văn Anh")
                .build();

        int sentCount = 0;

        // 1. Verification Code Email (OTP Email)
        try {
            authMailService.sendVerificationEmail(testUser, "849201");
            sentCount++;
        } catch (Exception e) {
            log.error("Failed sending email 1 (verification)", e);
        }

        // 2. Password Reset Code Email
        try {
            authMailService.sendPasswordResetEmail(testUser, "639148");
            sentCount++;
        } catch (Exception e) {
            log.error("Failed sending email 2 (password reset)", e);
        }

        // 3. Staff Created Account Setup Email
        try {
            authMailService.sendStaffCreatedAccountEmail(testUser, "510294");
            sentCount++;
        } catch (Exception e) {
            log.error("Failed sending email 3 (staff created account)", e);
        }

        // 4. Homework Assigned Email
        try {
            ClassroomHomework homework = ClassroomHomework.builder()
                    .id(101L)
                    .title("Luyện tập IELTS Writing Task 2 - PEEL Method")
                    .instruction("Viết bài essay 250 từ phân tích dạng đề Opinion Essay về chủ đề Education. Sử dụng 4 đoạn văn và kỹ thuật PEEL.")
                    .deadline(LocalDateTime.now().plusDays(3).withHour(23).withMinute(59))
                    .build();
            classroomHomeworkMailService.sendHomeworkAssigned(testUser, homework);
            sentCount++;
        } catch (Exception e) {
            log.error("Failed sending email 4 (homework)", e);
        }

        // 5. Test & Placement Appointment Email
        try {
            EnrollmentRequest testRequest = EnrollmentRequest.builder()
                    .id(202L)
                    .contactName("Phạm Minh Đức")
                    .contactEmail(email)
                    .testAppointmentAt(LocalDateTime.now().plusDays(2).withHour(14).withMinute(30))
                    .testLocation("EnglishLab Cơ sở 1 - 123 Nguyễn Trãi, Thanh Xuân, Hà Nội")
                    .learner(testUser)
                    .build();
            enrollmentRequestMailService.sendTestAppointment(testRequest);
            sentCount++;
        } catch (Exception e) {
            log.error("Failed sending email 5 (test appointment)", e);
        }

        // 6. Class Assignment Email
        try {
            LearningPackage pkg = LearningPackage.builder()
                    .id(301L)
                    .title("IELTS Intensive Master 6.5+")
                    .build();

            ClassroomOffering offering = ClassroomOffering.builder()
                    .id(303L)
                    .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                    .offlineAddress("Phòng A201 - EnglishLab Cơ sở 1")
                    .startDate(LocalDate.now().plusDays(7))
                    .primaryTeacher(teacherUser)
                    .learningPackage(pkg)
                    .build();

            EnrollmentRequest classRequest = EnrollmentRequest.builder()
                    .id(203L)
                    .contactName("Phạm Minh Đức")
                    .contactEmail(email)
                    .learner(testUser)
                    .build();

            enrollmentRequestMailService.sendClassAssignment(classRequest, offering);
            sentCount++;
        } catch (Exception e) {
            log.error("Failed sending email 6 (class assignment)", e);
        }

        // 7. Learning Reminder / Feedback Email
        try {
            learningReminderMailService.sendReminder(
                    testUser,
                    "Đánh giá chất lượng giảng dạy - EnglishLab",
                    "Đánh giá buổi học IELTS Speaking Live",
                    "Bạn vừa hoàn thành buổi học IELTS Speaking với giảng viên ThS. Nguyễn Văn Anh. Hãy dành 1 phút thực hiện đánh giá để giúp EnglishLab nâng cao chất lượng bài giảng.",
                    "/my-classrooms"
            );
            sentCount++;
        } catch (Exception e) {
            log.error("Failed sending email 7 (reminder)", e);
        }

        // 8. Course Enrollment Success Email
        try {
            LearningPackage coursePkg = LearningPackage.builder()
                    .id(404L)
                    .title("Bứt phá IELTS Speaking & Writing 7.0+")
                    .slug("but-pha-ielts-70")
                    .duration("12 tuần")
                    .shortDescription("Khóa học bứt phá kỹ năng IELTS Speaking & Writing từ mốc 5.5 lên 7.0+")
                    .build();

            OnlineCourse onlineCourse = OnlineCourse.builder()
                    .id(4040L)
                    .learningPackage(coursePkg)
                    .totalHours(48)
                    .targetOutcome("Đạt mốc 7.0+ Speaking & Writing theo chuẩn Cambridge IELTS")
                    .build();

            PackageEnrollment packageEnrollment = PackageEnrollment.builder()
                    .id(505L)
                    .registeredAt(LocalDateTime.now())
                    .build();

            courseEnrollmentMailService.sendEnrollmentSuccessEmail(testUser, onlineCourse, packageEnrollment);
            sentCount++;
        } catch (Exception e) {
            log.error("Failed sending email 8 (course enrollment)", e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("recipient", email);
        result.put("sentEmailCount", sentCount);
        result.put("message", "Đã gửi toàn bộ " + sentCount + " email mẫu tới " + email);

        return ResponseEntity.ok(result);
    }
}
