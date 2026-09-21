package fu.sep490.g23.backend.service.support;

import fu.sep490.g23.backend.dto.request.support.CreateSupportTicketRequest;
import fu.sep490.g23.backend.dto.request.support.LearnerSupportTicketStatusRequest;
import fu.sep490.g23.backend.dto.request.support.SupportTicketReplyRequest;
import fu.sep490.g23.backend.dto.request.support.UpdateSupportTicketRequest;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.entity.support.SupportTicket;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketCategory;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.support.SupportTicketMessageRepository;
import fu.sep490.g23.backend.repository.support.SupportTicketRepository;
import fu.sep490.g23.backend.service.notification.AppNotificationService;
import fu.sep490.g23.backend.service.support.impl.SupportTicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceImplTest {

    @Mock private SupportTicketRepository ticketRepository;
    @Mock private SupportTicketMessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private AppNotificationService appNotificationService;

    private SupportTicketServiceImpl service;
    private User learner;
    private User otherLearner;
    private User manager;
    private User secondStaff;

    @BeforeEach
    void setUp() {
        service = new SupportTicketServiceImpl(
                ticketRepository,
                messageRepository,
                userRepository,
                appNotificationService
        );
        learner = user(1L, "learner@test.vn", "Learner", RoleCodes.LEARNER);
        otherLearner = user(2L, "other@test.vn", "Other", RoleCodes.LEARNER);
        manager = user(3L, "staff@test.vn", "Nhân viên đào tạo", RoleCodes.STAFF);
        secondStaff = user(4L, "staff2@test.vn", "Nhân viên hỗ trợ", RoleCodes.STAFF);
    }

    @Test
    void create_TrimsContentAndCreatesOpenNormalTicket() {
        CreateSupportTicketRequest request = new CreateSupportTicketRequest();
        request.setSubject("  Không truy cập được khóa học  ");
        request.setCategory(SupportTicketCategory.ONLINE_COURSE);
        request.setMessage("  Tôi không thể mở bài học đã mua.  ");
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(userRepository.findEnabledByRoleCodeForUpdate(RoleCodes.STAFF)).thenReturn(List.of(manager));
        when(ticketRepository.findFirstByAssigneeInOrderByCreatedAtDescIdDesc(List.of(manager)))
                .thenReturn(Optional.empty());
        when(ticketRepository.save(any())).thenAnswer(invocation -> {
            SupportTicket ticket = invocation.getArgument(0);
            ticket.setId(11L);
            return ticket;
        });
        when(messageRepository.findByTicketIdOrderByCreatedAtAsc(11L)).thenReturn(List.of());

        var response = service.create(learner.getEmail(), request);

        ArgumentCaptor<SupportTicket> ticketCaptor = ArgumentCaptor.forClass(SupportTicket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getSubject()).isEqualTo("Không truy cập được khóa học");
        assertThat(ticketCaptor.getValue().getAssignee()).isEqualTo(manager);
        assertThat(response.getStatus()).isEqualTo(SupportTicketStatus.OPEN);
        assertThat(response.getPriority()).isEqualTo(SupportTicketPriority.NORMAL);
        verify(messageRepository).save(any());
    }

    @Test
    void create_AssignsNextStaffInRoundRobinOrder() {
        CreateSupportTicketRequest request = new CreateSupportTicketRequest();
        request.setSubject("Cần hỗ trợ thanh toán");
        request.setCategory(SupportTicketCategory.PAYMENT);
        request.setMessage("Tôi cần kiểm tra lại giao dịch thanh toán.");
        SupportTicket previous = ticket(10L, otherLearner);
        previous.setAssignee(manager);
        List<User> staffMembers = List.of(manager, secondStaff);
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(userRepository.findEnabledByRoleCodeForUpdate(RoleCodes.STAFF)).thenReturn(staffMembers);
        when(ticketRepository.findFirstByAssigneeInOrderByCreatedAtDescIdDesc(staffMembers))
                .thenReturn(Optional.of(previous));
        when(ticketRepository.save(any())).thenAnswer(invocation -> {
            SupportTicket ticket = invocation.getArgument(0);
            ticket.setId(12L);
            return ticket;
        });
        when(messageRepository.findByTicketIdOrderByCreatedAtAsc(12L)).thenReturn(List.of());

        var response = service.create(learner.getEmail(), request);

        assertThat(response.getAssigneeId()).isEqualTo(secondStaff.getId());
        verify(appNotificationService).createForUser(
                secondStaff,
                "SUPPORT_TICKET_ASSIGNED",
                "Yêu cầu hỗ trợ mới #12",
                "Cần hỗ trợ thanh toán",
                java.util.Map.of("ticketId", 12L, "path", "/staff/support-tickets")
        );
    }

    @Test
    void getMine_WhenTicketBelongsToAnotherLearner_IsRejected() {
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(ticket(20L, otherLearner)));

        assertThatThrownBy(() -> service.getMine(20L, learner.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không có quyền");
    }

    @Test
    void replyAsLearner_WhenTicketResolved_IsRejected() {
        SupportTicket ticket = ticket(21L, learner);
        ticket.setStatus(SupportTicketStatus.RESOLVED);
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(ticketRepository.findById(21L)).thenReturn(Optional.of(ticket));
        SupportTicketReplyRequest request = reply("Tôi cần hỗ trợ thêm.");

        assertThatThrownBy(() -> service.replyAsLearner(21L, learner.getEmail(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mở lại");
    }

    @Test
    void replyAsAssignedStaff_WaitsForLearnerAndNotifiesRequester() {
        SupportTicket ticket = ticket(22L, learner);
        ticket.setAssignee(manager);
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(ticketRepository.findById(22L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findByTicketIdOrderByCreatedAtAsc(22L)).thenReturn(List.of());

        var response = service.replyAsStaff(22L, manager.getEmail(), reply("Bạn vui lòng đăng nhập lại."));

        assertThat(response.getStatus()).isEqualTo(SupportTicketStatus.WAITING_FOR_LEARNER);
        assertThat(response.getAssigneeId()).isEqualTo(manager.getId());
        verify(appNotificationService).createForUser(any(), any(), any(), any(), any());
    }

    @Test
    void updateAsStaff_ResolvedRecordsResolver() {
        SupportTicket ticket = ticket(23L, learner);
        ticket.setAssignee(manager);
        UpdateSupportTicketRequest request = new UpdateSupportTicketRequest();
        request.setStatus(SupportTicketStatus.RESOLVED);
        request.setPriority(SupportTicketPriority.HIGH);
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(ticketRepository.findById(23L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findByTicketIdOrderByCreatedAtAsc(23L)).thenReturn(List.of());

        var response = service.updateAsStaff(23L, manager.getEmail(), request);

        assertThat(response.getStatus()).isEqualTo(SupportTicketStatus.RESOLVED);
        assertThat(response.getPriority()).isEqualTo(SupportTicketPriority.HIGH);
        assertThat(ticket.getResolvedBy()).isEqualTo(manager);
        assertThat(ticket.getResolvedAt()).isNotNull();
    }

    @Test
    void getForStaff_WhenAssignedToAnotherStaff_IsRejected() {
        SupportTicket ticket = ticket(25L, learner);
        ticket.setAssignee(secondStaff);
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(ticketRepository.findById(25L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.getForStaff(25L, manager.getEmail()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("nhân viên khác");
    }

    @Test
    void listQueue_ForStaffOnlyLoadsAssignedTickets() {
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(ticketRepository.findQueue(null, null, manager)).thenReturn(List.of());

        service.listQueue(manager.getEmail(), null, null);

        verify(ticketRepository).findQueue(null, null, manager);
    }

    @Test
    void getForStaff_ManagerCanInspectAnotherStaffTicket() {
        User supervisor = user(5L, "manager@test.vn", "Quản lý", RoleCodes.MANAGER);
        SupportTicket ticket = ticket(26L, learner);
        ticket.setAssignee(secondStaff);
        when(userRepository.findByEmail(supervisor.getEmail())).thenReturn(Optional.of(supervisor));
        when(ticketRepository.findById(26L)).thenReturn(Optional.of(ticket));
        when(messageRepository.findByTicketIdOrderByCreatedAtAsc(26L)).thenReturn(List.of());

        var response = service.getForStaff(26L, supervisor.getEmail());

        assertThat(response.getAssigneeId()).isEqualTo(secondStaff.getId());
    }

    @Test
    void claim_WhenTicketAlreadyAssigned_DoesNotReplaceOwner() {
        User supervisor = user(6L, "manager2@test.vn", "Quản lý", RoleCodes.MANAGER);
        SupportTicket ticket = ticket(27L, learner);
        ticket.setAssignee(secondStaff);
        when(userRepository.findByEmail(supervisor.getEmail())).thenReturn(Optional.of(supervisor));
        when(ticketRepository.findById(27L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.claim(27L, supervisor.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã được phân công");
        assertThat(ticket.getAssignee()).isEqualTo(secondStaff);
    }

    @Test
    void learnerCannotReopenClosedTicket() {
        SupportTicket ticket = ticket(24L, learner);
        ticket.setStatus(SupportTicketStatus.CLOSED);
        LearnerSupportTicketStatusRequest request = new LearnerSupportTicketStatusRequest();
        request.setStatus(SupportTicketStatus.OPEN);
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(ticketRepository.findById(24L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.updateMyStatus(24L, learner.getEmail(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không thể mở lại");
        assertThat(ticket.getStatus()).isEqualTo(SupportTicketStatus.CLOSED);
    }

    private SupportTicket ticket(Long id, User requester) {
        return SupportTicket.builder()
                .id(id)
                .requester(requester)
                .subject("Cần hỗ trợ")
                .category(SupportTicketCategory.OTHER)
                .status(SupportTicketStatus.OPEN)
                .priority(SupportTicketPriority.NORMAL)
                .build();
    }

    private SupportTicketReplyRequest reply(String message) {
        SupportTicketReplyRequest request = new SupportTicketReplyRequest();
        request.setMessage(message);
        return request;
    }

    private User user(Long id, String email, String name, String roleCode) {
        User user = User.builder().id(id).email(email).fullName(name).build();
        user.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(roleCode));
        return user;
    }
}
