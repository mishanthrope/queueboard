package com.mishanthrope.queueboard.core.presentation.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mishanthrope.queueboard.core.application.common.ApplicationError;
import com.mishanthrope.queueboard.core.domain.common.DomainError;
import com.mishanthrope.queueboard.core.domain.order.OrderNotFoundError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void unmappedDomainError_isBadRequestWithItsMessage() throws Exception {
        mockMvc.perform(get("/unmapped-domain"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("some domain rule"));
    }

    @Test
    void unmappedApplicationError_isBadRequestWithItsMessage() throws Exception {
        mockMvc.perform(get("/unmapped-application"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("some use case failure"));
    }

    @Test
    void mappedDomainError_keepsItsOwnStatus() throws Exception {
        mockMvc.perform(get("/mapped"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order 5 not found"));
    }

    @Test
    void unexpectedException_isInternalServerError() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/unmapped-domain")
        void unmappedDomain() {
            throw new DomainError("some domain rule") {
            };
        }

        @GetMapping("/unmapped-application")
        void unmappedApplication() {
            throw new ApplicationError("some use case failure") {
            };
        }

        @GetMapping("/mapped")
        void mapped() {
            throw new OrderNotFoundError(5);
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("boom");
        }
    }
}
