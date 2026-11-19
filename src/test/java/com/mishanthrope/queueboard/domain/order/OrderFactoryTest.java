package com.mishanthrope.queueboard.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OrderFactoryTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final Instant START_OF_DAY = Instant.parse("2026-11-17T22:00:00Z");
    private static final Duration SKEW = Duration.ofMinutes(2);
    private static final Duration ONE_ITEM_TIME = Duration.ofMinutes(15);

    private InMemoryOrderRepository repository;
    private OrderFactory factory;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository(KYIV);
        var settings = new KitchenSettings(
                Duration.ofMinutes(7), Duration.ofMinutes(3), Duration.ofMinutes(5), 5);
        factory = new OrderFactory(
                repository, new PrepTimeCalculator(settings), SKEW, Clock.fixed(NOW, KYIV));
    }

    private static OrderItem item(int quantity) {
        return new OrderItem(null, "Шаурма", quantity, List.of());
    }

    private static List<OrderItem> oneItem() {
        return List.of(item(1));
    }

    private Order create(long checkId, Instant paidAt, String comment, List<OrderItem> items) {
        return factory.createFromPoster(checkId, paidAt, comment, items);
    }

    private Order create() {
        return create(100L, NOW, null, oneItem());
    }

    private void seed(
            long checkId, int displayNumber, OrderStatus status, Instant createdAt,
            Instant estimatedReadyAt) {
        repository.save(Order.reconstitute(
                null, checkId, displayNumber, status, null, createdAt.minusSeconds(5),
                estimatedReadyAt, createdAt, null, null, null, null, oneItem()));
    }

    static Stream<Arguments> invalidIntakes() {
        return Stream.of(
                Arguments.of("zero check id", 0L, NOW, null, oneItem(),
                        "posterCheckId must be positive"),
                Arguments.of("negative check id", -5L, NOW, null, oneItem(),
                        "posterCheckId must be positive"),
                Arguments.of("paidAt beyond skew", 100L, NOW.plus(SKEW).plusSeconds(1), null,
                        oneItem(), "paidAt must not be in the future"),
                Arguments.of("comment over 500", 100L, NOW, "x".repeat(501), oneItem(),
                        "comment must be at most 500 characters"),
                Arguments.of("empty items", 100L, NOW, null, List.of(),
                        "Order must contain at least one item"));
    }

    static Stream<Arguments> validIntakes() {
        return Stream.of(
                Arguments.of("paidAt exactly at the skew limit", NOW.plus(SKEW), null),
                Arguments.of("paidAt slightly in the future", NOW.plusSeconds(30), null),
                Arguments.of("paidAt in the past", NOW.minusSeconds(3600), null),
                Arguments.of("comment of 500", NOW, "x".repeat(500)),
                Arguments.of("comment of 1", NOW, "x"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidIntakes")
    void createFromPoster_withInvalidData_throwsInvalidOrder(
            String description, long checkId, Instant paidAt, String comment,
            List<OrderItem> items, String message) {
        assertThatThrownBy(() -> create(checkId, paidAt, comment, items))
                .isInstanceOf(InvalidOrderError.class)
                .hasMessage(message);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validIntakes")
    void createFromPoster_withBoundaryData_buildsNewOrder(
            String description, Instant paidAt, String comment) {
        Order order = create(100L, paidAt, comment, oneItem());

        assertThat(order.getId()).isNull();
        assertThat(order.getPosterCheckId()).isEqualTo(100L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getPaidAt()).isEqualTo(paidAt);
        assertThat(order.getComment()).isEqualTo(comment);
        assertThat(order.getCreatedAt()).isEqualTo(NOW);
        assertThat(order.getStartedAt()).isNull();
        assertThat(order.getReadyAt()).isNull();
        assertThat(order.getIssuedAt()).isNull();
        assertThat(order.getCancelledAt()).isNull();
    }

    @Test
    void createFromPoster_keepsItems() {
        Order order = create(100L, NOW, null, List.of(item(2), item(3)));

        assertThat(order.getItems()).extracting(OrderItem::quantity).containsExactly(2, 3);
    }

    @Test
    void createFromPoster_doesNotSaveTheOrder() {
        create();

        assertThat(repository.findActive()).isEmpty();
    }

    @Test
    void createFromPoster_whenCheckIdIsUsed_throwsDuplicate() {
        seed(100L, 1, OrderStatus.ISSUED, NOW.minusSeconds(60), NOW);

        assertThatThrownBy(this::create)
                .isInstanceOf(DuplicatePosterCheckError.class)
                .hasMessage("Order for Poster check 100 already exists");
    }

    @Test
    void createFromPoster_whenNoOrdersToday_assignsDisplayNumberOne() {
        assertThat(create().getDisplayNumber()).isEqualTo(1);
    }

    @Test
    void createFromPoster_whenOrdersExistToday_assignsMaxPlusOne() {
        seed(1L, 3, OrderStatus.ISSUED, START_OF_DAY, NOW);
        seed(2L, 7, OrderStatus.NEW, NOW.minusSeconds(60), NOW);
        seed(3L, 5, OrderStatus.CANCELLED, NOW.minusSeconds(120), NOW);

        assertThat(create().getDisplayNumber()).isEqualTo(8);
    }

    @Test
    void createFromPoster_ignoresOrdersFromBeforeLocalMidnight() {
        seed(1L, 40, OrderStatus.ISSUED, START_OF_DAY.minusSeconds(1), NOW);

        assertThat(create().getDisplayNumber()).isEqualTo(1);
    }

    @Test
    void createFromPoster_whenQueueIsEmpty_estimatesFromNow() {
        assertThat(create().getEstimatedReadyAt()).isEqualTo(NOW.plus(ONE_ITEM_TIME));
    }

    @Test
    void createFromPoster_whenQueueTailIsInThePast_estimatesFromNow() {
        seed(1L, 1, OrderStatus.NEW, NOW.minusSeconds(600), NOW.minusSeconds(60));

        assertThat(create().getEstimatedReadyAt()).isEqualTo(NOW.plus(ONE_ITEM_TIME));
    }

    @Test
    void createFromPoster_whenQueueTailIsInTheFuture_estimatesFromTail() {
        Instant tail = NOW.plus(Duration.ofMinutes(10));
        seed(1L, 1, OrderStatus.COOKING, NOW.minusSeconds(600), tail);

        assertThat(create().getEstimatedReadyAt()).isEqualTo(tail.plus(ONE_ITEM_TIME));
    }

    @Test
    void createFromPoster_takesTheLatestEstimateInTheQueue() {
        seed(1L, 1, OrderStatus.NEW, NOW.minusSeconds(600), NOW.plus(Duration.ofMinutes(5)));
        seed(2L, 2, OrderStatus.COOKING, NOW.minusSeconds(500), NOW.plus(Duration.ofMinutes(20)));
        seed(3L, 3, OrderStatus.NEW, NOW.minusSeconds(400), NOW.plus(Duration.ofMinutes(8)));

        assertThat(create().getEstimatedReadyAt())
                .isEqualTo(NOW.plus(Duration.ofMinutes(20)).plus(ONE_ITEM_TIME));
    }

    @Test
    void createFromPoster_ignoresReadyIssuedAndCancelledOrdersInTheTail() {
        Instant far = NOW.plus(Duration.ofHours(1));
        seed(1L, 1, OrderStatus.READY, NOW.minusSeconds(600), far);
        seed(2L, 2, OrderStatus.ISSUED, NOW.minusSeconds(500), far);
        seed(3L, 3, OrderStatus.CANCELLED, NOW.minusSeconds(400), far);

        assertThat(create().getEstimatedReadyAt()).isEqualTo(NOW.plus(ONE_ITEM_TIME));
    }

    @Test
    void createFromPoster_countsQuantitiesOfAllItems() {
        Order order = create(100L, NOW, null, List.of(item(2), item(3), item(1)));

        assertThat(order.getEstimatedReadyAt()).isEqualTo(NOW.plus(Duration.ofMinutes(32)));
    }
}
