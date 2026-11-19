package com.mishanthrope.queueboard.application.order.command;

import static com.mishanthrope.queueboard.application.order.OrderFixtures.seed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mishanthrope.queueboard.domain.order.DuplicatePosterCheckError;
import com.mishanthrope.queueboard.domain.order.InMemoryOrderRepository;
import com.mishanthrope.queueboard.domain.order.InvalidOrderError;
import com.mishanthrope.queueboard.domain.order.KitchenSettings;
import com.mishanthrope.queueboard.domain.order.Order;
import com.mishanthrope.queueboard.domain.order.OrderFactory;
import com.mishanthrope.queueboard.domain.order.OrderItem;
import com.mishanthrope.queueboard.domain.order.OrderStatus;
import com.mishanthrope.queueboard.domain.order.PrepTimeCalculator;
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

class CreateOrderFromPosterHandlerTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final Instant START_OF_DAY = Instant.parse("2026-11-17T22:00:00Z");
    private static final Duration SKEW = Duration.ofMinutes(2);
    private static final Duration ONE_ITEM_TIME = Duration.ofMinutes(15);

    private InMemoryOrderRepository repository;
    private CreateOrderFromPosterHandler handler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        var settings = new KitchenSettings(
                Duration.ofMinutes(7), Duration.ofMinutes(3), Duration.ofMinutes(5), 5);
        var factory = new OrderFactory(
                repository, new PrepTimeCalculator(settings), SKEW, Clock.fixed(NOW, KYIV));
        handler = new CreateOrderFromPosterHandler(repository, factory);
    }

    private static CreateOrderFromPoster.Line item(
            String name, int quantity, String... modifiers) {
        return new CreateOrderFromPoster.Line(name, quantity, List.of(modifiers));
    }

    private static List<CreateOrderFromPoster.Line> oneItem() {
        return List.of(item("Шаурма", 1));
    }

    private Order create(
            Instant paidAt, String comment, List<CreateOrderFromPoster.Line> items) {
        handler.handle(new CreateOrderFromPoster(100L, paidAt, comment, items));
        return repository.findByPosterCheckId(100L).orElseThrow();
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
    void handle_withInvalidData_throwsInvalidOrderAndStoresNothing(
            String description,
            long posterCheckId,
            Instant paidAt,
            String comment,
            List<CreateOrderFromPoster.Line> items) {
        assertThatThrownBy(() -> handler.handle(
                new CreateOrderFromPoster(posterCheckId, paidAt, comment, items)))
                .isInstanceOf(InvalidOrderError.class);

        assertThat(repository.findByPosterCheckId(posterCheckId)).isEmpty();
    }

    static Stream<Arguments> invalidLines() {
        return Stream.of(
                Arguments.of("empty product name", item("", 1)),
                Arguments.of("blank product name", item("   ", 1)),
                Arguments.of("null product name", item(null, 1)),
                Arguments.of("product name over 100", item("x".repeat(101), 1)),
                Arguments.of("quantity 0", item("Шаурма", 0)),
                Arguments.of("quantity 100", item("Шаурма", 100)),
                Arguments.of("modifier over 100", item("Шаурма", 1, "x".repeat(101))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidLines")
    void handle_withLineBreakingItemRules_throwsInvalidOrderAndStoresNothing(
            String description, CreateOrderFromPoster.Line line) {
        assertThatThrownBy(() -> handler.handle(
                new CreateOrderFromPoster(100L, NOW, null, List.of(oneItem().get(0), line))))
                .isInstanceOf(InvalidOrderError.class);

        assertThat(repository.findByPosterCheckId(100L)).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validIntakes")
    void handle_withBoundaryData_storesNewOrder(
            String description, Instant paidAt, String comment,
            List<CreateOrderFromPoster.Line> items) {
        Order order = create(paidAt, comment, items);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getPaidAt()).isEqualTo(paidAt);
        assertThat(order.getComment()).isEqualTo(comment);
        assertThat(order.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void handle_storesItemsWithModifiers() {
        Order order = create(NOW, null, List.of(item("Шаурма", 2, "сир"), item("Кола", 1)));

        assertThat(order.getItems()).extracting(OrderItem::productName)
                .containsExactly("Шаурма", "Кола");
        assertThat(order.getItems().get(0).modifiers()).containsExactly("сир");
    }

    @Test
    void handle_whenCheckIdIsUsed_throwsDuplicateAndStoresNothingNew() {
        seed(repository, 100L, 1, OrderStatus.ISSUED, NOW.minusSeconds(60), NOW);

        assertThatThrownBy(() -> create(NOW, null, oneItem()))
                .isInstanceOf(DuplicatePosterCheckError.class);

        assertThat(repository.findMaxDisplayNumberCreatedSince(START_OF_DAY)).contains(1);
    }

    @Test
    void handle_whenNoOrdersToday_assignsDisplayNumberOne() {
        assertThat(create(NOW, null, oneItem()).getDisplayNumber()).isEqualTo(1);
    }

    @Test
    void handle_whenOrdersExistToday_assignsMaxPlusOne() {
        seed(repository, 1L, 7, OrderStatus.NEW, NOW.minusSeconds(60), NOW);

        assertThat(create(NOW, null, oneItem()).getDisplayNumber()).isEqualTo(8);
    }

    @Test
    void handle_whenQueueIsEmpty_estimatesFromNow() {
        assertThat(create(NOW, null, oneItem()).getEstimatedReadyAt())
                .isEqualTo(NOW.plus(ONE_ITEM_TIME));
    }

    @Test
    void handle_whenQueueTailIsInThePast_estimatesFromNow() {
        seed(repository, 1L, 1, OrderStatus.NEW, NOW.minusSeconds(600), NOW.minusSeconds(60));

        assertThat(create(NOW, null, oneItem()).getEstimatedReadyAt())
                .isEqualTo(NOW.plus(ONE_ITEM_TIME));
    }

    @Test
    void handle_whenQueueTailIsInTheFuture_estimatesFromTail() {
        Instant tail = NOW.plus(Duration.ofMinutes(10));
        seed(repository, 1L, 1, OrderStatus.COOKING, NOW.minusSeconds(600), tail);

        assertThat(create(NOW, null, oneItem()).getEstimatedReadyAt())
                .isEqualTo(tail.plus(ONE_ITEM_TIME));
    }

    @Test
    void handle_secondOrderIsEstimatedAfterTheFirst() {
        handler.handle(new CreateOrderFromPoster(100L, NOW, null, oneItem()));
        handler.handle(new CreateOrderFromPoster(101L, NOW, null, oneItem()));

        assertThat(repository.findByPosterCheckId(101L).orElseThrow().getEstimatedReadyAt())
                .isEqualTo(NOW.plus(ONE_ITEM_TIME).plus(ONE_ITEM_TIME));
    }
}
