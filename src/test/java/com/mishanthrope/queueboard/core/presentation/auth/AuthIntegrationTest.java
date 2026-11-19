package com.mishanthrope.queueboard.core.presentation.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mishanthrope.queueboard.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String REGISTER_BODY = """
            {"email":"cook@example.com","password":"secret123","fullName":"Іван Кухар"}""";

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void register_withValidBody_returns201AndEmployee() throws Exception {
        postJson("/api/auth/register", REGISTER_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("cook@example.com"))
                .andExpect(jsonPath("$.fullName").value("Іван Кухар"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_withSameEmailTwice_returns409() throws Exception {
        postJson("/api/auth/register", REGISTER_BODY).andExpect(status().isCreated());

        postJson("/api/auth/register", REGISTER_BODY.replace("cook@", "COOK@"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void register_withInvalidBody_returns400() throws Exception {
        postJson("/api/auth/register", """
                {"email":"not-an-email","password":"short","fullName":""}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void login_withCorrectCredentials_returns200AndToken() throws Exception {
        postJson("/api/auth/register", REGISTER_BODY).andExpect(status().isCreated());

        postJson("/api/auth/login", """
                {"email":"cook@example.com","password":"secret123"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        postJson("/api/auth/register", REGISTER_BODY).andExpect(status().isCreated());

        postJson("/api/auth/login", """
                {"email":"cook@example.com","password":"wrong-password"}""")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401WithJsonError() throws Exception {
        mockMvc.perform(get("/api/orders/kitchen"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401WithJsonError() throws Exception {
        mockMvc.perform(get("/api/orders/kitchen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void protectedEndpoint_withValidToken_returns200() throws Exception {
        String bearer = registerAndLogin();

        mockMvc.perform(get("/api/orders/kitchen").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());
    }
}
