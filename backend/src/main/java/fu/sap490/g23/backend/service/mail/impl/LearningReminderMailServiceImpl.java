package fu.sap490.g23.backend.service.mail.impl;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.service.mail.LearningReminderMailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningReminderMailServiceImpl implements LearningReminderMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${englishlab.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void sendReminder(User user, String subject, String heading, String message, String actionPath) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            if (fromAddress != null && !fromAddress.isBlank()) helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            String actionUrl = frontendUrl.replaceAll("/+$", "") + (actionPath == null ? "" : actionPath);
            helper.setText("""
                    <div style="font-family:Arial,sans-serif;max-width:620px;margin:auto;color:#2b2828">
                      <div style="background:#4b0009;color:white;padding:22px 28px;border-radius:18px 18px 0 0">
                        <strong style="font-size:20px">EnglishLab</strong>
                      </div>
                      <div style="border:1px solid #ead9db;border-top:0;padding:28px;border-radius:0 0 18px 18px">
                        <h2 style="margin:0 0 14px;color:#4b0009">%s</h2>
                        <p>Xin chào <strong>%s</strong>,</p>
                        <p style="line-height:1.7">%s</p>
                        <a href="%s" style="display:inline-block;margin-top:12px;background:#4b0009;color:white;text-decoration:none;padding:12px 18px;border-radius:10px;font-weight:bold">Mở EnglishLab</a>
                        <p style="margin-top:24px;color:#756361;font-size:12px">Bạn có thể thay đổi tùy chọn nhắc nhở trong Hồ sơ → Thông báo.</p>
                      </div>
                    </div>
                    """.formatted(
                    HtmlUtils.htmlEscape(heading),
                    HtmlUtils.htmlEscape(user.getFullName()),
                    HtmlUtils.htmlEscape(message),
                    HtmlUtils.htmlEscape(actionUrl)
            ), true);
            mailSender.send(mimeMessage);
        } catch (Exception exception) {
            log.warn("Không thể gửi email nhắc học tới {}: {}", user.getEmail(), exception.getMessage());
        }
    }
}
