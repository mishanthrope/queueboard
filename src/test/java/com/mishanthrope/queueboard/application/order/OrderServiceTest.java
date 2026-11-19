package com.mishanthrope.queueboard.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mishanthrope.queueboard.domain.common.PageResult;
import com.mishanthrope.queueboard.domain.order.DuplicatePosterCheckError;
import com.mishanthrope.queueboard.domain.order.InvalidOrderError;
import com.mishanthrope.queueboard.domain.order.KitchenSettings;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderFactory;
import com.mishanthrope.queueboard.domain.order.OrderItem;
import com.mishanthrope.queueboard.domain.order.OrderNotFoundError;
import com.mishanthrope.queueboard.domain.order.OrderRepository;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import com.mishanthrope.queueboard.domain.order.PrepTimeCalculator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
        Clock clock = Clock.fixed(NOW, KYIV);
        var settings = new KitchenSettings(
                Duration.ofMinutes(7), Duration.ofMinutes(3), Duration.ofMinutes(5), 5);
        var factory = new OrderFactory(
                orderRepository, new PrepTimeCalculator(settings), SKEW, clock);
        orderService = new OrderService(orderRepository, factory, clock);
        lenient().when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static OrderItem item(String name, int quantity, String... modifiers) {
        return new OrderItem(null, name, quantity, List.of(modifiers));
    }

    private static List<OrderItem> oneItem() {
        return List.of(item("Шаурма", 1));
    }

    private static Order order(long id, OrderStatus status) {
        return Order.reconstitute(
                id, 1000 + id, (int) id, status, null, NOW.minusSeconds(600),
                NOW.plusSeconds(600), NOW.minusSeconds(590), null, null, null, null,
                oneItem());
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
                Arguments.of("empty items", 100L, NOW, null, List.of()));
    }

    static Stream<Arguments> validIntakes() {
        return Stream.of(
                Arguments.of("paidAt exactly at the skew limit", NOW.plus(SKEW), null,
                        oneItem()),
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
    void createFromPoster_withInvalidData_throwsInvalidOrderAndSavesNothing(
            String description,
            long posterCheckId,
            Instant paidAt,
            String comment,
            List<OrderItem> items) {
        assertThatThrownBy(() ->
                orderService.createFromPoster(posterCheckId, paidAt, comment, items))
                .isInstanceOf(InvalidOrderError.class);

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
        verify(orderRepository).save(order);
    }

    @Test
    void createFromPoster_whenCheckIdIsUsed_throwsDuplicateAndSavesNothing() {
        when(orderRepository.existsByPosterCheckId(100L)).thenReturn(true);

        assertThatThrownBy(() -> create(NOW, null, oneItem()))
                .isInstanceOf(DuplicatePosterCheckError.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createFromPoster_whenNoOrdersToday_assignsDisplayNumberOne() {
        Order order = create(NOW, null, oneItem());

        assertThat(order.getDisplayNumber()).isEqualTo(1);
        verify(orderRepository).findMaxDisplayNumberCreatedSince(START_OF_DAY);
    }

    @Test
    void createFromPoster_whenOrdersExistToday_assignsMaxPlusOne() {
        when(orderRepository.findMaxDisplayNumberCreatedSince(START_OF_DAY))
                .thenReturn(Optional.of(7));

        assertThat(create(NOW, null, oneItem()).getDisplayNumber()).isEqualTo(8);
    }

    @Test
    void createFromPoster_whenQueueIsEmpty_estimatesFromNow() {
        assertThat(create(NOW, null, oneItem()).getEstimatedReadyAt())
                .isEqualTo(NOW.plus(ONE_ITEM_TIME));
    }

    @Test
    void createFromPoster_whenQueueTailIsInThePast_estimatesFromNow() {
        when(orderRepository.findLatestEstimatedReadyAtInQueue())
                .thenReturn(Optional.of(NOW.minusSeconds(60)));

        assertThat(create(NOW, null, oneItem()).getEstimatedReadyAt())
                .isEqualTo(NOW.plus(ONE_ITEM_TIME));
    }

    @Test
    void createFromPoster_whenQueueTailIsInTheFuture_estimatesFromTail() {
        Instant tail = NOW.plus(Duration.ofMinutes(10));
        when(orderRepository.findLatestEstimatedReadyAtInQueue()).thenReturn(Optional.of(tail));

        assertThat(create(NOW, null, oneItem()).getEstimatedReadyAt())
                .isEqualTo(tail.plus(ONE_ITEM_TIME));
    }

    @Test
    void startCooking_fromNew_savesCookingOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1, OrderStatus.NEW)));

        Order order = orderService.startCooking(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COOKING);
        assertThat(order.getStartedAt()).isEqualTo(NOW);
        verify(orderRepository).save(order);
    }

    @Test
    void markReady_fromCooking_savesReadyOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1, OrderStatus.COOKING)));

        Order order = orderService.markReady(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(order.getReadyAt()).isEqualTo(NOW);
        verify(orderRepository).save(order);
    }

    @Test
    void issue_fromReady_savesIssuedOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1, OrderStatus.READY)));

        Order order = orderService.issue(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ISSUED);
        assertThat(order.getIssuedAt()).isEqualTo(NOW);
        verify(orderRepository).save(order);
    }

    @Test
    void cancel_fromNew_savesCancelledOrder() {
        Order order = order(1, OrderStatus.NEW);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.cancel(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isEqualTo(NOW);
        verify(orderRepository).save(order);
    }

    @Test
    void restore_fromCancelledWithoutStart_savesCookingOrderStartedNow() {
        Order cancelled = order(1, OrderStatus.NEW);
        cancelled.cancel(NOW.minusSeconds(60));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(cancelled));

        Order order = orderService.restore(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COOKING);
        assertThat(order.getCancelledAt()).isNull();
        assertThat(order.getStartedAt()).isEqualTo(NOW);
        verify(orderRepository).save(order);
    }

    @Test
    void issue_fromNew_throwsAndSavesNothing() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1, OrderStatus.NEW)));

        assertThatThrownBy(() -> orderService.issue(1L))
                .hasMessage("Cannot issue order in status NEW");

        verify(orderRepository, never()).save(any());
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
    void action_withUnknownId_throwsOrderNotFound(String name, Consumer<OrderService> call) {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());
        ThrowingCallable callable = () -> call.accept(orderService);

        assertThatThrownBy(callable)
                .isInstanceOf(OrderNotFoundError.class)
                .hasMessage("Order 99 not found");
    }

    @Test
    void shiftPrepTime_movesEstimateOfEveryOrderInTheQueueAndSavesIt() {
        Order fresh = order(1, OrderStatus.NEW);
        Order cooking = order(2, OrderStatus.COOKING);
        when(orderRepository.findInQueue()).thenReturn(List.of(fresh, cooking));
        Instant freshBefore = fresh.getEstimatedReadyAt();
        Instant cookingBefore = cooking.getEstimatedReadyAt();

        orderService.shiftPrepTime(5);

        assertThat(fresh.getEstimatedReadyAt()).isEqualTo(freshBefore.plusSeconds(300));
        assertThat(cooking.getEstimatedReadyAt()).isEqualTo(cookingBefore.plusSeconds(300));
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void shiftPrepTime_withNegativeDelta_movesEstimateBack() {
        Order cooking = order(2, OrderStatus.COOKING);
        Instant before = cooking.getEstimatedReadyAt();
        when(orderRepository.findInQueue()).thenReturn(List.of(cooking));

        orderService.shiftPrepTime(-10);

        assertThat(cooking.getEstimatedReadyAt()).isEqualTo(before.minusSeconds(600));
    }

    @Test
    void shiftPrepTime_withEmptyQueue_savesNothing() {
        orderService.shiftPrepTime(5);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrder_returnsOrderFromRepository() {
        Order order = order(1, OrderStatus.READY);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThat(orderService.getOrder(1L)).isSameAs(order);
    }

    @Test
    void getKitchenQueue_returnsActiveOrdersFromRepository() {
        List<Order> active = List.of(order(1, OrderStatus.NEW), order(2, OrderStatus.READY));
        when(orderRepository.findActive()).thenReturn(active);

        assertThat(orderService.getKitchenQueue()).isEqualTo(active);
    }

    @Test
    void getHistory_delegatesFiltersAndPagingToRepository() {
        LocalDate date = LocalDate.of(2026, 11, 18);
        var page = new PageResult<>(List.of(order(1, OrderStatus.ISSUED)), 2, 10, 21L, 3);
        when(orderRepository.findHistory(date, OrderStatus.ISSUED, 2, 10)).thenReturn(page);

        assertThat(orderService.getHistory(date, OrderStatus.ISSUED, 2, 10)).isSameAs(page);
    }

    @Test
    void createFromPoster_savesTheOrderBuiltByTheFactory() {
        create(NOW, "з собою", List.of(item("Шаурма", 2, "сир")));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
        assertThat(saved.getValue().getPosterCheckId()).isEqualTo(100L);
        assertThat(saved.getValue().getItems()).extracting(OrderItem::productName)
                .containsExactly("Шаурма");
    }
}
