package com.mishanthrope.queueboard.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.mishanthrope.queueboard.AbstractIntegrationTest;
import com.mishanthrope.queueboard.order.model.Order;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class OrderIntegrationTest extends AbstractIntegrationTest {

    private String bearer;

    @BeforeEach
    void logIn() throws Exception {
        bearer = registerAndLogin();
    }

    private ResultActions action(String verb, long id) throws Exception {
        return mockMvc.perform(post("/api/orders/{id}/" + verb, id)
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions getJson(String url) throws Exception {
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions shift(String body) throws Exception {
        return mockMvc.perform(patch("/api/orders/prep-time")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Instant estimatedReadyAt(long id) throws Exception {
        String body = getJson("/api/orders/" + id).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Instant.parse(JsonPath.<String>read(body, "$.estimatedReadyAt"));
    }

    @Test
    void lifecycle_startReadyIssue_movesThroughAllStatuses() throws Exception {
        long id = createOrder(3001L).getId();

        action("start", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COOKING"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty());
        action("ready", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.readyAt").isNotEmpty());
        action("issue", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.issuedAt").isNotEmpty())
                .andExpect(jsonPath("$.estimatedReadyAt").value(nullValue()));
    }

    @Test
    void action_forbiddenInCurrentStatus_returns409WithMessage() throws Exception {
        long id = createOrder(3001L).getId();

        action("issue", id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot issue order in status NEW"));
    }

    @Test
    void restore_fromReady_movesBackToCooking() throws Exception {
        long id = createOrder(3001L).getId();
        action("start", id).andExpect(status().isOk());
        action("ready", id).andExpect(status().isOk());

        action("restore", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COOKING"))
                .andExpect(jsonPath("$.readyAt").value(nullValue()));
    }

    @Test
    void order_withUnknownId_returns404() throws Exception {
        getJson("/api/orders/999999").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order 999999 not found"));
        action("start", 999999L).andExpect(status().isNotFound());
    }

    @Test
    void delete_cancelsOrderAndRemovesItFromKitchenQueueAndBoard() throws Exception {
        long id = createOrder(3001L).getId();

        mockMvc.perform(delete("/api/orders/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());

        getJson("/api/orders/kitchen").andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/board"))
                .andExpect(jsonPath("$.preparing", hasSize(0)))
                .andExpect(jsonPath("$.ready", hasSize(0)));
        getJson("/api/orders/" + id).andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.estimatedReadyAt").value(nullValue()));
    }

    @Test
    void history_filteredByStatus_returnsOnlyMatchingOrders() throws Exception {
        createOrder(3001L);
        createOrder(3002L);
        long cancelled = createOrder(3003L).getId();
        mockMvc.perform(delete("/api/orders/{id}", cancelled)
                .header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isNoContent());

        getJson("/api/orders?status=CANCELLED").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(cancelled));
    }

    @Test
    void history_withPagination_returnsPageFields() throws Exception {
        createOrder(3001L);
        createOrder(3002L);
        createOrder(3003L);

        getJson("/api/orders?page=0&size=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)));
        getJson("/api/orders?page=1&size=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void history_withInvalidPaging_returns400() throws Exception {
        getJson("/api/orders?size=0").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        getJson("/api/orders?page=-1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void prepTime_patchWithFive_shiftsActiveOrdersByFiveMinutes() throws Exception {
        long first = createOrder(3001L).getId();
        long second = createOrder(3002L).getId();
        Instant firstBefore = estimatedReadyAt(first);
        Instant secondBefore = estimatedReadyAt(second);

        shift("{\"deltaMinutes\":5}").andExpect(status().isNoContent());

        assertThat(estimatedReadyAt(first)).isEqualTo(firstBefore.plus(Duration.ofMinutes(5)));
        assertThat(estimatedReadyAt(second)).isEqualTo(secondBefore.plus(Duration.ofMinutes(5)));
    }

    @Test
    void prepTime_patch_doesNotShiftFinishedOrders() throws Exception {
        long id = createOrder(3001L).getId();
        Order cancelled = createOrder(3002L);
        mockMvc.perform(delete("/api/orders/{id}", cancelled.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isNoContent());
        Instant before = orderRepository.findById(cancelled.getId()).orElseThrow()
                .getEstimatedReadyAt();

        shift("{\"deltaMinutes\":-5}").andExpect(status().isNoContent());

        assertThat(orderRepository.findById(cancelled.getId()).orElseThrow()
                .getEstimatedReadyAt()).isEqualTo(before);
        assertThat(orderRepository.findById(id)).isPresent();
    }

    @Test
    void prepTime_patchWithInvalidDelta_returns400() throws Exception {
        shift("{\"deltaMinutes\":0}").andExpect(status().isBadRequest());
        shift("{\"deltaMinutes\":31}").andExpect(status().isBadRequest());
        shift("{\"deltaMinutes\":-31}").andExpect(status().isBadRequest());
        shift("{}").andExpect(status().isBadRequest());
    }

    @Test
    void prepTime_patchWithoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/orders/prep-time")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deltaMinutes\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void secondWebhook_getsLaterEstimateThanFirst() throws Exception {
        long first = createOrder(3001L).getId();
        long second = createOrder(3002L).getId();

        assertThat(estimatedReadyAt(second)).isAfter(estimatedReadyAt(first));
    }

    @Test
    void putOnOrder_isNotSupported() throws Exception {
        long id = createOrder(3001L).getId();

        mockMvc.perform(put("/api/orders/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}
