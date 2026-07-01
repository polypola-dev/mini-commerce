package com.minicommerce.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test/out-of-stock")
        void outOfStock() {
            throw new OutOfStockException("prod-1");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new InventoryApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("OutOfStockException → 409 Conflict, title 'Out of stock', type 'out-of-stock' 포함")
    void outOfStockException_returns409WithProblemDetail() throws Exception {
        mockMvc.perform(get("/test/out-of-stock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Out of stock"))
                .andExpect(jsonPath("$.type").value(
                        org.hamcrest.Matchers.containsString("out-of-stock")));
    }
}
