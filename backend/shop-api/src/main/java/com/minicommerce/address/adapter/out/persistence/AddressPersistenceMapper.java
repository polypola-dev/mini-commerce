package com.minicommerce.address.adapter.out.persistence;

import com.minicommerce.address.domain.Address;

final class AddressPersistenceMapper {

    private AddressPersistenceMapper() {
    }

    static Address toDomain(AddressJpaEntity entity) {
        return new Address(
                entity.getId(),
                entity.getCustomerId(),
                entity.getRecipientName(),
                entity.getPhone(),
                entity.getAddress1(),
                entity.getAddress2(),
                entity.isDefaultAddress(),
                entity.getCreatedAt()
        );
    }

    static AddressJpaEntity toEntity(Address address) {
        return new AddressJpaEntity(
                address.getId(),
                address.getCustomerId(),
                address.getRecipientName(),
                address.getPhone(),
                address.getAddress1(),
                address.getAddress2(),
                address.isDefaultAddress(),
                address.getCreatedAt()
        );
    }
}
