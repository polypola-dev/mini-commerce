package com.minicommerce.wishlist.application.port.in;

import java.util.List;

/** 위시리스트 관리 유즈케이스 (Driving Port). */
public interface ManageWishlistUseCase {

    /** 소유자가 찜한 상품 ID 목록 (찜한 최신순). */
    List<String> listProductIds(String customerId);

    /** 찜하기. 이미 찜한 상품이면 멱등하게 무시한다. */
    void add(String customerId, String productId);

    /** 찜 해제. 찜하지 않은 상품이면 멱등하게 무시한다. */
    void remove(String customerId, String productId);
}
