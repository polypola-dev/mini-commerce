package com.minicommerce.order.adapter.in.web;

import com.minicommerce.order.domain.exception.CatalogUnavailableException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * catalog 원격 호출 장애의 웹 매핑(D6). {@code InventorySagaExceptionHandler}가 inventory 사가
 * 전용 계약(409 out-of-stock 등)을 소유하듯, catalog 쪽 계약은 여기서 따로 소유한다.
 *
 * <p>서킷이 열리면 어댑터가 {@link CatalogUnavailableException}을 던지는데, 매핑이 없으면
 * 500으로 새어 나간다. 500은 "이쪽 버그"라는 뜻이라 재시도해도 소용없다는 신호를 주지만
 * 실제로는 잠시 후 회복되는 의존 서비스 장애이므로, inventory와 동일하게 503으로 내린다.
 */
@RestControllerAdvice
class CatalogExceptionHandler {

    @ExceptionHandler(CatalogUnavailableException.class)
    ProblemDetail handleCatalogUnavailable(CatalogUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setType(URI.create("https://mini-commerce.local/problems/catalog-unavailable"));
        problem.setTitle("Catalog service unavailable");
        return problem;
    }
}
