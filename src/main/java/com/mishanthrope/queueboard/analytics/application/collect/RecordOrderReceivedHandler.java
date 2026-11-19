package com.mishanthrope.queueboard.analytics.application.collect;

import com.mishanthrope.queueboard.analytics.domain.KitchenAction;
import com.mishanthrope.queueboard.analytics.domain.KitchenActionRepository;
import com.mishanthrope.queueboard.analytics.domain.ModifierUsageRepository;
import com.mishanthrope.queueboard.analytics.domain.OrderTimingRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordOrderReceivedHandler {

    private final KitchenActionRepository actions;
    private final OrderTimingRepository timings;
    private final ModifierUsageRepository modifierUsage;
    private final Clock clock;

    @Transactional
    public void handle(OrderReceived fact) {
        if (!actions.registerIfNew(
                fact.factId(), KitchenAction.RECEIVED, fact.orderId(), fact.at())) {
            log.info("Analytics: fact {} already processed, ignored", fact.factId());
            return;
        }
        var timing = timings.getOrCreateLocked(fact.orderId(), fact.displayNumber());
        timing.registerPromise(fact.paidAt(), fact.promisedAt());
        timings.save(timing);
        var day = fact.at().atZone(clock.getZone()).toLocalDate();
        fact.modifiers().forEach(tally -> modifierUsage.add(day, tally));
    }
}
