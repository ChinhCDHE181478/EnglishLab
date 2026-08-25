package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.CourseDiscussionPost;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionPostType;
import fu.sep490.g23.backend.service.course.CourseDiscussionNotificationService;
import fu.sep490.g23.backend.service.notification.ClassroomNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseDiscussionNotificationServiceImpl implements CourseDiscussionNotificationService {
    private final ClassroomNotificationService notificationService;

    @Override
    public void notifyQuestionSent(CourseDiscussionPost thread) {
        notifySafely(thread.getAuthor(), "DISCUSSION_QUESTION_SENT", "Đã gửi câu hỏi",
                "Câu hỏi của bạn đã được đăng. Bạn sẽ nhận thông báo khi có người trả lời.",
                threadMetadata(thread));
    }

    @Override
    public void notifyNewReply(CourseDiscussionPost reply) {
        CourseDiscussionPost thread = reply.getParentPost();
        if (thread == null || thread.getPostType() != CourseDiscussionPostType.THREAD) {
            return;
        }
        if (thread.getAuthor().getId().equals(reply.getAuthor().getId())) {
            return;
        }

        Map<String, Object> metadata = threadMetadata(thread);
        metadata.put("replyId", reply.getId());
        notifySafely(thread.getAuthor(), "DISCUSSION_REPLY", "Câu hỏi của bạn có trả lời mới",
                preview(reply.getContent()), metadata);
    }

    private Map<String, Object> threadMetadata(CourseDiscussionPost thread) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("courseId", thread.getCourse().getId());
        metadata.put("lessonId", thread.getLesson() == null ? null : thread.getLesson().getId());
        metadata.put("threadId", thread.getId());
        return metadata;
    }

    private void notifySafely(User user, String type, String title, String body, Map<String, Object> metadata) {
        try {
            notificationService.notifyUser(user, type, title, body, metadata);
        } catch (RuntimeException exception) {
            log.warn("Không thể tạo thông báo thảo luận cho user {}: {}", user.getId(), exception.getMessage());
        }
    }

    private String preview(String content) {
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 117) + "...";
    }
}
