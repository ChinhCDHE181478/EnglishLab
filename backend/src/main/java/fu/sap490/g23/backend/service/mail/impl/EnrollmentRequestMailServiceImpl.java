package fu.sap490.g23.backend.service.mail.impl;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.EnrollmentRequest;
import fu.sap490.g23.backend.service.mail.EnrollmentRequestMailService;
import fu.sap490.g23.backend.service.notification.NotificationPreferenceService;
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

    @Value("${englishlab.mail.base-url:http://localhost:5173}")
    private String baseUrl;

    @Override
    public void sendTestAppointment(EnrollmentRequest request) {
        String appointment = request.getTestAppointmentAt() == null
                ? "Chưa xác định"
                : request.getTestAppointmentAt().format(DATE_TIME_FORMAT);
        send(
                request,
                "Xác nhận lịch tư vấn và kiểm tra đầu vào - EnglishLab",
                """
                <h2 style="color:#730014">Lịch tư vấn và kiểm tra đầu vào của bạn</h2>
                <p>Xin chào %s,</p>
                <p><strong>Ngày giờ:</strong> %s</p>
                <p><strong>Địa điểm:</strong> %s</p>
                <p>Vui lòng đến trước giờ hẹn 10 phút và mang theo giấy tờ tùy thân. Nếu cần đổi lịch, hãy liên hệ EnglishLab sớm nhất có thể.</p>
                """.formatted(name(request), escape(appointment), escape(request.getTestLocation()))
        );
    }

    @Override
    public void sendClassAssignment(EnrollmentRequest request, ClassroomOffering classroom) {
        String startDate = classroom.getStartDate() == null
                ? "Đang cập nhật"
                : classroom.getStartDate().format(DATE_FORMAT);
        String teacher = classroom.getPrimaryTeacher() == null
                ? "Đang cập nhật"
                : classroom.getPrimaryTeacher().getFullName();
        String location = classroom.getDeliveryMode() == null
                ? "Đang cập nhật"
                : classroom.getDeliveryMode().name().equals("VIRTUAL")
                    ? "Học trực tuyến"
                    : valueOrDefault(classroom.getOfflineAddress(), "EnglishLab");
        String classTitle = classroom.getLearningPackage() == null
                ? "Lớp EnglishLab"
                : classroom.getLearningPackage().getTitle();
        send(
                request,
                "Bạn đã được xếp lớp - EnglishLab",
                """
                <h2 style="color:#730014">Bạn đã được xếp lớp thành công</h2>
                <p>Xin chào %s,</p>
                <p>Bạn đã được xếp vào lớp <strong>%s</strong>.</p>
                <ul>
                  <li><strong>Khai giảng:</strong> %s</li>
                  <li><strong>Giáo viên:</strong> %s</li>
                  <li><strong>Địa điểm/Hình thức:</strong> %s</li>
                </ul>
                <p><a href="%s/my-classrooms/%d" style="display:inline-block;background:#730014;color:#fff;padding:12px 18px;border-radius:8px;text-decoration:none">Xem lớp học</a></p>
                """.formatted(
                        name(request),
                        escape(classTitle),
                        escape(startDate),
                        escape(teacher),
                        escape(location),
                        normalizedBaseUrl(),
                        classroom.getId()
                )
        );
    }

    private void send(EnrollmentRequest request, String subject, String content) {
        User learner = request == null ? null : request.getLearner();
        if (learner != null && !notificationPreferenceService.isEmailEnabled(learner)) return;
        String recipient = request == null ? null : valueOrDefault(request.getContactEmail(), learner == null ? null : learner.getEmail());
        if (!enabled || blank(mailHost) || blank(fromAddress) || blank(recipient)) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText("""
                    <html><body style="font-family:Arial,sans-serif;color:#2b2828;line-height:1.6">
                    %s
                    <p style="margin-top:24px;color:#6b7280">Trân trọng,<br/>EnglishLab</p>
                    </body></html>
                    """.formatted(content), true);
            mailSender.send(message);
            log.info("Sent enrollment request email '{}' to {}", subject, recipient);
        } catch (Exception exception) {
            log.error("Không thể gửi email '{}' tới {}", subject, recipient, exception);
        }
    }

    private String name(EnrollmentRequest request) {
        String fallback = request.getLearner() == null ? "bạn" : request.getLearner().getFullName();
        return escape(valueOrDefault(request.getContactName(), valueOrDefault(fallback, "bạn")));
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

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
