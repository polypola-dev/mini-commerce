-- ============================================================================
-- Supabase 전용 보안 설정 (GH #12, B1/B2 RLS) — Flyway 범위 밖, 수동 적용
-- ============================================================================
-- 이 스크립트는 anon/authenticated/service_role 등 **Supabase 전용 롤**을 참조하므로
-- 이식성 있는 Flyway baseline(V1__baseline.sql)에 넣을 수 없다(로컬 docker Postgres에는
-- 이 롤들이 없어 마이그레이션이 깨진다). 따라서 프로덕션 Supabase에만 별도(out-of-band)로
-- 적용한다. 적용 경로: Supabase SQL Editor 또는 MCP apply_migration.
--
-- 적용 전제(2026-07-11 조사 결과, GH #12):
--   - public 12개 테이블은 전부 백엔드 전용. 백엔드는 Session Pooler로 `postgres` 롤
--     (rolbypassrls=true) 접속 → RLS 우회. 프론트엔드는 Supabase를 auth(로그인)에만 쓰고
--     Data API로 이 테이블들을 쿼리하지 않는다(.from()/.rpc() 사용처 없음).
--   - 따라서 "RLS enabled + policy 0" = anon/authenticated에 default-deny = 의도한 상태.
--     별도 정책은 추가하지 않는다(명시적 정책은 오히려 실수로 접근을 열 위험).
--   - 신규 public 테이블은 이벤트 트리거 `ensure_rls`(함수 public.rls_auto_enable)가
--     RLS를 자동 활성화한다 → 이 가드레일은 유지한다.

-- ----------------------------------------------------------------------------
-- 1) SECURITY DEFINER 함수 노출 회수 (advisor WARN 0028/0029 해소) — 2026-07-13 적용 완료
-- ----------------------------------------------------------------------------
-- public.rls_auto_enable()은 event_trigger 함수(가드레일)라 정상 호출 대상이 아니지만,
-- PostgREST API 스키마에 노출되어 anon/authenticated가 /rest/v1/rpc/rls_auto_enable로
-- EXECUTE 가능하다고 linter가 경고한다. 이벤트 트리거는 DDL 엔진이 실행하므로 EXECUTE
-- 권한을 회수해도 가드레일 동작에는 영향이 없다.
REVOKE EXECUTE ON FUNCTION public.rls_auto_enable() FROM PUBLIC, anon, authenticated;

-- ----------------------------------------------------------------------------
-- 2) RLS 상태 확인 (참고용 — 변경 없음)
-- ----------------------------------------------------------------------------
-- 모든 public 테이블이 rls_enabled=true, policy_count=0 (=default-deny)인지 점검한다.
-- SELECT c.relname, c.relrowsecurity AS rls_enabled,
--   (SELECT count(*) FROM pg_policies p WHERE p.tablename=c.relname) AS policies
-- FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
-- WHERE c.relkind='r' AND n.nspname='public' ORDER BY c.relname;

-- ----------------------------------------------------------------------------
-- 3) (별건, 대시보드 액션) Auth > Leaked Password Protection 활성화 권장
--    Supabase Dashboard > Authentication > Policies 에서 HaveIBeenPwned 검사 켜기.
--    SQL로는 설정 불가.
-- ----------------------------------------------------------------------------
