package com.minicommerce.inventory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private static final Duration RESERVATION_TTL = Duration.ofMinutes(10);
    // 예약 해시 TTL(24h) — 예약 만료(10분)와 의도적으로 분리한다(GH #3 S3). 만료의 권위는 DB
    // expires_at 단독이고, 해시는 Redis 차감 여부의 증거로만 쓴다. 과거엔 해시 TTL=예약 TTL이라
    // 리퍼가 도는 시점(만료 후)엔 해시가 이미 소멸해 release Lua가 0을 반환 → INCRBY가 영영
    // 실행되지 않는 재고 누수 + 원장 RESERVED 고착이 있었다. 24h는 release/confirm/restock
    // 해시 TTL과 동일한 값으로, "해시 없음 = Redis 차감이 없었음" 불변식을 성립시킨다.
    private static final long RESERVATION_HASH_TTL_SECONDS = 86_400L;

    // 오버셀 발생 상품 구분용 태그 키(어떤 상품에서 얼마나 자주 오버셀되는지 관측 — 한정판 추적 목적).
    private static final AttributeKey<String> PRODUCT_ID_KEY = AttributeKey.stringKey("product_id");

    private final StringRedisTemplate redisTemplate;
    private final InventoryReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;
    // payment-wins force-confirm이 재고 소진(오버셀)으로 실패한 예약 라인 수량을 상품별로 집계한다.
    private final LongCounter oversoldCounter;

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

    // 반환값: 1=복원 완료(INCRBY), 3=해시 없음(차감이 없었음 — INCRBY 생략), 4=이미 RELEASED
    // (이전 시도가 복원까지 마치고 원장 전이 전에 끊긴 재시도 — INCRBY 생략), 0=release 불가 상태
    // (CONFIRMED/RESTOCKED — 결제가 이긴 경합). 1/3/4는 호출자가 원장을 RELEASED로 전이해야 한다.
    private final DefaultRedisScript<Long> releaseScript = new DefaultRedisScript<>("""
            local reservationKey = KEYS[1]
            local status = redis.call('HGET', reservationKey, 'status')

            if status == false then
              return 3
            end

            if status == 'RELEASED' then
              return 4
            end

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

    // 결제가 이긴 경합(payment-wins, GH #3 설계 D-C). 리퍼가 먼저 예약을 RELEASED로 만든 뒤
    // order.paid가 도착한 경우 — 결제는 이미 승인됐으므로 재고를 다시 차감(DECRBY)하고 CONFIRMED로
    // 강제한다. 단 reserve와 동일한 2-패스 구조로, 먼저 전 품목의 재고를 검사해 하나라도 부족하면
    // 아무것도 차감하지 않고 0을 반환한다 — 리퍼가 풀어준 재고를 이미 다른 주문이 채간 경우
    // (오버셀)를 막기 위함이다. 이미 CONFIRMED/RESTOCKED면 스킵(2)해 재전달·이중차감을 막는다.
    // 반환값: 1=재차감 후 CONFIRMED, 2=이미 확정/재입고(멱등 스킵), 0=재고 부족(force-confirm 불가 — 호출자가 오버셀 처리).
    // 해시가 TTL로 소멸했어도 DB 원장이 가드를 통과한 상태이므로 통과 시 DECRBY 후 해시를 재기록한다.
    private final DefaultRedisScript<Long> forceConfirmScript = new DefaultRedisScript<>("""
            local reservationKey = KEYS[1]
            local status = redis.call('HGET', reservationKey, 'status')

            if status == 'CONFIRMED' or status == 'RESTOCKED' then
              return 2
            end

            for i = 2, #KEYS do
              local stock = tonumber(redis.call('GET', KEYS[i]) or '0')
              local quantity = tonumber(ARGV[i - 1])
              if stock < quantity then
                return 0
              end
            end

            for i = 2, #KEYS do
              redis.call('DECRBY', KEYS[i], tonumber(ARGV[i - 1]))
            end

            redis.call('HSET', reservationKey, 'status', 'CONFIRMED')
            redis.call('EXPIRE', reservationKey, 86400)
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

    public InventoryService(StringRedisTemplate redisTemplate, InventoryReservationRepository reservationRepository,
            ApplicationEventPublisher eventPublisher, OpenTelemetry openTelemetry) {
        this.redisTemplate = redisTemplate;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
        this.oversoldCounter = openTelemetry.getMeter("inventory-api")
                .counterBuilder("inventory.reservation.oversold")
                .setDescription("payment-wins force-confirm이 재고 소진으로 실패한(오버셀) 예약 라인 수량")
                .setUnit("{item}")
                .build();
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

    public Optional<InventoryReservation> getByOrderId(String orderId) {
        return reservationRepository.findByOrderId(UUID.fromString(orderId));
    }

    /**
     * 주문 예약(GH #3 S3). 예약 ID = orderId(1주문 1예약 — 멱등 키). 트랜잭션 밖에서 호출해야 하며
     * 순서가 계약이다: ① DB 원장 insert 커밋(리퍼가 항상 찾을 수 있는 진실의 원천을 먼저 확보)
     * → ② Redis Lua(재고 검사+차감+예약 해시). ①과 ② 사이 크래시는 만료 후 리퍼가 release Lua
     * return 3(해시 없음 — INCRBY 생략)으로 정리한다. 재시도(타임아웃 후 재호출)는 기존 RESERVED
     * 원장을 승계하고 Lua를 재실행하므로(해시가 이미 있으면 2=성공) 크래시 창을 스스로 치유한다.
     *
     * @throws OutOfStockException 재고 부족(원장은 RELEASED로 전이)
     * @throws ReservationConflictException 같은 orderId가 이미 RESERVED 외 상태(재예약 불가)
     */
    public InventoryHold reserveForOrder(String orderId, List<InventoryItem> items) {
        UUID orderUuid = UUID.fromString(orderId);
        InventoryReservation reservation = reservationRepository.findByOrderId(orderUuid).orElse(null);
        if (reservation == null) {
            List<ReservationLine> lines = items.stream()
                    .map(item -> new ReservationLine(UUID.fromString(item.productId()), item.quantity()))
                    .toList();
            try {
                // 예약 ID = orderId(1주문 1예약 멱등 키). 파싱한 uuid 하나를 id/orderId 두 필드에 모두 넣는다.
                reservation = reservationRepository.save(new InventoryReservation(
                        orderUuid, orderUuid, Instant.now().plus(RESERVATION_TTL), lines));
            } catch (DataIntegrityViolationException e) {
                // 동시 중복 호출 — unique(order_id)/PK에서 진 쪽은 먼저 커밋된 원장을 승계한다(멱등).
                reservation = reservationRepository.findByOrderId(orderUuid).orElseThrow(() -> e);
            }
        }
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new ReservationConflictException(orderId, reservation.getStatus());
        }

        // 품목/수량은 요청이 아니라 원장을 권위로 삼는다 — 재시도 시 요청 내용이 달라져도 원장이 이긴다.
        List<InventoryItem> ledgerItems = reservation.getLines().stream()
                .map(line -> new InventoryItem(line.getProductId().toString(), line.getQuantity()))
                .toList();

        List<String> keys = stockKeys(ledgerItems);
        keys.add(reservationKey(reservation.getId().toString()));
        List<String> scriptArgs = new java.util.ArrayList<>(ledgerItems.stream()
                .map(item -> String.valueOf(item.quantity()))
                .toList());
        scriptArgs.add(itemPayload(ledgerItems));
        scriptArgs.add(String.valueOf(RESERVATION_HASH_TTL_SECONDS));

        Long result = redisTemplate.execute(reserveScript, keys, scriptArgs.toArray(String[]::new));
        if (result == null || result == 0L) {
            markReleased(orderId);
            throw new OutOfStockException(ledgerItems.getFirst().productId());
        }
        // 1(신규 차감) 또는 2(해시 이미 존재 — 재시도) 모두 성공.
        return new InventoryHold(reservation.getId().toString(), reservation.getExpiresAt(), ledgerItems);
    }

    /**
     * 예약 해제(주문 생성 실패 보상 + 리퍼 만료 공용). RESERVED 원장만 대상으로 release Lua를
     * 실행하고 원장을 RELEASED로 전이한다. 원장이 없거나 이미 다른 상태면 no-op(멱등).
     *
     * @return 이번 호출로 원장이 RELEASED로 전이됐으면 true — 리퍼는 이때만 만료 이벤트를 발행한다
     */
    public boolean releaseByOrderId(String orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(UUID.fromString(orderId)).orElse(null);
        if (reservation == null || reservation.getStatus() != ReservationStatus.RESERVED) {
            return false;
        }

        List<String> keys = new java.util.ArrayList<>();
        keys.add(reservationKey(reservation.getId().toString()));
        keys.addAll(reservation.getLines().stream()
                .map(line -> stockKey(line.getProductId().toString()))
                .toList());
        String[] args = reservation.getLines().stream()
                .map(line -> String.valueOf(line.getQuantity()))
                .toArray(String[]::new);

        Long result = redisTemplate.execute(releaseScript, keys, args);
        if (result == null || result == 0L) {
            // 해시가 CONFIRMED/RESTOCKED — 결제 확정이 이긴 경합. 원장 전이는 confirm 경로가 맡는다.
            return false;
        }
        // 1(복원), 3(해시 없음 — 미차감 크래시 창), 4(복원은 이전 시도에서 완료) 전부 원장만 전이.
        reservation.release();
        reservationRepository.save(reservation);
        return true;
    }

    /**
     * 결제 확정(order.paid 소비, GH #3 S4 코레오그래피). 정상 경로는 RESERVED→CONFIRMED.
     * 리퍼가 먼저 예약을 RELEASED로 만든 경합에서는 재고가 실제로 남아있는지 확인한 뒤에만 재차감+CONFIRMED로
     * 강제한다(payment-wins, D-C). 리퍼가 풀어준 재고를 이미 다른 주문이 채가 재고가 없으면(오버셀) 강제 확정
     * 대신 예약을 OVERSOLD로 표시하고 {@link InventoryReservationOversoldEvent}를 발행해 주문 자동 취소+환불을
     * 요청한다. CONFIRMED는 멱등 no-op, RESTOCKED/OVERSOLD는 이미 종결된 주문이라 확정을 적용하지 않고
     * 경고만 남긴다(재전달 방어).
     */
    @Transactional
    public void confirmByOrderId(String orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(UUID.fromString(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found for order: " + orderId));
        ReservationStatus before = reservation.getStatus();
        switch (before) {
            case CONFIRMED -> {
                return; // 멱등: 이미 확정됨
            }
            case RESTOCKED -> {
                log.warn("order.paid for already-restocked reservation orderId={} — skipping confirm", orderId);
                return;
            }
            case OVERSOLD -> {
                // 이미 오버셀로 판정돼 자동 취소+환불이 요청된 예약(order.paid 재전달). 멱등 스킵 — 이벤트 재발행 안 함.
                log.warn("order.paid for already-oversold reservation orderId={} — skipping confirm", orderId);
                return;
            }
            case RESERVED -> {
                reservation.confirm();
                reservationRepository.save(reservation);
                redisTemplate.execute(confirmScript, List.of(reservationKey(reservation.getId().toString())));
            }
            case RELEASED, EXPIRED -> {
                // payment-wins: 리퍼가 이긴 경합. Lua를 먼저 실행해 재고가 실제로 남아있는지 확인한 뒤에만
                // 원장을 확정한다(Lua 실패 시 원장이 CONFIRMED로 앞서가는 결함 방지).
                List<String> keys = new java.util.ArrayList<>();
                keys.add(reservationKey(reservation.getId().toString()));
                keys.addAll(reservation.getLines().stream()
                        .map(line -> stockKey(line.getProductId().toString()))
                        .toList());
                String[] args = reservation.getLines().stream()
                        .map(line -> String.valueOf(line.getQuantity()))
                        .toArray(String[]::new);
                Long result = redisTemplate.execute(forceConfirmScript, keys, args);
                if (result != null && (result == 1L || result == 2L)) {
                    // 재고가 남아있어 재차감 성공(또는 이미 확정된 멱등) — 결제 승인대로 확정한다.
                    log.warn("payment won the expiry race for orderId={} (reservation was {}) — force-confirming, "
                            + "stock may go momentarily negative", orderId, before);
                    reservation.forceConfirm();
                    reservationRepository.save(reservation);
                } else {
                    // 0/null: 리퍼가 풀어준 재고를 이미 다른 주문이 채감 — force-confirm 불가(오버셀).
                    // 정직하게 예약을 OVERSOLD로 표시하고 주문 자동 취소+환불을 요청한다.
                    log.warn("payment won the expiry race for orderId={} but stock was already claimed by another "
                            + "order — cannot force-confirm, marking oversold and requesting cancel+refund", orderId);
                    reservation.markOversold();
                    reservationRepository.save(reservation);
                    // 상품별 오버셀 수량을 관측 — 어떤 상품(특히 한정판)에서 얼마나 자주 발생하는지 태그로 구분.
                    for (ReservationLine line : reservation.getLines()) {
                        oversoldCounter.add(line.getQuantity(),
                                Attributes.of(PRODUCT_ID_KEY, line.getProductId().toString()));
                    }
                    eventPublisher.publishEvent(new InventoryReservationOversoldEvent(
                            reservation.getId().toString(), orderId, Instant.now()));
                }
            }
        }
    }

    public void restockByOrderId(String orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(UUID.fromString(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found for order: " + orderId));
        // DB 원장이 진실의 원천 — 이미 RESTOCKED면 Redis INCRBY를 건너뛰어 이중 복원을 막는다.
        if (!reservation.restock()) {
            return;
        }
        reservationRepository.save(reservation);
        List<String> keys = new java.util.ArrayList<>();
        keys.add(reservationKey(reservation.getId().toString()));
        keys.addAll(reservation.getLines().stream()
                .map(line -> stockKey(line.getProductId().toString()))
                .toList());
        String[] args = reservation.getLines().stream()
                .map(line -> String.valueOf(line.getQuantity()))
                .toArray(String[]::new);

        Long result = redisTemplate.execute(restockScript, keys, args);
        if (result == null || result == 0L) {
            throw new IllegalStateException("Restock failed for reservation: " + reservation.getId());
        }
    }

    private void markReleased(String orderId) {
        reservationRepository.findByOrderId(UUID.fromString(orderId)).ifPresent(reservation -> {
            reservation.release();
            reservationRepository.save(reservation);
        });
    }

    private static String itemPayload(List<InventoryItem> items) {
        return items.stream()
                .map(item -> item.productId() + ":" + item.quantity())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
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
