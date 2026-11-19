package com.mishanthrope.queueboard.core.presentation.poster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mishanthrope.queueboard.AbstractIntegrationTest;
import com.mishanthrope.queueboard.core.domain.order.OrderStatus;
import com.mishanthrope.queueboard.core.infrastructure.persistence.order.OrderEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PosterWebhookIntegrationTest extends AbstractIntegrationTest {

    private static final long CHECK_ID = 4801L;

    private ResultActions postRaw(String body) throws Exception {
        return mockMvc.perform(post("/api/webhooks/poster")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void webhook_withValidSignature_returns200AndCreatesOrderWithItems() throws Exception {
        String bearer = registerAndLogin();

        postRaw(webhooks.json("transaction", CHECK_ID, "changed"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/orders/kitchen").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].posterCheckId").value(CHECK_ID))
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[0].displayNumber").value(1))
                .andExpect(jsonPath("$[0].items", not(hasSize(0))))
                .andExpect(jsonPath("$[0].estimatedReadyAt").isNotEmpty());
    }

    @Test
    void webhook_sentTwice_keepsExactlyOneOrder() throws Exception {
        postRaw(webhooks.json("transaction", CHECK_ID, "changed")).andExpect(status().isOk());
        postRaw(webhooks.json("transaction", CHECK_ID, "changed")).andExpect(status().isOk());
        postRaw(webhooks.json("transaction", CHECK_ID, "added")).andExpect(status().isOk());

        assertThat(orderEntities.count()).isEqualTo(1);
    }

    @Test
    void webhook_removed_cancelsExistingOrder() throws Exception {
        OrderEntity order = createOrder(CHECK_ID);

        postRaw(webhooks.json("transaction", CHECK_ID, "removed")).andExpect(status().isOk());

        assertThat(orderEntities.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void webhook_removedForUnknownCheck_returns200() throws Exception {
        postRaw(webhooks.json("transaction", CHECK_ID, "removed")).andExpect(status().isOk());

        assertThat(orderEntities.count()).isZero();
    }

    @Test
    void webhook_withWrongSignature_returns401AndCreatesNothing() throws Exception {
        postRaw(webhooks.jsonWithVerify("transaction", CHECK_ID, "changed",
                "00000000000000000000000000000000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        assertThat(orderEntities.count()).isZero();
    }

    @Test
    void webhook_withoutSignature_returns401() throws Exception {
        postRaw("""
                {"account":"shawarma","object":"transaction","object_id":1,"action":"changed",\
                "time":"1794919500"}""")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_withMalformedJson_returns400() throws Exception {
        postRaw("{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void webhook_forOtherObject_returns200AndCreatesNothing() throws Exception {
        postRaw(webhooks.json("product", CHECK_ID, "changed")).andExpect(status().isOk());

        assertThat(orderEntities.count()).isZero();
    }
}
