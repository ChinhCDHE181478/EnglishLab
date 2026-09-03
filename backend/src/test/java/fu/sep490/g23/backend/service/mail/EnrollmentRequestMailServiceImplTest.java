package fu.sep490.g23.backend.service.mail;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.Room;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.service.mail.impl.EnrollmentRequestMailServiceImpl;
import fu.sep490.g23.backend.service.notification.NotificationPreferenceService;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
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
    private CourseRegistrationRequest request;

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
        request = CourseRegistrationRequest.builder()
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
        ClassSection classroom = ClassSection.builder()
                .id(30L)
                .name("IELTS Foundation F01")
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .room(Room.builder().locationAddress("EnglishLab Campus").build())
                .startDate(LocalDate.of(2026, 8, 5))
                .build();

        service.sendClassAssignment(request, classroom);

        verify(mailSender).send(message);
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("contact@example.com");
        assertThat(message.getSubject()).isEqualTo("Bạn đã được xếp lớp thành công - EnglishLab");
        assertThat(readTextContent(message)).contains("05/08/2026");
    }

    private String readTextContent(Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                result.append(readTextContent(multipart.getBodyPart(index)));
            }
            return result.toString();
        }
        return "";
    }
}
