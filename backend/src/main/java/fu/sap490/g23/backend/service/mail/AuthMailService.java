package fu.sap490.g23.backend.service.mail;

import fu.sap490.g23.backend.entity.User;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthMailService {

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

    public void sendVerificationEmail(User user, String code) {
        sendCodeEmail(
                user,
                "Xác thực email đăng ký - EnglishLab",
                "Xác thực tài khoản của bạn",
                "Cảm ơn bạn đã đăng ký EnglishLab. Nhập mã dưới đây trên trang xác thực để kích hoạt tài khoản và bắt đầu học.",
                "Mã xác thực của bạn",
                code
        );
    }

    public void sendPasswordResetEmail(User user, String code) {
        sendCodeEmail(
                user,
                "Đặt lại mật khẩu - EnglishLab",
                "Mã đặt lại mật khẩu của bạn",
                "Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu. Nhập mã dưới đây trên trang đặt lại mật khẩu để tiếp tục.",
                "Mã OTP của bạn",
                code
        );
    }

    private void sendCodeEmail(
            User user,
            String subject,
            String heading,
            String description,
            String codeLabel,
            String code
    ) {
        if (!enabled) {
            log.debug("Auth mail is disabled.");
            return;
        }
        if (isBlank(mailHost) || isBlank(fromAddress)) {
            log.warn("Auth mail was skipped because MAIL_HOST or ENGLISHLAB_MAIL_FROM is missing.");
            return;
        }
        if (user == null || isBlank(user.getEmail())) {
            log.warn("Auth mail was skipped because recipient email is missing.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(renderCodeHtml(user, heading, description, codeLabel, code), true);
            mailSender.send(message);
            log.info("Sent auth email '{}' to {}", subject, user.getEmail());
        } catch (Exception ex) {
            log.error("Failed to send auth email '{}' to {}", subject, user.getEmail(), ex);
        }
    }

    private String renderCodeHtml(User user, String heading, String description, String codeLabel, String code) {
        String safeName = escapeHtml(valueOrDefault(user.getFullName(), "bạn"));
        String safeHeading = escapeHtml(heading);
        String safeDescription = escapeHtml(description);
        String safeCodeLabel = escapeHtml(codeLabel);
        String safeCode = escapeHtml(code);
        String safeSupportEmail = escapeHtml(valueOrDefault(supportEmail, "support@englishlab.vn"));
        String year = String.valueOf(LocalDateTime.now().getYear());

        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>EnglishLab</title>
                </head>
                <body style="margin:0;padding:0;background:#f7f3f2;font-family:Arial,sans-serif;color:#2b1f1f;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border-radius:24px;overflow:hidden;border:1px solid #ead8d5;">
                          <tr>
                            <td style="padding:32px;background:linear-gradient(135deg,#fff7f5 0%%,#ffffff 52%%,#f6e3e0 100%%);">
                              <table role="presentation" cellspacing="0" cellpadding="0" style="border-collapse:collapse;">
                                <tr>
                                  <td style="padding:0;vertical-align:middle;">
                                    <span style="display:inline-block;width:12px;height:28px;background:#8a0018;border-radius:2px;"></span>
                                    <span style="display:inline-block;width:10px;height:20px;background:#c45a64;border-radius:2px;margin-left:4px;"></span>
                                  </td>
                                  <td style="padding:0 0 0 10px;vertical-align:middle;font-size:24px;line-height:1;font-weight:800;color:#1f1f24;font-family:Arial,sans-serif;">
                                    English<span style="color:#8a0018;">Lab</span>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:24px 0 0;font-size:14px;line-height:22px;color:#7a5c59;">Xin chào %s,</p>
                              <h1 style="margin:10px 0 0;font-size:28px;line-height:36px;color:#4b0009;">%s</h1>
                              <p style="margin:14px 0 0;font-size:15px;line-height:26px;color:#5f4745;">%s</p>
                              <div style="margin-top:28px;padding:18px 24px;border-radius:18px;background:#fff1f3;border:1px solid #dfbfbd;text-align:center;">
                                <p style="margin:0 0 8px;font-size:13px;line-height:20px;color:#7a5c59;font-weight:700;">%s</p>
                                <p style="margin:0;font-size:36px;line-height:44px;letter-spacing:10px;color:#730014;font-weight:800;font-family:Arial,sans-serif;">%s</p>
                              </div>
                              <p style="margin:18px 0 0;font-size:13px;line-height:22px;color:#7a5c59;">Mã này chỉ có hiệu lực trong thời gian ngắn. Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 32px 32px;">
                              <p style="margin:0;font-size:13px;line-height:22px;color:#7a5c59;">
                                Cần hỗ trợ? Liên hệ <a href="mailto:%s" style="color:#730014;text-decoration:none;">%s</a>.
                              </p>
                              <p style="margin:14px 0 0;font-size:12px;line-height:20px;color:#9b807d;">© %s EnglishLab. All rights reserved.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                safeName,
                safeHeading,
                safeDescription,
                safeCodeLabel,
                safeCode,
                safeSupportEmail,
                safeSupportEmail,
                year
        );
    }

    private String valueOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
