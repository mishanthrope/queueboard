package com.mishanthrope.queueboard.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mishanthrope.queueboard.analytics.infrastructure.persistence.ModifierUsageEntity;
import com.mishanthrope.queueboard.core.events.CookingStarted;
import com.mishanthrope.queueboard.core.events.OrderAccepted;
import com.mishanthrope.queueboard.core.events.OrderCancelled;
import com.mishanthrope.queueboard.core.events.OrderEvent;
import com.mishanthrope.queueboard.core.events.OrderIssued;
import com.mishanthrope.queueboard.core.events.OrderReady;
import com.mishanthrope.queueboard.core.events.OrderRestored;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderStatsIntegrationTest extends AbstractAnalyticsTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    private String bearer;

    @BeforeEach
    void logIn() throws Exception {
        bearer = registerAndLogin();
    }

    private static Instant at(int hour, int minute) {
        return ZonedDateTime.of(2026, 11, 10, hour, minute, 0, 0, KYIV).toInstant();
    }

    private static OrderAccepted accepted(
            long id, Instant paidAt, Instant estimate, OrderAccepted.Item... items) {
        return new OrderAccepted(
                UUID.randomUUID(), paidAt, id, (int) id, 9000 + id, paidAt, estimate,
                List.of(items));
    }

    private static CookingStarted started(long id, Instant when) {
        return new CookingStarted(UUID.randomUUID(), when, id, (int) id, when);
    }

    private static OrderReady ready(long id, Instant when) {
        return new OrderReady(UUID.randomUUID(), when, id, (int) id, when);
    }

    private static OrderIssued issued(long id, Instant when) {
        return new OrderIssued(UUID.randomUUID(), when, id, (int) id, when);
    }

    private static OrderCancelled cancelled(long id, Instant when) {
        return new OrderCancelled(UUID.randomUUID(), when, id, (int) id, when);
    }

    private static OrderRestored restored(long id, Instant when) {
        return new OrderRestored(UUID.randomUUID(), when, id, (int) id, when);
    }

    @Test
    void orderAccepted_storesOrderAndCountsModifiersByQuantity() {
        publishAndAwaitDelivery(accepted(1, at(10, 0), at(10, 15),
                new OrderAccepted.Item("Шаурма", 2, List.of("Гострий", "Без цибулі")),
                new OrderAccepted.Item("Лаваш", 1, List.of("Гострий"))));

        var order = timings.findById(1L).orElseThrow();
        assertThat(order.getDisplayNumber()).isEqualTo(1);
        assertThat(order.getPaidAt()).isEqualTo(at(10, 0));
        assertThat(order.getPromisedAt()).isEqualTo(at(10, 15));
        assertThat(order.getStartedAt()).isNull();
        assertThat(modifierUsage.findAll())
                .extracting(usage -> usage.getKey().modifier(), ModifierUsageEntity::getTimes)
                .containsExactlyInAnyOrder(tuple("Гострий", 3L), tuple("Без цибулі", 2L));
        assertThat(processedEvents.count()).isEqualTo(1);
    }

    @Test
    void cookingStarted_keepsTheEarliestStartTime() {
        publishAndAwaitDelivery(accepted(1, at(10, 0), at(10, 15)));

        publishAndAwaitDelivery(started(1, at(10, 9)), started(1, at(10, 2)));

        assertThat(timings.findById(1L).orElseThrow().getStartedAt()).isEqualTo(at(10, 2));
    }

    @Test
    void orderReady_keepsTheLatestReadyTime() {
        publishAndAwaitDelivery(accepted(1, at(10, 0), at(10, 15)));

        publishAndAwaitDelivery(ready(1, at(10, 20)), ready(1, at(10, 12)));

        assertThat(timings.findById(1L).orElseThrow().getReadyAt()).isEqualTo(at(10, 20));
    }

    @Test
    void factsBeforeTheAcceptance_stillBuildTheOrder() {
        publishAndAwaitDelivery(ready(1, at(10, 14)), started(1, at(10, 2)),
                accepted(1, at(10, 0), at(10, 15)));

        var order = timings.findById(1L).orElseThrow();
        assertThat(order.getPaidAt()).isEqualTo(at(10, 0));
        assertThat(order.getStartedAt()).isEqualTo(at(10, 2));
        assertThat(order.getReadyAt()).isEqualTo(at(10, 14));
    }

    @Test
    void issuedCancelledRestored_areOnlyLogged() {
        publishAndAwaitDelivery(
                issued(1, at(10, 0)), cancelled(2, at(10, 1)), restored(3, at(10, 2)));

        assertThat(processedEvents.findAll())
                .extracting(event -> event.getEventType())
                .containsExactlyInAnyOrder("ISSUED", "CANCELLED", "RESTORED");
        assertThat(timings.count()).isZero();
        assertThat(modifierUsage.count()).isZero();
    }

    @Test
    void sameEventDeliveredTwice_changesNothing() {
        OrderEvent event = accepted(1, at(10, 0), at(10, 15),
                new OrderAccepted.Item("Шаурма", 2, List.of("Гострий")));

        publishAndAwaitDelivery(event, event);

        assertThat(processedEvents.count()).isEqualTo(1);
        assertThat(timings.count()).isEqualTo(1);
        assertThat(modifierUsage.findAll()).singleElement()
                .extracting(ModifierUsageEntity::getTimes).isEqualTo(2L);
    }

    @Test
    void lateDuplicateOfAnOlderEvent_doesNotOverwriteNewerState() {
        publishAndAwaitDelivery(accepted(1, at(10, 0), at(10, 15)));
        var firstReady = ready(1, at(10, 12));
        publishAndAwaitDelivery(firstReady, ready(1, at(10, 20)));

        publishAndAwaitDelivery(firstReady);

        assertThat(timings.findById(1L).orElseThrow().getReadyAt()).isEqualTo(at(10, 20));
    }

    private void scriptTheDay() {
        publishAndAwaitDelivery(
                accepted(1, at(10, 0), at(10, 15),
                        new OrderAccepted.Item("Шаурма", 2, List.of("Гострий", "Без цибулі")),
                        new OrderAccepted.Item("Лаваш", 1, List.of("Гострий"))),
                accepted(2, at(10, 5), at(10, 20),
                        new OrderAccepted.Item("Шаурма", 1, List.of("Гострий"))),
                accepted(3, at(11, 0), at(11, 15)),
                started(1, at(10, 2)),
                started(2, at(10, 6)),
                ready(1, at(10, 14)),
                ready(2, at(10, 26)),
                issued(1, at(10, 20)),
                issued(2, at(10, 30)),
                cancelled(3, at(11, 1)),
                restored(3, at(11, 2)));
    }

    @Test
    void stats_forScriptedDay_returnsAveragesActionsAndTopModifiers() throws Exception {
        scriptTheDay();

        getStats(bearer, "?date=" + DAY).andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersAccepted").value(3))
                .andExpect(jsonPath("$.ordersIssued").value(2))
                .andExpect(jsonPath("$.ordersCancelled").value(1))
                .andExpect(jsonPath("$.averageCookingMinutes").value(16.0))
                .andExpect(jsonPath("$.averageWaitMinutes").value(17.5))
                .andExpect(jsonPath("$.averageEstimateErrorMinutes").value(2.5))
                .andExpect(jsonPath("$.cookActions.COOKING_STARTED").value(2))
                .andExpect(jsonPath("$.cookActions.READY").value(2))
                .andExpect(jsonPath("$.cookActions.ISSUED").value(2))
                .andExpect(jsonPath("$.cookActions.CANCELLED").value(1))
                .andExpect(jsonPath("$.cookActions.RESTORED").value(1))
                .andExpect(jsonPath("$.topModifiers", hasSize(2)))
                .andExpect(jsonPath("$.topModifiers[0].modifier").value("Гострий"))
                .andExpect(jsonPath("$.topModifiers[0].times").value(4))
                .andExpect(jsonPath("$.topModifiers[1].modifier").value("Без цибулі"))
                .andExpect(jsonPath("$.topModifiers[1].times").value(2));
    }

    @Test
    void stats_forOtherDay_isEmptyWithNullAverages() throws Exception {
        scriptTheDay();

        getStats(bearer, "?date=" + DAY.plusDays(1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersAccepted").value(0))
                .andExpect(jsonPath("$.averageCookingMinutes").value(nullValue()))
                .andExpect(jsonPath("$.averageWaitMinutes").value(nullValue()))
                .andExpect(jsonPath("$.averageEstimateErrorMinutes").value(nullValue()))
                .andExpect(jsonPath("$.cookActions.RESTORED").value(0))
                .andExpect(jsonPath("$.topModifiers", hasSize(0)));
    }

    @Test
    void stats_dayBoundaryIsHalfOpenInClockZone() throws Exception {
        var midnight = ZonedDateTime.of(2026, 11, 11, 0, 0, 0, 0, KYIV).toInstant();
        publishAndAwaitDelivery(issued(1, midnight.minusSeconds(1)), issued(2, midnight));

        getStats(bearer, "?date=" + DAY).andExpect(jsonPath("$.ordersIssued").value(1));
        getStats(bearer, "?date=" + DAY.plusDays(1))
                .andExpect(jsonPath("$.ordersIssued").value(1));
    }

    @Test
    void stats_withoutDate_usesToday() throws Exception {
        getStats(bearer, "").andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersAccepted").value(0));
    }

    @Test
    void stats_withBadDate_returns400() throws Exception {
        getStats(bearer, "?date=yesterday").andExpect(status().isBadRequest());
    }

    @Test
    void stats_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/stats")).andExpect(status().isUnauthorized());
    }
}
