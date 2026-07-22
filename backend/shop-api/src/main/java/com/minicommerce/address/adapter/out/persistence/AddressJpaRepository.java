package com.minicommerce.address.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AddressJpaRepository extends JpaRepository<AddressJpaEntity, UUID> {

    List<AddressJpaEntity> findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(UUID customerId);

    Optional<AddressJpaEntity> findByIdAndCustomerId(UUID id, UUID customerId);

    long countByCustomerId(UUID customerId);
}
