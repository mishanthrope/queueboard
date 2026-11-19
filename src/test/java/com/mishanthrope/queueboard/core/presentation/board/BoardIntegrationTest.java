package com.mishanthrope.queueboard.core.presentation.board;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mishanthrope.queueboard.AbstractIntegrationTest;
import com.mishanthrope.queueboard.core.infrastructure.persistence.order.OrderEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class BoardIntegrationTest extends AbstractIntegrationTest {

    @Test
    void board_withoutToken_returnsEmptyGroupsWhenNoOrders() throws Exception {
        mockMvc.perform(get("/api/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preparing", hasSize(0)))
                .andExpect(jsonPath("$.ready", hasSize(0)));
    }

    @Test
    void board_groupsOrdersIntoPreparingAndReady() throws Exception {
        String bearer = registerAndLogin();
        OrderEntity first = createOrder(2001L);
        OrderEntity second = createOrder(2002L);
        mockMvc.perform(post("/api/orders/{id}/start", first.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/{id}/ready", first.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isOk());

        mockMvc.perform(get("/api/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready", hasSize(1)))
                .andExpect(jsonPath("$.ready[0].displayNumber").value(first.getDisplayNumber()))
                .andExpect(jsonPath("$.preparing", hasSize(1)))
                .andExpect(jsonPath("$.preparing[0].displayNumber")
                        .value(second.getDisplayNumber()))
                .andExpect(jsonPath("$.preparing[0].minutesLeft").isNumber());
    }
}
