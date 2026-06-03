package com.minicommerce.catalog;

import com.minicommerce.inventory.InventoryService;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedData implements ApplicationRunner {
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public SeedData(ProductRepository productRepository, InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedProduct(new Product(
                "sku-keyboard",
                "Low Profile Keyboard",
                "조용한 타건감의 업무용 키보드",
                BigDecimal.valueOf(129000),
                100,
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=900&q=80"
        ));
        seedProduct(new Product(
                "sku-headphones",
                "Studio Headphones",
                "장시간 착용 가능한 모니터링 헤드폰",
                BigDecimal.valueOf(89000),
                80,
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80"
        ));
        seedProduct(new Product(
                "sku-lamp",
                "Desk Lamp",
                "밝기 조절이 쉬운 알루미늄 데스크 램프",
                BigDecimal.valueOf(64000),
                50,
                "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80"
        ));
    }

    private void seedProduct(Product product) {
        productRepository.findById(product.getId()).orElseGet(() -> productRepository.save(product));
        inventoryService.initializeStockIfAbsent(product.getId(), product.getStock());
    }
}
