package fu.sep490.g23.backend.service.course.impl;
import fu.sep490.g23.backend.service.course.CourseDiscussionNotificationService;
import fu.sep490.g23.backend.service.course.CourseDiscussionService;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;


import fu.sep490.g23.backend.dto.request.course.CourseDiscussionReplyRequest;
import fu.sep490.g23.backend.dto.request.course.CourseDiscussionReactionRequest;
import fu.sep490.g23.backend.dto.request.course.CourseDiscussionReportRequest;
import fu.sep490.g23.backend.dto.request.course.CourseDiscussionThreadRequest;
import fu.sep490.g23.backend.dto.response.ApiResponse;
import fu.sep490.g23.backend.dto.response.course.CourseDiscussionReactionResponse;
import fu.sep490.g23.backend.dto.response.course.CourseDiscussionReplyResponse;
import fu.sep490.g23.backend.dto.response.course.CourseDiscussionThreadResponse;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReply;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReaction;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReactionTarget;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReactionType;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReplyVote;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReport;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportReasonCategory;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportTarget;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionStatus;
import fu.sep490.g23.backend.entity.course.CourseDiscussionThread;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.Lesson;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReplyRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReactionRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReplyVoteRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReportRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionThreadRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.LessonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseDiscussionServiceImpl implements CourseDiscussionService {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Set<String> UNSAFE_PHRASES = Set.of(
            "địt", "đụ", "cặc", "lồn", "đĩ", "óc chó", "ngu", "chó chết",
            "chống phá", "phản động", "chia rẽ dân tộc", "kích động thù hằn"
    );

    private final CourseDiscussionThreadRepository threadRepository;
    private final CourseDiscussionReplyRepository replyRepository;
    private final CourseDiscussionReactionRepository reactionRepository;
    private final CourseDiscussionReplyVoteRepository voteRepository;
    private final CourseDiscussionReportRepository reportRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final LessonRepository lessonRepository;
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    private final UserRepository userRepository;
    private final CourseDiscussionNotificationService discussionNotificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDiscussionThreadResponse> getCourseDiscussions(Long courseId, Long moduleId, String filter, String email, Pageable pageable) {
        ensureCourseExists(courseId);
        ensureModuleInCourse(courseId, moduleId);
        User currentUser = email == null ? null : findUser(email);
        String normalizedFilter = normalizeFilter(filter);
        return threadRepository.findCourseDiscussionPage(
                        courseId,
                        moduleId,
                        normalizedFilter,
                        currentUser == null ? null : currentUser.getId(),
                        CourseDiscussionStatus.HIDDEN,
                        CourseDiscussionStatus.RESOLVED,
                        pageable)
                .map(thread -> toThreadResponse(thread, currentUser));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDiscussionThreadResponse> getLessonDiscussions(Long courseId, Long lessonId, String filter, String email, Pageable pageable) {
        ensureCourseExists(courseId);
        findLessonInCourse(courseId, lessonId);
        User currentUser = email == null ? null : findUser(email);
        String normalizedFilter = normalizeFilter(filter);
        return threadRepository.findLessonDiscussionPage(
                        courseId,
                        lessonId,
                        normalizedFilter,
                        currentUser == null ? null : currentUser.getId(),
                        CourseDiscussionStatus.HIDDEN,
                        CourseDiscussionStatus.RESOLVED,
                        pageable)
                .map(thread -> toThreadResponse(thread, currentUser));
    }

    private String normalizeFilter(String filter) {
        String normalized = String.valueOf(filter == null ? "ALL" : filter).toUpperCase(Locale.ROOT);
        return Set.of("ALL", "MINE", "UNANSWERED", "RESOLVED", "HELPFUL").contains(normalized) ? normalized : "ALL";
    }

    @Override
    public CourseDiscussionThreadResponse createThread(Long courseId, CourseDiscussionThreadRequest request, String email) {
        OnlineCourse course = findPublicCourse(courseId);
        User author = findUser(email);
        ensureDiscussionAccess(author, course);
        String title = clean(request.getTitle());
        String content = clean(request.getContent());
        CourseDiscussionStatus status = shouldModerate(title + " " + content)
                ? CourseDiscussionStatus.PENDING_REVIEW
                : CourseDiscussionStatus.OPEN;

        CourseDiscussionThread thread = CourseDiscussionThread.builder()
                .course(course)
                .author(author)
                .title(title)
                .content(content)
                .status(status)
                .build();

        CourseDiscussionThread savedThread = threadRepository.save(thread);
        discussionNotificationService.notifyQuestionSent(savedThread);
        return toThreadResponse(savedThread, author);
    }

    @Override
    public CourseDiscussionThreadResponse createLessonThread(Long courseId, Long lessonId, CourseDiscussionThreadRequest request, String email) {
        OnlineCourse course = findPublicCourse(courseId);
        Lesson lesson = findLessonInCourse(courseId, lessonId);
        User author = findUser(email);
        ensureDiscussionAccess(author, course);
        String title = clean(request.getTitle());
        String content = clean(request.getContent());
        CourseDiscussionStatus status = shouldModerate(title + " " + content)
                ? CourseDiscussionStatus.PENDING_REVIEW
                : CourseDiscussionStatus.OPEN;

        CourseDiscussionThread thread = CourseDiscussionThread.builder()
                .course(course)
                .lesson(lesson)
                .author(author)
                .title(title)
                .content(content)
                .status(status)
                .build();

        CourseDiscussionThread savedThread = threadRepository.save(thread);
        discussionNotificationService.notifyQuestionSent(savedThread);
        return toThreadResponse(savedThread, author);
    }

    @Override
    public CourseDiscussionReplyResponse createReply(Long threadId, CourseDiscussionReplyRequest request, String email) {
        CourseDiscussionThread thread = findThread(threadId);
        if (thread.getStatus() == CourseDiscussionStatus.HIDDEN) {
            throw new RuntimeException("Thảo luận này hiện không khả dụng.");
        }

        User author = findUser(email);
        ensureDiscussionAccess(author, thread.getCourse());
        String content = clean(request.getContent());
        CourseDiscussionStatus status = shouldModerate(content)
                ? CourseDiscussionStatus.PENDING_REVIEW
                : CourseDiscussionStatus.OPEN;

        CourseDiscussionReply reply = CourseDiscussionReply.builder()
                .thread(thread)
                .author(author)
                .content(content)
                .status(status)
                .build();

        CourseDiscussionReply savedReply = replyRepository.save(reply);
        discussionNotificationService.notifyNewReply(savedReply);
        return toReplyResponse(savedReply, author);
    }

    @Override
    public CourseDiscussionReplyResponse toggleHelpful(Long replyId, String email) {
        CourseDiscussionReply reply = findReply(replyId);
        User user = findUser(email);
        ensureDiscussionAccess(user, reply.getThread().getCourse());
        if (reply.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không thể tự đánh dấu câu trả lời của mình là hữu ích.");
        }
        voteRepository.findByReplyAndUser(reply, user)
                .ifPresentOrElse(existing -> {
                    voteRepository.delete(existing);
                    reply.setHelpfulCount(Math.max(0, reply.getHelpfulCount() - 1));
                }, () -> {
                    voteRepository.save(CourseDiscussionReplyVote.builder().reply(reply).user(user).build());
                    reply.setHelpfulCount(reply.getHelpfulCount() + 1);
                });
        return toReplyResponse(reply, user);
    }

    @Override
    public CourseDiscussionThreadResponse toggleThreadReaction(Long threadId, CourseDiscussionReactionRequest request, String email) {
        CourseDiscussionThread thread = findThread(threadId);
        if (thread.getStatus() == CourseDiscussionStatus.HIDDEN) {
            throw new RuntimeException("Thảo luận này hiện không khả dụng.");
        }
        User user = findUser(email);
        ensureDiscussionAccess(user, thread.getCourse());
        toggleReaction(CourseDiscussionReactionTarget.THREAD, threadId, request.getType(), user);
        return toThreadResponse(thread, user);
    }

    @Override
    public CourseDiscussionReplyResponse toggleReplyReaction(Long replyId, CourseDiscussionReactionRequest request, String email) {
        CourseDiscussionReply reply = findReply(replyId);
        if (reply.getStatus() == CourseDiscussionStatus.HIDDEN) {
            throw new RuntimeException("Câu trả lời này hiện không khả dụng.");
        }
        User user = findUser(email);
        ensureDiscussionAccess(user, reply.getThread().getCourse());
        toggleReaction(CourseDiscussionReactionTarget.REPLY, replyId, request.getType(), user);
        return toReplyResponse(reply, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDiscussionReactionResponse> getThreadReactions(Long threadId) {
        CourseDiscussionThread thread = findThread(threadId);
        if (thread.getStatus() == CourseDiscussionStatus.HIDDEN) {
            throw new RuntimeException("Thảo luận này hiện không khả dụng.");
        }
        return getReactionResponses(CourseDiscussionReactionTarget.THREAD, threadId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDiscussionReactionResponse> getReplyReactions(Long replyId) {
        CourseDiscussionReply reply = findReply(replyId);
        if (reply.getStatus() == CourseDiscussionStatus.HIDDEN) {
            throw new RuntimeException("Câu trả lời này hiện không khả dụng.");
        }
        return getReactionResponses(CourseDiscussionReactionTarget.REPLY, replyId);
    }

    @Override
    public CourseDiscussionThreadResponse markResolved(Long threadId, Long replyId, String email) {
        CourseDiscussionThread thread = findThread(threadId);
        User user = findUser(email);
        ensureDiscussionAccess(user, thread.getCourse());
        if (!canModerate(user) && !thread.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn chỉ có thể đánh dấu đã giải quyết cho thảo luận của mình.");
        }

        thread.setStatus(CourseDiscussionStatus.RESOLVED);
        if (replyId != null) {
            CourseDiscussionReply acceptedReply = findReply(replyId);
            if (!acceptedReply.getThread().getId().equals(thread.getId())) {
                throw new RuntimeException("Câu trả lời không thuộc thảo luận này.");
            }
            thread.getReplies().forEach(reply -> reply.setAccepted(reply.getId().equals(replyId)));
        }
        return toThreadResponse(thread, user);
    }

    @Override
    public ApiResponse reportContent(CourseDiscussionReportTarget targetType, Long targetId, CourseDiscussionReportRequest request, String email) {
        User reporter = findUser(email);
        validateReportRequest(request);
        reportRepository.findByTargetTypeAndTargetIdAndReporter(targetType, targetId, reporter)
                .ifPresent(existing -> {
                    throw new RuntimeException("Bạn đã báo cáo nội dung này trước đó.");
                });

        if (targetType == CourseDiscussionReportTarget.THREAD) {
            CourseDiscussionThread thread = findThread(targetId);
            ensureDiscussionAccess(reporter, thread.getCourse());
            thread.setReportedCount(thread.getReportedCount() + 1);
            if (thread.getReportedCount() >= 3 && thread.getStatus() != CourseDiscussionStatus.RESOLVED) {
                thread.setStatus(CourseDiscussionStatus.PENDING_REVIEW);
            }
        } else {
            CourseDiscussionReply reply = findReply(targetId);
            ensureDiscussionAccess(reporter, reply.getThread().getCourse());
            reply.setReportedCount(reply.getReportedCount() + 1);
            if (reply.getReportedCount() >= 3) {
                reply.setStatus(CourseDiscussionStatus.PENDING_REVIEW);
            }
        }

        reportRepository.save(CourseDiscussionReport.builder()
                .targetType(targetType)
                .targetId(targetId)
                .reporter(reporter)
                .reason(clean(request.getReason()))
                .reasonCategory(request.getReasonCategory())
                .build());

        return ApiResponse.builder()
                .message("Cảm ơn bạn đã báo cáo. Nội dung sẽ được xem xét.")
                .description("EnglishLab đã ghi nhận báo cáo của bạn.")
                .build();
    }

    private OnlineCourse findPublicCourse(Long courseId) {
        OnlineCourse course = onlineCourseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        if (course.getLearningPackage().isDeleted() || course.getLearningPackage().getStatus() != PackageStatus.PUBLISHED) {
            throw new RuntimeException("Khóa học này hiện không khả dụng.");
        }
        return course;
    }

    private void validateReportRequest(CourseDiscussionReportRequest request) {
        if (request == null || request.getReasonCategory() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn loại báo cáo.");
        }
        if (request.getReasonCategory() == CourseDiscussionReportReasonCategory.OTHER
                && clean(request.getReason()).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do khi chọn loại báo cáo Khác.");
        }
    }

    private void ensureCourseExists(Long courseId) {
        findPublicCourse(courseId);
    }

    private Lesson findLessonInCourse(Long courseId, Long lessonId) {
        return lessonRepository.findByIdAndModuleOnlineCourseId(lessonId, courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học trong khóa học này."));
    }

    private void ensureModuleInCourse(Long courseId, Long moduleId) {
        if (moduleId != null && !lessonRepository.existsByModuleIdAndModuleOnlineCourseId(moduleId, courseId)) {
            throw new RuntimeException("Không tìm thấy mô-đun trong khóa học này.");
        }
    }

    private CourseDiscussionThread findThread(Long threadId) {
        return threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thảo luận."));
    }

    private CourseDiscussionReply findReply(Long replyId) {
        return replyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu trả lời."));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
    }

    private boolean canModerate(User user) {
        return user.hasAnyRole(java.util.EnumSet.of(RoleEnum.ADMIN, RoleEnum.MANAGER, RoleEnum.CONTENT_MANAGER));
    }

    private void ensureDiscussionAccess(User user, OnlineCourse course) {
        if (canModerate(user)) {
            return;
        }
        courseEnrollmentAccessPolicy.requireLearningAccess(user, course);
    }

    private String clean(String value) {
        return String.valueOf(value == null ? "" : value).trim().replaceAll("\\s+", " ");
    }

    private boolean shouldModerate(String value) {
        String normalized = DIACRITICS.matcher(Normalizer.normalize(clean(value).toLowerCase(Locale.ROOT), Normalizer.Form.NFD)).replaceAll("");
        String searchable = " " + normalized.replaceAll("[^\\p{L}\\p{N}]+", " ").replaceAll("\\s+", " ").trim() + " ";
        return UNSAFE_PHRASES.stream()
                .map(this::removeDiacritics)
                .anyMatch(phrase -> searchable.contains(" " + phrase + " "));
    }

    private String removeDiacritics(String value) {
        return DIACRITICS.matcher(Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)).replaceAll("");
    }

    private void toggleReaction(CourseDiscussionReactionTarget targetType, Long targetId, CourseDiscussionReactionType reactionType, User user) {
        reactionRepository.findByTargetTypeAndTargetIdAndUser(targetType, targetId, user)
                .ifPresentOrElse(existing -> {
                    if (existing.getReactionType() == reactionType) {
                        reactionRepository.delete(existing);
                    } else {
                        existing.setReactionType(reactionType);
                    }
                }, () -> reactionRepository.save(CourseDiscussionReaction.builder()
                        .targetType(targetType)
                        .targetId(targetId)
                        .reactionType(reactionType)
                        .user(user)
                        .build()));
    }

    private CourseDiscussionThreadResponse toThreadResponse(CourseDiscussionThread thread, User currentUser) {
        List<CourseDiscussionReplyResponse> replies = thread.getReplies().stream()
                .filter(reply -> reply.getStatus() != CourseDiscussionStatus.HIDDEN)
                .map(reply -> toReplyResponse(reply, currentUser))
                .toList();
        int helpfulCount = replies.stream().mapToInt(CourseDiscussionReplyResponse::getHelpfulCount).sum();

        return CourseDiscussionThreadResponse.builder()
                .id(thread.getId())
                .title(maskIfPending(thread.getTitle(), thread.getStatus(), "Nội dung đang chờ kiểm duyệt"))
                .content(maskIfPending(thread.getContent(), thread.getStatus(), "Nội dung đang chờ kiểm duyệt."))
                .status(thread.getStatus())
                .replyCount(replies.size())
                .helpfulCount(helpfulCount)
                .reactionCounts(getReactionCounts(CourseDiscussionReactionTarget.THREAD, thread.getId()))
                .myReaction(getMyReaction(CourseDiscussionReactionTarget.THREAD, thread.getId(), currentUser))
                .reportedCount(thread.getReportedCount())
                .resolved(thread.getStatus() == CourseDiscussionStatus.RESOLVED)
                .authorName(resolveAuthorName(thread.getAuthor()))
                .authorId(thread.getAuthor().getId())
                .lessonId(thread.getLesson() == null ? null : thread.getLesson().getId())
                .lessonTitle(thread.getLesson() == null ? null : thread.getLesson().getTitle())
                .createdAt(thread.getCreatedAt())
                .updatedAt(thread.getUpdatedAt())
                .replies(replies)
                .build();
    }

    private CourseDiscussionReplyResponse toReplyResponse(CourseDiscussionReply reply, User currentUser) {
        return CourseDiscussionReplyResponse.builder()
                .id(reply.getId())
                .content(maskIfPending(reply.getContent(), reply.getStatus(), "Nội dung đang chờ kiểm duyệt."))
                .status(reply.getStatus())
                .accepted(reply.isAccepted())
                .helpfulCount(reply.getHelpfulCount())
                .reactionCounts(getReactionCounts(CourseDiscussionReactionTarget.REPLY, reply.getId()))
                .myReaction(getMyReaction(CourseDiscussionReactionTarget.REPLY, reply.getId(), currentUser))
                .authorName(resolveAuthorName(reply.getAuthor()))
                .authorId(reply.getAuthor().getId())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .build();
    }

    private Map<String, Integer> getReactionCounts(CourseDiscussionReactionTarget targetType, Long targetId) {
        Map<CourseDiscussionReactionType, Integer> counts = new EnumMap<>(CourseDiscussionReactionType.class);
        reactionRepository.findByTargetTypeAndTargetId(targetType, targetId)
                .forEach(reaction -> counts.merge(reaction.getReactionType(), 1, Integer::sum));

        Map<String, Integer> response = new java.util.LinkedHashMap<>();
        for (CourseDiscussionReactionType type : CourseDiscussionReactionType.values()) {
            response.put(type.name(), counts.getOrDefault(type, 0));
        }
        return response;
    }

    private String getMyReaction(CourseDiscussionReactionTarget targetType, Long targetId, User currentUser) {
        if (currentUser == null) {
            return null;
        }
        Optional<CourseDiscussionReaction> reaction = reactionRepository.findByTargetTypeAndTargetIdAndUser(targetType, targetId, currentUser);
        return reaction.map(item -> item.getReactionType().name()).orElse(null);
    }

    private List<CourseDiscussionReactionResponse> getReactionResponses(CourseDiscussionReactionTarget targetType, Long targetId) {
        return reactionRepository.findByTargetTypeAndTargetIdOrderByUpdatedAtDesc(targetType, targetId)
                .stream()
                .map(reaction -> CourseDiscussionReactionResponse.builder()
                        .userId(reaction.getUser().getId())
                        .userName(resolveAuthorName(reaction.getUser()))
                        .type(reaction.getReactionType())
                        .createdAt(reaction.getUpdatedAt() == null ? reaction.getCreatedAt() : reaction.getUpdatedAt())
                        .build())
                .toList();
    }

    private String maskIfPending(String value, CourseDiscussionStatus status, String fallback) {
        return status == CourseDiscussionStatus.PENDING_REVIEW ? fallback : value;
    }

    private String resolveAuthorName(User user) {
        String fullName = user == null ? "" : user.getFullName();
        return fullName == null || fullName.isBlank() ? "Học viên EnglishLab" : fullName;
    }
}
