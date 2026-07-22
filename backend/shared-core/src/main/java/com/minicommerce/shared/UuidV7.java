package com.minicommerce.shared;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/**
 * RFC 9562 UUID version 7 생성기(프로젝트 공용 진입점).
 *
 * <p>JDK의 {@link UUID#randomUUID()}는 v4(완전 랜덤)만 생성한다. v7은 상위 48비트에 Unix 밀리초
 * 타임스탬프를 담아 <b>생성 시각순으로 대략 정렬</b>되므로, PK로 쓰면 B-tree 인덱스 삽입 지역성이
 * 좋아진다(랜덤 v4 대비 페이지 분할 감소).
 *
 * <p>구현은 {@code com.github.f4b6a3:uuid-creator}(순수 자바·zero-dependency·MIT)에 위임한다.
 * 버전/variant 비트 조작과 같은 밀리초 내 단조성(monotonicity) 보장을 직접 구현하면 미묘한 버그
 * 위험이 커서, RFC 9562를 1급으로 지원하는 검증된 라이브러리를 사용한다. 이 클래스는 프로젝트 내
 * 단일 진입점을 유지해, 소비 코드가 라이브러리 API에 직접 결합하지 않도록 한다.
 */
public final class UuidV7 {

    private UuidV7() {
    }

    /**
     * RFC 9562 version 7 UUID를 생성한다.
     *
     * @return 상위 48비트가 현재 Unix 밀리초 타임스탬프인 UUIDv7
     */
    public static UUID randomUUID() {
        // getTimeOrderedEpoch() = UUIDv7 (Unix Epoch time-ordered).
        return UuidCreator.getTimeOrderedEpoch();
    }
}
