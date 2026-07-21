package com.minicommerce.catalog;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * inventory-api 재고 조회의 마지막 성공값 캐시(D6). 서킷이 열렸거나 조회가 실패했을 때
 * {@link InventoryStockGateway}가 0 대신 이 값을 돌려주기 위한 것이다.
 *
 * <p><b>키 네임스페이스는 {@code catalog:stock:*} 로 고정한다.</b> inventory-api가 원장으로 쓰는
 * {@code stock:*} / {@code reservation:*} 키와 같은 Redis를 공유할 수 있으므로, 이 캐시는 자기
 * 네임스페이스 밖을 절대 읽거나 쓰지 않는다 — 재고 원장을 캐시가 덮어쓰는 사고를 구조적으로 막는다.
 *
 * <p><b>Redis 장애가 상품 조회를 깨뜨리면 안 된다.</b> 폴백을 위해 존재하는 캐시가 그 자체로 새
 * 장애원이 되면 목적이 뒤집힌다. 그래서 모든 read/write를 삼키고(로그만 남김) 캐시 미적중과 같이
 * 취급한다.
 */
@Component
public class InventoryStockCache {

    private static final Logger log = LoggerFactory.getLogger(InventoryStockCache.class);
    private static final String KEY_PREFIX = "catalog:stock:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public InventoryStockCache(StringRedisTemplate redisTemplate,
                               @Value("${app.inventory.stock-cache.ttl:1h}") Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    /**
     * 요청한 productId 중 캐시에 남아 있는 것만 담아 돌려준다. 미적중분은 키 자체가 빠진다 —
     * 호출자가 "캐시에 없음"과 "재고 0"을 구분할 수 있어야 하기 때문이다.
     */
    public Map<String, Long> readAll(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        try {
            List<String> keys = productIds.stream().map(InventoryStockCache::key).toList();
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                return Map.of();
            }
            Map<String, Long> cached = new LinkedHashMap<>();
            for (int i = 0; i < productIds.size() && i < values.size(); i++) {
                String value = values.get(i);
                if (value == null) {
                    continue;
                }
                try {
                    cached.put(productIds.get(i), Long.parseLong(value));
                } catch (NumberFormatException e) {
                    log.warn("재고 폴백 캐시에 숫자가 아닌 값이 있어 무시한다: key={}, value={}",
                            key(productIds.get(i)), value);
                }
            }
            return cached;
        } catch (RuntimeException e) {
            log.warn("재고 폴백 캐시 조회 실패 — 미적중으로 처리한다: ids={}", productIds, e);
            return Map.of();
        }
    }

    public void writeAll(Map<String, Long> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return;
        }
        try {
            stocks.forEach((productId, stock) ->
                    redisTemplate.opsForValue().set(key(productId), String.valueOf(stock), ttl));
        } catch (RuntimeException e) {
            log.warn("재고 폴백 캐시 갱신 실패 — 조회 결과에는 영향 없다: ids={}", stocks.keySet(), e);
        }
    }

    public void write(String productId, long stock) {
        writeAll(Map.of(productId, stock));
    }

    private static String key(String productId) {
        return KEY_PREFIX + productId;
    }
}
