package com.minicommerce.address;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerce.address.adapter.in.web.AddressController;
import com.minicommerce.address.application.port.in.ManageAddressUseCase;
import com.minicommerce.address.application.port.in.ManageAddressUseCase.NewAddress;
import com.minicommerce.address.domain.Address;
import com.minicommerce.address.domain.AddressNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageAddressUseCase manageAddressUseCase;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AddressController(manageAddressUseCase)).build();
        objectMapper = new ObjectMapper();
    }

    private Address sampleAddress(String id, boolean isDefault) {
        return new Address(id, "cust-1", "집", "홍길동", "010-1234-5678",
                "서울시 강남구", "101동 202호", isDefault, Instant.now());
    }

    @Test
    @DisplayName("성공: GET /api/addresses 호출 시 200 OK와 배송지 목록을 반환한다")
    void list_returns200WithAddresses() throws Exception {
        when(manageAddressUseCase.list("cust-1")).thenReturn(List.of(sampleAddress("addr-1", true)));

        mockMvc.perform(get("/api/addresses")
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("addr-1"))
                .andExpect(jsonPath("$[0].name").value("홍길동"))
                .andExpect(jsonPath("$[0].isDefault").value(true));
    }

    @Test
    @DisplayName("성공: POST /api/addresses 호출 시 201 Created와 생성된 배송지를 반환한다")
    void add_returns201() throws Exception {
        when(manageAddressUseCase.add(eq("cust-1"), any(NewAddress.class)))
                .thenReturn(sampleAddress("addr-1", true));

        String body = """
                {"name":"홍길동","phone":"010-1234-5678","address1":"서울시 강남구","address2":"101동 202호"}
                """;

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("addr-1"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("실패: name이 비어 있으면 400 Bad Request를 반환한다")
    void add_blankName_returns400() throws Exception {
        String body = """
                {"name":"","phone":"010-1234-5678","address1":"서울시 강남구","address2":""}
                """;

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("성공: DELETE /api/addresses/{id} 호출 시 204 No Content를 반환한다")
    void remove_returns204() throws Exception {
        doNothing().when(manageAddressUseCase).remove("cust-1", "addr-1");

        mockMvc.perform(delete("/api/addresses/addr-1")
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isNoContent());

        verify(manageAddressUseCase).remove("cust-1", "addr-1");
    }

    @Test
    @DisplayName("성공: PUT /api/addresses/{id}/default 호출 시 204 No Content를 반환한다")
    void setDefault_returns204() throws Exception {
        doNothing().when(manageAddressUseCase).setDefault("cust-1", "addr-1");

        mockMvc.perform(put("/api/addresses/addr-1/default")
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isNoContent());

        verify(manageAddressUseCase).setDefault("cust-1", "addr-1");
    }

    @Test
    @DisplayName("실패: 존재하지 않는 배송지 삭제 시 404 Not Found를 반환한다")
    void remove_notFound_returns404() throws Exception {
        doThrow(new AddressNotFoundException("addr-x"))
                .when(manageAddressUseCase).remove("cust-1", "addr-x");

        mockMvc.perform(delete("/api/addresses/addr-x")
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isNotFound());
    }
}
