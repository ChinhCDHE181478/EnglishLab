package fu.sep490.g23.backend.ut.courseDiscussionService;

import fu.sep490.g23.backend.dto.request.course.CourseDiscussionReportRequest;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.CourseDiscussionPost;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionPostType;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportReasonCategory;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportTarget;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionPostRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReactionRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReportRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineLessonRepository;
import fu.sep490.g23.backend.service.course.CourseDiscussionNotificationService;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.DiscussionPostIdResolver;
import fu.sep490.g23.backend.service.course.impl.CourseDiscussionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteOwnDiscussionTest {

    @Mock
    private CourseDiscussionPostRepository postRepository;
    @Mock
    private CourseDiscussionReactionRepository reactionRepository;
    @Mock
    private CourseDiscussionReportRepository reportRepository;
    @Mock
    private DiscussionPostIdResolver discussionPostIdResolver;
    @Mock
    private OnlineCourseRepository onlineCourseRepository;
    @Mock
    private OnlineLessonRepository lessonRepository;
    @Mock
    private CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseDiscussionNotificationService discussionNotificationService;

    @InjectMocks
    private CourseDiscussionServiceImpl service;

    @Test
    void deleteThreadByAuthorMarksThreadDeletedWithoutEnrollmentCheck() {
        User author = user(1L, "author@englishlab.vn");
        CourseDiscussionPost thread = post(10L, author, CourseDiscussionPostType.THREAD);
        when(discussionPostIdResolver.requirePost(CourseDiscussionPostType.THREAD, 10L)).thenReturn(thread);
        when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));

        service.deleteThread(10L, author.getEmail());

        assertThat(thread.getStatus()).isEqualTo(CourseDiscussionStatus.DELETED);
        assertThat(thread.isAccepted()).isFalse();
        verifyNoInteractions(courseEnrollmentAccessPolicy);
    }

    @Test
    void deleteReplyByAuthorMarksReplyDeleted() {
        User author = user(2L, "reply.author@englishlab.vn");
        CourseDiscussionPost reply = post(20L, author, CourseDiscussionPostType.REPLY);
        reply.setAccepted(true);
        when(discussionPostIdResolver.requirePost(CourseDiscussionPostType.REPLY, 20L)).thenReturn(reply);
        when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));

        service.deleteReply(20L, author.getEmail());

        assertThat(reply.getStatus()).isEqualTo(CourseDiscussionStatus.DELETED);
        assertThat(reply.isAccepted()).isFalse();
    }

    @Test
    void deleteThreadByAnotherUserIsForbidden() {
        User author = user(3L, "author@englishlab.vn");
        User anotherUser = user(4L, "other@englishlab.vn");
        CourseDiscussionPost thread = post(30L, author, CourseDiscussionPostType.THREAD);
        when(discussionPostIdResolver.requirePost(CourseDiscussionPostType.THREAD, 30L)).thenReturn(thread);
        when(userRepository.findByEmail(anotherUser.getEmail())).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> service.deleteThread(30L, anotherUser.getEmail()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        assertThat(thread.getStatus()).isEqualTo(CourseDiscussionStatus.OPEN);
    }

    @Test
    void reportOwnThreadIsRejected() {
        User author = user(5L, "author@englishlab.vn");
        CourseDiscussionPost thread = post(40L, author, CourseDiscussionPostType.THREAD);
        CourseDiscussionReportRequest request = new CourseDiscussionReportRequest();
        request.setReasonCategory(CourseDiscussionReportReasonCategory.SPAM);
        when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
        when(discussionPostIdResolver.requirePost(CourseDiscussionPostType.THREAD, 40L)).thenReturn(thread);

        assertThatThrownBy(() -> service.reportContent(
                CourseDiscussionReportTarget.THREAD,
                40L,
                request,
                author.getEmail()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        verifyNoInteractions(reportRepository);
    }

    private User user(Long id, String email) {
        return User.builder().id(id).email(email).fullName(email).build();
    }

    private CourseDiscussionPost post(Long id, User author, CourseDiscussionPostType type) {
        return CourseDiscussionPost.builder()
                .id(id)
                .author(author)
                .postType(type)
                .content("Nội dung thảo luận")
                .status(CourseDiscussionStatus.OPEN)
                .build();
    }
}
