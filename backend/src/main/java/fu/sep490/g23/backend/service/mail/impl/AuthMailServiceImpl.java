package fu.sap490.g23.backend.service.mail.impl;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.service.mail.AuthMailService;
import fu.sap490.g23.backend.service.mail.EmailTemplateUtil;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthMailServiceImpl implements AuthMailService {

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

    @Value("${englishlab.mail.base-url:http://localhost:5173}")
    private String baseUrl;

    @Override
    public void sendVerificationEmail(User user, String code) {
        sendCodeEmail(
                user,
                "Xác thực email đăng ký - EnglishLab",
                "Xác thực tài khoản của bạn",
                "Cảm ơn bạn đã đăng ký EnglishLab. Nhập mã dưới đây trên trang xác thực để kích hoạt tài khoản và bắt đầu học.",
                "Mã xác thực của bạn",
                code,
                "/verify-email?email=" + encodedEmail(user),
                "Xác thực tài khoản"
        );
    }

    @Override
    public void sendPasswordResetEmail(User user, String code) {
        sendCodeEmail(
                user,
                "Đặt lại mật khẩu - EnglishLab",
                "Mã đặt lại mật khẩu của bạn",
                "Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu. Nhập mã dưới đây trên trang đặt lại mật khẩu để tiếp tục.",
                "Mã OTP của bạn",
                code,
                "/reset-password?email=" + encodedEmail(user),
                "Đặt lại mật khẩu"
        );
    }

    @Override
    public void sendStaffCreatedAccountEmail(User user, String code) {
        boolean teacher = user != null && user.hasRole(RoleEnum.TEACHER);
        String nextPath = teacher ? "&next=" + encode("/teacher/professional-profile?connect=google-meet") : "";
        sendCodeEmail(
                user,
                "Thiết lập tài khoản EnglishLab",
                teacher ? "Tài khoản giáo viên của bạn đã sẵn sàng" : "Tài khoản EnglishLab của bạn đã sẵn sàng",
                teacher
                        ? "EnglishLab đã tạo tài khoản giáo viên cho bạn. Hãy thiết lập mật khẩu, đăng nhập và kết nối Google để sẵn sàng tạo phòng học trực tuyến."
                        : "EnglishLab đã tạo tài khoản theo thông tin bạn cung cấp. Dùng mã dưới đây để tự thiết lập mật khẩu đăng nhập.",
                "Mã thiết lập mật khẩu",
                code,
                "/reset-password?email=" + encodedEmail(user) + nextPath,
                "Thiết lập tài khoản"
        );
    }

    @Override
    public void sendTeacherGoogleMeetInvitation(User user) {
        String actionUrl = normalizedBaseUrl() + "/teacher/professional-profile?connect=google-meet";
        String html = EmailTemplateUtil.buildBrandedEmailHtml(
                user == null ? null : user.getFullName(),
                "Kết nối Google Meet với EnglishLab",
                "Bạn đã được cấp quyền giáo viên. Hãy đăng nhập và kết nối tài khoản Google dùng để giảng dạy; các phòng học sau đó sẽ được tạo dưới tài khoản này.",
                null,
                actionUrl,
                "Kết nối Google Meet",
                supportEmail,
                "Nếu bạn chưa đăng nhập, EnglishLab sẽ yêu cầu đăng nhập trước khi mở trang kết nối."
        );
        sendHtmlEmail(user, "Kết nối Google Meet - EnglishLab", html);
    }

    private void sendCodeEmail(
            User user,
            String subject,
            String heading,
            String description,
            String codeLabel,
            String code,
            String actionPath,
            String actionLabel
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
            String highlight = "<p style=\"margin:0 0 8px;font-size:13px;line-height:20px;color:#7a5c59;font-weight:600;text-align:center;\">"
                    + EmailTemplateUtil.escapeHtml(codeLabel)
                    + "</p><p style=\"margin:0;font-size:32px;line-height:42px;letter-spacing:8px;color:#730014;font-weight:600;text-align:center;\">"
                    + EmailTemplateUtil.escapeHtml(code)
                    + "</p>";
            String html = EmailTemplateUtil.buildBrandedEmailHtml(
                    user.getFullName(),
                    heading,
                    description,
                    highlight,
                    normalizedBaseUrl() + actionPath,
                    actionLabel,
                    supportEmail,
                    "Mã này chỉ có hiệu lực trong thời gian ngắn. Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email."
            );
            sendHtmlEmail(user, subject, html);
        } catch (Exception exception) {
            log.error("Failed to send auth email '{}' to {}", subject, user.getEmail(), exception);
        }
    }

    private void sendHtmlEmail(User user, String subject, String html) {
        if (!enabled || user == null || isBlank(user.getEmail())) return;
        if (isBlank(mailHost) || isBlank(fromAddress)) {
            log.warn("Auth mail was skipped because MAIL_HOST or ENGLISHLAB_MAIL_FROM is missing.");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent auth email '{}' to {}", subject, user.getEmail());
        } catch (Exception exception) {
            log.error("Failed to send auth email '{}' to {}", subject, user.getEmail(), exception);
        }
    }

    private String encodedEmail(User user) {
        return encode(user == null ? "" : valueOrDefault(user.getEmail(), ""));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizedBaseUrl() {
        String value = valueOrDefault(baseUrl, "http://localhost:5173");
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
