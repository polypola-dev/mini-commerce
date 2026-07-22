package com.minicommerce.wishlist.adapter.in.web;

import com.minicommerce.wishlist.application.port.in.ManageWishlistUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final ManageWishlistUseCase manageWishlistUseCase;

    public WishlistController(ManageWishlistUseCase manageWishlistUseCase) {
        this.manageWishlistUseCase = manageWishlistUseCase;
    }

    /** 찜한 상품 ID 목록 (프론트 useWishlist의 ids와 대응). */
    @GetMapping
    List<String> list(HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        return manageWishlistUseCase.listProductIds(customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void add(@Valid @RequestBody WishlistRequest request, HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        manageWishlistUseCase.add(customerId, request.productId());
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable String productId, HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        manageWishlistUseCase.remove(customerId, productId);
    }

    record WishlistRequest(@NotBlank String productId) {
    }
}
