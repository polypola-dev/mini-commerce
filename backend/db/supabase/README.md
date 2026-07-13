# Supabase 전용 보안 설정 (RLS / 롤 / Auth)

프로덕션 DB는 **단일 Supabase Postgres**의 `public` 스키마에 전 테이블(로컬 docker에서는
`minicommerce`/`orderdb`로 분리)이 모여 있다. RLS·GRANT 등 **Supabase 전용 롤
(`anon`/`authenticated`/`service_role`)에 의존하는 보안 설정**은 이식성 있는 Flyway
baseline에 넣을 수 없어(로컬 docker에는 이 롤이 없다), 여기서 별도(out-of-band)로 관리한다.

- 스키마(테이블) 소유: **Flyway** (`*/src/main/resources/db/migration/`, GH #11)
- Supabase 보안 설정: **이 디렉토리** (수동/`apply_migration` 적용, GH #12)
- DB 자체 생성(`CREATE DATABASE`): 인프라 초기화(`docker/postgres-init/`, 추후 k8s)

## 현재 보안 태세 (2026-07-11, GH #12 조사)

| 대상 | 상태 | 판단 |
|---|---|---|
| public 12개 테이블 | RLS enabled, policy 0 | anon/authenticated **default-deny**. 백엔드 전용 테이블에 원하는 상태. |
| 백엔드 접근 | Session Pooler, `postgres` 롤(BYPASSRLS) | RLS 우회 — RLS 활성화가 백엔드 기능에 영향 없음 |
| 프론트 접근 | Supabase는 **auth 전용**, Data API 테이블 쿼리 없음 | RLS 노출 경로 자체가 없음 |
| `ensure_rls` 이벤트 트리거 | 신규 public 테이블에 RLS 자동 활성화 | **가드레일 유지** |
| advisor **critical** | **0건** | 이슈의 RLS-disabled critical은 이전에 해소됨 |

## 정책 설계 결정: deny-all 유지 (명시적 정책 미추가)

12개 테이블은 전부 백엔드 전용이고 Data API로 노출되지 않으므로, "RLS enabled + 정책 미생성"
= anon/authenticated 완전 차단 = 이슈가 요구한 "백엔드 전용 deny-all + service role만"과
정확히 일치한다. **명시적 정책은 추가하지 않는다** — 중복이며, permissive 정책을 잘못 넣으면
오히려 접근을 여는 위험이 있다. `rls_enabled_no_policy`(INFO) lint는 이 의도된 상태를 알리는
정보성 알림으로 방치한다.

향후 프론트엔드가 특정 테이블을 Data API로 직접 읽어야 하는 요구가 생기면, 그때 해당 테이블에
한해 세분화 정책(예: `products` authenticated read)을 설계한다.

## 적용 방법

`rls_hardening.sql`은 **미적용 상태**다(2026-07-11, 사용자 보류 결정). 적용 시:

1. Supabase Dashboard > SQL Editor에 `rls_hardening.sql` 붙여넣기 실행, 또는 MCP
   `apply_migration`으로 적용.
2. 적용 후 security advisor 재실행 → `anon/authenticated_security_definer_function_executable`
   (WARN) 2건이 사라지는지 확인.
3. (별건) Dashboard > Authentication에서 **Leaked Password Protection** 토글 켜기(WARN 해소, SQL 불가).

## 잔여 advisor 요약 (적용 전 기준)

- `rls_enabled_no_policy` (INFO) × 12 — 의도된 deny-all, 방치.
- `anon/authenticated_security_definer_function_executable` (WARN) × 2 — `rls_hardening.sql`로 해소.
- `auth_leaked_password_protection` (WARN) × 1 — 대시보드 토글(사용자 액션).
- **critical: 0건** (완료 조건 이미 충족).
