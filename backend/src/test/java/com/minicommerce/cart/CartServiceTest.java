package com.minicommerce.cart;

import com.minicommerce.catalog.ProductOption;
import com.minicommerce.catalog.ProductOptionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, productOptionRepository);
    }

    @Test
    @DisplayName("성공: 장바구니가 없으면 빈 Cart를 생성하여 반환한다")
    void getCart_returnsEmptyCartWhenNotExists() {
        String customerId = "cust-1";
        Cart newCart = new Cart(customerId);
        when(cartRepository.findById(customerId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        Cart result = cartService.getCart(customerId);

        assertThat(result.getId()).isEqualTo(customerId);
        assertThat(result.getItems()).isEmpty();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("성공: 기존 장바구니가 있으면 그대로 반환한다")
    void getCart_returnsExistingCart() {
        String customerId = "cust-1";
        Cart existingCart = new Cart(customerId);
        when(cartRepository.findById(customerId)).thenReturn(Optional.of(existingCart));

        Cart result = cartService.getCart(customerId);

        assertThat(result).isSameAs(existingCart);
    }

    @Test
    @DisplayName("성공: 아이템을 장바구니에 추가한다")
    void addItem_addsItemToCart() {
        String customerId = "cust-1";
        Cart cart = new Cart(customerId);
        AddCartItemRequest request = new AddCartItemRequest("prod-1", "상품1", BigDecimal.valueOf(10000), 2, null);
        when(cartRepository.findById(customerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartItem item = cartService.addItem(customerId, request);

        assertThat(item.getProductId()).isEqualTo("prod-1");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("성공: 옵션이 지정되면 추가금액이 단가에 더해지고 옵션값이 저장된다")
    void addItem_withOption_addsAdditionalPrice() {
        String customerId = "cust-1";
        Cart cart = new Cart(customerId);
        AddCartItemRequest request = new AddCartItemRequest("prod-1", "상품1", BigDecimal.valueOf(10000), 1, "option-1");
        ProductOption option = new ProductOption("option-1", "prod-1", "색상", "화이트", BigDecimal.valueOf(5000));
        when(cartRepository.findById(customerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(productOptionRepository.findById("option-1")).thenReturn(Optional.of(option));

        CartItem item = cartService.addItem(customerId, request);

        assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(item.getSelectedOptionId()).isEqualTo("option-1");
        assertThat(item.getSelectedOptionValue()).isEqualTo("화이트");
        assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(15000));
    }

    @Test
    @DisplayName("성공: 아이템 수량을 업데이트한다")
    void updateItem_updatesQuantity() {
        String customerId = "cust-1";
        Cart cart = new Cart(customerId);
        CartItem item = new CartItem("item-1", cart, "prod-1", "상품1", BigDecimal.valueOf(10000), 1);
        cart.addItem(item);
        when(cartRepository.findById(customerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        Cart updated = cartService.updateItem(customerId, "item-1", new UpdateCartItemRequest(3));

        assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("성공: 아이템을 장바구니에서 삭제한다")
    void removeItem_removesItem() {
        String customerId = "cust-1";
        Cart cart = new Cart(customerId);
        CartItem item = new CartItem("item-1", cart, "prod-1", "상품1", BigDecimal.valueOf(10000), 1);
        cart.addItem(item);
        when(cartRepository.findById(customerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.removeItem(customerId, "item-1");

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("성공: 장바구니를 전체 비운다")
    void clearCart_clearsAllItems() {
        String customerId = "cust-1";
        Cart cart = new Cart(customerId);
        cart.addItem(new CartItem("item-1", cart, "prod-1", "상품1", BigDecimal.valueOf(10000), 1));
        cart.addItem(new CartItem("item-2", cart, "prod-2", "상품2", BigDecimal.valueOf(5000), 2));
        when(cartRepository.findById(customerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.clearCart(customerId);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("실패: 존재하지 않는 Cart의 아이템 수량 업데이트 시 EntityNotFoundException을 던진다")
    void updateItem_cartNotFound_throwsException() {
        when(cartRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItem("ghost", "item-1", new UpdateCartItemRequest(2)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cart not found");
    }

    @Test
    @DisplayName("실패: 장바구니에 아이템이 200개 이상이면 추가 시 CartFullException을 던진다")
    void addItem_cartAtMaxCapacity_throwsCartFullException() {
        String customerId = "cust-1";
        Cart cart = new Cart(customerId);
        for (int i = 0; i < 200; i++) {
            cart.addItem(new CartItem("item-" + i, cart, "prod-" + i, "상품" + i, BigDecimal.valueOf(1000), 1));
        }
        when(cartRepository.findById(customerId)).thenReturn(Optional.of(cart));
        AddCartItemRequest request = new AddCartItemRequest("prod-new", "신상품", BigDecimal.valueOf(10000), 1, null);

        assertThatThrownBy(() -> cartService.addItem(customerId, request))
                .isInstanceOf(CartFullException.class);
    }

    @Test
    @DisplayName("성공: 90일이 지난 아이템은 조회 시 자동으로 삭제된다")
    void getCart_removesItemsOlderThan90Days() throws Exception {
        String customerId = "cust-1";
        Cart cart = new Cart(customerId);
        CartItem freshItem = new CartItem("item-fresh", cart, "prod-1", "상품1", BigDecimal.valueOf(10000), 1);
        CartItem expiredItem = new CartItem("item-expired", cart, "prod-2", "상품2", BigDecimal.valueOf(5000), 1);
        setAddedAt(expiredItem, Instant.now().minus(91, ChronoUnit.DAYS));
        cart.addItem(freshItem);
        cart.addItem(expiredItem);
        when(cartRepository.findById(customerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        Cart result = cartService.getCart(customerId);

        assertThat(result.getItems()).extracting(CartItem::getId).containsExactly("item-fresh");
        verify(cartRepository).save(cart);
    }

    private static void setAddedAt(CartItem item, Instant addedAt) throws Exception {
        Field field = CartItem.class.getDeclaredField("addedAt");
        field.setAccessible(true);
        field.set(item, addedAt);
    }
}
