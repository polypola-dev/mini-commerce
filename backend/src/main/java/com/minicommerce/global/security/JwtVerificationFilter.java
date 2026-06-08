package com.minicommerce.global.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.util.concurrent.TimeUnit;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtVerificationFilter extends OncePerRequestFilter {

    private final JwkProvider jwkProvider;
    private final String bffSecretKey;

    public JwtVerificationFilter(String jwksUrl, String bffSecretKey) {
        this(buildProvider(jwksUrl), bffSecretKey);
    }

    private static JwkProvider buildProvider(String jwksUrl) {
        try {
            return new JwkProviderBuilder(new URL(jwksUrl))
                    .cached(10, 24, TimeUnit.HOURS)
                    .rateLimited(10, 1, TimeUnit.MINUTES)
                    .build();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid JWKS URL: " + jwksUrl, e);
        }
    }

    // 테스트용 생성자 (JwkProvider 직접 주입)
    public JwtVerificationFilter(JwkProvider jwkProvider, String bffSecretKey) {
        this.jwkProvider = jwkProvider;
        this.bffSecretKey = bffSecretKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. BFF 공유 키 검증 (다이렉트 침투 방어)
        String incomingBffKey = request.getHeader("X-Internal-BFF-Key");
        if (incomingBffKey == null || !incomingBffKey.equals(bffSecretKey)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN,
                    "Forbidden: Access through unauthorized gateway only.");
            return;
        }

        // 2. Authorization JWT 헤더 파싱
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Missing or invalid token");
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 3. kid로 Supabase JWKS에서 EC 공개키 조회
            DecodedJWT unverified = JWT.decode(token);
            Jwk jwk = jwkProvider.get(unverified.getKeyId());
            ECPublicKey publicKey = (ECPublicKey) jwk.getPublicKey();

            // 4. ES256으로 서명 및 만료 검증
            Algorithm algorithm = Algorithm.ECDSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT verified = verifier.verify(token);

            // 5. 검증 성공 - userId를 Request Attribute에 보관
            request.setAttribute("authenticatedUserId", verified.getSubject());

        } catch (TokenExpiredException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Token has expired");
            return;
        } catch (JWTVerificationException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Invalid token signature");
            return;
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}
