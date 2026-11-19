package com.mishanthrope.queueboard.analytics.infrastructure.acl;

import static org.assertj.core.api.Assertions.assertThat;

import com.mishanthrope.queueboard.analytics.application.collect.ActionPerformed;
import com.mishanthrope.queueboard.analytics.application.collect.OrderReceived;
import com.mishanthrope.queueboard.analytics.domain.KitchenAction;
import com.mishanthrope.queueboard.analytics.domain.ModifierTally;
import com.mishanthrope.queueboard.core.events.CookingStarted;
import com.mishanthrope.queueboard.core.events.OrderAccepted;
import com.mishanthrope.queueboard.core.events.OrderCancelled;
import com.mishanthrope.queueboard.core.events.OrderIssued;
import com.mishanthrope.queueboard.core.events.OrderReady;
import com.mishanthrope.queueboard.core.events.OrderRestored;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoreEventTranslatorTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(60);

    private final CoreEventTranslator translator = new CoreEventTranslator();

    @Test
    void orderAccepted_becomesOrderReceivedWithPromiseAndModifierTallies() {
        var event = new OrderAccepted(ID, NOW, 5L, 12, 900L, NOW.minusSeconds(30), LATER,
                List.of(
                        new OrderAccepted.Item("Шаурма", 2, List.of("Гострий", "Без цибулі")),
                        new OrderAccepted.Item("Лаваш", 1, List.of("Гострий"))));

        var fact = (OrderReceived) translator.translate(event);

        assertThat(fact.factId()).isEqualTo(ID);
        assertThat(fact.orderId()).isEqualTo(5L);
        assertThat(fact.displayNumber()).isEqualTo(12);
        assertThat(fact.at()).isEqualTo(NOW);
        assertThat(fact.paidAt()).isEqualTo(NOW.minusSeconds(30));
        assertThat(fact.promisedAt()).isEqualTo(LATER);
        assertThat(fact.modifiers()).containsExactlyInAnyOrder(
                new ModifierTally("Гострий", 3), new ModifierTally("Без цибулі", 2));
    }

    @Test
    void orderAcceptedWithoutModifiers_hasNoTallies() {
        var event = new OrderAccepted(ID, NOW, 5L, 12, 900L, NOW, LATER,
                List.of(new OrderAccepted.Item("Шаурма", 1, List.of())));

        var fact = (OrderReceived) translator.translate(event);

        assertThat(fact.modifiers()).isEmpty();
    }

    @Test
    void cookingStarted_becomesStartedAtItsStartTime() {
        var fact = (ActionPerformed) translator.translate(
                new CookingStarted(ID, NOW, 5L, 12, LATER));

        assertAction(fact, KitchenAction.STARTED, LATER);
    }

    @Test
    void orderReady_becomesCompletedAtItsReadyTime() {
        var fact = (ActionPerformed) translator.translate(
                new OrderReady(ID, NOW, 5L, 12, LATER));

        assertAction(fact, KitchenAction.COMPLETED, LATER);
    }

    @Test
    void orderIssued_becomesHandedOut() {
        var fact = (ActionPerformed) translator.translate(
                new OrderIssued(ID, NOW, 5L, 12, LATER));

        assertAction(fact, KitchenAction.HANDED_OUT, LATER);
    }

    @Test
    void orderCancelled_becomesCancelled() {
        var fact = (ActionPerformed) translator.translate(
                new OrderCancelled(ID, NOW, 5L, 12, LATER));

        assertAction(fact, KitchenAction.CANCELLED, LATER);
    }

    @Test
    void orderRestored_becomesReopened() {
        var fact = (ActionPerformed) translator.translate(
                new OrderRestored(ID, NOW, 5L, 12, LATER));

        assertAction(fact, KitchenAction.REOPENED, LATER);
    }

    private static void assertAction(ActionPerformed fact, KitchenAction action, Instant at) {
        assertThat(fact.factId()).isEqualTo(ID);
        assertThat(fact.action()).isEqualTo(action);
        assertThat(fact.orderId()).isEqualTo(5L);
        assertThat(fact.displayNumber()).isEqualTo(12);
        assertThat(fact.at()).isEqualTo(at);
    }
}
