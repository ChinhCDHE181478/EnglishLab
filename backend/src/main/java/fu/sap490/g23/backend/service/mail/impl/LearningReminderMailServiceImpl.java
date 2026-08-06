package fu.sap490.g23.backend.service.mail.impl;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.service.mail.EmailTemplateUtil;
import fu.sap490.g23.backend.service.mail.LearningReminderMailService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningReminderMailServiceImpl implements LearningReminderMailService {

    private final JavaMailSender mailSender;

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

    @Value("${englishlab.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void sendReminder(User user, String subject, String heading, String message, String actionPath) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;
        String resolvedFrom = (fromAddress != null && !fromAddress.isBlank()) ? fromAddress : "englishlab.edu.vn@gmail.com";
        if (!enabled || mailHost == null || mailHost.isBlank()) return;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(resolvedFrom, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(user.getEmail());
            helper.setSubject(subject);

            String actionUrl = frontendUrl.replaceAll("/+$", "") + (actionPath == null ? "" : actionPath);

            String html = EmailTemplateUtil.buildBrandedEmailHtml(
                    user.getFullName(),
                    heading,
                    message,
                    null,
                    actionUrl,
                    "Mở EnglishLab",
                    supportEmail,
                    "Bạn có thể thay đổi tùy chọn thông báo trong Hồ sơ cá nhân → Cài đặt thông báo."
            );

            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("Sent learning reminder email to {}", user.getEmail());
        } catch (Exception exception) {
            log.warn("Không thể gửi email nhắc học tới {}: {}", user.getEmail(), exception.getMessage());
        }
    }
}
