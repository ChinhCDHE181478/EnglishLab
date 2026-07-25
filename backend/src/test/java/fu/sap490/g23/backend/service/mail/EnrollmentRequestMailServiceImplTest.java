package fu.sap490.g23.backend.service.mail;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.EnrollmentRequest;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.service.mail.impl.EnrollmentRequestMailServiceImpl;
import fu.sap490.g23.backend.service.notification.NotificationPreferenceService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentRequestMailServiceImplTest {
    @Mock private JavaMailSender mailSender;
    @Mock private NotificationPreferenceService preferenceService;

    private EnrollmentRequestMailServiceImpl service;
    private MimeMessage message;
    private EnrollmentRequest request;

    @BeforeEach
    void setUp() {
        service = new EnrollmentRequestMailServiceImpl(mailSender, preferenceService);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "mailHost", "smtp.example.com");
        ReflectionTestUtils.setField(service, "fromAddress", "hello@englishlab.vn");
        ReflectionTestUtils.setField(service, "fromName", "EnglishLab");
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:5173");

        User learner = User.builder()
                .id(10L)
                .fullName("Nguyễn Văn A")
                .email("account@example.com")
                .build();
        request = EnrollmentRequest.builder()
                .id(20L)
                .learner(learner)
                .contactName("Nguyễn Văn A")
                .contactEmail("contact@example.com")
                .build();
        message = new MimeMessage((Session) null);
        when(preferenceService.isEmailEnabled(learner)).thenReturn(true);
        when(mailSender.createMimeMessage()).thenReturn(message);
    }

    @Test
    void sendsAssignmentEmailToContactAddressWithVietnameseDate() throws Exception {
        ClassroomOffering classroom = ClassroomOffering.builder()
                .id(30L)
                .learningPackage(LearningPackage.builder().title("IELTS Foundation F01").build())
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .offlineAddress("EnglishLab Campus")
                .startDate(LocalDate.of(2026, 8, 5))
                .build();

        service.sendClassAssignment(request, classroom);

        verify(mailSender).send(message);
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("contact@example.com");
        assertThat(message.getSubject()).isEqualTo("Bạn đã được xếp lớp - EnglishLab");
        assertThat(message.getContent().toString()).contains("05/08/2026");
    }
}
