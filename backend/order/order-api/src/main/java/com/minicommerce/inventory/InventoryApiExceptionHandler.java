package com.minicommerce.inventory;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * OutOfStockException은 BusinessException 체계에 편입되지 않아(shared-web/ApiExceptionHandler
 * 참조, doc/architecture/shared.md) shared-web이 inventory 컨텍스트에 의존하지 않도록
 * shop-api에 별도 어드바이스로 둔다.
 */
@RestControllerAdvice
public class InventoryApiExceptionHandler {
    @ExceptionHandler(OutOfStockException.class)
    ProblemDetail handleOutOfStock(OutOfStockException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://mini-commerce.local/problems/out-of-stock"));
        problem.setTitle("Out of stock");
        return problem;
    }
}
