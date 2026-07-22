package com.minicommerce.address.application.port.in;

import com.minicommerce.address.domain.Address;
import java.util.List;

/** 배송지 주소록 관리 유즈케이스 (Driving Port). */
public interface ManageAddressUseCase {

    /** 소유자의 배송지 목록. 기본 배송지가 먼저, 그다음 생성 최신순. */
    List<Address> list(String customerId);

    /** 배송지 추가. 소유자의 첫 배송지면 자동으로 기본 배송지가 된다. */
    Address add(String customerId, NewAddress command);

    /** 배송지 삭제. 기본 배송지를 지우면 남은 것 중 최신 항목이 기본이 된다. */
    void remove(String customerId, String addressId);

    /** 지정한 배송지를 기본으로 설정하고 나머지의 기본 표시를 해제한다. */
    void setDefault(String customerId, String addressId);

    /** 배송지 신규 등록 입력값. */
    record NewAddress(String recipientName, String phone, String address1, String address2) {
    }
}
