package com.minicommerce.inventory.application.port.in;

import java.util.List;
import java.util.Map;

/**
 * 가용재고 배치 조회(N+1 방지). 호출자(catalog)는 자기 DB의 seed 기본값을 전달하지 않으므로
 * Redis 미존재 시 fallback은 0으로 고정한다 — 실제 초기값은 catalog가 생성 시점에 setStock으로 넣는다.
 */
public interface GetStocksUseCase {

    Map<String, Long> availableStocks(List<String> productIds);
}
