-- MSA S3-3b: order-api 전용 DB(orderdb). 같은 Postgres 인스턴스에 새 DB만 만든다 — 기존
-- minicommerce DB(POSTGRES_DB로 자동 생성)의 order 관련 데이터는 이관하지 않는다(개발 단계 결정,
-- 2026-07-03). docker-entrypoint-initdb.d는 볼륨이 "처음" 초기화될 때만 실행된다 — 기존
-- postgres-data 볼륨을 재사용 중이라면 이 스크립트가 실행되지 않으므로, orderdb가 필요하다면
-- 볼륨을 리셋(docker compose down -v)한 뒤 재기동해야 한다. \gexec로 idempotent하게 만들어두는
-- 이유는 "새 볼륨" 경로에서 스크립트가 여러 번(재시도 등) 실행되더라도 안전하게 하기 위함.
SELECT 'CREATE DATABASE orderdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orderdb')\gexec
