package com.minicommerce.inventory;

import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final InventoryReservationRepository reservationRepository;

    private final DefaultRedisScript<Long> reserveScript = new DefaultRedisScript<>("""
            local reservationKey = KEYS[#KEYS]
            local itemCount = #KEYS - 1

            if redis.call('HGET', reservationKey, 'status') then
                return 2
            end

            for i = 1, itemCount do
              local stock = tonumber(redis.call('GET', KEYS[i]) or '0')
              local quantity = tonumber(ARGV[i])
              if stock < quantity then
                return 0
              end
            end

            for i = 1, itemCount do
              redis.call('DECRBY', KEYS[i], tonumber(ARGV[i]))
            end

            redis.call('HSET', reservationKey, 'status', 'RESERVED', 'items', ARGV[itemCount + 1])
            redis.call('EXPIRE', reservationKey, tonumber(ARGV[itemCount + 2]))
            return 1
            """, Long.class);

    private final DefaultRedisScript<Long> releaseScript = new DefaultRedisScript<>("""
            local reservationKey = KEYS[1]
            local status = redis.call('HGET', reservationKey, 'status')

            if status ~= 'RESERVED' then
              return 0
            end

            for i = 2, #KEYS do
              redis.call('INCRBY', KEYS[i], tonumber(ARGV[i - 1]))
            end

            redis.call('HSET', reservationKey, 'status', 'RELEASED')
            redis.call('EXPIRE', reservationKey, 86400)
            return 1
            """, Long.class);

    private final DefaultRedisScript<Long> confirmScript = new DefaultRedisScript<>("""
            local status = redis.call('HGET', KEYS[1], 'status')

            if status ~= 'RESERVED' then
              return 0
            end

            redis.call('HSET', KEYS[1], 'status', 'CONFIRMED')
            redis.call('EXPIRE', KEYS[1], 86400)
            return 1
            """, Long.class);

    // 확정재고 재입고(주문취소). 해시가 CONFIRMED면 복원, 이미 RESTOCKED면 INCRBY 스킵(멱등, 이중 복원 방지).
    // confirm 후 TTL 24h가 지나 해시가 없을 수도 있으나 DB 원장이 이미 가드를 통과한 상태이므로 그때도 INCRBY 후 해시를 재기록한다.
    private final DefaultRedisScript<Long> restockScript = new DefaultRedisScript<>("""
            local reservationKey = KEYS[1]
            local status = redis.call('HGET', reservationKey, 'status')

            if status == 'RESTOCKED' then
              return 2
            end

            if status and status ~= 'CONFIRMED' then
              return 0
            end

            for i = 2, #KEYS do
              redis.call('INCRBY', KEYS[i], tonumber(ARGV[i - 1]))
            end

            redis.call('HSET', reservationKey, 'status', 'RESTOCKED')
            redis.call('EXPIRE', reservationKey, 86400)
            return 1
            """, Long.class);

    public InventoryService(StringRedisTemplate redisTemplate, InventoryReservationRepository reservationRepository) {
        this.redisTemplate = redisTemplate;
        this.reservationRepository = reservationRepository;
    }

    public void setStock(String productId, long stock) {
        redisTemplate.opsForValue().set(stockKey(productId), String.valueOf(stock));
    }

    public long availableStock(String productId, long defaultStock) {
        String value = redisTemplate.opsForValue().get(stockKey(productId));
        if (value == null) {
            return defaultStock;
        }
        return Long.parseLong(value);
    }

    public InventoryHold reserve(List<InventoryItem> items) {
        String reservationId = UUID.randomUUID().toString();
        String reservationKey = reservationKey(reservationId);
        List<String> keys = stockKeys(items);
        keys.add(reservationKey);

        List<String> args = items.stream()
                .map(item -> String.valueOf(item.quantity()))
                .toList();
        String itemPayload = items.stream()
                .map(item -> item.productId() + ":" + item.quantity())
                .reduce((left, right) -> left + "," + right)
                .orElse("");

        List<String> scriptArgs = new java.util.ArrayList<>(args);
        scriptArgs.add(itemPayload);
        scriptArgs.add(String.valueOf(RESERVATION_TTL.toSeconds()));

        Long result = redisTemplate.execute(reserveScript, keys, scriptArgs.toArray(String[]::new));
        if (result == null || result != 1L) {
            throw new OutOfStockException(items.getFirst().productId());
        }

        return new InventoryHold(reservationId, Instant.now().plus(RESERVATION_TTL), items);
    }

    public boolean release(InventoryHold hold) {
        List<String> keys = new java.util.ArrayList<>();
        keys.add(reservationKey(hold.reservationId()));
        keys.addAll(stockKeys(hold.items()));

        String[] args = hold.items().stream()
                .map(item -> String.valueOf(item.quantity()))
                .toArray(String[]::new);

        Long result = redisTemplate.execute(releaseScript, keys, args);
        return result != null && result == 1L;
    }

    public boolean confirm(String reservationId) {
        Long result = redisTemplate.execute(confirmScript, List.of(reservationKey(reservationId)));
        return result != null && result == 1L;
    }

    public void createReservationForOrder(String orderId, String reservationId, Instant expiresAt, List<InventoryItem> items) {
        List<ReservationLine> lines = items.stream()
                .map(item -> new ReservationLine(item.productId(), item.quantity()))
                .toList();
        reservationRepository.save(new InventoryReservation(reservationId, orderId, expiresAt, lines));
    }

    public void confirmByOrderId(String orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found for order: " + orderId));
        reservation.confirm();
        confirm(reservation.getId());
    }

    public void restockByOrderId(String orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found for order: " + orderId));
        // DB 원장이 진실의 원천 — 이미 RESTOCKED면 Redis INCRBY를 건너뛰어 이중 복원을 막는다.
        if (!reservation.restock()) {
            return;
        }
        List<String> keys = new java.util.ArrayList<>();
        keys.add(reservationKey(reservation.getId()));
        keys.addAll(reservation.getLines().stream()
                .map(line -> stockKey(line.getProductId()))
                .toList());
        String[] args = reservation.getLines().stream()
                .map(line -> String.valueOf(line.getQuantity()))
                .toArray(String[]::new);

        Long result = redisTemplate.execute(restockScript, keys, args);
        if (result == null || result == 0L) {
            throw new IllegalStateException("Restock failed for reservation: " + reservation.getId());
        }
    }

    private static List<String> stockKeys(List<InventoryItem> items) {
        return new java.util.ArrayList<>(items.stream()
                .map(item -> stockKey(item.productId()))
                .toList());
    }

    private static String stockKey(String productId) {
        return "stock:" + productId;
    }

    private static String reservationKey(String reservationId) {
        return "reservation:" + reservationId;
    }
}
