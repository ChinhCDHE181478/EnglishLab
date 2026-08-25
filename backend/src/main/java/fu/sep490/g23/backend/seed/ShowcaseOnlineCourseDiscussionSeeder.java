package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReaction;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReply;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReport;
import fu.sep490.g23.backend.entity.course.CourseDiscussionThread;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.Lesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReactionTarget;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReactionType;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportReasonCategory;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportTarget;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionStatus;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReactionRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReplyRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReportRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionThreadRepository;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@Order(340)
@RequiredArgsConstructor
@Slf4j
public class ShowcaseOnlineCourseDiscussionSeeder implements CommandLineRunner {
    private static final String LEARNER_EMAIL = "0386852628z@gmail.com";
    private static final String CONTENT_MANAGER_EMAIL = "content.manager@englishlab.vn";
    private static final String VOCAB_SLUG = "ielts-master-vocabulary-band-7-plus";
    private static final String E2_SLUG = "e2-ielts-practice-tests";

    private final UserRepository userRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;
    private final CourseDiscussionThreadRepository threadRepository;
    private final CourseDiscussionReplyRepository replyRepository;
    private final CourseDiscussionReportRepository reportRepository;
    private final CourseDiscussionReactionRepository reactionRepository;

    @Value("${app.seed.sheet.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        User showcase = userRepository.findByEmail(LEARNER_EMAIL).orElse(null);
        if (showcase == null) {
            return;
        }
        List<User> classmates = loadClassmates();
        if (classmates.size() < 2) {
            log.warn("Không đủ học viên phụ để seed thảo luận khóa online.");
            return;
        }
        User peerA = classmates.get(0);
        User peerB = classmates.get(1);
        User peerC = classmates.size() > 2 ? classmates.get(2) : peerB;
        User reviewer = userRepository.findByEmail(CONTENT_MANAGER_EMAIL).orElse(null);

        seedCourse(VOCAB_SLUG, showcase, peerA, peerB, peerC, reviewer, true);
        seedCourse(E2_SLUG, showcase, peerA, peerB, peerC, reviewer, false);
        log.info("Đã seed thảo luận và báo cáo bình luận cho khóa vocabulary và E2.");
    }

