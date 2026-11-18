package com.mishanthrope.queueboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.mishanthrope.queueboard.employee.repository.EmployeeRepository;
import com.mishanthrope.queueboard.order.model.Order;
import com.mishanthrope.queueboard.order.repository.OrderRepository;
import com.mishanthrope.queueboard.poster.SignedWebhooks;
import com.mishanthrope.queueboard.poster.config.PosterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Tag("integration")
public abstract class AbstractIntegrationTest {

    protected static final String EMAIL = "cook@example.com";
    protected static final String PASSWORD = "secret123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected EmployeeRepository employeeRepository;

    protected SignedWebhooks webhooks;

    @Autowired
    private PosterProperties posterProperties;

    @BeforeEach
    protected void cleanDatabase() {
        orderRepository.deleteAll();
        employeeRepository.deleteAll();
        webhooks = new SignedWebhooks(posterProperties);
    }

    protected void sendWebhook(String object, long objectId, String action) throws Exception {
        mockMvc.perform(post("/api/webhooks/poster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhooks.json(object, objectId, action)))
                .andExpect(status().isOk());
    }

    protected Order createOrder(long posterCheckId) throws Exception {
        sendWebhook("transaction", posterCheckId, "changed");
        return orderRepository.findByPosterCheckId(posterCheckId).orElseThrow();
    }

    protected String registerAndLogin() throws Exception {
        String credentials = """
                {"email":"%s","password":"%s","fullName":"Іван Кухар"}"""
                .formatted(EMAIL, PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.<String>read(body, "$.token");
    }
}
