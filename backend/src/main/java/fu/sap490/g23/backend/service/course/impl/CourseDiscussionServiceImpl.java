package fu.sap490.g23.backend.service.course.impl;

import fu.sap490.g23.backend.service.course.*;


import fu.sap490.g23.backend.dto.request.course.CourseDiscussionReplyRequest;
import fu.sap490.g23.backend.dto.request.course.CourseDiscussionReactionRequest;
import fu.sap490.g23.backend.dto.request.course.CourseDiscussionReportRequest;
import fu.sap490.g23.backend.dto.request.course.CourseDiscussionThreadRequest;
import fu.sap490.g23.backend.dto.response.ApiResponse;
import fu.sap490.g23.backend.dto.response.course.CourseDiscussionReactionResponse;
import fu.sap490.g23.backend.dto.response.course.CourseDiscussionReplyResponse;
import fu.sap490.g23.backend.dto.response.course.CourseDiscussionThreadResponse;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.CourseDiscussionReply;
import fu.sap490.g23.backend.entity.course.CourseDiscussionReaction;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReactionTarget;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReactionType;
import fu.sap490.g23.backend.entity.course.CourseDiscussionReplyVote;
import fu.sap490.g23.backend.entity.course.CourseDiscussionReport;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportTarget;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionStatus;
import fu.sap490.g23.backend.entity.course.CourseDiscussionThread;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.CourseDiscussionReplyRepository;
import fu.sap490.g23.backend.repository.course.CourseDiscussionReactionRepository;
import fu.sap490.g23.backend.repository.course.CourseDiscussionReplyVoteRepository;
import fu.sap490.g23.backend.repository.course.CourseDiscussionReportRepository;
import fu.sap490.g23.backend.repository.course.CourseDiscussionThreadRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
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
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CourseDiscussionThreadResponse> getCourseDiscussions(Long courseId, String filter, String email) {
        ensureCourseExists(courseId);
        User currentUser = email == null ? null : findUser(email);
        List<CourseDiscussionThreadResponse> items = threadRepository
                .findByCourseIdAndStatusNotOrderByUpdatedAtDesc(courseId, CourseDiscussionStatus.HIDDEN)
                .stream()
                .map(thread -> toThreadResponse(thread, currentUser))
                .toList();

        String normalizedFilter = String.valueOf(filter == null ? "ALL" : filter).toUpperCase(Locale.ROOT);
        if ("UNANSWERED".equals(normalizedFilter)) {
            return items.stream().filter(item -> item.getReplyCount() == 0 && !item.isResolved()).toList();
        }
        if ("RESOLVED".equals(normalizedFilter)) {
            return items.stream().filter(CourseDiscussionThreadResponse::isResolved).toList();
        }
        if ("HELPFUL".equals(normalizedFilter)) {
            return items.stream()
                    .sorted(Comparator.comparingInt(CourseDiscussionThreadResponse::getHelpfulCount).reversed())
                    .toList();
        }
        return items;
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

        return toThreadResponse(threadRepository.save(thread), author);
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

        return toReplyResponse(replyRepository.save(reply), author);
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
                .reason(clean(request == null ? "" : request.getReason()))
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

    private void ensureCourseExists(Long courseId) {
        findPublicCourse(courseId);
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
        String original = clean(value).toLowerCase(Locale.ROOT);
        return UNSAFE_PHRASES.stream().anyMatch(phrase -> original.contains(phrase) || normalized.contains(removeDiacritics(phrase)));
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
