package com.mishanthrope.queueboard.analytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.mishanthrope.queueboard.AbstractIntegrationTest;
import com.mishanthrope.queueboard.analytics.internal.SimulationSettings;
import com.mishanthrope.queueboard.analytics.internal.SpringDataAnalyticsOrderRepository;
import com.mishanthrope.queueboard.analytics.internal.SpringDataModifierUsageRepository;
import com.mishanthrope.queueboard.analytics.internal.SpringDataProcessedEventRepository;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.servlet.ResultActions;

/** Cleans the analytics tables and waits for the analytics executor between tests. */
abstract class AbstractAnalyticsTest extends AbstractIntegrationTest {

    @Autowired
    protected SpringDataProcessedEventRepository processedEvents;

    @Autowired
    protected SpringDataAnalyticsOrderRepository analyticsOrders;

    @Autowired
    protected SpringDataModifierUsageRepository modifierUsage;

    @Autowired
    protected SimulationSettings simulation;

    @Autowired
    @Qualifier("analyticsExecutor")
    private ThreadPoolTaskExecutor analyticsExecutor;

    @BeforeEach
    void cleanAnalytics() {
        simulation.setSimulatedDelay(Duration.ZERO);
        simulation.setFail(false);
        awaitAnalyticsIdle();
        processedEvents.deleteAll();
        analyticsOrders.deleteAll();
        modifierUsage.deleteAll();
    }

    @AfterEach
    void resetSimulation() {
        simulation.setSimulatedDelay(Duration.ZERO);
        simulation.setFail(false);
        awaitAnalyticsIdle();
    }

    protected void awaitAnalyticsIdle() {
        var executor = analyticsExecutor.getThreadPoolExecutor();
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(
                () -> executor.getQueue().isEmpty() && executor.getActiveCount() == 0);
    }

    protected ResultActions getStats(String bearer, String query) throws Exception {
        return mockMvc.perform(get("/api/stats" + query)
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }
}
