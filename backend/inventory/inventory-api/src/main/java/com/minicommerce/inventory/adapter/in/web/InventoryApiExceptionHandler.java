package com.minicommerce.inventory.adapter.in.web;

import com.minicommerce.inventory.OutOfStockException;
import com.minicommerce.inventory.ReservationConflictException;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 예약 사가 REST 경계의 에러 계약(GH #3 S3). ProblemDetail의 type URI가 계약이다 —
 * order-infra InventoryRestAdapter가 type으로 도메인 예외를 복원하므로 임의 변경 금지.
 * (out-of-stock 형식은 과거 order-api에 있던 어드바이스에서 그대로 승계.)
 */
@RestControllerAdvice
class InventoryApiExceptionHandler {

    @ExceptionHandler(OutOfStockException.class)
    ProblemDetail handleOutOfStock(OutOfStockException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://mini-commerce.local/problems/out-of-stock"));
        problem.setTitle("Out of stock");
        return problem;
    }

    @ExceptionHandler(ReservationConflictException.class)
    ProblemDetail handleReservationConflict(ReservationConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://mini-commerce.local/problems/reservation-conflict"));
        problem.setTitle("Reservation conflict");
        return problem;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail handleNotFound(EntityNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://mini-commerce.local/problems/reservation-not-found"));
        problem.setTitle("Reservation not found");
        return problem;
    }
}
