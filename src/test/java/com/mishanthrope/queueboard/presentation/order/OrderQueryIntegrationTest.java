package com.mishanthrope.queueboard.presentation.order;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mishanthrope.queueboard.AbstractIntegrationTest;
import com.mishanthrope.queueboard.infrastructure.persistence.order.OrderEntity;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

class OrderQueryIntegrationTest extends AbstractIntegrationTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private String bearer;

    @BeforeEach
    void logIn() throws Exception {
        bearer = registerAndLogin();
    }

    private ResultActions getJson(String url) throws Exception {
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private void action(String verb, long id) throws Exception {
        mockMvc.perform(post("/api/orders/{id}/" + verb, id)
                .header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isOk());
    }

    private void cancel(long id) throws Exception {
        mockMvc.perform(delete("/api/orders/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isNoContent());
    }

    @Test
    void kitchen_returnsActiveOrdersWithAllFieldsAndItems() throws Exception {
        OrderEntity order = createOrder(4001L);

        getJson("/api/orders/kitchen").andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(order.getId()))
                .andExpect(jsonPath("$[0].posterCheckId").value(4001))
                .andExpect(jsonPath("$[0].displayNumber").value(order.getDisplayNumber()))
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[0].paidAt").isNotEmpty())
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$[0].estimatedReadyAt").isNotEmpty())
                .andExpect(jsonPath("$[0].startedAt").value(nullValue()))
                .andExpect(jsonPath("$[0].items", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].items[0].id").isNumber())
                .andExpect(jsonPath("$[0].items[0].productName").isNotEmpty())
                .andExpect(jsonPath("$[0].items[0].quantity").isNumber())
                .andExpect(jsonPath("$[0].items[0].modifiers").isArray());
    }

    @Test
    void kitchen_containsNewCookingAndReadyButNotIssuedOrCancelled() throws Exception {
        final long fresh = createOrder(4001L).getId();
        final long cooking = createOrder(4002L).getId();
        final long ready = createOrder(4003L).getId();
        final long issued = createOrder(4004L).getId();
        final long cancelled = createOrder(4005L).getId();
        action("start", cooking);
        action("start", ready);
        action("ready", ready);
        action("start", issued);
        action("ready", issued);
        action("issue", issued);
        cancel(cancelled);

        getJson("/api/orders/kitchen").andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.id == " + fresh + ")].status").value("NEW"))
                .andExpect(jsonPath("$[?(@.id == " + cooking + ")].status").value("COOKING"))
                .andExpect(jsonPath("$[?(@.id == " + ready + ")].status").value("READY"))
                .andExpect(jsonPath("$[?(@.id == " + issued + ")]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == " + cancelled + ")]").isEmpty());
    }

    @Test
    void details_returnsTheOrderWithItsItems() throws Exception {
        OrderEntity order = createOrder(4001L);

        getJson("/api/orders/" + order.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.posterCheckId").value(4001))
                .andExpect(jsonPath("$.displayNumber").value(order.getDisplayNumber()))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.estimatedReadyAt").isNotEmpty())
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.items[0].productName").isNotEmpty())
                .andExpect(jsonPath("$.items[0].modifiers").isArray());
    }

    @Test
    void details_hasEstimatedReadyAtWhileActiveAndNullWhenFinished() throws Exception {
        long id = createOrder(4001L).getId();
        action("start", id);
        action("ready", id);
        getJson("/api/orders/" + id).andExpect(jsonPath("$.estimatedReadyAt").isNotEmpty());

        action("issue", id);

        getJson("/api/orders/" + id).andExpect(jsonPath("$.estimatedReadyAt").value(nullValue()))
                .andExpect(jsonPath("$.issuedAt").isNotEmpty());
    }

    @Test
    void history_withDate_returnsOnlyThatLocalDay() throws Exception {
        createOrder(4001L);
        createOrder(4002L);
        LocalDate today = LocalDate.now(Clock.system(KYIV));

        getJson("/api/orders?date=" + today).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)));
        getJson("/api/orders?date=" + today.minusDays(1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));
        getJson("/api/orders?date=" + today.plusDays(1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void history_withDateAndStatus_combinesBothFilters() throws Exception {
        createOrder(4001L);
        final long cancelled = createOrder(4002L).getId();
        cancel(cancelled);
        LocalDate today = LocalDate.now(Clock.system(KYIV));

        getJson("/api/orders?date=" + today + "&status=CANCELLED").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(cancelled));
        getJson("/api/orders?date=" + today.minusDays(1) + "&status=CANCELLED")
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void history_listsNewestFirstAndIncludesFinishedOrders() throws Exception {
        long first = createOrder(4001L).getId();
        long second = createOrder(4002L).getId();
        long third = createOrder(4003L).getId();
        cancel(first);

        getJson("/api/orders").andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id").value(third))
                .andExpect(jsonPath("$.content[1].id").value(second))
                .andExpect(jsonPath("$.content[2].id").value(first))
                .andExpect(jsonPath("$.content[2].estimatedReadyAt").value(nullValue()))
                .andExpect(jsonPath("$.content[0].items[0].productName").isNotEmpty());
    }
}
