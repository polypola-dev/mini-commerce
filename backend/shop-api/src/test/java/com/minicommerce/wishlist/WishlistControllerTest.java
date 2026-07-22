package com.minicommerce.wishlist;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minicommerce.wishlist.adapter.in.web.WishlistController;
import com.minicommerce.wishlist.application.port.in.ManageWishlistUseCase;
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
class WishlistControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageWishlistUseCase manageWishlistUseCase;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WishlistController(manageWishlistUseCase)).build();
    }

    @Test
    @DisplayName("성공: GET /api/wishlist 호출 시 200 OK와 상품 ID 배열을 반환한다")
    void list_returns200WithProductIds() throws Exception {
        when(manageWishlistUseCase.listProductIds("cust-1")).thenReturn(List.of("prod-1", "prod-2"));

        mockMvc.perform(get("/api/wishlist")
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("prod-1"))
                .andExpect(jsonPath("$[1]").value("prod-2"));
    }

    @Test
    @DisplayName("성공: POST /api/wishlist 호출 시 204 No Content를 반환하고 add를 호출한다")
    void add_returns204() throws Exception {
        mockMvc.perform(post("/api/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"prod-1\"}")
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isNoContent());

        verify(manageWishlistUseCase).add("cust-1", "prod-1");
    }

    @Test
    @DisplayName("실패: productId가 비어 있으면 400 Bad Request를 반환한다")
    void add_blankProductId_returns400() throws Exception {
        mockMvc.perform(post("/api/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"\"}")
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("성공: DELETE /api/wishlist/{productId} 호출 시 204 No Content를 반환하고 remove를 호출한다")
    void remove_returns204() throws Exception {
        mockMvc.perform(delete("/api/wishlist/prod-1")
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isNoContent());

        verify(manageWishlistUseCase).remove("cust-1", "prod-1");
    }
}
