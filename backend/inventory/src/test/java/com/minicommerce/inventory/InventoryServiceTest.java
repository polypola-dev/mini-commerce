package com.minicommerce.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        // opsForValue() 호출 시 mock ValueOperations 반환
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ----------------------------------------------------------------
    // initializeStockIfAbsent
    // ----------------------------------------------------------------

    @Test
    @DisplayName("initializeStockIfAbsent: stock:{productId} 키로 Redis setIfAbsent 호출")
    void initializeStockIfAbsent_callsSetIfAbsent() {
        // when
        inventoryService.initializeStockIfAbsent("prod-1", 100L);

        // then
        verify(valueOps).setIfAbsent("stock:prod-1", "100");
    }

    // ----------------------------------------------------------------
    // availableStock
    // ----------------------------------------------------------------

    @Test
    @DisplayName("availableStock: Redis에 값이 있으면 Long 파싱 후 반환")
    void availableStock_whenKeyExists_returnsParsedValue() {
        // given
        when(valueOps.get("stock:prod-1")).thenReturn("50");

        // when
        long stock = inventoryService.availableStock("prod-1", 100L);

        // then
        assertThat(stock).isEqualTo(50L);
        // 이미 값이 있으면 초기화 안 함
        verify(valueOps, never()).setIfAbsent(any(), any());
    }

    @Test
    @DisplayName("availableStock: Redis에 값이 없으면 기본값을 반환하되 Redis에 쓰지 않음(read-only)")
    void availableStock_whenKeyMissing_returnsDefaultWithoutWriting() {
        // given
        when(valueOps.get("stock:prod-1")).thenReturn(null);

        // when
        long stock = inventoryService.availableStock("prod-1", 100L);

        // then
        assertThat(stock).isEqualTo(100L);
        // 조회는 부작용을 남기지 않는다 — 초기화는 별도로 명시 호출해야 한다
        verify(valueOps, never()).setIfAbsent(any(), any());
        verify(valueOps, never()).set(any(), any());
    }

    @Test
    @DisplayName("availableStock: 키가 없는 상태로 반복 조회해도 이전 defaultStock에 고착되지 않음(회귀)")
    void availableStock_whenKeyMissing_repeatedCallsDoNotStickToStaleDefault() {
        // given — 상품이 아직 seed되지 않아 Redis에 키가 없는 상태를 재현
        when(valueOps.get("stock:prod-1")).thenReturn(null);

        // when — 배치 조회(default=0)가 먼저 들어오고, 이어서 실제 재고로 조회되는 시나리오
        long firstRead = inventoryService.availableStock("prod-1", 0L);
        long secondRead = inventoryService.availableStock("prod-1", 100L);

        // then — 첫 조회가 0을 반환했다고 해서 두 번째 조회가 0으로 고착되지 않는다
        assertThat(firstRead).isEqualTo(0L);
        assertThat(secondRead).isEqualTo(100L);
        verify(valueOps, never()).setIfAbsent(any(), any());
    }

    // ----------------------------------------------------------------
    // reserve
    // reserve Lua 스크립트 args: [qty, itemPayload, ttlSeconds] → varargs 3개
    // ----------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("reserve: Lua 스크립트 결과 1 → InventoryHold 정상 반환")
    void reserve_success_returnsInventoryHold() {
        // given — varargs: qty("3"), payload("prod-1:3"), ttl("600") → 3개
        doReturn(1L).when(redisTemplate).execute(any(), anyList(), any(), any(), any());
        List<InventoryItem> items = List.of(new InventoryItem("prod-1", 3L));

        // when
        InventoryHold hold = inventoryService.reserve(items);

        // then
        assertThat(hold).isNotNull();
        assertThat(hold.reservationId()).isNotNull().isNotEmpty();
        assertThat(hold.items()).isEqualTo(items);
        assertThat(hold.expiresAt()).isAfter(Instant.now().minusSeconds(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("reserve: Lua 스크립트 결과 0 → OutOfStockException 발생")
    void reserve_outOfStock_throwsException() {
        // given — varargs 3개
        doReturn(0L).when(redisTemplate).execute(any(), anyList(), any(), any(), any());
        List<InventoryItem> items = List.of(new InventoryItem("prod-1", 99L));

        // when & then
        assertThatThrownBy(() -> inventoryService.reserve(items))
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining("prod-1");
    }

    // ----------------------------------------------------------------
    // release
    // release Lua 스크립트 args: [qty] → varargs 1개 (항목 1개 기준)
    // ----------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("release: Lua 스크립트 결과 1 → true 반환")
    void release_success_returnsTrue() {
        // given — varargs: qty("2") → 1개
        doReturn(1L).when(redisTemplate).execute(any(), anyList(), any());
        InventoryHold hold = new InventoryHold(
                "res-1",
                Instant.now().plusSeconds(600),
                List.of(new InventoryItem("prod-1", 2L))
        );

        // when
        boolean result = inventoryService.release(hold);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("release: Lua 스크립트 결과 0 → false 반환")
    void release_fail_returnsFalse() {
        // given — varargs 1개
        doReturn(0L).when(redisTemplate).execute(any(), anyList(), any());
        InventoryHold hold = new InventoryHold(
                "res-1",
                Instant.now().plusSeconds(600),
                List.of(new InventoryItem("prod-1", 2L))
        );

        // when
        boolean result = inventoryService.release(hold);

        // then
        assertThat(result).isFalse();
    }

    // ----------------------------------------------------------------
    // confirm
    // confirm Lua 스크립트는 keys만 사용, args 없음 → varargs 0개
    // ----------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("confirm: Lua 스크립트 결과 1 → true 반환")
    void confirm_success_returnsTrue() {
        // given — varargs 없음(0개)
        doReturn(1L).when(redisTemplate).execute(any(), anyList());

        // when
        boolean result = inventoryService.confirm("res-1");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("confirm: Lua 스크립트 결과 0 → false 반환")
    void confirm_fail_returnsFalse() {
        // given
        doReturn(0L).when(redisTemplate).execute(any(), anyList());

        // when
        boolean result = inventoryService.confirm("res-1");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("confirm: Lua 스크립트 결과 null → false 반환")
    void confirm_null_returnsFalse() {
        // given — null 반환
        doReturn(null).when(redisTemplate).execute(any(), anyList());

        // when
        boolean result = inventoryService.confirm("res-1");

        // then
        assertThat(result).isFalse();
    }
}
