package com.minicommerce.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 다른 컨텍스트(order 등)가 REST로 조회하는 내부 전용 API. 스토어프론트용
 * {@link ProductController}와 달리 {@link ProductReader} 공개 API 형태를 그대로 노출한다.
 *
 * <p>order-api가 별도 프로세스로 분리된 지금(ADR-005 S3-3b) 이 엔드포인트가 실제 네트워크 경계다.
 */
@RestController
@RequestMapping("/internal/products")
class ProductInternalController {

    private final ProductReader productReader;

    ProductInternalController(ProductReader productReader) {
        this.productReader = productReader;
    }

    @GetMapping("/{id}")
    ProductReader.ProductInfo getProduct(@PathVariable String id) {
        return productReader.findProduct(id);
    }

    @GetMapping("/options/{id}")
    ProductReader.OptionInfo getOption(@PathVariable String id) {
        return productReader.findOption(id);
    }
}
