package com.minicommerce.cart;

import com.minicommerce.catalog.ProductOption;
import com.minicommerce.catalog.ProductOptionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final ProductOptionRepository productOptionRepository;

    public CartService(CartRepository cartRepository, ProductOptionRepository productOptionRepository) {
        this.cartRepository = cartRepository;
        this.productOptionRepository = productOptionRepository;
    }

    public Cart getCart(String customerId) {
        return cartRepository.findById(customerId)
                .orElseGet(() -> cartRepository.save(new Cart(customerId)));
    }

    public CartItem addItem(String customerId, AddCartItemRequest request) {
        Cart cart = getCart(customerId);

        String rawOptionId = request.selectedOptionId();
        boolean hasOption = rawOptionId != null && !rawOptionId.isBlank();

        BigDecimal unitPrice = request.unitPrice();
        String resolvedOptionId = null;
        String selectedOptionValue = null;
        if (hasOption) {
            ProductOption option = productOptionRepository.findById(rawOptionId)
                    .orElseThrow(() -> new EntityNotFoundException("Product option not found: " + rawOptionId));
            unitPrice = unitPrice.add(option.getAdditionalPrice());
            selectedOptionValue = option.getOptionValue();
            resolvedOptionId = rawOptionId;
        }

        CartItem item = new CartItem(
                UUID.randomUUID().toString(),
                cart,
                request.productId(),
                request.productName(),
                unitPrice,
                request.quantity(),
                resolvedOptionId,
                selectedOptionValue
        );
        cart.addItem(item);
        cartRepository.save(cart);
        return item;
    }

    public Cart updateItem(String customerId, String itemId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found: " + customerId));
        cart.updateItemQuantity(itemId, request.quantity());
        return cartRepository.save(cart);
    }

    public void removeItem(String customerId, String itemId) {
        Cart cart = cartRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found: " + customerId));
        cart.removeItem(itemId);
        cartRepository.save(cart);
    }

    public void clearCart(String customerId) {
        Cart cart = cartRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found: " + customerId));
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
