package fu.sep490.g23.backend.service.mail.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.service.mail.EmailTemplateUtil;
import fu.sep490.g23.backend.service.mail.EnrollmentRequestMailService;
import fu.sep490.g23.backend.service.notification.NotificationPreferenceService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentRequestMailServiceImpl implements EnrollmentRequestMailService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final NotificationPreferenceService notificationPreferenceService;

    @Value("${englishlab.mail.enabled:true}")
    private boolean enabled;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${englishlab.mail.from:}")
    private String fromAddress;

    @Value("${englishlab.mail.from-name:EnglishLab}")
    private String fromName;

    @Value("${englishlab.mail.support-email:support@englishlab.vn}")
    private String supportEmail;

    @Value("${englishlab.mail.base-url:http://localhost:5173}")
    private String baseUrl;

    @Override
    public void sendTestAppointment(CourseRegistrationRequest request) {
        String appointment = request.getTestAppointmentAt() == null
                ? "Chưa xác định"
                : request.getTestAppointmentAt().format(DATE_TIME_FORMAT);
        String location = valueOrDefault(request.getTestLocation(), "Trung tâm EnglishLab");

        String highlightContent = """
                <p style="margin:0 0 4px;font-size:12px;color:#7a5c59;font-weight:700;">THỜI GIAN TƯ VẤN & TEST</p>
                <p style="margin:0 0 12px;font-size:16px;font-weight:700;color:#730014;">%s</p>
                <p style="margin:0 0 4px;font-size:12px;color:#7a5c59;font-weight:700;">ĐỊA ĐIỂM</p>
                <p style="margin:0;font-size:14px;font-weight:700;color:#2b1f1f;">%s</p>
                """.formatted(EmailTemplateUtil.escapeHtml(appointment), EmailTemplateUtil.escapeHtml(location));

        String html = EmailTemplateUtil.buildBrandedEmailHtml(
                name(request),
                "Xác nhận lịch hẹn tư vấn & test",
                "Cảm ơn bạn đã đăng ký tư vấn tại EnglishLab. Dưới đây là thông tin chi tiết buổi làm việc và kiểm tra trình độ đầu vào của bạn.",
                highlightContent,
                normalizedBaseUrl() + "/placement-tests",
                "Xem chi tiết lịch hẹn",
                supportEmail,
                "Vui lòng đến trước giờ hẹn 10 phút và mang theo giấy tờ tùy thân. Nếu cần thay đổi lịch hẹn, xin liên hệ hotline/email của EnglishLab."
        );

        send(request, "Xác nhận lịch tư vấn và kiểm tra đầu vào - EnglishLab", html);
    }

    @Override
    public void sendClassAssignment(CourseRegistrationRequest request, ClassSection classroom) {
        String startDate = classroom.getStartDate() == null
                ? "Đang cập nhật"
                : classroom.getStartDate().format(DATE_FORMAT);
        String teacher = classroom.getPrimaryTeacher() == null
                ? "Đang phân công"
                : classroom.getPrimaryTeacher().getFullName();
        String location = classroom.getDeliveryMode() == null
                ? "Đang cập nhật"
                : classroom.getDeliveryMode().name().equals("VIRTUAL")
                    ? "Học trực tuyến (Virtual)"
                    : valueOrDefault(classroom.getRoom() == null ? null : classroom.getRoom().getLocationAddress(),
                    "Học tại trung tâm EnglishLab");
        String classTitle = classroom.getInstructorLedCourse() != null
                ? classroom.getInstructorLedCourse().getTitle()
                : (classroom.getInstructorLedCourse() != null ? classroom.getInstructorLedCourse().getTitle() : "Lớp EnglishLab");

        String highlightContent = """
                <p style="margin:0 0 4px;font-size:12px;color:#7a5c59;font-weight:700;">LỚP HỌC</p>
                <p style="margin:0 0 12px;font-size:16px;font-weight:700;color:#730014;">%s</p>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                  <tr>
                    <td style="padding:4px 0;font-size:13px;color:#7a5c59;font-weight:700;">Khai giảng:</td>
                    <td style="padding:4px 0;font-size:13px;color:#2b1f1f;font-weight:700;text-align:right;">%s</td>
                  </tr>
                  <tr>
                    <td style="padding:4px 0;font-size:13px;color:#7a5c59;font-weight:700;">Giáo viên:</td>
                    <td style="padding:4px 0;font-size:13px;color:#2b1f1f;font-weight:700;text-align:right;">%s</td>
                  </tr>
                  <tr>
                    <td style="padding:4px 0;font-size:13px;color:#7a5c59;font-weight:700;">Hình thức:</td>
                    <td style="padding:4px 0;font-size:13px;color:#2b1f1f;font-weight:700;text-align:right;">%s</td>
                  </tr>
                </table>
                """.formatted(
                EmailTemplateUtil.escapeHtml(classTitle),
                EmailTemplateUtil.escapeHtml(startDate),
                EmailTemplateUtil.escapeHtml(teacher),
                EmailTemplateUtil.escapeHtml(location)
        );

        String html = EmailTemplateUtil.buildBrandedEmailHtml(
                name(request),
                "Thông báo xếp lớp thành công!",
                "EnglishLab xin chúc mừng bạn đã hoàn tất đăng ký và được xếp vào lớp học chính thức.",
                highlightContent,
                normalizedBaseUrl() + "/my-classrooms/" + classroom.getId(),
                "Vào lớp học ngay",
                supportEmail,
                "Truy cập EnglishLab để xem thời khóa biểu chi tiết và tài liệu học tập của lớp."
        );

        send(request, "Bạn đã được xếp lớp thành công - EnglishLab", html);
    }

    private void send(CourseRegistrationRequest request, String subject, String htmlContent) {
        User learner = request == null ? null : request.getLearner();
        if (learner != null && !notificationPreferenceService.isEmailEnabled(learner)) return;
        String recipient = request == null ? null : valueOrDefault(request.getContactEmail(), learner == null ? null : learner.getEmail());
        if (!enabled || blank(mailHost) || blank(fromAddress) || blank(recipient)) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Sent enrollment request email '{}' to {}", subject, recipient);
        } catch (Exception exception) {
            log.error("Không thể gửi email '{}' tới {}", subject, recipient, exception);
        }
    }

    private String name(CourseRegistrationRequest request) {
        String fallback = request.getLearner() == null ? "bạn" : request.getLearner().getFullName();
        return valueOrDefault(request.getContactName(), valueOrDefault(fallback, "bạn"));
    }

    private String normalizedBaseUrl() {
        String value = valueOrDefault(baseUrl, "http://localhost:5173");
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
