package com.minicommerce.global.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class InternalAuthFilterTest {

    private static final String KEY = "internal-key-current";
    private static final String NEW_KEY = "internal-key-rotated";

    private MockMvc mockMvcWith(String acceptedKeysCsv) {
        return MockMvcBuilders.standaloneSetup(new StubInternalController())
                .addFilters(new InternalAuthFilter(acceptedKeysCsv))
                .build();
    }

    @Test
    @DisplayName("올바른 키: 통과")
    void accepts_valid_key() throws Exception {
        mockMvcWith(KEY).perform(get("/internal/ping").header(InternalAuthFilter.HEADER, KEY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("헤더 누락: 404 — 엔드포인트 존재를 드러내지 않는다")
    void rejects_missing_header() throws Exception {
        mockMvcWith(KEY).perform(get("/internal/ping"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("틀린 키: 404")
    void rejects_wrong_key() throws Exception {
        mockMvcWith(KEY).perform(get("/internal/ping").header(InternalAuthFilter.HEADER, "nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("빈 키: 404 — 빈 문자열이 허용 목록과 우연히 맞지 않는다")
    void rejects_blank_key() throws Exception {
        mockMvcWith(KEY).perform(get("/internal/ping").header(InternalAuthFilter.HEADER, ""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("로테이션 중(구키+신키 동시 허용): 양쪽 다 통과")
    void accepts_both_keys_during_rotation() throws Exception {
        MockMvc mockMvc = mockMvcWith(KEY + " , " + NEW_KEY);

        mockMvc.perform(get("/internal/ping").header(InternalAuthFilter.HEADER, KEY))
                .andExpect(status().isOk());
        mockMvc.perform(get("/internal/ping").header(InternalAuthFilter.HEADER, NEW_KEY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로테이션 완료(구키 제거) 후: 구키는 거부")
    void rejects_retired_key_after_rotation() throws Exception {
        mockMvcWith(NEW_KEY).perform(get("/internal/ping").header(InternalAuthFilter.HEADER, KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("허용 키 미설정: 기동 시점에 실패 — 전 호출 404로 조용히 죽지 않는다")
    void fails_fast_when_no_key_configured() {
        assertThatThrownBy(() -> new InternalAuthFilter("  ,  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INTERNAL_API_KEY");
    }

    @RestController
    static class StubInternalController {
        @GetMapping("/internal/ping")
        String ping() {
            return "pong";
        }
    }
}
