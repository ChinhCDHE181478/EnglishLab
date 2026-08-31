package fu.sep490.g23.backend.service.support.impl;

import fu.sep490.g23.backend.dto.request.support.CreateSupportTicketRequest;
import fu.sep490.g23.backend.dto.request.support.LearnerSupportTicketStatusRequest;
import fu.sep490.g23.backend.dto.request.support.SupportTicketReplyRequest;
import fu.sep490.g23.backend.dto.request.support.UpdateSupportTicketRequest;
import fu.sep490.g23.backend.dto.response.support.SupportTicketMessageResponse;
import fu.sep490.g23.backend.dto.response.support.SupportTicketResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.entity.support.SupportTicket;
import fu.sep490.g23.backend.entity.support.SupportTicketMessage;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.support.SupportTicketMessageRepository;
import fu.sep490.g23.backend.repository.support.SupportTicketRepository;
import fu.sep490.g23.backend.service.notification.AppNotificationService;
import fu.sep490.g23.backend.service.support.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportTicketServiceImpl implements SupportTicketService {

    private static final Set<String> SUPPORT_STAFF_ROLES = Set.of(
            RoleCodes.STAFF, RoleCodes.MANAGER, RoleCodes.ADMIN);

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AppNotificationService appNotificationService;

    @Override
    public SupportTicketResponse create(String userEmail, CreateSupportTicketRequest request) {
        User learner = requireLearner(userEmail);
        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
                .requester(learner)
                .subject(request.getSubject().trim())
                .category(request.getCategory())
                .status(SupportTicketStatus.OPEN)
                .priority(SupportTicketPriority.NORMAL)
                .build());
        saveMessage(ticket, learner, request.getMessage());
        return toResponse(ticket, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> listMine(String userEmail) {
        User learner = requireLearner(userEmail);
        return ticketRepository.findByRequesterIdOrderByUpdatedAtDesc(learner.getId()).stream()
                .map(ticket -> toResponse(ticket, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse getMine(Long ticketId, String userEmail) {
        User learner = requireLearner(userEmail);
        return toResponse(requireOwnedTicket(ticketId, learner), true);
    }

    @Override
    public SupportTicketResponse replyAsLearner(
            Long ticketId,
            String userEmail,
            SupportTicketReplyRequest request
    ) {
        User learner = requireLearner(userEmail);
        SupportTicket ticket = requireOwnedTicket(ticketId, learner);
        assertActive(ticket);
        saveMessage(ticket, learner, request.getMessage());
        ticket.setStatus(ticket.getAssignee() == null
                ? SupportTicketStatus.OPEN
                : SupportTicketStatus.IN_PROGRESS);
        ticket = ticketRepository.save(ticket);
        if (ticket.getAssignee() != null) {
            appNotificationService.createForUser(
                    ticket.getAssignee(),
                    "SUPPORT_TICKET_REPLY",
                    "Học viên đã phản hồi ticket #" + ticket.getId(),
                    ticket.getSubject(),
                    Map.of("ticketId", ticket.getId(), "path", "/staff/support-tickets")
            );
        }
        return toResponse(ticket, true);
    }

    @Override
    public SupportTicketResponse updateMyStatus(
            Long ticketId,
            String userEmail,
            LearnerSupportTicketStatusRequest request
    ) {
        User learner = requireLearner(userEmail);
        SupportTicket ticket = requireOwnedTicket(ticketId, learner);
        // Học viên chỉ được đóng ticket; không được mở lại sau khi CLOSED/RESOLVED.
        if (request.getStatus() != SupportTicketStatus.CLOSED) {
            throw new IllegalArgumentException(
                    "Học viên không thể mở lại ticket. Vui lòng tạo ticket mới nếu cần hỗ trợ thêm.");
        }
        if (isTerminal(ticket)) {
            throw new IllegalArgumentException(
                    "Ticket đã đóng hoặc đã giải quyết, không thể thay đổi trạng thái.");
        }
        ticket.setStatus(SupportTicketStatus.CLOSED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket.setResolvedBy(null);
        return toResponse(ticketRepository.save(ticket), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> listQueue(
            String staffEmail,
            SupportTicketStatus status,
            SupportTicketPriority priority
    ) {
        requireSupportStaff(staffEmail);
        return ticketRepository.findQueue(status, priority).stream()
                .map(ticket -> toResponse(ticket, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> pageQueue(
            String staffEmail,
            SupportTicketStatus status,
            SupportTicketPriority priority,
            String keyword,
            Pageable pageable
    ) {
        requireSupportStaff(staffEmail);
        Specification<SupportTicket> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        if (priority != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("priority"), priority));
        }
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalizedKeyword.isBlank()) {
            String pattern = "%" + normalizedKeyword + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("subject")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.join("requester").get("fullName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.join("requester").get("email")), pattern),
                    criteriaBuilder.equal(root.get("id").as(String.class), normalizedKeyword)
            ));
        }
        return ticketRepository.findAll(specification, pageable)
                .map(ticket -> toResponse(ticket, false));
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse getForStaff(Long ticketId, String staffEmail) {
        requireSupportStaff(staffEmail);
        return toResponse(requireTicket(ticketId), true);
    }

    @Override
    public SupportTicketResponse claim(Long ticketId, String staffEmail) {
        User staff = requireSupportStaff(staffEmail);
        SupportTicket ticket = requireTicket(ticketId);
        assertActive(ticket);
        ticket.setAssignee(staff);
        ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        return toResponse(ticketRepository.save(ticket), true);
    }

    @Override
    public SupportTicketResponse replyAsStaff(
            Long ticketId,
            String staffEmail,
            SupportTicketReplyRequest request
    ) {
        User staff = requireSupportStaff(staffEmail);
        SupportTicket ticket = requireTicket(ticketId);
        assertActive(ticket);
        if (ticket.getAssignee() == null) {
            ticket.setAssignee(staff);
        }
        saveMessage(ticket, staff, request.getMessage());
        ticket.setStatus(SupportTicketStatus.WAITING_FOR_LEARNER);
        ticket = ticketRepository.save(ticket);
        notifyLearner(ticket, "Support đã phản hồi ticket #" + ticket.getId());
        return toResponse(ticket, true);
    }

    @Override
    public SupportTicketResponse updateAsStaff(
            Long ticketId,
            String staffEmail,
            UpdateSupportTicketRequest request
    ) {
        User staff = requireSupportStaff(staffEmail);
        if (request.getStatus() == null && request.getPriority() == null) {
            throw new IllegalArgumentException("Cần cung cấp trạng thái hoặc độ ưu tiên cần cập nhật.");
        }
        SupportTicket ticket = requireTicket(ticketId);
        SupportTicketStatus previousStatus = ticket.getStatus();
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            applyStaffStatus(ticket, staff, request.getStatus());
        }
        ticket = ticketRepository.save(ticket);
        if (request.getStatus() != null && request.getStatus() != previousStatus) {
            notifyLearner(ticket, "Ticket #" + ticket.getId() + " đã được cập nhật trạng thái");
        }
        return toResponse(ticket, true);
    }

    private void applyStaffStatus(SupportTicket ticket, User staff, SupportTicketStatus status) {
        ticket.setStatus(status);
        if (status == SupportTicketStatus.RESOLVED || status == SupportTicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
            ticket.setResolvedBy(staff);
        } else {
            ticket.setResolvedAt(null);
            ticket.setResolvedBy(null);
        }
        if (status == SupportTicketStatus.IN_PROGRESS && ticket.getAssignee() == null) {
            ticket.setAssignee(staff);
        }
    }

    private void notifyLearner(SupportTicket ticket, String title) {
        appNotificationService.createForUser(
                ticket.getRequester(),
                "SUPPORT_TICKET_UPDATED",
                title,
                ticket.getSubject(),
                Map.of("ticketId", ticket.getId(), "path", "/support")
        );
    }

    private SupportTicketMessage saveMessage(SupportTicket ticket, User author, String body) {
        return messageRepository.save(SupportTicketMessage.builder()
                .ticket(ticket)
                .author(author)
                .body(body.trim())
                .build());
    }

    private User requireLearner(String email) {
        User user = requireUser(email);
        if (!user.hasRole(RoleCodes.LEARNER)) {
            throw new IllegalArgumentException("Chỉ học viên mới có thể sử dụng cổng gửi yêu cầu hỗ trợ.");
        }
        return user;
    }

    private User requireSupportStaff(String email) {
        User user = requireUser(email);
        if (!user.hasAnyRoleCodes(SUPPORT_STAFF_ROLES)) {
            throw new IllegalArgumentException("Bạn không có quyền xử lý support ticket.");
        }
        return user;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
    }

    private SupportTicket requireOwnedTicket(Long ticketId, User learner) {
        SupportTicket ticket = requireTicket(ticketId);
        if (!ticket.getRequester().getId().equals(learner.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập ticket này.");
        }
        return ticket;
    }

    private SupportTicket requireTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy support ticket."));
    }

    private void assertActive(SupportTicket ticket) {
        if (isTerminal(ticket)) {
            throw new IllegalArgumentException("Ticket đã hoàn tất. Hãy mở lại ticket trước khi phản hồi.");
        }
    }

    private boolean isTerminal(SupportTicket ticket) {
        return ticket.getStatus() == SupportTicketStatus.RESOLVED
                || ticket.getStatus() == SupportTicketStatus.CLOSED;
    }

    private SupportTicketResponse toResponse(SupportTicket ticket, boolean includeMessages) {
        List<SupportTicketMessageResponse> messages = includeMessages
                ? messageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
                        .map(message -> toMessageResponse(message, ticket))
                        .toList()
                : List.of();
        return SupportTicketResponse.builder()
                .id(ticket.getId())
                .subject(ticket.getSubject())
                .category(ticket.getCategory())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .requesterId(ticket.getRequester().getId())
                .requesterName(ticket.getRequester().getFullName())
                .requesterEmail(ticket.getRequester().getEmail())
                .assigneeId(ticket.getAssignee() == null ? null : ticket.getAssignee().getId())
                .assigneeName(ticket.getAssignee() == null ? null : ticket.getAssignee().getFullName())
                .resolvedAt(ticket.getResolvedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .messages(messages)
                .build();
    }

    private SupportTicketMessageResponse toMessageResponse(
            SupportTicketMessage message,
            SupportTicket ticket
    ) {
        return SupportTicketMessageResponse.builder()
                .id(message.getId())
                .authorId(message.getAuthor().getId())
                .authorName(message.getAuthor().getFullName())
                .staffMessage(!message.getAuthor().getId().equals(ticket.getRequester().getId()))
                .body(message.getBody())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
