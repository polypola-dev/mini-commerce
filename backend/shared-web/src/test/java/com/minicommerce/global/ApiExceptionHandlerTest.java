package com.minicommerce.global;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    /**
     * 예외 발생용 더미 컨트롤러 — 실제 비즈니스 로직 없이 각 예외를 throw
     */
    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new EntityNotFoundException("Product not found");
        }

        @PostMapping("/test/validation")
        void validation(@Valid @RequestBody ValidRequest req) {
            // 검증 실패 시 MethodArgumentNotValidException 자동 발생
        }

        static class ValidRequest {
            @NotNull(message = "name이 필수입니다")
            public String name;
        }
    }

    @BeforeEach
    void setUp() {
        // Bean Validation 활성화를 위해 LocalValidatorFactoryBean 직접 구성
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ----------------------------------------------------------------
    // EntityNotFoundException → 404 Not Found
    // ----------------------------------------------------------------

    @Test
    @DisplayName("EntityNotFoundException → 404 Not Found, title 'Not found'")
    void entityNotFoundException_returns404WithProblemDetail() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"));
    }

    // ----------------------------------------------------------------
    // MethodArgumentNotValidException → 400 Bad Request
    // ----------------------------------------------------------------

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 Bad Request, title 'Invalid request'")
    void methodArgumentNotValidException_returns400WithProblemDetail() throws Exception {
        // name 필드가 null → @NotNull 검증 실패
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }
}
