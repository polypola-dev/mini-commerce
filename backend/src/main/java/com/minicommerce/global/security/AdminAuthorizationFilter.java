package com.minicommerce.global.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.web.filter.OncePerRequestFilter;

public class AdminAuthorizationFilter extends OncePerRequestFilter {

    private final JwkProvider jwkProvider;
    private final String bffSecretKey;

    public AdminAuthorizationFilter(String jwksUrl, String bffSecretKey) {
        try {
            this.jwkProvider = new JwkProviderBuilder(new URL(jwksUrl))
                    .cached(10, 24, TimeUnit.HOURS)
                    .rateLimited(10, 1, TimeUnit.MINUTES)
                    .build();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid JWKS URL: " + jwksUrl, e);
        }
        this.bffSecretKey = bffSecretKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String incomingBffKey = request.getHeader("X-Internal-BFF-Key");
        if (incomingBffKey == null || !incomingBffKey.equals(bffSecretKey)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Missing token");
            return;
        }

        try {
            DecodedJWT unverified = JWT.decode(authHeader.substring(7));
            Jwk jwk = jwkProvider.get(unverified.getKeyId());
            ECPublicKey publicKey = (ECPublicKey) jwk.getPublicKey();
            Algorithm algorithm = Algorithm.ECDSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT verified = verifier.verify(authHeader.substring(7));

            // Supabase app_metadata.role 또는 최상위 role 클레임 확인
            String role = null;
            var appMetadata = verified.getClaim("app_metadata");
            if (!appMetadata.isNull() && !appMetadata.isMissing()) {
                Map<String, Object> meta = appMetadata.asMap();
                if (meta != null && meta.containsKey("role")) {
                    role = (String) meta.get("role");
                }
            }
            if (role == null) {
                var roleClaim = verified.getClaim("role");
                if (!roleClaim.isNull() && !roleClaim.isMissing()) {
                    role = roleClaim.asString();
                }
            }

            if (!"admin".equals(role)) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden: Admin only");
                return;
            }

            request.setAttribute("authenticatedUserId", verified.getSubject());
        } catch (JWTVerificationException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Invalid token");
            return;
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: " + e.getMessage());
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
