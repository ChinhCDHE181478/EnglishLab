package fu.sep490.g23.backend.service.mail.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
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
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentRequestMailServiceImpl implements EnrollmentRequestMailService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");
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
    public void sendTestAppointment(
            CourseRegistrationRequest request,
            LocalDateTime appointmentAt,
            String location
    ) {
        String appointment = appointmentAt == null
                ? "Đang cập nhật"
                : appointmentAt.format(DATE_TIME_FORMAT);
        String appointmentLocation = valueOrDefault(location, "Tại trung tâm EnglishLab");

        String highlightContent = """
                <p style="margin:0 0 4px;font-size:12px;color:#7a5c59;font-weight:700;">THỜI GIAN TƯ VẤN & TEST</p>
                <p style="margin:0 0 12px;font-size:16px;font-weight:700;color:#730014;">%s</p>
                <p style="margin:0 0 4px;font-size:12px;color:#7a5c59;font-weight:700;">ĐỊA ĐIỂM</p>
                <p style="margin:0;font-size:14px;font-weight:700;color:#2b1f1f;">%s</p>
                """.formatted(
                EmailTemplateUtil.escapeHtml(appointment),
                EmailTemplateUtil.escapeHtml(appointmentLocation)
        );

        String html = EmailTemplateUtil.buildBrandedEmailHtml(
                name(request),
                "Xác nhận lịch hẹn tư vấn & test",
                "Cảm ơn bạn đã đăng ký tư vấn tại EnglishLab. Dưới đây là thông tin chi tiết buổi làm việc và kiểm tra trình độ đầu vào của bạn.",
                highlightContent,
                null,
                null,
                supportEmail,
                "Vui lòng đến trước giờ hẹn 10 phút và mang theo giấy tờ tùy thân. Nếu cần thay đổi lịch hẹn, xin liên hệ hotline/email của EnglishLab."
        );

        send(request, "Xác nhận lịch tư vấn và kiểm tra đầu vào - EnglishLab", html);
    }

    @Override
    public void sendTestResult(
            CourseRegistrationRequest request,
            boolean eligible,
            String evaluatedCourseTitle
    ) {
        boolean hasRecommendation = request.getStatus() == EnrollmentRequestStatus.CLASS_PROPOSED
                && request.getCourseOffering() != null;
        String result = eligible
                ? "Phù hợp với khóa học đã đăng ký"
                : hasRecommendation ? "Đề xuất khóa học phù hợp hơn" : "Chưa phù hợp với khóa học đã đăng ký";
        String recommendation = hasRecommendation
                ? """
                  <p style="margin:12px 0 4px;font-size:12px;color:#7a5c59;font-weight:700;">KHÓA HỌC ĐỀ XUẤT</p>
                  <p style="margin:0;font-size:16px;font-weight:700;color:#730014;">%s</p>
                  """.formatted(EmailTemplateUtil.escapeHtml(request.getCourseOffering().getTitle()))
                : "";
        String highlightContent = """
                <p style="margin:0 0 4px;font-size:12px;color:#7a5c59;font-weight:700;">KẾT QUẢ ĐÁNH GIÁ</p>
                <p style="margin:0 0 12px;font-size:16px;font-weight:700;color:#730014;">%s</p>
                <p style="margin:0 0 4px;font-size:12px;color:#7a5c59;font-weight:700;">KHÓA HỌC ĐÃ ĐÁNH GIÁ</p>
                <p style="margin:0;font-size:14px;font-weight:700;color:#2b1f1f;">%s</p>
                %s
                """.formatted(
                EmailTemplateUtil.escapeHtml(result),
                EmailTemplateUtil.escapeHtml(valueOrDefault(evaluatedCourseTitle, "Khóa học đã đăng ký")),
                recommendation
        );
        String description = eligible
                ? "EnglishLab đã ghi nhận kết quả đánh giá đầu vào của bạn. Hồ sơ đang được chuyển sang bước xếp lớp phù hợp."
                : hasRecommendation
                    ? "Khóa học ban đầu chưa phù hợp với kết quả đánh giá hiện tại. Vui lòng đăng nhập để xác nhận khóa học được trung tâm đề xuất."
                    : "EnglishLab đã ghi nhận kết quả đánh giá đầu vào. Hiện chưa có khóa học phù hợp để tiếp tục hồ sơ này.";

        String html = EmailTemplateUtil.buildBrandedEmailHtml(
                name(request),
                "Kết quả đánh giá đầu vào",
                description,
                highlightContent,
                normalizedBaseUrl() + "/my-enrollment-requests",
                hasRecommendation ? "Xác nhận khóa học đề xuất" : "Xem kết quả hồ sơ",
                supportEmail,
                "Nếu cần trao đổi thêm, vui lòng liên hệ EnglishLab để được tư vấn."
        );

        send(request, "Kết quả đánh giá đầu vào - EnglishLab", html);
    }

    @Override
    public void sendClassAssignment(
            CourseRegistrationRequest request,
            ClassSection classroom,
            ClassroomEnrollmentResponse enrollment
    ) {
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
        boolean fullPaymentRequired = enrollment != null && enrollment.isTuitionFullPaymentRequired();
        BigDecimal tuitionDue = enrollment == null ? BigDecimal.ZERO : valueOrZero(enrollment.getTuitionAmountDue());
        BigDecimal paymentAmount = fullPaymentRequired
                ? tuitionDue.subtract(valueOrZero(enrollment.getTuitionAmountPaid())).max(BigDecimal.ZERO)
                : enrollment == null ? BigDecimal.ZERO : valueOrZero(enrollment.getTuitionDepositRemaining());
        String paymentTitle = fullPaymentRequired ? "THANH TOÁN TOÀN BỘ" : "ĐẶT CỌC 30%";
        String paymentDeadline = enrollment == null || enrollment.getTuitionPaymentDeadline() == null
                ? "Đang cập nhật"
                : enrollment.getTuitionPaymentDeadline().format(DATE_TIME_FORMAT);

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
                <div style="margin-top:16px;padding-top:16px;border-top:1px solid #dfbfbd;">
                  <p style="margin:0 0 4px;font-size:12px;color:#7a5c59;font-weight:700;">%s</p>
                  <p style="margin:0;font-size:20px;font-weight:700;color:#730014;">%s</p>
                  <p style="margin:6px 0 0;font-size:13px;color:#5f4745;">Tổng học phí: %s · Hạn thanh toán: %s</p>
                </div>
                <div style="margin-top:16px;">
                  <p style="margin:0 0 8px;font-size:12px;color:#7a5c59;font-weight:700;">HƯỚNG DẪN THANH TOÁN</p>
                  <p style="margin:0 0 6px;font-size:13px;line-height:20px;color:#2b1f1f;">1. Bấm nút <strong>Thanh toán học phí</strong> bên dưới và đăng nhập EnglishLab.</p>
                  <p style="margin:0 0 6px;font-size:13px;line-height:20px;color:#2b1f1f;">2. Mở hồ sơ lớp đã chọn, chọn <strong>%s</strong> rồi thanh toán qua PayOS.</p>
                  <p style="margin:0;font-size:13px;line-height:20px;color:#2b1f1f;">3. Nếu trung tâm đã cung cấp thông tin chuyển khoản riêng, bạn có thể tải minh chứng ngay tại cùng hồ sơ.</p>
                </div>
                """.formatted(
                EmailTemplateUtil.escapeHtml(classTitle),
                EmailTemplateUtil.escapeHtml(startDate),
                EmailTemplateUtil.escapeHtml(teacher),
                EmailTemplateUtil.escapeHtml(location),
                EmailTemplateUtil.escapeHtml(paymentTitle),
                EmailTemplateUtil.escapeHtml(formatMoney(paymentAmount)),
                EmailTemplateUtil.escapeHtml(formatMoney(tuitionDue)),
                EmailTemplateUtil.escapeHtml(paymentDeadline),
                EmailTemplateUtil.escapeHtml(fullPaymentRequired ? "Thanh toán toàn bộ" : "Đặt cọc 30%")
        );

        String html = EmailTemplateUtil.buildBrandedEmailHtml(
                name(request),
                "Lớp học của bạn đã được xác nhận",
                "EnglishLab đã giữ chỗ cho bạn trong lớp dưới đây. Vui lòng hoàn tất học phí để được cấp quyền vào học.",
                highlightContent,
                normalizedBaseUrl() + "/my-enrollment-requests",
                "Thanh toán học phí",
                supportEmail,
                fullPaymentRequired
                        ? "Vui lòng hoàn tất toàn bộ học phí trong thời hạn trên để giữ chỗ."
                        : "Khoản đặt cọc bằng 30% học phí. Phần còn lại cần hoàn tất trước hạn thanh toán."
        );

        send(request, "Hoàn tất học phí lớp học - EnglishLab", html);
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

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatMoney(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(valueOrZero(value)) + " đ";
    }

    private String valueOrDefault(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
