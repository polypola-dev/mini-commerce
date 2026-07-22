package com.minicommerce.address.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AddressJpaRepository extends JpaRepository<AddressJpaEntity, String> {

    List<AddressJpaEntity> findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(String customerId);

    Optional<AddressJpaEntity> findByIdAndCustomerId(String id, String customerId);

    long countByCustomerId(String customerId);
}
