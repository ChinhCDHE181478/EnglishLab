package fu.sap490.g23.backend.service.support;

import fu.sap490.g23.backend.dto.request.support.CreateSupportTicketRequest;
import fu.sap490.g23.backend.dto.request.support.LearnerSupportTicketStatusRequest;
import fu.sap490.g23.backend.dto.request.support.SupportTicketReplyRequest;
import fu.sap490.g23.backend.dto.request.support.UpdateSupportTicketRequest;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.entity.support.SupportTicket;
import fu.sap490.g23.backend.entity.support.enums.SupportTicketCategory;
import fu.sap490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sap490.g23.backend.entity.support.enums.SupportTicketStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.support.SupportTicketMessageRepository;
import fu.sap490.g23.backend.repository.support.SupportTicketRepository;
import fu.sap490.g23.backend.service.notification.AppNotificationService;
import fu.sap490.g23.backend.service.support.impl.SupportTicketServiceImpl;
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

    @BeforeEach
    void setUp() {
        service = new SupportTicketServiceImpl(
                ticketRepository,
                messageRepository,
                userRepository,
                appNotificationService
        );
        learner = user(1L, "learner@test.vn", "Learner", RoleEnum.LEARNER);
        otherLearner = user(2L, "other@test.vn", "Other", RoleEnum.LEARNER);
        manager = user(3L, "staff@test.vn", "Nhân viên đào tạo", RoleEnum.STAFF);
    }

    @Test
    void create_TrimsContentAndCreatesOpenNormalTicket() {
        CreateSupportTicketRequest request = new CreateSupportTicketRequest();
        request.setSubject("  Không truy cập được khóa học  ");
        request.setCategory(SupportTicketCategory.ONLINE_COURSE);
        request.setMessage("  Tôi không thể mở bài học đã mua.  ");
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
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
        assertThat(response.getStatus()).isEqualTo(SupportTicketStatus.OPEN);
        assertThat(response.getPriority()).isEqualTo(SupportTicketPriority.NORMAL);
        verify(messageRepository).save(any());
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
    void replyAsStaff_ClaimsTicketWaitsForLearnerAndNotifiesRequester() {
        SupportTicket ticket = ticket(22L, learner);
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

    private User user(Long id, String email, String name, RoleEnum role) {
        User user = User.builder().id(id).email(email).fullName(name).build();
        user.setRole(role);
        return user;
    }
}
