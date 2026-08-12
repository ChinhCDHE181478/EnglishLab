package fu.sep490.g23.backend.service.mail.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.service.notification.NotificationPreferenceService;
import fu.sep490.g23.backend.service.mail.ClassroomHomeworkMailService;
import fu.sep490.g23.backend.service.mail.EmailTemplateUtil;
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
public class ClassroomHomeworkMailServiceImpl implements ClassroomHomeworkMailService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final JavaMailSender mailSender;
    private final NotificationPreferenceService notificationPreferenceService;

    @Value("${englishlab.mail.enabled:true}") private boolean enabled;
    @Value("${spring.mail.host:}") private String mailHost;
    @Value("${englishlab.mail.from:}") private String fromAddress;
    @Value("${englishlab.mail.from-name:EnglishLab}") private String fromName;
    @Value("${englishlab.mail.support-email:support@englishlab.vn}") private String supportEmail;
    @Value("${englishlab.mail.base-url:http://localhost:5173}") private String baseUrl;

    public void sendHomeworkAssigned(User student, ClassroomHomework homework) {
        if (student != null && !notificationPreferenceService.isEmailEnabled(student)) return;
        if (!enabled || blank(mailHost) || blank(fromAddress) || student == null || blank(student.getEmail())) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(student.getEmail());
            helper.setSubject("Bạn có bài tập mới - EnglishLab");
            helper.setText(render(student, homework), true);
            mailSender.send(message);
            log.info("Sent homework assigned email to {}", student.getEmail());
        } catch (Exception exception) {
            log.error("Không thể gửi email bài tập {} tới {}", homework.getId(), student.getEmail(), exception);
        }
    }

    private String render(User student, ClassroomHomework homework) {
        String deadline = homework.getDeadline() == null ? "Giảng viên chưa đặt hạn nộp" : homework.getDeadline().format(DATE_FORMAT);
        String instruction = homework.getInstruction() == null ? "" : homework.getInstruction().trim();

        String highlightContent = """
                <p style="margin:0 0 6px;font-size:13px;color:#7a5c59;font-weight:700;">TÊN BÀI TẬP</p>
                <p style="margin:0 0 12px;font-size:17px;font-weight:700;color:#4b0009;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;">%s</p>
                <p style="margin:0 0 4px;font-size:12px;color:#7a5c59;font-weight:700;">HẠN NỘP BÀI</p>
                <p style="margin:0 0 12px;font-size:14px;font-weight:700;color:#8a0018;">%s</p>
                %s
                """.formatted(
                EmailTemplateUtil.escapeHtml(homework.getTitle()),
                EmailTemplateUtil.escapeHtml(deadline),
                instruction.isEmpty() ? "" : "<p style=\"margin:0 0 4px;font-size:12px;color:#7a5c59;font-weight:700;\">HƯỚNG DẪN</p><p style=\"margin:0;font-size:13px;line-height:20px;color:#5f4745;\">" + EmailTemplateUtil.escapeHtml(instruction) + "</p>"
        );

        return EmailTemplateUtil.buildBrandedEmailHtml(
                student.getFullName(),
                "Bạn có bài tập mới!",
                "Giảng viên vừa giao bài tập mới cho bạn. Hãy hoàn thành đúng hạn để duy trì tiến độ học tập tốt nhất.",
                highlightContent,
                normalizedBaseUrl() + "/my-homework",
                "Xem bài tập",
                supportEmail,
                "Vui lòng truy cập EnglishLab để làm và nộp bài trước thời hạn."
        );
    }

    private String normalizedBaseUrl() { return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
