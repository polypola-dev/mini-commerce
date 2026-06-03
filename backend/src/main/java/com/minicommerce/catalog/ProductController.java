package com.minicommerce.catalog;

import com.minicommerce.inventory.InventoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public ProductController(ProductRepository productRepository, InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    List<ProductResponse> listProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(product -> ProductResponse.from(product, inventoryService.availableStock(product.getId(), product.getStock())))
                .toList();
    }
}
