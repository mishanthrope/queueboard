package com.mishanthrope.queueboard.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mishanthrope.queueboard.infrastructure.event.DirectOrderEventPublisher;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

@TestPropertySource(properties = "app.analytics.mode=sync")
class AnalyticsSyncModeIntegrationTest extends AbstractAnalyticsTest {

    @Autowired
    private ApplicationContext context;

    private String bearer;

    @BeforeEach
    void logIn() throws Exception {
        bearer = registerAndLogin();
    }

    private ResultActions action(String verb, long id) throws Exception {
        return mockMvc.perform(post("/api/orders/{id}/" + verb, id)
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }

    @Test
    void syncPublisherIsActive() {
        assertThat(context.getBeansOfType(DirectOrderEventPublisher.class)).hasSize(1);
    }

    @Test
    void stats_areVisibleImmediatelyAfterTheHttpCall() throws Exception {
        long id = createOrder(7001L).getId();
        action("start", id).andExpect(status().isOk());
        action("ready", id).andExpect(status().isOk());

        getStats(bearer, "").andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersAccepted").value(1))
                .andExpect(jsonPath("$.cookActions.COOKING_STARTED").value(1))
                .andExpect(jsonPath("$.cookActions.READY").value(1))
                .andExpect(jsonPath("$.averageCookingMinutes").isNumber());
    }

    @Test
    void simulatedDelay_makesTheCommandWait() throws Exception {
        long id = createOrder(7002L).getId();
        action("start", id).andExpect(status().isOk());
        simulation.setSimulatedDelay(Duration.ofMillis(300));

        long begin = System.nanoTime();
        action("ready", id).andExpect(status().isOk());
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - begin).toMillis();

        assertThat(elapsedMillis).isGreaterThanOrEqualTo(300);
    }

    @Test
    void failingAnalytics_doesNotBreakTheCommandAndLeavesStatsUnchanged() throws Exception {
        long id = createOrder(7003L).getId();
        action("start", id).andExpect(status().isOk());
        simulation.setFail(true);

        action("ready", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        assertThat(orderEntities.findById(id).orElseThrow().getStatus().name())
                .isEqualTo("READY");
        getStats(bearer, "")
                .andExpect(jsonPath("$.cookActions.READY").value(0))
                .andExpect(jsonPath("$.cookActions.COOKING_STARTED").value(1));
    }

    @Test
    void failedCommand_changesNothingInAnalytics() throws Exception {
        long id = createOrder(7004L).getId();
        long before = processedEvents.count();

        action("issue", id).andExpect(status().isConflict());

        assertThat(processedEvents.count()).isEqualTo(before);
    }
}
