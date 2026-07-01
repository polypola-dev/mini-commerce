package com.minicommerce.cart;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    CartResponse getCart(HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        return CartResponse.from(cartService.getCart(customerId));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    CartItemResponse addItem(
            @Valid @RequestBody AddCartItemRequest request,
            HttpServletRequest httpRequest
    ) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        return CartItemResponse.from(cartService.addItem(customerId, request));
    }

    @PutMapping("/items/{itemId}")
    CartResponse updateItem(
            @PathVariable String itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            HttpServletRequest httpRequest
    ) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        return CartResponse.from(cartService.updateItem(customerId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeItem(@PathVariable String itemId, HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        cartService.removeItem(customerId, itemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clearCart(HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        cartService.clearCart(customerId);
    }

    @ExceptionHandler(CartFullException.class)
    ProblemDetail handleCartFull(CartFullException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://mini-commerce.local/problems/cart-full"));
        problem.setTitle("Cart full");
        return problem;
    }
}
