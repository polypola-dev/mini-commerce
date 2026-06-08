package com.minicommerce.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("성공: GET /api/cart 호출 시 200 OK와 CartResponse를 반환한다")
    void getCart_returns200WithCartResponse() throws Exception {
        String customerId = "cust-1";
        Cart cart = new Cart(customerId);
        when(cartService.getCart(customerId)).thenReturn(cart);

        mockMvc.perform(get("/api/cart")
                        .requestAttr("authenticatedUserId", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    @DisplayName("성공: POST /api/cart/items 호출 시 201 Created와 CartItemResponse를 반환한다")
    void addItem_returns201WithCartItemResponse() throws Exception {
        String customerId = "cust-1";
        AddCartItemRequest request = new AddCartItemRequest("prod-1", "상품1", BigDecimal.valueOf(10000), 2, null);
        Cart cart = new Cart(customerId);
        CartItem item = new CartItem("item-1", cart, "prod-1", "상품1", BigDecimal.valueOf(10000), 2);
        when(cartService.addItem(eq(customerId), any(AddCartItemRequest.class))).thenReturn(item);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("authenticatedUserId", customerId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemId").value("item-1"))
                .andExpect(jsonPath("$.productId").value("prod-1"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.subtotal").value(20000));
    }

    @Test
    @DisplayName("성공: PUT /api/cart/items/{itemId} 호출 시 200 OK와 CartResponse를 반환한다")
    void updateItem_returns200WithCartResponse() throws Exception {
        String customerId = "cust-1";
        Cart cart = new Cart(customerId);
        when(cartService.updateItem(eq(customerId), eq("item-1"), any(UpdateCartItemRequest.class))).thenReturn(cart);

        mockMvc.perform(put("/api/cart/items/item-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCartItemRequest(3)))
                        .requestAttr("authenticatedUserId", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId));
    }

    @Test
    @DisplayName("성공: DELETE /api/cart/items/{itemId} 호출 시 204 No Content를 반환한다")
    void deleteItem_returns204() throws Exception {
        doNothing().when(cartService).removeItem("cust-1", "item-1");

        mockMvc.perform(delete("/api/cart/items/item-1")
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("성공: DELETE /api/cart 호출 시 204 No Content를 반환한다")
    void clearCart_returns204() throws Exception {
        doNothing().when(cartService).clearCart("cust-1");

        mockMvc.perform(delete("/api/cart")
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("실패: quantity가 0인 addItem 요청 시 400 Bad Request를 반환한다")
    void addItem_invalidQuantity_returns400() throws Exception {
        String body = """
                {"productId":"prod-1","productName":"상품1","unitPrice":10000,"quantity":0}
                """;

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isBadRequest());
    }
}