    private void seedCourse(
            String slug,
            User showcase,
            User peerA,
            User peerB,
            User peerC,
            User reviewer,
            boolean vocabulary
    ) {
        var pack = learningPackageRepository.findBySlugAndDeletedFalse(slug).orElse(null);
        if (pack == null) {
            return;
        }
        OnlineCourse course = onlineCourseRepository.findByLearningPackage(pack).orElse(null);
        if (course == null) {
            return;
        }
        enroll(showcase, pack);
        enroll(peerA, pack);
        enroll(peerB, pack);
        enroll(peerC, pack);

        Lesson firstLesson = firstLesson(course);

        if (vocabulary) {
            CourseDiscussionThread family = upsertThread(
                    course,
                    firstLesson,
                    peerA,
                    "immediate family và extended family khác nhau chỗ nào?",
                    "Trong bài Family, mình vẫn hay lẫn immediate family với extended family khi viết Task 2. Có ai có câu ví dụ band 7 không?",
                    CourseDiscussionStatus.RESOLVED
            );
            CourseDiscussionReply accepted = upsertReply(
                    family,
                    showcase,
                    "Immediate family là bố mẹ và anh chị em. Extended family gồm ông bà, cô dì, chú bác. Ví dụ: Grandparents in the extended family often help with childcare.",
                    true,
                    4
            );
            upsertReply(
                    family,
                    peerB,
                    "Mình hay nhớ: immediate = nhà mình ở cùng; extended = họ hàng. Collocation hay dùng là maintain close ties with the extended family.",
                    false,
                    2
            );
            react(CourseDiscussionReactionTarget.THREAD, family.getId(), showcase, CourseDiscussionReactionType.LIKE);
            react(CourseDiscussionReactionTarget.THREAD, family.getId(), peerB, CourseDiscussionReactionType.LOVE);
            react(CourseDiscussionReactionTarget.REPLY, accepted.getId(), peerA, CourseDiscussionReactionType.LIKE);
            react(CourseDiscussionReactionTarget.REPLY, accepted.getId(), peerC, CourseDiscussionReactionType.WOW);

            upsertThread(
                    course,
                    firstLesson,
                    showcase,
                    "Câu về family dynamics nên tách thế nào cho nhịp tự nhiên?",
                    "Mình viết: Grandparents in the extended family often help with childcare and that quietly changes family dynamics. Có nên tách thành hai câu không?",
                    CourseDiscussionStatus.OPEN
            );

            CourseDiscussionThread collocation = upsertThread(
                    course,
                    null,
                    peerB,
                    "nurture confidence dùng được trong Speaking Part 2 không?",
                    "Mình muốn nói về người đã giúp mình tự tin hơn. nurture confidence nghe có tự nhiên không, hay nên đổi thành build confidence?",
                    CourseDiscussionStatus.OPEN
            );
            CourseDiscussionReply rude = upsertReply(
                    collocation,
                    peerC,
                    "Hỏi linh tinh vậy học làm gì. Lên Google mà tìm, đừng spam diễn đàn.",
                    false,
                    0
            );
            ensureReport(
                    CourseDiscussionReportTarget.REPLY,
                    rude.getId(),
                    showcase,
                    CourseDiscussionReportReasonCategory.INAPPROPRIATE_LANGUAGE,
                    "Bình luận thiếu tôn trọng, không giúp được người hỏi.",
                    CourseDiscussionReportStatus.PENDING,
                    null,
                    null
            );
            rude.setReportedCount(Math.max(rude.getReportedCount(), 1));
            replyRepository.save(rude);

            CourseDiscussionThread spam = upsertThread(
                    course,
                    null,
                    peerC,
                    "Giảm 90% học phí IELTS, inbox ngay",
                    "Ae vào link bit.ly/ielts-re hoi để nhận voucher. Inbox mình để được tư vấn 1-1 miễn phí.",
                    CourseDiscussionStatus.HIDDEN
            );
            spam.setReportedCount(2);
            threadRepository.save(spam);
            ensureReport(
                    CourseDiscussionReportTarget.THREAD,
                    spam.getId(),
                    showcase,
                    CourseDiscussionReportReasonCategory.SPAM,
                    "Quảng cáo khóa học bên ngoài, có link rút gọn.",
                    CourseDiscussionReportStatus.ACTION_TAKEN,
                    reviewer,
                    "Đã ẩn bài vì spam / quảng cáo."
            );
            ensureReport(
                    CourseDiscussionReportTarget.THREAD,
                    spam.getId(),
                    peerA,
                    CourseDiscussionReportReasonCategory.SPAM,
                    "Bài này toàn link quảng cáo.",
                    CourseDiscussionReportStatus.ACTION_TAKEN,
                    reviewer,
                    "Đã ẩn bài vì spam / quảng cáo."
            );
            return;
        }

        CourseDiscussionThread listening = upsertThread(
                course,
                firstLesson,
                peerA,
                "Listening map labelling bị mất hướng Bắc thì làm sao?",
                "Đề map trong bài luyện có north arrow khá nhỏ. Mình hay bị lệch một nhãn. Mọi người có mẹo canh hướng không?",
                CourseDiscussionStatus.RESOLVED
        );
        CourseDiscussionReply tip = upsertReply(
                listening,
                showcase,
                "Mình pause audio, khoanh north trước, rồi đánh số chỗ trống theo chiều kim đồng hồ trước khi nghe lại.",
                true,
                3
        );
        upsertReply(
                listening,
                peerB,
                "Nên nghe hết instruction vì đôi khi đề nói you are at the entrance, không phải ở giữa bản đồ.",
                false,
                1
        );
        react(CourseDiscussionReactionTarget.THREAD, listening.getId(), peerB, CourseDiscussionReactionType.LIKE);
        react(CourseDiscussionReactionTarget.REPLY, tip.getId(), peerA, CourseDiscussionReactionType.LOVE);

        CourseDiscussionThread unanswered = upsertThread(
                course,
                null,
                showcase,
                "Writing Task 1 overview có cần số liệu cụ thể không?",
                "Mình viết overview chỉ nêu xu hướng chung, không đưa số. Có bị trừ band Task Achievement không?",
                CourseDiscussionStatus.OPEN
        );
        CourseDiscussionReply offTopic = upsertReply(
                unanswered,
                peerC,
                "Ai bán tài khoản Netflix premium không, inbox mình nhé.",
                false,
                0
        );
        ensureReport(
                CourseDiscussionReportTarget.REPLY,
                offTopic.getId(),
                peerA,
                CourseDiscussionReportReasonCategory.OFF_TOPIC,
                "Bình luận không liên quan đến bài Writing.",
                CourseDiscussionReportStatus.PENDING,
                null,
                null
        );
        offTopic.setReportedCount(Math.max(offTopic.getReportedCount(), 1));
        replyRepository.save(offTopic);

        CourseDiscussionThread falseAlarm = upsertThread(
                course,
                null,
                peerB,
                "Speaking Part 2 nên ghi bullet hay viết câu đầy đủ?",
                "Mình hay viết đầy đủ rồi không kịp nói. Có nên chỉ ghi từ khóa không?",
                CourseDiscussionStatus.OPEN
        );
        CourseDiscussionReply normal = upsertReply(
                falseAlarm,
                showcase,
                "Nên ghi 8-10 từ khóa theo past-present-future. Viết câu đầy đủ dễ bị đọc bài.",
                false,
                2
        );
        ensureReport(
                CourseDiscussionReportTarget.REPLY,
                normal.getId(),
                peerC,
                CourseDiscussionReportReasonCategory.HARASSMENT,
                "Bình luận này công kích mình.",
                CourseDiscussionReportStatus.DISMISSED,
                reviewer,
                "Nội dung mang tính góp ý học tập, không phải quấy rối."
        );
    }

