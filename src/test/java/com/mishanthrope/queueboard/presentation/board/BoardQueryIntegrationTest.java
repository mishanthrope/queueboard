package com.mishanthrope.queueboard.presentation.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.mishanthrope.queueboard.AbstractIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class BoardQueryIntegrationTest extends AbstractIntegrationTest {

    @Test
    void board_minutesLeftMatchesTheEstimateOfTheOrder() throws Exception {
        String bearer = registerAndLogin();
        long id = createOrder(5001L).getId();
        String details = mockMvc.perform(get("/api/orders/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andReturn().getResponse().getContentAsString();
        Instant estimate = Instant.parse(JsonPath.<String>read(details, "$.estimatedReadyAt"));
        long minutesLeftUpperBound = Duration.between(Instant.now(), estimate).toMinutes() + 1;

        String board = mockMvc.perform(get("/api/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preparing", hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        int minutesLeft = JsonPath.read(board, "$.preparing[0].minutesLeft");
        assertThat((long) minutesLeft)
                .isBetween(minutesLeftUpperBound - 1, minutesLeftUpperBound);
    }

    @Test
    void board_listsOrdersInTheOrderTheyWerePaidAndSkipsCancelled() throws Exception {
        String bearer = registerAndLogin();
        long first = createOrder(5001L).getId();
        createOrder(5002L);
        createOrder(5003L);
        mockMvc.perform(delete("/api/orders/{id}", first)
                .header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/board"))
                .andExpect(jsonPath("$.preparing", hasSize(2)))
                .andExpect(jsonPath("$.ready", hasSize(0)));
    }

    @Test
    void board_afterIssue_dropsTheOrderFromBothGroups() throws Exception {
        String bearer = registerAndLogin();
        long id = createOrder(5001L).getId();
        for (String verb : new String[] {"start", "ready", "issue"}) {
            mockMvc.perform(post("/api/orders/{id}/" + verb, id)
                    .header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/board"))
                .andExpect(jsonPath("$.preparing", hasSize(0)))
                .andExpect(jsonPath("$.ready", hasSize(0)));
    }
}
