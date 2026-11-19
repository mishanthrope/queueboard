package com.mishanthrope.queueboard.analytics.infrastructure.acl;

import com.mishanthrope.queueboard.analytics.application.collect.ActionPerformed;
import com.mishanthrope.queueboard.analytics.application.collect.OrderReceived;
import com.mishanthrope.queueboard.analytics.application.collect.RecordActionHandler;
import com.mishanthrope.queueboard.analytics.application.collect.RecordOrderReceivedHandler;
import com.mishanthrope.queueboard.analytics.infrastructure.config.SimulationSettings;
import com.mishanthrope.queueboard.core.events.OrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to Core events. {@code @ApplicationModuleListener} runs asynchronously after the
 * publishing transaction has committed, in a transaction of its own. An exception leaves the
 * publication incomplete in the registry, so it can be resubmitted.
 */
@Component
@RequiredArgsConstructor
public class CoreEventListener {

    private final CoreEventTranslator translator;
    private final RecordOrderReceivedHandler orderReceived;
    private final RecordActionHandler actionPerformed;
    private final SimulationSettings simulation;

    @ApplicationModuleListener
    void on(OrderEvent event) {
        simulation.simulate();
        switch (translator.translate(event)) {
            case OrderReceived fact -> orderReceived.handle(fact);
            case ActionPerformed fact -> actionPerformed.handle(fact);
        }
    }
}
