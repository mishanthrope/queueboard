package com.mishanthrope.queueboard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mishanthrope.queueboard.common.exception.BadRequestException;
import com.mishanthrope.queueboard.common.exception.NotFoundException;
import com.mishanthrope.queueboard.order.config.OrdersProperties;
import com.mishanthrope.queueboard.order.dto.OrderResponse;
import com.mishanthrope.queueboard.order.mapper.OrderMapperImpl;
import com.mishanthrope.queueboard.order.model.Order;
import com.mishanthrope.queueboard.order.model.OrderItem;
import com.mishanthrope.queueboard.order.model.OrderStatus;
import com.mishanthrope.queueboard.order.repository.OrderRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final Instant START_OF_DAY = Instant.parse("2026-11-17T22:00:00Z");
    private static final Duration SKEW = Duration.ofMinutes(2);
    private static final Duration ONE_ITEM_TIME = Duration.ofMinutes(15);

    @Mock
    private OrderRepository orderRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        var prepTime = new OrdersProperties.PrepTime(
                Duration.ofMinutes(7), Duration.ofMinutes(3), Duration.ofMinutes(5), 5);
        orderService = new OrderService(
                orderRepository,
                new OrderMapperImpl(),
                new PrepTimeCalculator(new OrdersProperties(SKEW, prepTime)),
                new OrdersProperties(SKEW, prepTime),
                Clock.fixed(NOW, KYIV));
        lenient().when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static OrderItem item(String name, int quantity, String... modifiers) {
        var item = new OrderItem();
        item.setProductName(name);
        item.setQuantity(quantity);
        item.setModifiers(List.of(modifiers));
        return item;
    }

    private static List<OrderItem> oneItem() {
        return List.of(item("Шаурма", 1));
    }

    private static Order order(long id, OrderStatus status) {
        var order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setDisplayNumber((int) id);
        order.setPaidAt(NOW.minusSeconds(600));
        order.setEstimatedReadyAt(NOW.plusSeconds(600));
        return order;
    }

    private Order create(Instant paidAt, String comment, List<OrderItem> items) {
        return orderService.createFromPoster(100L, paidAt, comment, items);
    }

    static Stream<Arguments> invalidIntakes() {
        return Stream.of(
                Arguments.of("zero check id", 0L, NOW, null, oneItem()),
                Arguments.of("negative check id", -5L, NOW, null, oneItem()),
                Arguments.of("paidAt beyond skew", 100L, NOW.plus(SKEW).plusSeconds(1), null,
                        oneItem()),
                Arguments.of("comment over 500", 100L, NOW, "x".repeat(501), oneItem()),
                Arguments.of("empty items", 100L, NOW, null, List.of()),
                Arguments.of("empty product name", 100L, NOW, null, List.of(item("", 1))),
                Arguments.of("blank product name", 100L, NOW, null, List.of(item("   ", 1))),
                Arguments.of("null product name", 100L, NOW, null, List.of(item(null, 1))),
                Arguments.of("product name over 100", 100L, NOW, null,
                        List.of(item("x".repeat(101), 1))),
                Arguments.of("quantity 0", 100L, NOW, null, List.of(item("Шаурма", 0))),
                Arguments.of("quantity 100", 100L, NOW, null, List.of(item("Шаурма", 100))),
                Arguments.of("modifier over 100", 100L, NOW, null,
                        List.of(item("Шаурма", 1, "x".repeat(101)))));
    }

    static Stream<Arguments> validIntakes() {
        return Stream.of(
                Arguments.of("paidAt exactly at the skew limit", NOW.plus(SKEW), null, oneItem()),
                Arguments.of("paidAt slightly in the future", NOW.plusSeconds(30), null,
                        oneItem()),
                Arguments.of("paidAt in the past", NOW.minusSeconds(3600), null, oneItem()),
                Arguments.of("comment of 500", NOW, "x".repeat(500), oneItem()),
                Arguments.of("quantity 99", NOW, null, List.of(item("Шаурма", 99))),
                Arguments.of("modifier of 100", NOW, null,
                        List.of(item("Шаурма", 1, "x".repeat(100)))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidIntakes")
    void createFromPoster_withInvalidData_throwsBadRequest(
            String description,
            long posterCheckId,
            Instant paidAt,
            String comment,
            List<OrderItem> items) {
        assertThatThrownBy(() ->
                orderService.createFromPoster(posterCheckId, paidAt, comment, items))
                .isInstanceOf(BadRequestException.class);

        verify(orderRepository, never()).save(any());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validIntakes")
    void createFromPoster_withBoundaryData_savesNewOrder(
            String description, Instant paidAt, String comment, List<OrderItem> items) {
        Order order = create(paidAt, comment, items);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getPaidAt()).isEqualTo(paidAt);
        assertThat(order.getComment()).isEqualTo(comment);
        assertThat(order.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void createFromPoster_whenNoOrdersToday_assignsDisplayNumberOne() {
        Order order = create(NOW, null, oneItem());

        assertThat(order.getDisplayNumber()).isEqualTo(1);
        verify(orderRepository)
                .findTopByCreatedAtGreaterThanEqualOrderByDisplayNumberDesc(START_OF_DAY);
    }

    @Test
    void createFromPoster_whenOrdersExistToday_assignsMaxPlusOne() {
        when(orderRepository.findTopByCreatedAtGreaterThanEqualOrderByDisplayNumberDesc(
                START_OF_DAY)).thenReturn(Optional.of(order(7, OrderStatus.NEW)));

        Order order = create(NOW, null, oneItem());

        assertThat(order.getDisplayNumber()).isEqualTo(8);
    }

    @Test
    void createFromPoster_whenQueueIsEmpty_estimatesFromNow() {
        Order order = create(NOW, null, oneItem());

        assertThat(order.getEstimatedReadyAt()).isEqualTo(NOW.plus(ONE_ITEM_TIME));
    }

    @Test
    void createFromPoster_whenQueueTailIsInThePast_estimatesFromNow() {
        when(orderRepository.findLatestEstimatedReadyAt(any()))
                .thenReturn(Optional.of(NOW.minusSeconds(60)));

        Order order = create(NOW, null, oneItem());

        assertThat(order.getEstimatedReadyAt()).isEqualTo(NOW.plus(ONE_ITEM_TIME));
    }

    @Test
    void createFromPoster_whenQueueTailIsInTheFuture_estimatesFromTail() {
        Instant tail = NOW.plus(Duration.ofMinutes(10));
        when(orderRepository.findLatestEstimatedReadyAt(any())).thenReturn(Optional.of(tail));

        Order order = create(NOW, null, oneItem());

        assertThat(order.getEstimatedReadyAt()).isEqualTo(tail.plus(ONE_ITEM_TIME));
    }

    @Test
    void createFromPoster_looksAtTailOfNewAndCookingOrdersOnly() {
        create(NOW, null, oneItem());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<OrderStatus>> statuses =
                ArgumentCaptor.forClass(Collection.class);
        verify(orderRepository).findLatestEstimatedReadyAt(statuses.capture());
        assertThat(statuses.getValue())
                .containsExactlyInAnyOrder(OrderStatus.NEW, OrderStatus.COOKING);
    }

    @Test
    void startCooking_fromNew_setsStatusAndStartedAt() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1, OrderStatus.NEW)));

        OrderResponse response = orderService.startCooking(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.COOKING);
        assertThat(response.startedAt()).isEqualTo(NOW);
    }

    @Test
    void markReady_fromCooking_setsStatusAndReadyAt() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1, OrderStatus.COOKING)));

        OrderResponse response = orderService.markReady(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.READY);
        assertThat(response.readyAt()).isEqualTo(NOW);
    }

    @Test
    void issue_fromReady_setsStatusAndIssuedAt() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1, OrderStatus.READY)));

        OrderResponse response = orderService.issue(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.ISSUED);
        assertThat(response.issuedAt()).isEqualTo(NOW);
    }

    @Test
    void cancel_fromNew_setsStatusAndCancelledAt() {
        Order order = order(1, OrderStatus.NEW);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.cancel(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void restore_fromReady_clearsReadyAtAndKeepsStartedAt() {
        Order order = order(1, OrderStatus.READY);
        Instant startedAt = NOW.minusSeconds(300);
        order.setStartedAt(startedAt);
        order.setReadyAt(NOW.minusSeconds(60));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.restore(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.COOKING);
        assertThat(response.readyAt()).isNull();
        assertThat(response.cancelledAt()).isNull();
        assertThat(response.startedAt()).isEqualTo(startedAt);
    }

    @Test
    void restore_fromCancelledWithoutStart_clearsCancelledAtAndSetsStartedAt() {
        Order order = order(1, OrderStatus.CANCELLED);
        order.setCancelledAt(NOW.minusSeconds(60));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.restore(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.COOKING);
        assertThat(response.cancelledAt()).isNull();
        assertThat(response.startedAt()).isEqualTo(NOW);
    }

    static Stream<Arguments> actionsOnUnknownOrder() {
        return Stream.of(
                action("startCooking", service -> service.startCooking(99L)),
                action("markReady", service -> service.markReady(99L)),
                action("issue", service -> service.issue(99L)),
                action("cancel", service -> service.cancel(99L)),
                action("restore", service -> service.restore(99L)),
                action("getOrder", service -> service.getOrder(99L)));
    }

    private static Arguments action(String name, Consumer<OrderService> call) {
        return Arguments.of(name, call);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("actionsOnUnknownOrder")
    void action_withUnknownId_throwsNotFound(String name, Consumer<OrderService> call) {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());
        ThrowingCallable callable = () -> call.accept(orderService);

        assertThatThrownBy(callable)
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order 99 not found");
    }

    @Test
    void shiftPrepTime_movesEstimateOfNewAndCookingOrders() {
        Order fresh = order(1, OrderStatus.NEW);
        Order cooking = order(2, OrderStatus.COOKING);
        when(orderRepository.findAllByStatusInOrderByPaidAtAsc(any()))
                .thenReturn(List.of(fresh, cooking));
        Instant freshBefore = fresh.getEstimatedReadyAt();
        Instant cookingBefore = cooking.getEstimatedReadyAt();

        orderService.shiftPrepTime(5);

        assertThat(fresh.getEstimatedReadyAt()).isEqualTo(freshBefore.plusSeconds(300));
        assertThat(cooking.getEstimatedReadyAt()).isEqualTo(cookingBefore.plusSeconds(300));
    }

    @Test
    void shiftPrepTime_withNegativeDelta_movesEstimateBack() {
        Order cooking = order(2, OrderStatus.COOKING);
        Instant before = cooking.getEstimatedReadyAt();
        when(orderRepository.findAllByStatusInOrderByPaidAtAsc(any()))
                .thenReturn(List.of(cooking));

        orderService.shiftPrepTime(-10);

        assertThat(cooking.getEstimatedReadyAt()).isEqualTo(before.minusSeconds(600));
    }

    @Test
    void shiftPrepTime_selectsOnlyNewAndCookingOrders() {
        orderService.shiftPrepTime(5);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<OrderStatus>> statuses =
                ArgumentCaptor.forClass(Collection.class);
        verify(orderRepository).findAllByStatusInOrderByPaidAtAsc(statuses.capture());
        assertThat(statuses.getValue())
                .containsExactlyInAnyOrder(OrderStatus.NEW, OrderStatus.COOKING);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"ISSUED", "CANCELLED"})
    void getOrder_whenFinished_hasNoEstimatedReadyAt(OrderStatus status) {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1, status)));

        assertThat(orderService.getOrder(1L).estimatedReadyAt()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"NEW", "COOKING", "READY"})
    void getOrder_whenActive_returnsStoredEstimatedReadyAt(OrderStatus status) {
        Order order = order(1, status);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThat(orderService.getOrder(1L).estimatedReadyAt())
                .isEqualTo(order.getEstimatedReadyAt());
    }

    @Test
    void getKitchenQueue_returnsResponsesFromRepository() {
        when(orderRepository.findAllByStatusInOrderByPaidAtAsc(any()))
                .thenReturn(List.of(order(1, OrderStatus.NEW), order(2, OrderStatus.READY)));

        assertThat(orderService.getKitchenQueue()).extracting(OrderResponse::id)
                .containsExactly(1L, 2L);
    }
}
