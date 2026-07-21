package com.minicommerce.inventory.adapter.in.web;

import com.minicommerce.inventory.OutOfStockException;
import com.minicommerce.inventory.ReservationConflictException;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    /**
     * 본문 검증 실패 → 400. 이전에는 {@code items}가 null이면 유즈케이스에서 NPE가 나 500으로
     * 응답했다 — 호출자 잘못이 서버 장애로 보여 RED 대시보드/알림을 오염시키고, order-infra의
     * 어댑터가 이를 "inventory 불가"로 해석해 무의미한 보상 경로를 태우는 문제가 있었다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://mini-commerce.local/problems/invalid-request"));
        problem.setTitle("Invalid request");
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
