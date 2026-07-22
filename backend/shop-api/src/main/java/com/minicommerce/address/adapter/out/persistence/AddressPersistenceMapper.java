package com.minicommerce.address.adapter.out.persistence;

import com.minicommerce.address.domain.Address;
import java.util.UUID;

final class AddressPersistenceMapper {

    private AddressPersistenceMapper() {
    }

    static Address toDomain(AddressJpaEntity entity) {
        return new Address(
                entity.getId().toString(),
                entity.getCustomerId().toString(),
                entity.getLabel(),
                entity.getRecipientName(),
                entity.getPhone(),
                entity.getAddress1(),
                entity.getAddress2(),
                entity.getZipCode(),
                entity.isDefaultAddress(),
                entity.getCreatedAt()
        );
    }

    static AddressJpaEntity toEntity(Address address) {
        return new AddressJpaEntity(
                UUID.fromString(address.getId()),
                UUID.fromString(address.getCustomerId()),
                address.getLabel(),
                address.getRecipientName(),
                address.getPhone(),
                address.getAddress1(),
                address.getAddress2(),
                address.getZipCode(),
                address.isDefaultAddress(),
                address.getCreatedAt()
        );
    }
}
