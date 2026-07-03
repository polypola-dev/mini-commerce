package com.minicommerce.catalog;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedData implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SeedData.class);
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final InventoryClient inventoryClient;

    public SeedData(ProductRepository productRepository, ProductOptionRepository productOptionRepository, InventoryClient inventoryClient) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
        this.inventoryClient = inventoryClient;
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

        seedOption(new ProductOption("option-keyboard-black", "sku-keyboard", "색상", "블랙", BigDecimal.ZERO));
        seedOption(new ProductOption("option-keyboard-white", "sku-keyboard", "색상", "화이트", BigDecimal.valueOf(10000)));
        seedOption(new ProductOption("option-headphones-std", "sku-headphones", "모델", "Standard", BigDecimal.ZERO));
        seedOption(new ProductOption("option-headphones-pro", "sku-headphones", "모델", "Pro", BigDecimal.valueOf(30000)));
        seedOption(new ProductOption("option-lamp-warm", "sku-lamp", "색온도", "웜", BigDecimal.ZERO));
        seedOption(new ProductOption("option-lamp-cool", "sku-lamp", "색온도", "쿨", BigDecimal.valueOf(5000)));
    }

    private void seedProduct(Product product) {
        productRepository.findById(product.getId()).orElseGet(() -> productRepository.save(product));
        // order-api(재고)가 REST 경계(ADR-005 S3-3b 옵션 A) 너머에 있어, 부팅 순서 경합이나 일시적
        // 장애로 응답 못 하는 경우가 있을 수 있다. 시드는 dev 편의 기능이라 이 호출 실패로 shop-api
        // 전체 부팅이 죽는 건 과한 결합 — 로그만 남기고 계속 진행한다.
        try {
            inventoryClient.initializeStockIfAbsent(product.getId(), product.getStock());
        } catch (RuntimeException e) {
            log.warn("재고 시드 초기화 실패(productId={}), order-api 준비 여부를 확인하세요: {}",
                    product.getId(), e.getMessage());
        }
    }

    private void seedOption(ProductOption option) {
        productOptionRepository.findById(option.getId()).orElseGet(() -> productOptionRepository.save(option));
    }
}
