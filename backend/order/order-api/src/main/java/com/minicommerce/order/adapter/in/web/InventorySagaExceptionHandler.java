package com.minicommerce.order.adapter.in.web;

import com.minicommerce.order.domain.exception.InventoryUnavailableException;
import com.minicommerce.order.domain.exception.OutOfStockException;
import com.minicommerce.order.domain.exception.ReservationNotActiveException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 재고 사가 도메인 예외의 웹 매핑(GH #3 S3). out-of-stock의 ProblemDetail 형식(409,
 * type=.../out-of-stock)은 과거 in-process 시절 어드바이스의 응답 계약을 그대로 보존한다 —
 * 프론트가 이 type으로 품절을 분기하므로 변경 금지. BusinessException(ErrorCode) 체계에
 * 편입하지 않는 이유도 이 기존 계약 보존이다(OutOfStockException 주석 참고).
 */
@RestControllerAdvice
class InventorySagaExceptionHandler {

    @ExceptionHandler(OutOfStockException.class)
    ProblemDetail handleOutOfStock(OutOfStockException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://mini-commerce.local/problems/out-of-stock"));
        problem.setTitle("Out of stock");
        return problem;
    }

    @ExceptionHandler(ReservationNotActiveException.class)
    ProblemDetail handleReservationNotActive(ReservationNotActiveException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://mini-commerce.local/problems/reservation-not-active"));
        problem.setTitle("Reservation not active");
        return problem;
    }

    @ExceptionHandler(InventoryUnavailableException.class)
    ProblemDetail handleInventoryUnavailable(InventoryUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setType(URI.create("https://mini-commerce.local/problems/inventory-unavailable"));
        problem.setTitle("Inventory service unavailable");
        return problem;
    }
}
