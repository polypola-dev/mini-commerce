package com.minicommerce.address.application;

import com.minicommerce.address.application.port.in.ManageAddressUseCase;
import com.minicommerce.address.application.port.out.AddressRepositoryPort;
import com.minicommerce.address.domain.Address;
import com.minicommerce.address.domain.AddressNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddressService implements ManageAddressUseCase {

    private final AddressRepositoryPort addressRepository;

    public AddressService(AddressRepositoryPort addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> list(String customerId) {
        return addressRepository.findByCustomerId(customerId);
    }

    @Override
    public Address add(String customerId, NewAddress command) {
        boolean first = addressRepository.countByCustomerId(customerId) == 0;
        Address address = new Address(
                UUID.randomUUID().toString(),
                customerId,
                command.label(),
                command.recipientName(),
                command.phone(),
                command.address1(),
                command.address2(),
                first,
                Instant.now()
        );
        return addressRepository.save(address);
    }

    @Override
    public void remove(String customerId, String addressId) {
        Address target = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
        addressRepository.delete(target);

        if (target.isDefaultAddress()) {
            promoteNextDefault(customerId);
        }
    }

    @Override
    public void setDefault(String customerId, String addressId) {
        Address target = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
        if (target.isDefaultAddress()) {
            return;
        }
        for (Address address : addressRepository.findByCustomerId(customerId)) {
            if (address.isDefaultAddress()) {
                address.unmarkDefault();
                addressRepository.save(address);
            }
        }
        target.markDefault();
        addressRepository.save(target);
    }

    /** 기본 배송지가 사라졌을 때 남은 것 중 최신 항목을 기본으로 승격. */
    private void promoteNextDefault(String customerId) {
        List<Address> remaining = addressRepository.findByCustomerId(customerId);
        if (remaining.isEmpty()) {
            return;
        }
        Address next = remaining.get(0);
        next.markDefault();
        addressRepository.save(next);
    }
}
