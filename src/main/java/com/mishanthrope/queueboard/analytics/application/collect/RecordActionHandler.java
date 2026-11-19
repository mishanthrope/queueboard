package com.mishanthrope.queueboard.analytics.application.collect;

import com.mishanthrope.queueboard.analytics.domain.KitchenActionRepository;
import com.mishanthrope.queueboard.analytics.domain.OrderTimingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordActionHandler {

    private final KitchenActionRepository actions;
    private final OrderTimingRepository timings;

    @Transactional
    public void handle(ActionPerformed fact) {
        if (!actions.registerIfNew(
                fact.factId(), fact.action(), fact.orderId(), fact.at())) {
            log.info("Analytics: fact {} already processed, ignored", fact.factId());
            return;
        }
        switch (fact.action()) {
            case STARTED -> {
                var timing = timings.getOrCreateLocked(fact.orderId(), fact.displayNumber());
                timing.start(fact.at());
                timings.save(timing);
            }
            case COMPLETED -> {
                var timing = timings.getOrCreateLocked(fact.orderId(), fact.displayNumber());
                timing.complete(fact.at());
                timings.save(timing);
            }
            default -> log.debug("Analytics: order {} {}", fact.orderId(), fact.action());
        }
    }
}
