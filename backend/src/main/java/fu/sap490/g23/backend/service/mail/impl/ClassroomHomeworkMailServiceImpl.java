package fu.sap490.g23.backend.service.mail.impl;

import fu.sap490.g23.backend.service.mail.*;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomHomework;
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

    @Value("${englishlab.mail.enabled:true}") private boolean enabled;
    @Value("${spring.mail.host:}") private String mailHost;
    @Value("${englishlab.mail.from:}") private String fromAddress;
    @Value("${englishlab.mail.from-name:EnglishLab}") private String fromName;
    @Value("${englishlab.mail.base-url:http://localhost:5173}") private String baseUrl;

    public void sendHomeworkAssigned(User student, ClassroomHomework homework) {
        if (!enabled || blank(mailHost) || blank(fromAddress) || student == null || blank(student.getEmail())) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(student.getEmail());
            helper.setSubject("Bạn có bài tập mới - EnglishLab");
            helper.setText(render(student, homework), true);
            mailSender.send(message);
        } catch (Exception exception) {
            log.error("Không thể gửi email bài tập {} tới {}", homework.getId(), student.getEmail(), exception);
        }
    }

    private String render(User student, ClassroomHomework homework) {
        String deadline = homework.getDeadline() == null ? "Giảng viên chưa đặt hạn nộp" : homework.getDeadline().format(DATE_FORMAT);
        return """
                <html><body style="font-family:Arial,sans-serif;color:#2b2828;line-height:1.6">
                <h2 style="color:#8a0018">Bạn có bài tập mới</h2>
                <p>Xin chào %s,</p>
                <p>Giảng viên vừa giao bài tập <strong>%s</strong>.</p>
                <p><strong>Hạn nộp:</strong> %s</p>
                <p>%s</p>
                <p><a href="%s/my-homework" style="display:inline-block;background:#8a0018;color:#fff;padding:12px 18px;border-radius:8px;text-decoration:none">Xem bài tập</a></p>
                </body></html>
                """.formatted(escape(student.getFullName()), escape(homework.getTitle()), escape(deadline), escape(homework.getInstruction()), normalizedBaseUrl());
    }

    private String normalizedBaseUrl() { return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String escape(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
