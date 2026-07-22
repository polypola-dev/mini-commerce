package com.minicommerce.address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minicommerce.address.application.AddressService;
import com.minicommerce.address.application.port.in.ManageAddressUseCase.NewAddress;
import com.minicommerce.address.application.port.out.AddressRepositoryPort;
import com.minicommerce.address.domain.Address;
import com.minicommerce.address.domain.AddressNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepositoryPort repository;

    private AddressService service;

    @BeforeEach
    void setUp() {
        service = new AddressService(repository);
    }

    private Address address(String id, boolean isDefault) {
        return new Address(id, "cust-1", "집", "수령인", "010", "주소1", "주소2", "12345", isDefault, Instant.now());
    }

    @Test
    @DisplayName("첫 배송지는 자동으로 기본 배송지가 된다")
    void firstAddressBecomesDefault() {
        when(repository.countByCustomerId("cust-1")).thenReturn(0L);
        when(repository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        NewAddress cmd = new NewAddress("집", "수령인", "010", "주소1", "주소2", "12345");
        Address saved = service.add("cust-1", cmd);

        assertThat(saved.isDefaultAddress()).isTrue();
    }

    @Test
    @DisplayName("두 번째 배송지는 기본 배송지가 아니다")
    void secondAddressIsNotDefault() {
        when(repository.countByCustomerId("cust-1")).thenReturn(1L);
        when(repository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        Address saved = service.add("cust-1", new NewAddress("집", "수령인", "010", "주소1", "주소2", "12345"));

        assertThat(saved.isDefaultAddress()).isFalse();
    }

    @Test
    @DisplayName("setDefault: 기존 기본을 해제하고 대상만 기본으로 설정한다")
    void setDefault_swapsDefaultFlag() {
        Address current = address("addr-1", true);
        Address target = address("addr-2", false);
        when(repository.findByIdAndCustomerId("addr-2", "cust-1")).thenReturn(Optional.of(target));
        when(repository.findByCustomerId("cust-1")).thenReturn(List.of(current, target));

        service.setDefault("cust-1", "addr-2");

        assertThat(current.isDefaultAddress()).isFalse();
        assertThat(target.isDefaultAddress()).isTrue();
    }

    @Test
    @DisplayName("기본 배송지를 삭제하면 남은 최신 항목이 기본으로 승격된다")
    void remove_defaultPromotesNext() {
        Address target = address("addr-1", true);
        Address remaining = address("addr-2", false);
        when(repository.findByIdAndCustomerId("addr-1", "cust-1")).thenReturn(Optional.of(target));
        when(repository.findByCustomerId("cust-1")).thenReturn(List.of(remaining));

        service.remove("cust-1", "addr-1");

        verify(repository).delete(target);
        assertThat(remaining.isDefaultAddress()).isTrue();
    }

    @Test
    @DisplayName("비-기본 배송지를 삭제하면 승격 로직이 동작하지 않는다")
    void remove_nonDefaultDoesNotPromote() {
        Address target = address("addr-2", false);
        when(repository.findByIdAndCustomerId("addr-2", "cust-1")).thenReturn(Optional.of(target));

        service.remove("cust-1", "addr-2");

        verify(repository).delete(target);
        verify(repository, never()).findByCustomerId(any());
    }

    @Test
    @DisplayName("update: 소유자·기본배송지 여부는 유지하고 내용만 갱신한다")
    void update_keepsOwnerAndDefault() {
        Address target = address("addr-1", true);
        when(repository.findByIdAndCustomerId("addr-1", "cust-1")).thenReturn(Optional.of(target));
        when(repository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        Address result = service.update("cust-1", "addr-1",
                new NewAddress("회사", "김철수", "010-9999-8888", "판교로 1", "5층", "13494"));

        assertThat(result.getLabel()).isEqualTo("회사");
        assertThat(result.getRecipientName()).isEqualTo("김철수");
        assertThat(result.getZipCode()).isEqualTo("13494");
        assertThat(result.isDefaultAddress()).isTrue();
        assertThat(result.getCustomerId()).isEqualTo("cust-1");
    }

    @Test
    @DisplayName("존재하지 않는 배송지 수정 시 AddressNotFoundException")
    void update_notFound_throws() {
        when(repository.findByIdAndCustomerId("addr-x", "cust-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("cust-1", "addr-x",
                new NewAddress("집", "수령인", "010-1234-5678", "주소1", "주소2", "12345")))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 배송지 삭제 시 AddressNotFoundException")
    void remove_notFound_throws() {
        when(repository.findByIdAndCustomerId("addr-x", "cust-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove("cust-1", "addr-x"))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    @DisplayName("이미 기본인 배송지에 setDefault 하면 아무 저장도 하지 않는다")
    void setDefault_alreadyDefault_noop() {
        Address target = address("addr-1", true);
        when(repository.findByIdAndCustomerId("addr-1", "cust-1")).thenReturn(Optional.of(target));

        service.setDefault("cust-1", "addr-1");

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(repository, never()).save(captor.capture());
    }
}
