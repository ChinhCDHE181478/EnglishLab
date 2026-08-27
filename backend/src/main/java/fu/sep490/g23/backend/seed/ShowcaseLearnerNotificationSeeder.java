package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.notification.AppNotification;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.notification.AppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(340)
@RequiredArgsConstructor
@Slf4j
public class ShowcaseLearnerNotificationSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AppNotificationRepository notificationRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void run(String... args) {
        log.info("[ShowcaseNotification] Bắt đầu seed thông báo cho các tài khoản học viên demo...");
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        List<String> targetEmails = List.of(
                "0386852628z@gmail.com",
                "chinhcdhe181478@fpt.edu.vn",
                "classroom.manager@englishlab.vn",
                "staff@englishlab.vn"
        );

        for (String email : targetEmails) {
            tx.executeWithoutResult(status -> {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    try {
                        seedNotificationsForUser(user);
                    } catch (Exception ex) {
                        log.warn("Không thể seed thông báo cho {}: {}", email, ex.getMessage(), ex);
                    }
                }
            });
        }
    }

    private void seedNotificationsForUser(User user) {
        LocalDateTime now = LocalDateTime.now();

        record NotifSeed(
                String type,
                String title,
                String body,
                String actionPath,
                String deduplicationKey,
                boolean read,
                LocalDateTime createdAt
        ) {}

        List<NotifSeed> list = List.of(
                new NotifSeed(
                        "COURSE_COMPLETION",
                        "Hoàn thành khóa học xuất sắc! 🎉",
                        "Chúc mừng bạn đã hoàn thành 100% khóa học 'E2 IELTS Practice Tests' và đạt tất cả tiêu chí đánh giá AI.",
                        "/courses/e2-ielts-practice-tests/home",
                        "SHOWCASE_COMPLETION_E2_" + user.getId(),
                        false,
                        now.minusHours(2)
                ),
                new NotifSeed(
                        "AI_ASSESSMENT_EVALUATED",
                        "Kết quả chấm AI cho bài kiểm tra Module",
                        "Bài kiểm tra 'AI Vocabulary Output Check' của bạn đã được AI phân tích chi tiết kèm gợi ý sửa lỗi phát âm và từ vựng.",
                        "/courses/ielts-master-vocabulary-band-7-plus/home",
                        "SHOWCASE_AI_EVAL_" + user.getId(),
                        false,
                        now.minusHours(6)
                ),
                new NotifSeed(
                        "COURSE_PROGRESS",
                        "Cập nhật tiến độ khóa từ vựng",
                        "Bạn đã hoàn thành 2 mô-đun đầu tiên (25% tiến độ) của khóa 'IELTS Master Vocabulary Band 7+'. Hãy tiếp tục duy trì phong độ!",
                        "/courses/ielts-master-vocabulary-band-7-plus/home",
                        "SHOWCASE_VOCAB_PROG_" + user.getId(),
                        true,
                        now.minusDays(1)
                ),
                new NotifSeed(
                        "LEARNING_PATH_UPDATE",
                        "Tiến độ lộ trình học tập",
                        "Lộ trình 'IELTS 5.5 to 7.0 Self-Paced Path' của bạn đã đạt tiến độ mới. Kiểm tra các mốc tiếp theo để sẵn sàng cho bài thi.",
                        "/learning-paths/IELTS_BAND_55_TO_70",
                        "SHOWCASE_PATH_UPDATE_" + user.getId(),
                        true,
                        now.minusDays(3)
                ),
                new NotifSeed(
                        "PAYMENT_SUCCESS",
                        "Thanh toán đơn hàng thành công",
                        "Đơn hàng kích hoạt các gói tự học IELTS và lộ trình đã được xử lý và kích hoạt tự động trên tài khoản của bạn.",
                        "/orders",
                        "SHOWCASE_PAYMENT_SUCCESS_" + user.getId(),
                        true,
                        now.minusDays(5)
                ),
                new NotifSeed(
                        "CLASS_REMINDER",
                        "Lịch học lớp trực tuyến sắp tới",
                        "Nhắc lịch: Buổi học tiếp theo của bạn sẽ diễn ra trực tuyến. Vui lòng chuẩn bị tài liệu và vào lớp đúng giờ.",
                        "/my-classrooms",
                        "SHOWCASE_CLASS_REMIND_" + user.getId(),
                        true,
                        now.minusDays(7)
                ),
                new NotifSeed(
                        "WELCOME",
                        "Chào mừng đến với EnglishLab",
                        "Chào mừng bạn gia nhập nền tảng học tiếng Anh trực tuyến EnglishLab. Khám phá các khóa học và lộ trình học tập cá nhân hóa ngay hôm nay!",
                        "/",
                        "SHOWCASE_WELCOME_" + user.getId(),
                        true,
                        now.minusDays(14)
                )
        );

        int addedCount = 0;
        for (NotifSeed item : list) {
            if (!notificationRepository.existsByUserIdAndDeduplicationKey(user.getId(), item.deduplicationKey())) {
                AppNotification notif = AppNotification.builder()
                        .user(user)
                        .type(item.type())
                        .title(item.title())
                        .body(item.body())
                        .actionPath(item.actionPath())
                        .deduplicationKey(item.deduplicationKey())
                        .read(item.read())
                        .readAt(item.read() ? item.createdAt().plusMinutes(15) : null)
                        .createdAt(item.createdAt())
                        .build();
                notificationRepository.save(notif);
                addedCount++;
            }
        }
        if (addedCount > 0) {
            log.info("[ShowcaseNotification] Đã tạo {} thông báo mẫu cho {}.", addedCount, user.getEmail());
        }
    }
}
