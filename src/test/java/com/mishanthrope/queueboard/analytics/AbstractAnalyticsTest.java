package com.mishanthrope.queueboard.analytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.mishanthrope.queueboard.AbstractIntegrationTest;
import com.mishanthrope.queueboard.analytics.infrastructure.config.SimulationSettings;
import com.mishanthrope.queueboard.analytics.infrastructure.persistence.SpringDataModifierUsageRepository;
import com.mishanthrope.queueboard.analytics.infrastructure.persistence.SpringDataOrderTimingRepository;
import com.mishanthrope.queueboard.analytics.infrastructure.persistence.SpringDataProcessedEventRepository;
import com.mishanthrope.queueboard.core.events.OrderEvent;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/** Cleans the analytics tables and the event registry between tests. */
abstract class AbstractAnalyticsTest extends AbstractIntegrationTest {

    @Autowired
    protected SpringDataProcessedEventRepository processedEvents;

    @Autowired
    protected SpringDataOrderTimingRepository timings;

    @Autowired
    protected SpringDataModifierUsageRepository modifierUsage;

    @Autowired
    protected SimulationSettings simulation;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private TransactionTemplate transaction;

    @BeforeEach
    void cleanAnalytics() {
        resetSimulation();
        awaitNothingInFlight();
        processedEvents.deleteAll();
        timings.deleteAll();
        modifierUsage.deleteAll();
        jdbc.update("DELETE FROM event_publication");
    }

    @AfterEach
    void restoreSimulation() {
        resetSimulation();
        awaitNothingInFlight();
        jdbc.update("DELETE FROM event_publication");
    }

    private void resetSimulation() {
        simulation.setSimulatedDelay(Duration.ZERO);
        simulation.setFail(false);
    }

    /** Publishes events the way a Core command does: inside a transaction. */
    protected void publish(OrderEvent... events) {
        for (var event : events) {
            transaction.executeWithoutResult(status -> publisher.publishEvent(event));
        }
    }

    protected void publishAndAwaitDelivery(OrderEvent... events) {
        publish(events);
        awaitNothingInFlight();
    }

    protected void awaitNothingInFlight() {
        Awaitility.await().atMost(Duration.ofSeconds(15))
                .until(() -> countPublications("completion_date IS NULL AND status <> 'FAILED'")
                        == 0);
    }

    protected long countPublications(String condition) {
        var count = jdbc.queryForObject(
                "SELECT count(*) FROM event_publication WHERE " + condition, Long.class);
        return count == null ? 0 : count;
    }

    protected ResultActions getStats(String bearer, String query) throws Exception {
        return mockMvc.perform(get("/api/stats" + query)
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }
}
