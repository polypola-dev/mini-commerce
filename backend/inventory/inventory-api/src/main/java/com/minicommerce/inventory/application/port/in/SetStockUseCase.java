package com.minicommerce.inventory.application.port.in;

public interface SetStockUseCase {

    /** 재고를 설정하고 갱신된 가용재고를 반환한다. */
    long setStock(String productId, long stock);
}
