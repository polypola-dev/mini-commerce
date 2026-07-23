package com.minicommerce.global.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JwtVerificationFilterTest {

    private MockMvc mockMvc;

    private static final String BFF_SECRET_KEY = "bff-private-tunnel-key";
    private static final String KEY_ID = "test-key-id";

    private String validToken;
    private String expiredToken;
    private String invalidToken;
    private String issuedSlightlyInFutureToken;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1")); // P-256
        KeyPair keyPair = generator.generateKeyPair();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

        // JwkProvider Mock - KEY_ID 요청 시 테스트용 공개키 반환
        JwkProvider jwkProvider = mock(JwkProvider.class);
        Jwk mockJwk = mock(Jwk.class);
        when(mockJwk.getPublicKey()).thenReturn(publicKey);
        when(jwkProvider.get(KEY_ID)).thenReturn(mockJwk);

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(new JwtVerificationFilter(jwkProvider, BFF_SECRET_KEY))
                .build();

        Algorithm algorithm = Algorithm.ECDSA256(publicKey, privateKey);

        validToken = JWT.create()
                .withSubject("user-123")
                .withKeyId(KEY_ID)
                .withClaim("email", "test@example.com")
                .withExpiresAt(new Date(System.currentTimeMillis() + 600_000))
                .sign(algorithm);

        // acceptLeeway(10)를 넘어서는 과거여야 leeway와 무관하게 확실히 만료로 판정된다.
        expiredToken = JWT.create()
                .withSubject("user-123")
                .withKeyId(KEY_ID)
                .withExpiresAt(new Date(System.currentTimeMillis() - 20_000))
                .sign(algorithm);

        // 검증 서버 시계가 발급 서버보다 살짝 늦은 상황 재현 — leeway(10s) 이내라 통과해야 한다.
        issuedSlightlyInFutureToken = JWT.create()
                .withSubject("user-123")
                .withKeyId(KEY_ID)
                .withIssuedAt(new Date(System.currentTimeMillis() + 5_000))
                .withExpiresAt(new Date(System.currentTimeMillis() + 600_000))
                .sign(algorithm);

        // 다른 키쌍으로 서명한 위조 토큰 (kid는 동일하지만 서명이 다름)
        KeyPair wrongKeyPair = generator.generateKeyPair();
        invalidToken = JWT.create()
                .withSubject("user-123")
                .withKeyId(KEY_ID)
                .sign(Algorithm.ECDSA256(
                        (ECPublicKey) wrongKeyPair.getPublic(),
                        (ECPrivateKey) wrongKeyPair.getPrivate()
                ));
    }

    @Test
    @DisplayName("보안 실패: X-Internal-BFF-Key 헤더가 누락되면 403 Forbidden을 반환한다")
    void shouldRejectWhenBffKeyMissing() throws Exception {
        mockMvc.perform(get("/api/test-secure")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Forbidden: Access through unauthorized gateway only."));
    }

    @Test
    @DisplayName("보안 실패: Authorization 헤더가 누락되면 401 Unauthorized를 반환한다")
    void shouldRejectWhenAuthHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/test-secure")
                        .header("X-Internal-BFF-Key", BFF_SECRET_KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized: Missing or invalid token"));
    }

    @Test
    @DisplayName("보안 실패: 위조된 서명의 JWT 토큰인 경우 401 Unauthorized를 반환한다")
    void shouldRejectWhenTokenSignatureInvalid() throws Exception {
        mockMvc.perform(get("/api/test-secure")
                        .header("X-Internal-BFF-Key", BFF_SECRET_KEY)
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized: Invalid token signature"));
    }

    @Test
    @DisplayName("보안 실패: 만료된 JWT 토큰인 경우 401 Unauthorized를 반환한다")
    void shouldRejectWhenTokenExpired() throws Exception {
        mockMvc.perform(get("/api/test-secure")
                        .header("X-Internal-BFF-Key", BFF_SECRET_KEY)
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized: Token has expired"));
    }

    @Test
    @DisplayName("보안 성공: 올바른 BFF 키와 유효한 JWT 토큰인 경우 HTTP 200과 유저 ID를 정상 반환한다")
    void shouldPassWhenEverythingIsValid() throws Exception {
        mockMvc.perform(get("/api/test-secure")
                        .header("X-Internal-BFF-Key", BFF_SECRET_KEY)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Success: user-123"));
    }

    @Test
    @DisplayName("보안 성공: 검증 서버 시계가 발급 서버보다 leeway 이내로 늦어도 통과한다(clock skew 허용)")
    void shouldPassWhenIssuedAtIsWithinLeeway() throws Exception {
        mockMvc.perform(get("/api/test-secure")
                        .header("X-Internal-BFF-Key", BFF_SECRET_KEY)
                        .header("Authorization", "Bearer " + issuedSlightlyInFutureToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Success: user-123"));
    }

    @RestController
    static class TestController {
        @GetMapping("/api/test-secure")
        public String secureEndpoint(HttpServletRequest request) {
            String userId = (String) request.getAttribute("authenticatedUserId");
            return "Success: " + userId;
        }
    }
}
