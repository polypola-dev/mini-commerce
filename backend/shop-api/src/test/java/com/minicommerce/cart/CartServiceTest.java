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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    // id들이 uuid 컬럼/필드로 전환됐으므로(GH #20) 테스트도 유효 UUID 문자열을 쓴다.
    private static final String CUSTOMER_ID = "00000000-0000-7000-8000-0000000000c1";
    private static final UUID CUSTOMER_UUID = UUID.fromString(CUSTOMER_ID);
    private static final String PROD_1 = "00000000-0000-7000-8000-0000000000a1";
    private static final UUID PROD_1_UUID = UUID.fromString(PROD_1);
    private static final String PROD_2 = "00000000-0000-7000-8000-0000000000a2";
    private static final UUID PROD_2_UUID = UUID.fromString(PROD_2);
    private static final String OPTION_1 = "00000000-0000-7000-8000-0000000000b1";
    private static final UUID OPTION_1_UUID = UUID.fromString(OPTION_1);
    private static final String OPTION_BLACK = "00000000-0000-7000-8000-0000000000b2";
    private static final UUID OPTION_BLACK_UUID = UUID.fromString(OPTION_BLACK);
    private static final String OPTION_WHITE = "00000000-0000-7000-8000-0000000000b3";
    private static final UUID OPTION_WHITE_UUID = UUID.fromString(OPTION_WHITE);
    private static final UUID ITEM_1_UUID = UUID.fromString("00000000-0000-7000-8000-0000000000d1");
    private static final String ITEM_1_ID = ITEM_1_UUID.toString();
    private static final UUID ITEM_FRESH_UUID = UUID.fromString("00000000-0000-7000-8000-0000000000d2");
    private static final UUID ITEM_EXPIRED_UUID = UUID.fromString("00000000-0000-7000-8000-0000000000d3");

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
        Cart newCart = new Cart(CUSTOMER_UUID);
        when(cartRepository.findById(CUSTOMER_UUID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        Cart result = cartService.getCart(CUSTOMER_ID);

        assertThat(result.getId()).isEqualTo(CUSTOMER_UUID);
        assertThat(result.getItems()).isEmpty();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("성공: 기존 장바구니가 있으면 그대로 반환한다")
    void getCart_returnsExistingCart() {
        Cart existingCart = new Cart(CUSTOMER_UUID);
        when(cartRepository.findById(CUSTOMER_UUID)).thenReturn(Optional.of(existingCart));

        Cart result = cartService.getCart(CUSTOMER_ID);

        assertThat(result).isSameAs(existingCart);
    }

    @Test
    @DisplayName("성공: 아이템을 장바구니에 추가한다")
    void addItem_addsItemToCart() {
        Cart cart = new Cart(CUSTOMER_UUID);
        AddCartItemRequest request = new AddCartItemRequest(PROD_1, "상품1", BigDecimal.valueOf(10000), 2, null);
        when(cartRepository.findById(CUSTOMER_UUID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartItem item = cartService.addItem(CUSTOMER_ID, request);

        assertThat(item.getProductId()).isEqualTo(PROD_1_UUID);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("성공: 옵션이 지정되면 추가금액이 단가에 더해지고 옵션값이 저장된다")
    void addItem_withOption_addsAdditionalPrice() {
        Cart cart = new Cart(CUSTOMER_UUID);
        AddCartItemRequest request = new AddCartItemRequest(PROD_1, "상품1", BigDecimal.valueOf(10000), 1, OPTION_1);
        ProductOption option = new ProductOption(OPTION_1_UUID, PROD_1_UUID, "색상", "화이트", BigDecimal.valueOf(5000));
        when(cartRepository.findById(CUSTOMER_UUID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(productOptionRepository.findById(OPTION_1_UUID)).thenReturn(Optional.of(option));

        CartItem item = cartService.addItem(CUSTOMER_ID, request);

        assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(item.getSelectedOptionId()).isEqualTo(OPTION_1_UUID);
        assertThat(item.getSelectedOptionValue()).isEqualTo("화이트");
        assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(15000));
    }

    @Test
    @DisplayName("성공: 동일 상품+동일 옵션을 다시 담으면 새 행 대신 기존 항목의 수량이 합산된다")
    void addItem_sameProductAndOption_mergesIntoExistingItem() {
        Cart cart = new Cart(CUSTOMER_UUID);
        AddCartItemRequest firstRequest = new AddCartItemRequest(PROD_1, "상품1", BigDecimal.valueOf(10000), 1, OPTION_1);
        AddCartItemRequest secondRequest = new AddCartItemRequest(PROD_1, "상품1", BigDecimal.valueOf(10000), 2, OPTION_1);
        ProductOption option = new ProductOption(OPTION_1_UUID, PROD_1_UUID, "색상", "화이트", BigDecimal.valueOf(5000));
        when(cartRepository.findById(CUSTOMER_UUID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(productOptionRepository.findById(OPTION_1_UUID)).thenReturn(Optional.of(option));

        CartItem first = cartService.addItem(CUSTOMER_ID, firstRequest);
        CartItem second = cartService.addItem(CUSTOMER_ID, secondRequest);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("성공: 같은 상품이라도 옵션이 다르면 별개의 행으로 추가된다")
    void addItem_sameProductDifferentOption_addsSeparateItem() {
        Cart cart = new Cart(CUSTOMER_UUID);
        AddCartItemRequest blackRequest = new AddCartItemRequest(PROD_1, "상품1", BigDecimal.valueOf(10000), 1, OPTION_BLACK);
        AddCartItemRequest whiteRequest = new AddCartItemRequest(PROD_1, "상품1", BigDecimal.valueOf(10000), 1, OPTION_WHITE);
        ProductOption black = new ProductOption(OPTION_BLACK_UUID, PROD_1_UUID, "색상", "블랙", BigDecimal.ZERO);
        ProductOption white = new ProductOption(OPTION_WHITE_UUID, PROD_1_UUID, "색상", "화이트", BigDecimal.ZERO);
        when(cartRepository.findById(CUSTOMER_UUID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(productOptionRepository.findById(OPTION_BLACK_UUID)).thenReturn(Optional.of(black));
        when(productOptionRepository.findById(OPTION_WHITE_UUID)).thenReturn(Optional.of(white));

        cartService.addItem(CUSTOMER_ID, blackRequest);
        cartService.addItem(CUSTOMER_ID, whiteRequest);

        assertThat(cart.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("성공: 아이템 수량을 업데이트한다")
    void updateItem_updatesQuantity() {
        Cart cart = new Cart(CUSTOMER_UUID);
        CartItem item = new CartItem(ITEM_1_UUID, cart, PROD_1_UUID, "상품1", BigDecimal.valueOf(10000), 1);
        cart.addItem(item);
        when(cartRepository.findById(CUSTOMER_UUID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        Cart updated = cartService.updateItem(CUSTOMER_ID, ITEM_1_ID, new UpdateCartItemRequest(3));

        assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("성공: 아이템을 장바구니에서 삭제한다")
    void removeItem_removesItem() {
        Cart cart = new Cart(CUSTOMER_UUID);
        CartItem item = new CartItem(ITEM_1_UUID, cart, PROD_1_UUID, "상품1", BigDecimal.valueOf(10000), 1);
        cart.addItem(item);
        when(cartRepository.findById(CUSTOMER_UUID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.removeItem(CUSTOMER_ID, ITEM_1_ID);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("성공: 장바구니를 전체 비운다")
    void clearCart_clearsAllItems() {
        Cart cart = new Cart(CUSTOMER_UUID);
        cart.addItem(new CartItem(ITEM_1_UUID, cart, PROD_1_UUID, "상품1", BigDecimal.valueOf(10000), 1));
        cart.addItem(new CartItem(UUID.randomUUID(), cart, PROD_2_UUID, "상품2", BigDecimal.valueOf(5000), 2));
        when(cartRepository.findById(CUSTOMER_UUID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.clearCart(CUSTOMER_ID);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("실패: 존재하지 않는 Cart의 아이템 수량 업데이트 시 EntityNotFoundException을 던진다")
    void updateItem_cartNotFound_throwsException() {
        String ghost = "00000000-0000-7000-8000-00000000ffff";
        when(cartRepository.findById(UUID.fromString(ghost))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItem(ghost, ITEM_1_ID, new UpdateCartItemRequest(2)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cart not found");
    }

    @Test
    @DisplayName("실패: 장바구니에 아이템이 200개 이상이면 추가 시 CartFullException을 던진다")
    void addItem_cartAtMaxCapacity_throwsCartFullException() {
        Cart cart = new Cart(CUSTOMER_UUID);
        for (int i = 0; i < 200; i++) {
            cart.addItem(new CartItem(new UUID(0, i), cart, new UUID(1, i), "상품" + i, BigDecimal.valueOf(1000), 1));
        }
        when(cartRepository.findById(CUSTOMER_UUID)).thenReturn(Optional.of(cart));
        AddCartItemRequest request = new AddCartItemRequest(PROD_1, "신상품", BigDecimal.valueOf(10000), 1, null);

        assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, request))
                .isInstanceOf(CartFullException.class);
    }

    @Test
    @DisplayName("성공: 90일이 지난 아이템은 조회 시 자동으로 삭제된다")
    void getCart_removesItemsOlderThan90Days() throws Exception {
        Cart cart = new Cart(CUSTOMER_UUID);
        CartItem freshItem = new CartItem(ITEM_FRESH_UUID, cart, PROD_1_UUID, "상품1", BigDecimal.valueOf(10000), 1);
        CartItem expiredItem = new CartItem(ITEM_EXPIRED_UUID, cart, PROD_2_UUID, "상품2", BigDecimal.valueOf(5000), 1);
        setAddedAt(expiredItem, Instant.now().minus(91, ChronoUnit.DAYS));
        cart.addItem(freshItem);
        cart.addItem(expiredItem);
        when(cartRepository.findById(CUSTOMER_UUID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        Cart result = cartService.getCart(CUSTOMER_ID);

        assertThat(result.getItems()).extracting(CartItem::getId).containsExactly(ITEM_FRESH_UUID);
        verify(cartRepository).save(cart);
    }

    private static void setAddedAt(CartItem item, Instant addedAt) throws Exception {
        Field field = CartItem.class.getDeclaredField("addedAt");
        field.setAccessible(true);
        field.set(item, addedAt);
    }
}
