package com.minicommerce.address.application.port.out;

import com.minicommerce.address.domain.Address;
import java.util.List;
import java.util.Optional;

/** 배송지 영속성 포트 (Driven Port). */
public interface AddressRepositoryPort {

    /** 소유자의 배송지를 기본 배송지 우선, 생성 최신순으로 조회. */
    List<Address> findByCustomerId(String customerId);

    /** 소유자 스코프로 단건 조회 (타 사용자 배송지 접근 차단). */
    Optional<Address> findByIdAndCustomerId(String addressId, String customerId);

    Address save(Address address);

    void delete(Address address);

    long countByCustomerId(String customerId);
}
