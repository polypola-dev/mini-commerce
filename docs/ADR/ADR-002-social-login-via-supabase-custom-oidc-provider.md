# ADR-002: 소셜 로그인 확장은 Supabase Custom OIDC Provider로 처리, 백엔드는 단일 issuer만 신뢰

- 상태: 승인 (2026-06-19)

## 컨텍스트
기존 인증 구조는 Supabase Auth(이메일/비밀번호 + Google OAuth)를 거쳐 발급된 ES256 JWT 하나를 백엔드(`JwtVerificationFilter`, `AdminAuthorizationFilter`)가 JWKS로 검증하는 단일 issuer 구조였다. 카카오/네이버 로그인을 추가하면서 백엔드가 여러 issuer(Supabase, 카카오, 네이버)의 토큰을 직접 검증해야 하는지 검토가 필요했다.

초기에 검토한 방향은 백엔드에 `TrustedIssuerRegistry` + `MultiIssuerJwtVerifier`를 도입해 issuer별로 JWKS endpoint를 분기하고, 키 타입(EC/RSA)별로 서명 알고리즘을 분기하는 멀티 issuer 검증 구조였다.

이후 조사 과정에서 Supabase가 `auth.custom_oauth_providers` 테이블 기반의 **Custom OAuth/OIDC Provider** 기능을 제공한다는 것을 확인했다 (issuer URL만 입력하면 discovery document로 jwks_uri까지 자동 설정). 또한 카카오는 Supabase가 **네이티브 provider**로 이미 지원하고 있었다.

## 결정
- **카카오**: Supabase 네이티브 provider 사용 (`signInWithOAuth({provider:'kakao'})`)
- **네이버**: Supabase Custom OIDC Provider로 등록 (issuer: `https://nid.naver.com`)
- 두 경로 모두 최종적으로 **Supabase가 발급한 동일한 ES256 JWT**로 귀결되므로, 백엔드의 `JwtVerificationFilter`/`AdminAuthorizationFilter`는 **변경하지 않고 기존 단일 Supabase issuer 검증을 그대로 유지**한다.
- Admin 권한 역시 issuer 분기 없이 기존 `app_metadata.role == "admin"` 클레임 체크만 유지한다.

## 대안
1. **백엔드 멀티 issuer 검증 구조** (`TrustedIssuerRegistry`/`MultiIssuerJwtVerifier`) — issuer마다 JWKS endpoint와 키 타입(EC/RSA)을 분기해 백엔드가 직접 다중 IdP를 신뢰. Admin 권한도 issuer 화이트리스트로 별도 제한 필요. → Supabase가 이미 이 역할(issuer 통합, discovery, JWKS 캐싱)을 대신 해주므로 중복 구현이며 폐기.
2. **네이버를 OAuth2(비표준)로 직접 연동** — 네이버가 OIDC discovery(`/.well-known/openid-configuration`)와 JWKS(`/oauth2/jwks`)를 정식 제공하는 것을 확인하여 채택하지 않음.

## 결과
- 백엔드 코드 변경 없이 소셜 로그인 provider 확장 가능 (인증 로직은 Supabase 레이어에 위임)
- 트레이드오프: 네이버의 userinfo_endpoint(`https://openapi.naver.com/v1/nid/me`)가 비표준 응답 포맷(`{"response": {...}}` 래핑)이라, Supabase가 email을 직접 못 읽는 문제가 발생 → `frontend/app/api/auth/naver-userinfo/route.ts`에 응답을 평탄화하는 프록시 어댑터를 추가하고, Supabase Naver provider를 Manual configuration으로 전환해 userinfo_url을 이 프록시로 교체하는 보완이 필요했다.
- 이 프록시 어댑터는 BFF(Next.js, Vercel 운영 도메인)에 위치 — Spring 백엔드는 `X-Internal-BFF-Key`로 외부 비공개 상태를 유지해야 하므로, 외부 IdP와 통신하는 코드는 BFF 레이어에 두는 것이 기존 경계 설계와 일치한다.
- 로컬 개발 환경에서는 이 프록시(userinfo_url)가 항상 고정된 공개 도메인을 가리켜야 하므로, 네이버 로그인 흐름은 로컬에서 완전히 격리된 테스트가 불가능하다 — "코드 수정 → 배포 → 실제 클릭 확인" 루프로 대체.
- Identity Linking은 이메일 기준 자동 매칭이므로, 카카오/네이버 둘 다 이메일을 필수 동의 항목으로 설정해야 계정 분리를 방지할 수 있다.

## 관련 세션
- [[sessions/2026-06-19]]
