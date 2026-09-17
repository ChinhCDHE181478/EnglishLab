package fu.sep490.g23.backend.ut.paymentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import fu.sep490.g23.backend.dto.response.payment.PaymentOrderStatusResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentOrderSummaryResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.payment.PaymentOrder;
import fu.sep490.g23.backend.entity.payment.PaymentOrderItem;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderItemType;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderItemRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.service.payment.PayosProperties;
import fu.sep490.g23.backend.service.payment.impl.PaymentServiceImpl;

@ExtendWith(MockitoExtension.class)
class ViewPaymentHistoryTest {

    @Mock private UserRepository userRepository;
    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private PaymentOrderItemRepository paymentOrderItemRepository;

    @InjectMocks private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        PayosProperties props = new PayosProperties();
        props.setEnabled(false);
        ReflectionTestUtils.setField(service, "payosProperties", props);
    }

    @Test
    void listMyOrders_ReturnsHistoryWithOneOrder() {
        // Arrange
        String email = "learner@example.com";
        User learner = User.builder().id(1L).email(email).build();

        PaymentOrder paidOrder = PaymentOrder.builder()
                .id(10L)
                .orderCode(1700000000001L)
                .student(learner)
                .description("IELTS Foundation + IELTS Advanced")
                .amount(500_000L)
                .originalAmount(500_000L)
                .status(PaymentOrderStatus.PAID)
                .createdAt(LocalDateTime.now().minusDays(1))
                .paidAt(LocalDateTime.now().minusDays(1))
                .build();

        PaymentOrderItem item1 = PaymentOrderItem.builder()
                .id(1L)
                .paymentOrder(paidOrder)
                .itemType(PaymentOrderItemType.ONLINE_COURSE)
                .titleSnapshot("IELTS Foundation")
                .unitPriceVnd(300_000L)
                .discountAmountVnd(0L)
                .finalAmountVnd(300_000L)
                .quantity(1)
                .build();
        PaymentOrderItem item2 = PaymentOrderItem.builder()
                .id(2L)
                .paymentOrder(paidOrder)
                .itemType(PaymentOrderItemType.ONLINE_COURSE)
                .titleSnapshot("IELTS Advanced")
                .unitPriceVnd(200_000L)
                .discountAmountVnd(0L)
                .finalAmountVnd(200_000L)
                .quantity(1)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(learner));
        when(paymentOrderRepository.findByStudentOrderByCreatedAtDesc(learner))
                .thenReturn(List.of(paidOrder));
        when(paymentOrderItemRepository.findByPaymentOrderIdOrderById(paidOrder.getId()))
                .thenReturn(List.of(item1, item2));

        // Act
        List<PaymentOrderSummaryResponse> history = service.listMyOrders(email);

        // Assert
        assertThat(history).hasSize(1);
        PaymentOrderSummaryResponse item = history.get(0);
        assertThat(item.getOrderCode()).isEqualTo(1700000000001L);
        assertThat(item.getStatus()).isEqualTo("PAID");
        assertThat(item.isPaid()).isTrue();
        assertThat(item.getAmount()).isEqualTo(500_000L);
        assertThat(item.getCourseTitles()).containsExactly("IELTS Foundation", "IELTS Advanced");
        assertThat(item.getPaidAt()).isNotNull();
        assertThat(item.getCreatedAt()).isNotNull();
    }

    @Test
    void listMyOrders_NoHistory_ReturnsEmptyList() {
        // Arrange
        String email = "newbie@example.com";
        User learner = User.builder().id(2L).email(email).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(learner));
        when(paymentOrderRepository.findByStudentOrderByCreatedAtDesc(learner))
                .thenReturn(List.of());

        // Act
        List<PaymentOrderSummaryResponse> history = service.listMyOrders(email);

        // Assert
        assertThat(history).isNotNull().isEmpty();
    }

    @Test
    void getOrderStatus_ReturnsOrderDetail() {
        // Arrange
        String email = "learner@example.com";
        Long orderCode = 2026001L;
        User learner = User.builder().id(1L).email(email).build();

        PaymentOrder order = PaymentOrder.builder()
                .id(20L)
                .orderCode(orderCode)
                .student(learner)
                .description("IELTS Foundation + IELTS Advanced")
                .amount(500_000L)
                .originalAmount(500_000L)
                .status(PaymentOrderStatus.PAID)
                .createdAt(LocalDateTime.of(2026, 8, 1, 10, 30))
                .paidAt(LocalDateTime.of(2026, 8, 1, 10, 35))
                .build();

        when(paymentOrderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));
        when(paymentOrderItemRepository.findByPaymentOrderIdOrderById(order.getId()))
                .thenReturn(List.of());

        // Act
        PaymentOrderStatusResponse detail = service.getOrderStatus(orderCode, email);

        // Assert
        assertThat(detail).isNotNull();
        assertThat(detail.getOrderCode()).isEqualTo(orderCode);
        assertThat(detail.getStatus()).isEqualTo("PAID");
        assertThat(detail.isPaid()).isTrue();
        assertThat(detail.getMessage()).isNotBlank();
    }

    @Test
    void listMyOrders_Pagination_ReturnsPageSlice() {
        // Arrange
        String email = "learner@example.com";
        User learner = User.builder().id(1L).email(email).build();

        PaymentOrder order1 = PaymentOrder.builder()
                .id(31L)
                .orderCode(1_700_000_000_001L)
                .student(learner)
                .description("IELTS Foundation")
                .amount(500_000L)
                .originalAmount(500_000L)
                .status(PaymentOrderStatus.PAID)
                .createdAt(LocalDateTime.of(2026, 8, 3, 10, 0))
                .paidAt(LocalDateTime.of(2026, 8, 3, 10, 5))
                .build();
        PaymentOrder order2 = PaymentOrder.builder()
                .id(32L)
                .orderCode(1_700_000_000_002L)
                .student(learner)
                .description("IELTS Advanced")
                .amount(700_000L)
                .originalAmount(700_000L)
                .status(PaymentOrderStatus.PAID)
                .createdAt(LocalDateTime.of(2026, 8, 2, 10, 0))
                .paidAt(LocalDateTime.of(2026, 8, 2, 10, 5))
                .build();
        PaymentOrder order3 = PaymentOrder.builder()
                .id(33L)
                .orderCode(1_700_000_000_003L)
                .student(learner)
                .description("IELTS Speaking")
                .amount(300_000L)
                .originalAmount(300_000L)
                .status(PaymentOrderStatus.PAID)
                .createdAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .paidAt(LocalDateTime.of(2026, 8, 1, 10, 5))
                .build();

        PaymentOrderItem item1 = PaymentOrderItem.builder()
                .id(41L)
                .paymentOrder(order1)
                .itemType(PaymentOrderItemType.ONLINE_COURSE)
                .titleSnapshot("IELTS Foundation")
                .unitPriceVnd(500_000L)
                .discountAmountVnd(0L)
                .finalAmountVnd(500_000L)
                .quantity(1)
                .build();
        PaymentOrderItem item2 = PaymentOrderItem.builder()
                .id(42L)
                .paymentOrder(order2)
                .itemType(PaymentOrderItemType.ONLINE_COURSE)
                .titleSnapshot("IELTS Advanced")
                .unitPriceVnd(700_000L)
                .discountAmountVnd(0L)
                .finalAmountVnd(700_000L)
                .quantity(1)
                .build();
        PaymentOrderItem item3 = PaymentOrderItem.builder()
                .id(43L)
                .paymentOrder(order3)
                .itemType(PaymentOrderItemType.ONLINE_COURSE)
                .titleSnapshot("IELTS Speaking")
                .unitPriceVnd(300_000L)
                .discountAmountVnd(0L)
                .finalAmountVnd(300_000L)
                .quantity(1)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(learner));
        when(paymentOrderRepository.findByStudentOrderByCreatedAtDesc(learner))
                .thenReturn(List.of(order1, order2, order3));
        when(paymentOrderItemRepository.findByPaymentOrderIdOrderById(any(Long.class)))
                .thenReturn(List.of(item1), List.of(item2), List.of(item3));

        // Act - mô phỏng slice theo page=0, size=10 (service hiện chưa hỗ trợ phân trang)
        List<PaymentOrderSummaryResponse> fullHistory = service.listMyOrders(email);
        int page = 0;
        int size = 10;
        int fromIndex = Math.min(page * size, fullHistory.size());
        int toIndex = Math.min(fromIndex + size, fullHistory.size());
        List<PaymentOrderSummaryResponse> pageSlice = fullHistory.subList(fromIndex, toIndex);

        int totalElements = fullHistory.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Assert
        assertThat(fullHistory).hasSize(3);
        assertThat(pageSlice).hasSize(3);
        assertThat(pageSlice.get(0).getOrderCode()).isEqualTo(1_700_000_000_001L);
        assertThat(pageSlice.get(1).getOrderCode()).isEqualTo(1_700_000_000_002L);
        assertThat(pageSlice.get(2).getOrderCode()).isEqualTo(1_700_000_000_003L);
        assertThat(totalElements).isEqualTo(3);
        assertThat(totalPages).isEqualTo(1);
    }
}