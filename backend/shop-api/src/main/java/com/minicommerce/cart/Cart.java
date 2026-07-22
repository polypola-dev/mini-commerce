package com.minicommerce.cart;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class Cart {
    @Id
    private UUID id; // customerId

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    @OrderBy("addedAt DESC")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public Cart(UUID customerId) {
        this.id = customerId;
    }

    public UUID getId() {
        return id;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void addItem(CartItem item) {
        items.add(item);
    }

    public Optional<CartItem> findItem(UUID productId, UUID selectedOptionId) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId)
                        && Objects.equals(item.getSelectedOptionId(), selectedOptionId))
                .findFirst();
    }

    public void removeItem(UUID itemId) {
        items.removeIf(item -> item.getId().equals(itemId));
    }

    public void updateItemQuantity(UUID itemId, int quantity) {
        items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
