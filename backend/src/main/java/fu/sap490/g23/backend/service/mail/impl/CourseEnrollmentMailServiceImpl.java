package fu.sap490.g23.backend.service.mail.impl;

import fu.sap490.g23.backend.service.mail.*;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseEnrollmentMailServiceImpl implements CourseEnrollmentMailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String TEMPLATE_PATH = "email-templates/course-enrollment-success.html";
    private static final String DEFAULT_HERO_IMAGE_PATH = "static/email/course-success-hero.png";
    private static final String DEFAULT_HERO_CONTENT_ID = "paymentSuccessHero";

    private final JavaMailSender mailSender;

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

    @Value("${englishlab.mail.hero-image-url:}")
    private String heroImageUrl;

    @Value("${englishlab.mail.support-email:support@englishlab.vn}")
    private String supportEmail;

    @Value("${englishlab.mail.support-center-url:http://localhost:5173/support}")
    private String supportCenterUrl;

    @Value("${englishlab.mail.privacy-policy-url:http://localhost:5173/privacy}")
    private String privacyPolicyUrl;

    public void sendEnrollmentSuccessEmail(User student, OnlineCourse course, PackageEnrollment enrollment) {
        if (!enabled) {
            log.debug("Course enrollment email is disabled.");
            return;
        }
        if (isBlank(mailHost) || isBlank(fromAddress)) {
            log.warn("Course enrollment email was skipped because MAIL_HOST or ENGLISHLAB_MAIL_FROM is missing.");
            return;
        }
        if (student == null || isBlank(student.getEmail())) {
            log.warn("Course enrollment email was skipped because student email is missing.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(student.getEmail());
            helper.setSubject("Đăng ký khóa học thành công - EnglishLab");
            helper.setText(renderTemplate(student, course, enrollment), true);
            if (useInlineDefaultHero()) {
                helper.addInline(DEFAULT_HERO_CONTENT_ID, new ClassPathResource(DEFAULT_HERO_IMAGE_PATH));
            }

            mailSender.send(message);
            log.info("Sent course enrollment success email to {}", student.getEmail());
        } catch (Exception ex) {
            log.error("Failed to send course enrollment success email to {}", student.getEmail(), ex);
        }
    }

    private String renderTemplate(User student, OnlineCourse course, PackageEnrollment enrollment) throws Exception {
        LearningPackage learningPackage = course.getLearningPackage();
        String template = new ClassPathResource(TEMPLATE_PATH).getContentAsString(StandardCharsets.UTF_8);

        Map<String, String> values = new LinkedHashMap<>();
        values.put("heroImageUrl", absoluteUrl(heroImageUrl()));
        values.put("studentName", valueOrDefault(student.getFullName(), "bạn"));
        values.put("courseTitle", valueOrDefault(learningPackage.getTitle(), "khóa học EnglishLab"));
        values.put("enrollmentCode", enrollmentCode(enrollment));
        values.put("activatedAt", activatedAt(enrollment));
        values.put("courseDuration", courseDuration(course));
        values.put("courseOutcome", courseOutcome(course));
        values.put("courseUrl", courseUrl(learningPackage));
        values.put("supportCenterUrl", absoluteUrl(valueOrDefault(supportCenterUrl, "/support")));
        values.put("privacyPolicyUrl", absoluteUrl(valueOrDefault(privacyPolicyUrl, "/privacy")));
        values.put("supportEmail", valueOrDefault(supportEmail, "support@englishlab.vn"));
        values.put("copyrightYear", String.valueOf(LocalDateTime.now().getYear()));

        String rendered = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", escapeHtml(entry.getValue()));
        }
        return rendered;
    }

    private String courseDuration(OnlineCourse course) {
        LearningPackage learningPackage = course.getLearningPackage();
        if (!isBlank(learningPackage.getDuration())) {
            return learningPackage.getDuration();
        }
        if (course.getTotalHours() != null && course.getTotalHours() > 0) {
            return course.getTotalHours() + " giờ";
        }
        return "Cập nhật theo lộ trình khóa học";
    }

    private String courseOutcome(OnlineCourse course) {
        LearningPackage learningPackage = course.getLearningPackage();
        if (!isBlank(course.getTargetOutcome())) {
            return course.getTargetOutcome();
        }
        if (!isBlank(learningPackage.getShortDescription())) {
            return learningPackage.getShortDescription();
        }
        return "Hoàn thành lộ trình học tập trong workspace EnglishLab";
    }

    private String courseUrl(LearningPackage learningPackage) {
        String slugOrId = !isBlank(learningPackage.getSlug()) ? learningPackage.getSlug() : String.valueOf(learningPackage.getId());
        return normalizedBaseUrl() + "/courses/" + slugOrId + "/learn";
    }

    private String enrollmentCode(PackageEnrollment enrollment) {
        if (enrollment.getId() == null) {
            return "EL-ENR";
        }
        return "EL-ENR-%06d".formatted(enrollment.getId());
    }

    private String activatedAt(PackageEnrollment enrollment) {
        LocalDateTime registeredAt = enrollment.getRegisteredAt() == null ? LocalDateTime.now() : enrollment.getRegisteredAt();
        return registeredAt.format(DATE_TIME_FORMATTER);
    }

    private String normalizedBaseUrl() {
        String url = valueOrDefault(baseUrl, "http://localhost:5173").trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String heroImageUrl() {
        String configuredHero = valueOrDefault(heroImageUrl, null);
        if (!isBlank(configuredHero)) {
            return configuredHero;
        }
        return fallbackHeroImageUrl();
    }

    private String fallbackHeroImageUrl() {
        return "cid:" + DEFAULT_HERO_CONTENT_ID;
    }

    private boolean useInlineDefaultHero() {
        return isBlank(heroImageUrl);
    }

    private String absoluteUrl(String value) {
        if (isBlank(value)) {
            return normalizedBaseUrl();
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("data:") || trimmed.startsWith("cid:")) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            return normalizedBaseUrl() + trimmed;
        }
        return normalizedBaseUrl() + "/" + trimmed;
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