    private List<User> loadClassmates() {
        List<User> users = new ArrayList<>();
        for (int index = 1; index <= 6; index++) {
            userRepository.findByEmail("hs.sheet.%03d@englishlab.vn".formatted(index))
                    .ifPresent(users::add);
        }
        userRepository.findByEmail("certificate.learner@englishlab.vn").ifPresent(users::add);
        return users;
    }

    private void enroll(User learner, LearningPackage pack) {
        enrollmentRepository.findByStudentAndLearningPackage(learner, pack)
                .orElseGet(() -> enrollmentRepository.save(OnlineCourseEnrollment.builder()
                        .student(learner)
                        .learningPackage(pack)
                        .status(EnrollmentStatus.ACTIVE)
                        .progressPercent(20)
                        .registeredAt(LocalDateTime.now().minusDays(18))
                        .build()));
    }

    private Lesson firstLesson(OnlineCourse course) {
        return course.getModules().stream()
                .sorted(Comparator.comparing(module -> module.getDisplayOrder() == null ? Integer.MAX_VALUE : module.getDisplayOrder()))
                .flatMap(module -> module.getLessons().stream()
                        .sorted(Comparator.comparing(lesson -> lesson.getDisplayOrder() == null ? Integer.MAX_VALUE : lesson.getDisplayOrder())))
                .findFirst()
                .orElse(null);
    }

    private CourseDiscussionThread upsertThread(
            OnlineCourse course,
            Lesson lesson,
            User author,
            String title,
            String content,
            CourseDiscussionStatus status
    ) {
        return threadRepository.findFirstByCourseAndTitle(course, title)
                .map(existing -> {
                    existing.setLesson(lesson);
                    existing.setContent(content);
                    existing.setStatus(status);
                    return threadRepository.save(existing);
                })
                .orElseGet(() -> threadRepository.save(CourseDiscussionThread.builder()
                        .course(course)
                        .lesson(lesson)
                        .author(author)
                        .title(title)
                        .content(content)
                        .status(status)
                        .build()));
    }

    private CourseDiscussionReply upsertReply(
            CourseDiscussionThread thread,
            User author,
            String content,
            boolean accepted,
            int helpfulCount
    ) {
        return thread.getReplies() == null
                ? saveReply(thread, author, content, accepted, helpfulCount)
                : thread.getReplies().stream()
                .filter(reply -> author.getId().equals(reply.getAuthor().getId()) && content.equals(reply.getContent()))
                .findFirst()
                .map(existing -> {
                    existing.setAccepted(accepted);
                    existing.setHelpfulCount(helpfulCount);
                    existing.setStatus(CourseDiscussionStatus.OPEN);
                    return replyRepository.save(existing);
                })
                .orElseGet(() -> saveReply(thread, author, content, accepted, helpfulCount));
    }

    private CourseDiscussionReply saveReply(
            CourseDiscussionThread thread,
            User author,
            String content,
            boolean accepted,
            int helpfulCount
    ) {
        CourseDiscussionReply saved = replyRepository.save(CourseDiscussionReply.builder()
                .thread(thread)
                .author(author)
                .content(content)
                .accepted(accepted)
                .helpfulCount(helpfulCount)
                .status(CourseDiscussionStatus.OPEN)
                .build());
        if (thread.getReplies() == null) {
            thread.setReplies(new ArrayList<>());
        }
        thread.getReplies().add(saved);
        return saved;
    }

    private void react(
            CourseDiscussionReactionTarget targetType,
            Long targetId,
            User user,
            CourseDiscussionReactionType type
    ) {
        reactionRepository.findByTargetTypeAndTargetIdAndUser(targetType, targetId, user)
                .ifPresentOrElse(existing -> {
                    existing.setReactionType(type);
                    reactionRepository.save(existing);
                }, () -> reactionRepository.save(CourseDiscussionReaction.builder()
                        .targetType(targetType)
                        .targetId(targetId)
                        .user(user)
                        .reactionType(type)
                        .build()));
    }

    private void ensureReport(
            CourseDiscussionReportTarget targetType,
            Long targetId,
            User reporter,
            CourseDiscussionReportReasonCategory category,
            String reason,
            CourseDiscussionReportStatus status,
            User reviewer,
            String actionNote
    ) {
        CourseDiscussionReport report = reportRepository
                .findByTargetTypeAndTargetIdAndReporter(targetType, targetId, reporter)
                .orElseGet(() -> CourseDiscussionReport.builder()
                        .targetType(targetType)
                        .targetId(targetId)
                        .reporter(reporter)
                        .build());
        report.setReasonCategory(category);
        report.setReason(reason);
        report.setStatus(status);
        report.setReviewedBy(reviewer);
        report.setReviewedAt(status == CourseDiscussionReportStatus.PENDING ? null : LocalDateTime.now().minusDays(1));
        report.setActionNote(actionNote);
        reportRepository.save(report);
    }
}
