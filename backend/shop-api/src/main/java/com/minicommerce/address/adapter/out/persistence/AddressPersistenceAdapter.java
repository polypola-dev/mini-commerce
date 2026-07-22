package com.minicommerce.address.adapter.out.persistence;

import com.minicommerce.address.application.port.out.AddressRepositoryPort;
import com.minicommerce.address.domain.Address;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class AddressPersistenceAdapter implements AddressRepositoryPort {

    private final AddressJpaRepository jpaRepository;

    AddressPersistenceAdapter(AddressJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Address> findByCustomerId(String customerId) {
        return jpaRepository.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(UUID.fromString(customerId)).stream()
                .map(AddressPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Address> findByIdAndCustomerId(String addressId, String customerId) {
        return jpaRepository.findByIdAndCustomerId(UUID.fromString(addressId), UUID.fromString(customerId))
                .map(AddressPersistenceMapper::toDomain);
    }

    @Override
    public Address save(Address address) {
        AddressJpaEntity saved = jpaRepository.save(AddressPersistenceMapper.toEntity(address));
        return AddressPersistenceMapper.toDomain(saved);
    }

    @Override
    public void delete(Address address) {
        jpaRepository.deleteById(UUID.fromString(address.getId()));
    }

    @Override
    public long countByCustomerId(String customerId) {
        return jpaRepository.countByCustomerId(UUID.fromString(customerId));
    }
}
