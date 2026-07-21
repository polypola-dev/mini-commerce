package com.minicommerce.global.security;

import com.minicommerce.global.InternalApiContract;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 서비스간 {@code /internal/**} 호출의 앱 레이어 인증(B3, ADR-020). 네트워크 레이어의
 * NetworkPolicy default-deny(ADR-014)와 2중 방어를 이루며, 그쪽이 미지원 CNI에서 조용히 무시되는
 * 단일 방어선 위험을 해소한다.
 *
 * <p>호출자가 셋뿐인 신뢰 도메인 내부라 "누가 호출했나"(호출자별 신원)가 아니라 "내부 호출인가"만
 * 판정한다 — 공유 시크릿 방식의 한계와 그 수용 근거는 ADR-020 참고.
 */
public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = InternalApiContract.INTERNAL_KEY_HEADER;

    /**
     * 허용 키의 SHA-256 다이제스트. 원문 대신 고정 길이 해시를 비교해 키 길이가 다를 때도
     * 비교 시간이 변하지 않게 한다(MessageDigest.isEqual은 길이가 다르면 조기 반환).
     */
    private final List<byte[]> acceptedDigests;

    /**
     * @param acceptedKeysCsv 콤마 구분 허용 키 목록. 무중단 로테이션을 위해 복수를 허용한다 —
     *                        신키 추가 배포 → 호출자 전환 → 구키 제거 순으로 3단 배포하면
     *                        어느 시점에도 거부되는 조합이 없다.
     */
    public InternalAuthFilter(String acceptedKeysCsv) {
        this.acceptedDigests = Arrays.stream(acceptedKeysCsv.split(","))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .map(InternalAuthFilter::sha256)
                .toList();

        // 설정 누락으로 전 호출이 404가 되면 원인 추적이 어렵다 — 기동 시점에 실패시킨다.
        if (acceptedDigests.isEmpty()) {
            throw new IllegalArgumentException(
                    "INTERNAL_API_KEY must contain at least one non-blank key");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String presented = request.getHeader(HEADER);
        if (presented == null || !isAccepted(presented)) {
            // 401/403이 아니라 404 — 엔드포인트의 존재 자체를 드러내지 않는다. 외부에서 본
            // /internal은 "라우팅 없음"이어야 하고(ADR-012 Ingress 미노출), ADR-014의 부정 테스트도
            // 이미 404를 차단 판정 기준으로 쓰고 있어 관측 계약이 일관된다.
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAccepted(String presented) {
        byte[] presentedDigest = sha256(presented);
        boolean matched = false;
        // 첫 일치에서 빠져나가지 않는다 — 허용 키가 목록 앞/뒤 어디에 있든 비교 시간이 같다.
        for (byte[] accepted : acceptedDigests) {
            matched |= MessageDigest.isEqual(accepted, presentedDigest);
        }
        return matched;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available", e);
        }
    }
}
