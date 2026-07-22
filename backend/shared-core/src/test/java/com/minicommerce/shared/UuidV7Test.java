package com.minicommerce.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void generates_version_7() {
        UUID uuid = UuidV7.randomUUID();
        assertEquals(7, uuid.version(), "UUIDv7의 버전 니블은 7이어야 한다");
    }

    @Test
    void generates_rfc4122_variant() {
        UUID uuid = UuidV7.randomUUID();
        // java.util.UUID#variant() == 2 → RFC 4122 (Leach-Salz), variant 비트 10xx
        assertEquals(2, uuid.variant(), "variant는 RFC 4122(=2)여야 한다");
    }

    @Test
    void timestamp_prefix_is_non_decreasing_over_time() {
        // 연속 생성한 UUID들의 상위 48비트(밀리초 타임스탬프)가 시간순으로 비내림차순인지 확인.
        // 같은 밀리초 내 생성분은 앞 48비트가 동일(비내림차순 만족).
        int count = 2000;
        List<UUID> generated = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            generated.add(UuidV7.randomUUID());
        }

        long previousTimestamp = -1L;
        for (UUID uuid : generated) {
            long timestamp = uuid.getMostSignificantBits() >>> 16; // 상위 48비트
            assertTrue(timestamp >= previousTimestamp,
                    "타임스탬프 프리픽스는 비내림차순이어야 한다: " + timestamp + " < " + previousTimestamp);
            previousTimestamp = timestamp;
        }
    }

    @Test
    void distinct_across_many_generations() {
        int count = 10_000;
        List<UUID> generated = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            generated.add(UuidV7.randomUUID());
        }
        long unique = generated.stream().distinct().count();
        assertEquals(count, unique, "생성된 UUID는 모두 고유해야 한다");
    }
}
