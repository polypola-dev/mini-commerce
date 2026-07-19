-- GH #3 전략(c): inventory-api 전용 DB(inventorydb). 경계/멱등/볼륨 재사용 주의사항은
-- 01-create-orderdb.sql 주석 참고 — 기존 postgres-data 볼륨을 재사용 중이라면 이 스크립트가
-- 실행되지 않으므로 `docker compose down -v` 후 재기동하거나 수동으로 CREATE DATABASE 해야 한다.
SELECT 'CREATE DATABASE inventorydb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'inventorydb')\gexec
