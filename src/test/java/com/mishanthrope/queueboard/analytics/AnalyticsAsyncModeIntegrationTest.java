package com.mishanthrope.queueboard.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mishanthrope.queueboard.infrastructure.event.SpringOrderEventPublisher;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

class AnalyticsAsyncModeIntegrationTest extends AbstractAnalyticsTest {

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
    void asyncPublisherIsTheDefault() {
        assertThat(context.getBeansOfType(SpringOrderEventPublisher.class)).hasSize(1);
    }

    @Test
    void stats_appearEventually() throws Exception {
        long id = createOrder(8001L).getId();
        action("start", id).andExpect(status().isOk());
        action("ready", id).andExpect(status().isOk());

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                getStats(bearer, "").andExpect(status().isOk())
                        .andExpect(jsonPath("$.ordersAccepted").value(1))
                        .andExpect(jsonPath("$.cookActions.COOKING_STARTED").value(1))
                        .andExpect(jsonPath("$.cookActions.READY").value(1)));
    }

    @Test
    void simulatedDelay_doesNotSlowDownTheCommand() throws Exception {
        long id = createOrder(8002L).getId();
        action("start", id).andExpect(status().isOk());
        awaitAnalyticsIdle();
        simulation.setSimulatedDelay(Duration.ofSeconds(1));

        long begin = System.nanoTime();
        action("ready", id).andExpect(status().isOk());
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - begin).toMillis();

        assertThat(elapsedMillis).isLessThan(500);
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                getStats(bearer, "").andExpect(jsonPath("$.cookActions.READY").value(1)));
    }

    @Test
    void failingAnalytics_doesNotAffectTheOrderFlow() throws Exception {
        long id = createOrder(8003L).getId();
        action("start", id).andExpect(status().isOk());
        awaitAnalyticsIdle();
        simulation.setFail(true);

        action("ready", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
        awaitAnalyticsIdle();

        assertThat(orderEntities.findById(id).orElseThrow().getStatus().name())
                .isEqualTo("READY");
        getStats(bearer, "")
                .andExpect(jsonPath("$.cookActions.READY").value(0))
                .andExpect(jsonPath("$.cookActions.COOKING_STARTED").value(1));
    }

    @Test
    void failedCommand_producesNoAnalyticsChange() throws Exception {
        long id = createOrder(8004L).getId();
        awaitAnalyticsIdle();
        long before = processedEvents.count();

        action("issue", id).andExpect(status().isConflict());
        awaitAnalyticsIdle();

        assertThat(before).isEqualTo(1);
        assertThat(processedEvents.count()).isEqualTo(before);
    }
}
