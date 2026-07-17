package com.minicommerce.order.adapter.out.payment;

import com.minicommerce.order.application.port.out.PaymentGatewayPort;
import com.minicommerce.order.domain.exception.PaymentConfirmFailedException;
import com.minicommerce.order.domain.exception.PaymentRefundFailedException;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class TossPaymentAdapter implements PaymentGatewayPort {

    private static final String ALREADY_CANCELED_PAYMENT = "ALREADY_CANCELED_PAYMENT";

    private record ConfirmRequest(String paymentKey, String orderId, long amount) {
    }

    private record ConfirmResponse(String paymentKey, String method, String approvedAt) {
    }

    private record CancelRequest(String cancelReason) {
    }

    private record TossErrorResponse(String code, String message) {
    }

    private final RestClient tossRestClient;

    public TossPaymentAdapter(RestClient tossRestClient) {
        this.tossRestClient = tossRestClient;
    }

    @Override
    public Confirmation confirm(String paymentKey, String orderId, BigDecimal amount) {
        // Toss는 amount를 정수(KRW)로 받는다. 소수부가 있으면 승인 불가 요청이므로 500이 아니라 결제 실패로 처리.
        long amountValue;
        try {
            amountValue = amount.longValueExact();
        } catch (ArithmeticException e) {
            throw new PaymentConfirmFailedException("결제 금액이 정수(KRW)가 아닙니다: " + amount.toPlainString());
        }
        try {
            ConfirmResponse response = tossRestClient.post()
                    .uri("/v1/payments/confirm")
                    .body(new ConfirmRequest(paymentKey, orderId, amountValue))
                    .retrieve()
                    .body(ConfirmResponse.class);
            return new Confirmation(
                    response.paymentKey(),
                    response.method(),
                    response.approvedAt() != null ? Instant.parse(response.approvedAt()) : Instant.now());
        } catch (RestClientResponseException e) {
            throw new PaymentConfirmFailedException(extractMessage(e));
        }
    }

    @Override
    public Cancellation cancel(String paymentKey, String cancelReason) {
        try {
            tossRestClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .body(new CancelRequest(cancelReason))
                    .retrieve()
                    .toBodilessEntity();
            return new Cancellation(Instant.now());
        } catch (RestClientResponseException e) {
            // 이미 취소된 결제는 취소 성공과 동일하게 취급 — 재시도 수렴(로컬 재입고/상태 전이만 재실행).
            if (isAlreadyCanceled(e)) {
                return new Cancellation(Instant.now());
            }
            throw new PaymentRefundFailedException(extractMessage(e));
        }
    }

    private boolean isAlreadyCanceled(RestClientResponseException e) {
        try {
            TossErrorResponse error = e.getResponseBodyAs(TossErrorResponse.class);
            return error != null && ALREADY_CANCELED_PAYMENT.equals(error.code());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String extractMessage(RestClientResponseException e) {
        try {
            TossErrorResponse error = e.getResponseBodyAs(TossErrorResponse.class);
            if (error != null && error.message() != null) {
                return error.message();
            }
        } catch (RuntimeException ignored) {
            // 에러 바디 파싱 실패 시 상태 코드 기반 기본 메시지로 폴백
        }
        return "결제 승인 실패 (" + e.getStatusCode() + ")";
    }
}
