package com.mishanthrope.queueboard.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.web.servlet.ResultActions;

class EventualConsistencyIntegrationTest extends AbstractAnalyticsTest {

    private static final Duration WAIT = Duration.ofSeconds(15);

    @Autowired
    private IncompleteEventPublications incompletePublications;

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
    void stats_appearEventually() throws Exception {
        long id = createOrder(8001L).getId();
        action("start", id).andExpect(status().isOk());
        action("ready", id).andExpect(status().isOk());

        Awaitility.await().atMost(WAIT).untilAsserted(() ->
                getStats(bearer, "").andExpect(status().isOk())
                        .andExpect(jsonPath("$.ordersAccepted").value(1))
                        .andExpect(jsonPath("$.cookActions.COOKING_STARTED").value(1))
                        .andExpect(jsonPath("$.cookActions.READY").value(1)));
    }

    @Test
    void deliveredPublications_areMarkedCompleted() throws Exception {
        createOrder(8002L);

        Awaitility.await().atMost(WAIT).until(
                () -> countPublications("completion_date IS NOT NULL") == 1);
        assertThat(countPublications("completion_date IS NULL")).isZero();
    }

    @Test
    void simulatedDelay_doesNotSlowDownTheCommand() throws Exception {
        long id = createOrder(8003L).getId();
        action("start", id).andExpect(status().isOk());
        awaitNothingInFlight();
        simulation.setSimulatedDelay(Duration.ofSeconds(1));

        long begin = System.nanoTime();
        action("ready", id).andExpect(status().isOk());
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - begin).toMillis();

        assertThat(elapsedMillis).isLessThan(500);
        assertThat(processedEvents.count()).isEqualTo(2);
        Awaitility.await().atMost(WAIT).untilAsserted(() ->
                getStats(bearer, "").andExpect(jsonPath("$.cookActions.READY").value(1)));
    }

    @Test
    void failingAnalytics_leavesThePublicationIncompleteAndDoesNotAffectTheOrder()
            throws Exception {
        long id = createOrder(8004L).getId();
        action("start", id).andExpect(status().isOk());
        awaitNothingInFlight();
        simulation.setFail(true);

        action("ready", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        Awaitility.await().atMost(WAIT).until(() -> countPublications("status = 'FAILED'") == 1);
        assertThat(countPublications("completion_date IS NULL")).isEqualTo(1);
        assertThat(orderEntities.findById(id).orElseThrow().getStatus().name())
                .isEqualTo("READY");
        getStats(bearer, "")
                .andExpect(jsonPath("$.cookActions.READY").value(0))
                .andExpect(jsonPath("$.cookActions.COOKING_STARTED").value(1));
    }

    @Test
    void resubmittingIncompletePublications_makesTheStatsCatchUp() throws Exception {
        long id = createOrder(8005L).getId();
        action("start", id).andExpect(status().isOk());
        awaitNothingInFlight();
        simulation.setFail(true);
        action("ready", id).andExpect(status().isOk());
        Awaitility.await().atMost(WAIT).until(() -> countPublications("status = 'FAILED'") == 1);
        simulation.setFail(false);

        incompletePublications.resubmitIncompletePublications(publication -> true);

        Awaitility.await().atMost(WAIT).until(
                () -> countPublications("completion_date IS NULL") == 0);
        getStats(bearer, "")
                .andExpect(jsonPath("$.cookActions.READY").value(1))
                .andExpect(jsonPath("$.averageCookingMinutes").isNumber());
    }

    @Test
    void failedCommand_publishesNothing() throws Exception {
        long id = createOrder(8006L).getId();
        awaitNothingInFlight();
        long before = countPublications("true");

        action("issue", id).andExpect(status().isConflict());
        awaitNothingInFlight();

        assertThat(before).isEqualTo(1);
        assertThat(countPublications("true")).isEqualTo(before);
        assertThat(processedEvents.count()).isEqualTo(1);
    }
}
